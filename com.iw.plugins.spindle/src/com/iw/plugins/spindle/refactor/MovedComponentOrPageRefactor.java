/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Spindle, an Eclipse Plugin for Tapestry.
 *
 * The Initial Developer of the Original Code is
 * Intelligent Works Incorporated.
 * Portions created by the Initial Developer are Copyright (C) 2002
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * 
 *  glongman@intelligentworks.com
 *
 * ***** END LICENSE BLOCK ***** */
package com.iw.plugins.spindle.refactor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.dialogs.ListSelectionDialog;

import com.iw.plugins.spindle.TapestryPlugin;
import com.iw.plugins.spindle.model.TapestryLibraryModel;
import com.iw.plugins.spindle.project.TapestryProject;
import com.iw.plugins.spindle.spec.IPluginLibrarySpecification;
import com.iw.plugins.spindle.ui.TapestryStorageLabelProvider;
import com.iw.plugins.spindle.util.Utils;

/**
 * @author gwl
 * @version $Id$
 *
 * Copyright 2002, Intelligent Works Inc.
 * All Rights Reserved.
 */
public class MovedComponentOrPageRefactor implements IResourceChangeListener, IResourceDeltaVisitor {

  private TapestryProject project;
  private HashMap potentialModelMoves;
  private HashMap templatesToBeMoved;
  private LibraryRefactorer refactorer = null;
  private IResourceDelta topLevelDelta = null;
  private boolean hasAliases = false;

  public MovedComponentOrPageRefactor(TapestryProject project) {
    super();
    this.project = project;
  }

  /**
   * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(IResourceChangeEvent)
   */
  public void resourceChanged(IResourceChangeEvent event) {

    if (event.getType() != IResourceChangeEvent.PRE_AUTO_BUILD) {

      return;

    }

    IProject thisProject = project.getProject();
    topLevelDelta = event.getDelta();

    if (topLevelDelta != null && topLevelDelta.getKind() == IResourceDelta.CHANGED) {

      IResourceDelta projectDelta = topLevelDelta.findMember(thisProject.getFullPath());

      if (projectDelta != null) {

        IJavaProject jproject = null;
        potentialModelMoves = new HashMap();
        templatesToBeMoved = new HashMap();
        hasAliases = false;
        refactorer = null;

        try {

          jproject = TapestryPlugin.getDefault().getJavaProjectFor(thisProject);

        } catch (CoreException e) {

        }

        if (jproject == null) {

          return;

        }

        for (Iterator iter = getSourceRootPaths(jproject).iterator(); iter.hasNext();) {
          IPath element = (IPath) iter.next();
          IResourceDelta packageRootDelta = topLevelDelta.findMember(element);

          if (packageRootDelta != null) {

            try {

              packageRootDelta.accept(this);

            } catch (CoreException e) {
            }

          }

        }

        if (potentialModelMoves.isEmpty()) {

          return;

        }

        TapestryLibraryModel baseModel = null;

       try {
           baseModel = (TapestryLibraryModel) project.getProjectModel();
            if (baseModel == null) {
          
              return;
          
            }
            
            getRefactorer(baseModel);
        } catch (CoreException e) {
        }
        
        if (refactorer == null) {
        	
        	return;
        	
        }
        

        try {

          HashMap confirmed = findConfirmed();

          if (!confirmed.isEmpty()) {

            if (hasAliases) {

              // we must update the project App/Lib

              updateProjectModel(confirmed);

            }

            // now we move the templates (if there are any!)

            moveTemplates(confirmed);

          }
        } catch (CoreException e) {

          ErrorDialog.openError(
            TapestryPlugin.getDefault().getActiveWorkbenchShell(),
            "Spindle Refactor Error",
            "can't continue",
            e.getStatus());
        }

      }

    }

  }

