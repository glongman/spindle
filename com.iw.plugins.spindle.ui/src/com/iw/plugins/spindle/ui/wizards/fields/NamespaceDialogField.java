/*******************************************************************************
 * ***** BEGIN LICENSE BLOCK Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.mozilla.org/MPL/
 * 
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * 
 * The Original Code is Spindle, an Eclipse Plugin for Tapestry.
 * 
 * The Initial Developer of the Original Code is Geoffrey Longman.
 * Portions created by the Initial Developer are Copyright (C) 2001-2005 the Initial
 * Developer. All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * glongman@gmail.com
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.iw.plugins.spindle.ui.wizards.fields;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tapestry.INamespace;
import org.apache.tapestry.IResourceLocation;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;

import com.iw.plugins.spindle.UIPlugin;
import com.iw.plugins.spindle.core.ITapestryProject;
import com.iw.plugins.spindle.core.builder.TapestryArtifactManager;
import com.iw.plugins.spindle.core.namespace.ICoreNamespace;
import com.iw.plugins.spindle.core.resources.IResourceWorkspaceLocation;
import com.iw.plugins.spindle.core.util.Assert;
import com.iw.plugins.spindle.core.util.CoreUtils;
import com.iw.plugins.spindle.core.util.SpindleStatus;
import com.iw.plugins.spindle.ui.dialogfields.DialogField;
import com.iw.plugins.spindle.ui.dialogfields.UneditableComboBoxDialogField;

public class NamespaceDialogField extends UneditableComboBoxDialogField
{

  private String fName;

  private ComponentNameField fComponentNameField;

  private boolean fIsComponentWizard;

  private TapestryProjectDialogField fProjectField;

  private List fValidNamespaces = new ArrayList();

  private INamespace fSelectedNamespace;

  private ITapestryProject fTapestryProject;

  public NamespaceDialogField(String name, int labelWidth)
  {
    super(UIPlugin.getString(name + ".label"), labelWidth, new String[]{});
    this.fName = name;
  }

  public NamespaceDialogField(String name)
  {
    this(name, -1);
  }

  public void init(
      TapestryProjectDialogField projectField,
      ComponentNameField componentNameField,
      IJavaElement initElement,
      IResource initResource,
      boolean isComponentWizard)
  {

    fProjectField = projectField;
    fProjectField.addListener(this);
    fComponentNameField = componentNameField;
    fComponentNameField.addListener(this);
    this.fIsComponentWizard = isComponentWizard;
    populateNamespaces();
    addListener(this);
    if (initResource != null)
    {
      initSelection(initResource);
    } else
    {
      initSelectionJava(initElement);
    }

  }

  private void initSelectionJava(IJavaElement initElement)
  {
    int firstClasspathNamespace = -1;
    int count = 0;
    for (Iterator iter = fValidNamespaces.iterator(); iter.hasNext();)
    {
      INamespace namespace = (INamespace) iter.next();

      if (firstClasspathNamespace == -1 && ((IResourceWorkspaceLocation) namespace
          .getSpecificationLocation()).isOnClasspath())
        firstClasspathNamespace = count;

    }
    if (firstClasspathNamespace >= 0)
    {
      select(firstClasspathNamespace);
    }

  }

  /**
   * @param initResource
   */
  private void initSelection(IResource initResource)
  {
    
    if (initResource == null)
      return;
    
    int found = -1;
    boolean isOnClasspath = JavaCore.create(initResource.getParent()) != null;
    int firstClasspathNamespace = -1;
    int count = 0;
    for (Iterator iter = fValidNamespaces.iterator(); iter.hasNext();)
    {
      INamespace namespace = (INamespace) iter.next();

      IResourceWorkspaceLocation location = (IResourceWorkspaceLocation) namespace
          .getSpecificationLocation();

      if (firstClasspathNamespace == -1 && location.isOnClasspath())
        firstClasspathNamespace = count;

      if (location.getStorage().equals(initResource))
      {
        found = count;
        break;
      }
      count++;
    }
    if (found >= 0)
    {
      select(found);
    } else if (isOnClasspath && firstClasspathNamespace >= 0)
    {
      select(firstClasspathNamespace);
    }

  }

  public void dialogFieldChanged(DialogField field)
  {
    if (field == fProjectField)
    {

      clearNamespaces();

      if (fProjectField.getStatus().isOK())
      {
        populateNamespaces();
      }
    } else if (field == this || field == fComponentNameField)
    {
      refreshStatus();
    }
  }

  private void clearNamespaces()
  {
    fValidNamespaces.clear();
    setValues(new String[]{});
    select(-1);
    fSelectedNamespace = null;
    fireDialogFieldChanged(this);
  }

  private void populateNamespaces()
  {
    ITapestryProject newProject = fProjectField.getTapestryProject();
    if (newProject != null)
    {
      List libNames = new ArrayList();

      INamespace projectNamespace = TapestryArtifactManager
          .getTapestryArtifactManager()
          .getProjectNamespace(newProject.getProject());
      if (projectNamespace != null)
      {
        // add the primary namespace, if possible
        IResourceLocation location = projectNamespace.getSpecificationLocation();
        // special check to see if its a valid target.
        // the thing is that there may not be a .application file
        // as Tapestry may create one at runtime.
        boolean canAddProjectNamespace = true;
        IResourceWorkspaceLocation realLocation = (IResourceWorkspaceLocation) location;
        IStorage locationStorage = realLocation.getStorage();
        if (locationStorage != null)
          canAddProjectNamespace = CoreUtils.toResource(location) != null;

        if (canAddProjectNamespace)
        {
          libNames.add(UIPlugin.getString(fName + ".DefaultContainerName"));
          fValidNamespaces.add(projectNamespace);
        }

        //find all the ILibrarySpecs in the namespace that are in the workbench
        // (i.e. not in jars)
        List subTargets = collectValidNamespaces(projectNamespace, new ArrayList());
        //there may be circular references...
        List alreadySeen = new ArrayList();

        for (Iterator iter = subTargets.iterator(); iter.hasNext();)
        {
          INamespace element;
          try
          {
            element = (INamespace) iter.next();
          } catch (RuntimeException e)
          {
            UIPlugin.log(e);
            throw e;
          }
          IStorage storage = ((IResourceWorkspaceLocation) element
              .getSpecificationLocation()).getStorage();
          if (storage != null && !alreadySeen.contains(storage))
          {
            alreadySeen.add(storage);
            libNames.add(storage.getName());
            fValidNamespaces.add(element);
          }
        }

        if (!libNames.isEmpty())
        {
          setValues((String[]) libNames.toArray(new String[libNames.size()]));
          select(0);
          fireDialogFieldChanged(this);
        }
      }
    }
    namespaceChanged();
    refreshStatus();
  }

  /**
   * @param projectNamespace
   */
  private List collectValidNamespaces(INamespace parentNamespace, List results)
  {
    List children = parentNamespace.getChildIds();
    if (children.isEmpty())
      return results;

    for (Iterator iter = children.iterator(); iter.hasNext();)
    {
      String childName = (String) iter.next();
      INamespace childNamespace = parentNamespace.getChildNamespace(childName);
      if (childNamespace == null)
      {
        UIPlugin.log("null namespace found - Namespace Dialog Field!");
        continue;
      }
      IResourceWorkspaceLocation childLocation = (IResourceWorkspaceLocation) childNamespace
          .getSpecificationLocation();
      if (CoreUtils.toResource(childLocation) != null)
      {
        results.add(childNamespace);
        collectValidNamespaces(childNamespace, results);
      }
    }
    return results;
  }

  public void refreshStatus()
  {
    setStatus(namespaceChanged());
  }

  public IStatus namespaceChanged()
  {
    SpindleStatus newStatus = new SpindleStatus();
    if (fValidNamespaces.isEmpty())
    {
      newStatus.setError(UIPlugin.getString(fName + ".NoValidNamespaceToSelect"));
      return newStatus;
    }

    int selectedIndex = getSelectedIndex();

    if (selectedIndex == -1)
    {
      fSelectedNamespace = null;
      newStatus.setError(UIPlugin.getString(fName + ".MustSelectATarget"));
      return newStatus;
    }

    fSelectedNamespace = ((ICoreNamespace) fValidNamespaces.get(selectedIndex));

    return newStatus;
  }

//  private boolean pathExists(IResourceLocation base, String path)
//  {
//    if (path == null || path.trim().length() == 0)
//      return false;
//    IResourceWorkspaceLocation checkLocation = (IResourceWorkspaceLocation) ((IResourceWorkspaceLocation) base)
//        .getRelativeLocation(path);
//    return checkLocation.getStorage() != null;
//
//  }

  public void setSelectedNamespace(INamespace namespace)
  {
    int index = -1;

    if (namespace != null)
    {
      fValidNamespaces.indexOf(namespace);
      Assert.isLegal(index > 0);
    }
    select(index);
  }

  public INamespace getSelectedNamespace()
  {

    return fSelectedNamespace;

  }
}