  private void moveTemplates(HashMap confirmed) {

    for (Iterator iter = templatesToBeMoved.keySet().iterator(); iter.hasNext();) {
      IFile oldFile = (IFile) iter.next();
      IFile newFile = (IFile) confirmed.get(oldFile);

      IPath newPath = newFile.getFullPath();
      int segmentCount = newPath.segmentCount();

      IPath newTemplatePath = null;

      if (segmentCount > 1) {

        newTemplatePath = newPath.removeLastSegments(1);
        List templates = (List) templatesToBeMoved.get(oldFile);

        for (Iterator iterator = templates.iterator(); iterator.hasNext();) {
          IFile oldTemplate = (IFile) iterator.next();

          IPath completeNewPath = newTemplatePath.append(oldTemplate.getName());

          try {

            oldTemplate.move(completeNewPath, true, true, null);

          } catch (CoreException e) {
            e.printStackTrace();
          }
        }

      }

    }
  }

  private void updateProjectModel(HashMap confirmed) throws CoreException {


    IPluginLibrarySpecification refactorSpec = refactorer.getSpecification();

    for (Iterator iter = confirmed.keySet().iterator(); iter.hasNext();) {

      IFile element = (IFile) iter.next();
      String extension = element.getFileExtension();

      String tapestryPath = getTapestryPath(element);

      String alias = findAlias(tapestryPath, extension);

      if (alias != null) {

        IFile newFile = (IFile) confirmed.get(element);
        String newTapestryPath = getTapestryPath(newFile);

        if ("jwc".equals(extension)) {

          refactorSpec.setComponentSpecificationPath(alias, newTapestryPath);

        } else {

          refactorSpec.setPageSpecificationPath(alias, newTapestryPath);

        }

      }

    }

    refactorer.commit(new NullProgressMonitor());
  }

  private void getRefactorer(TapestryLibraryModel baseModel) throws CoreException {
    if (refactorer == null) {

      refactorer = new LibraryRefactorer(project, baseModel, true);

    }
  }

  private HashMap getUserConfirmed(HashMap confirmed) {

    if (confirmed.isEmpty()) {

      return confirmed;

    }

    final HashMap useConfirmed = confirmed;

    HashMap userConfirmed = new HashMap();

    IStructuredContentProvider content = new IStructuredContentProvider() {
      public Object[] getElements(Object inputElement) {
        return useConfirmed.entrySet().toArray();
      }
      public void dispose() {
      }
      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }
    };

    ILabelProvider labels = new TapestryStorageLabelProvider() {

      public Image getImage(Object element) {
        Map.Entry entry = (Map.Entry) element;
        return super.getImage((IStorage) entry.getKey());
      }

      public String getText(Object element) {
        Map.Entry entry = (Map.Entry) element;
        IResource resource = (IResource) entry.getKey();
        String extension = resource.getFileExtension();
        String tapestryPath = resource.getFullPath().toString();
        try {

          tapestryPath = getTapestryPath(resource);

        } catch (CoreException e) {
        }

        if ("jwc".equals(extension)) {

          return "<component-alias type=\"" + (String) entry.getValue() + "\" specification-path=\"" + tapestryPath + "\"/>";

        } else {

          return "<page name=\"" + (String) entry.getValue() + "\" specification-path=\"" + tapestryPath + "\"/>";
        }
      }
    };

    ListSelectionDialog dialog =
      new ListSelectionDialog(
        TapestryPlugin.getDefault().getActiveWorkbenchShell(),
        confirmed,
        content,
        labels,
        "Selected will be removed from " + refactorer.getEditableModel().getUnderlyingStorage().getName());

    if (dialog.open() == dialog.OK) {

      Object[] results = dialog.getResult();
      for (int i = 0; i < results.length; i++) {

        Map.Entry resultEntry = (Map.Entry) results[i];
        userConfirmed.put(resultEntry.getKey(), resultEntry.getValue());

      }

    }

    return userConfirmed;

  }

  // we have a Hashmap containing all the .jwc's and .page files moved
  // in this event.
  //
  // To remain in the list of potential moves:
  //
  // 1. The component/page must be defined in the project's app/lib
  // 
  // OR
  //
  // 2. The component/page must have templates
  //

  // plus we filter out straight renames. ie. a move to the same folder.
  // another refactor will handle those!

  private HashMap findConfirmed() {
    HashMap result = new HashMap();

    for (Iterator iter = potentialModelMoves.keySet().iterator(); iter.hasNext();) {

      IFile potential = (IFile) iter.next();

      IFile target = (IFile) potentialModelMoves.get(potential);

      if (potential.getParent().equals(target.getParent())) {

        // its not a move, but rather a renaming - discard
        continue;

      }

      try {

        String tapestryPath = getTapestryPath(potential);

        String extension = potential.getFileExtension();

        String alias = findAlias(tapestryPath, extension);

        if (alias != null || templatesToBeMoved.containsKey(potential)) {

          result.put(potential, potentialModelMoves.get(potential));

        }

      } catch (CoreException e) {
      }

    }

    return result;
  }

  private String findAlias(String tapestryPath, String extension) throws CoreException {
    String alias = null;

    TapestryLibraryModel libModel = refactorer.getEditableModel();

    if ("jwc".equals(extension)) {

      alias = libModel.findComponentAlias(tapestryPath);

    } else {

      alias = libModel.findPageName(tapestryPath);
    }

    if (alias != null && !hasAliases) {

      hasAliases = true;

    }
    return alias;
  }

  private String getTapestryPath(IResource potential) throws CoreException {

    IPackageFragment fragment = project.getLookup().findPackageFragment((IStorage) potential);

    if ("".equals(fragment.getElementName())) {

      return "/" + potential.getName();

    }

    String tapestryPath = "/" + fragment.getElementName().replace('.', '/') + "/" + potential.getName();
    return tapestryPath;
  }

  private List getSourceRootPaths(IJavaProject jproject) {

    ArrayList result = new ArrayList();

    try {

      IPackageFragmentRoot[] roots = jproject.getPackageFragmentRoots();

      for (int i = 0; i < roots.length; i++) {

        if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE) {

          result.add(roots[i].getPath());

        }

      }
    } catch (JavaModelException e) {
    }

    return result;

  }

  /**
   * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(IResourceDelta)
   */
  public boolean visit(IResourceDelta delta) throws CoreException {

    try {

      IFile movedFrom = (IFile) delta.getResource();
      String oldExtension = movedFrom.getFullPath().getFileExtension();

      if (oldExtension != null && ("jwc".equals(oldExtension) || "page".equals(oldExtension))) {

        if (delta.getKind() == IResourceDelta.REMOVED && (delta.getFlags() & IResourceDelta.MOVED_TO) != 0) {

          IPath movedToPath = delta.getMovedToPath();

          //ensure the new name has the same extension!

          String newExtension = movedToPath.getFileExtension();

          if (oldExtension.equals(newExtension)) {

            IResourceDelta movedToDelta = topLevelDelta.findMember(movedToPath);
            IResource movedTo = movedToDelta.getResource();

            // ensure the new location is in the same project!
            if (movedTo.getProject().equals(project.getProject())) {

              try {

                //now ensure the new location is still within the classpath!
                IPackageFragment fragment = project.getLookup().findPackageFragment((IStorage) movedTo);
                if (fragment != null) {

                  potentialModelMoves.put(movedFrom, movedTo);
                  findTemplatesFor(movedFrom);

                }
              } catch (CoreException e) {
              }

            }
          }

        }

      }

    } catch (ClassCastException e) {
    }

    return true;
  }

  private void findTemplatesFor(IResource movedFrom) {

    List templates = Utils.findTemplatesFor((IFile) movedFrom);

    for (Iterator iter = templates.iterator(); iter.hasNext();) {
      IResource element = (IResource) iter.next();

      // ensure the template is not already being moved during this event

      if (topLevelDelta.findMember(element.getFullPath()) != null) {

        templates.remove(element);

      }

    }
    if (!templates.isEmpty()) {

      templatesToBeMoved.put(movedFrom, templates);

    }

  }
}
