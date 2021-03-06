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
package com.iw.plugins.spindle.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICodeFormatter;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.part.ISetSelectionTarget;

import com.iw.plugins.spindle.TapestryPlugin;
import com.iw.plugins.spindle.editors.SpindleMultipageEditor;
import com.iw.plugins.spindle.model.ITapestryModel;
import com.iw.plugins.spindle.model.TapestryComponentModel;
import com.iw.plugins.spindle.model.manager.TapestryProjectModelManager;
import com.iw.plugins.spindle.spec.PluginComponentSpecification;
import com.iw.plugins.spindle.spec.PluginContainedComponent;
import com.iw.plugins.spindle.ui.RequiredSaveEditorAction;

/** 
 * A class to collect useful methods in. May move them elsewhere or, then again
 * I may not.
 */

public class Utils {

  public static void selectAndReveal(IResource resource, IWorkbenchWindow window) {
    // validate the input
    if (window == null || resource == null)
      return;
    IWorkbenchPage page = window.getActivePage();
    if (page == null)
      return;

    // get all the view and editor parts
    List parts = new ArrayList();
    IViewReference[] viewRefs = page.getViewReferences();
    for (int i = 0; i < viewRefs.length; i++) {
      parts.add(viewRefs[i].getPart(false));
    }
    IEditorReference refs[] = page.getEditorReferences();
    for (int i = 0; i < refs.length; i++) {
      if (refs[i].getPart(false) != null)
        parts.add(refs[i].getPart(false));
    }

    final ISelection selection = new StructuredSelection(resource);
    for (Iterator iter = parts.iterator(); iter.hasNext();) {
      IWorkbenchPart part = (IWorkbenchPart) iter.next();

      // get the part's ISetSelectionTarget implementation
      ISetSelectionTarget target = null;
      if (part instanceof ISetSelectionTarget)
        target = (ISetSelectionTarget) part;
      else
        target = (ISetSelectionTarget) part.getAdapter(ISetSelectionTarget.class);

      if (target != null) {
        // select and reveal resource
        final ISetSelectionTarget finalTarget = target;
        window.getShell().getDisplay().asyncExec(new Runnable() {
          public void run() {
            finalTarget.selectReveal(selection);
          }
        });
      }
    }
  }

  /**
   * @return all the editors in the workbench that need saving
   */
  public static IEditorPart[] getOpenEditors() {
    List result = new ArrayList(0);
    IWorkbench workbench = TapestryPlugin.getDefault().getWorkbench();
    IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
    for (int i = 0; i < windows.length; i++) {
      IWorkbenchPage[] pages = windows[i].getPages();
      for (int x = 0; x < pages.length; x++) {
        IEditorReference[] erefs = pages[x].getEditorReferences();
        for (int j = 0; j < erefs.length; j++) {
          IEditorReference iEditorReference = erefs[j];
          IEditorPart part = iEditorReference.getEditor(false);
          result.add(part);

        }
      }
    }
    return (IEditorPart[]) result.toArray(new IEditorPart[result.size()]);
  }

  public static IEditorPart[] getDirtyEditors() {
    IEditorPart[] openEditors = getOpenEditors();
    List result = new ArrayList(0);
    for (int i = 0; i < openEditors.length; i++) {
      if (openEditors[i].isDirty()) {
        result.add(openEditors[i]);
      }
    }
    return (IEditorPart[]) result.toArray(new IEditorPart[result.size()]);
  }

  /**
    * @return the editor for a Tapestry model
    */
  public static IEditorPart getEditorFor(IStorage storage) {

    IWorkbench workbench = TapestryPlugin.getDefault().getWorkbench();
    IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();

    for (int i = 0; i < windows.length; i++) {
      IWorkbenchPage[] pages = windows[i].getPages();
      for (int x = 0; x < pages.length; x++) {

        IEditorReference[] editors = pages[x].getEditorReferences();

        for (int z = 0; z < editors.length; z++) {

          IEditorReference ref = editors[z];
          IEditorPart editor = ref.getEditor(true);

          if (editor == null) {
            continue;
          }

          if (editor instanceof SpindleMultipageEditor) {

            SpindleMultipageEditor spindleEditor = ((SpindleMultipageEditor) editor);
            IStorage editorStorage = ((ITapestryModel) spindleEditor.getModel()).getUnderlyingStorage();

            if (editorStorage == null) {
              continue;
            }
            if (editorStorage.getFullPath().equals(storage.getFullPath())) {
              return editor;
            }

          } else if (storage instanceof IFile) {

            IFile file = (IFile) storage;
            IFile editorFile = (IFile) editor.getEditorInput().getAdapter(IFile.class);

            if (editorFile != null && file.equals(editorFile)) {
              return editor;
            }

          }
        }
      }
    }
    return null;
  }

  /**
   * 
   * this is ugly - should handle this nicely like TapestryLookup does for models
   * @return the editor for a model instance, or null if there is no such editor or if the found editor is not dirty
   * 
   */
  public static IEditorPart getDirtyEditorFor(ITapestryModel model) {
    if (model == null) {
      return null;
    }
    IEditorPart[] dirty = getDirtyEditors();
    for (int i = 0; i < dirty.length; i++) {
      if (dirty[i] instanceof SpindleMultipageEditor) {
        if (((SpindleMultipageEditor) dirty[i]).getModel() == model) {
          return dirty[i];
        }
      }
    }
    return null;

  }

  //  public static List getApplicationsWithAlias(String alias) {
  //    ArrayList result = new ArrayList();
  //    Iterator iter = ModelUtils.getApplicationModels();
  //    while (iter.hasNext()) {
  //      TapestryApplicationModel model = (TapestryApplicationModel) iter.next();
  //      if (!model.isLoaded()) {
  //        try {
  //          model.load();
  //        } catch (Exception e) {
  //          continue;
  //        }
  //      }
  //      PluginApplicationSpecification spec =
  //        (PluginApplicationSpecification) model.getSpecification();
  //      if (spec != null && spec.getComponentSpecificationPath(alias) != null) {
  //        result.add(model);
  //      }
  //    }
  //    return result;
  //  }

  // assumes target ComponentModel is loaded.
  // this could use some refactoring for sure!
  public static void copyContainedComponentTo(
    String sourceName,
    PluginContainedComponent componentClone,
    TapestryComponentModel target)
    throws Exception {
    String useName = sourceName;
    PluginComponentSpecification targetSpec = (PluginComponentSpecification) target.getComponentSpecification();
    if (targetSpec.getComponent(sourceName + 1) != null) {
      int counter = 2;
      while (targetSpec.getComponent(sourceName + counter) != null) {
        counter++;
      }
      useName = sourceName + counter;
    } else {
      sourceName = sourceName + 1;
    }

    targetSpec.addComponent(useName, componentClone);
    target.setOutOfSynch(true);
  }

  public static void createContainedComponentIn(String jwcid, String containedComponentPath, TapestryComponentModel target) {
    PluginComponentSpecification spec = target.getComponentSpecification();
    if (spec.getComponent(jwcid) == null) {
      PluginContainedComponent contained = new PluginContainedComponent();
      contained.setType(containedComponentPath);
      spec.addComponent(jwcid, contained);
      target.setOutOfSynch(true);
    }
  }

  /**
   * Returns the first java element that conforms to the given type walking the
   * java element's parent relationship. If the given element alrady conforms to
   * the given kind, the element is returned.
   * Returns <code>null</code> if no such element exits.
   */
  public static IJavaElement findElementOfKind(IJavaElement element, int kind) {
    while (element != null && element.getElementType() != kind)
      element = element.getParent();
    return element;
  }

  /**
   * Returns the package fragment root of <code>IJavaElement</code>. If the given
   * element is already a package fragment root, the element itself is returned.
   */
  public static IPackageFragmentRoot getPackageFragmentRoot(IJavaElement element) {
    return (IPackageFragmentRoot) findElementOfKind(element, IJavaElement.PACKAGE_FRAGMENT_ROOT);
  }

  /**
   * Returns true if the element is on the build path of the given project
   */
  public static boolean isOnBuildPath(IJavaProject jproject, IJavaElement element) throws JavaModelException {
    IPath rootPath;
    if (element.getElementType() == IJavaElement.JAVA_PROJECT) {
      rootPath = ((IJavaProject) element).getProject().getFullPath();
    } else {
      IPackageFragmentRoot root = getPackageFragmentRoot(element);
      if (root == null) {
        return false;
      }
      rootPath = root.getPath();
    }
    return jproject.findPackageFragmentRoot(rootPath) != null;
  }

  //  /** 
  //   * Finds a type by its qualified type name (dot separated).
  //   * @param jproject The java project to search in
  //   * @param str The fully qualified name (type name with enclosing type names and package (all separated by dots))
  //   * @return The type found, or null if not existing
  //   * The method only finds top level types and its inner types. Waiting for a Java Core solution
  //   */
  //  public static IType findType(IJavaProject jproject, String fullyQualifiedName) throws JavaModelException {
  //    String pathStr = fullyQualifiedName.replace('.', '/') + ".java"; //$NON-NLS-1$
  //    IJavaElement jelement = jproject.findElement(new Path(pathStr));
  //    if (jelement == null) {
  //      // try to find it as inner type
  //      String qualifier = Signature.getQualifier(fullyQualifiedName);
  //      if (qualifier.length() > 0) {
  //        IType type = findType(jproject, qualifier); // recursive!
  //        if (type != null) {
  //          IType res = type.getType(Signature.getSimpleName(fullyQualifiedName));
  //          if (res.exists()) {
  //            return res;
  //          }
  //        }
  //      }
  //    } else if (jelement.getElementType() == IJavaElement.COMPILATION_UNIT) {
  //      String simpleName = Signature.getSimpleName(fullyQualifiedName);
  //      return ((ICompilationUnit) jelement).getType(simpleName);
  //    } else if (jelement.getElementType() == IJavaElement.CLASS_FILE) {
  //      return ((IClassFile) jelement).getType();
  //    }
  //    return null;
  //  }

  //  /** 
  //   * Finds a type by package and type name.
  //   * @param jproject the java project to search in
  //   * @param pack The package name
  //   * @param typeQualifiedName the type qualified name (type name with enclosing type names (separated by dots))
  //   * @return the type found, or null if not existing
  //   * The method only finds top level types and its inner types. Waiting for a Java Core solution
  //   * 
  //   * @deprecated
  //   */
  //  public static IType findType(IJavaProject jproject, String pack, String typeQualifiedName) throws JavaModelException {
  //    // should be supplied from java core
  //    int dot = typeQualifiedName.indexOf('.');
  //    if (dot == -1) {
  //      return findType(jproject, concatenateName(pack, typeQualifiedName));
  //    }
  //    IPath packPath;
  //    if (pack.length() > 0) {
  //      packPath = new Path(pack.replace('.', '/'));
  //    } else {
  //      packPath = new Path(""); //$NON-NLS-1$
  //    }
  //    // fixed for 1GEXEI6: ITPJUI:ALL - Incorrect error message on class creation wizard
  //    IPath path = packPath.append(typeQualifiedName.substring(0, dot) + ".java"); //$NON-NLS-1$
  //    IJavaElement elem = jproject.findElement(path);
  //    if (elem instanceof ICompilationUnit) {
  //      return findTypeInCompilationUnit((ICompilationUnit) elem, typeQualifiedName);
  //    } else if (elem instanceof IClassFile) {
  //      path = packPath.append(typeQualifiedName.replace('.', '$') + ".class"); //$NON-NLS-1$
  //      elem = jproject.findElement(path);
  //      if (elem instanceof IClassFile) {
  //        return ((IClassFile) elem).getType();
  //      }
  //    }
  //    return null;
  //  }

  //  /** 
  //   * Finds a type in a compilation unit. Typical usage is to find the corresponding
  //   * type in a working copy.
  //   * @param cu the compilation unit to search in
  //   * @param typeQualifiedName the type qualified name (type name with enclosing type names (separated by dots))
  //   * @return the type found, or null if not existing
  //   * 
  //   * @deprecated
  //   */
  //  public static IType findTypeInCompilationUnit(ICompilationUnit cu, String typeQualifiedName) throws JavaModelException {
  //    IType[] types = cu.getAllTypes();
  //    for (int i = 0; i < types.length; i++) {
  //      String currName = getTypeQualifiedName(types[i]);
  //      if (typeQualifiedName.equals(currName)) {
  //        return types[i];
  //      }
  //    }
  //    return null;
  //  }
  //
  //  /**
  //  * Concatenates two names. Uses a dot for separation.
  //  * Both strings can be empty or <code>null</code>.
  //  */
  //  public static String concatenateName(String name1, String name2) {
  //    StringBuffer buf = new StringBuffer();
  //    if (name1 != null && name1.length() > 0) {
  //      buf.append(name1);
  //    }
  //    if (name2 != null && name2.length() > 0) {
  //      if (buf.length() > 0) {
  //        buf.append('.');
  //      }
  //      buf.append(name2);
  //    }
  //    return buf.toString();
  //  }
  //
  //  /**
  //   * Returns the qualified type name of the given type using '.' as separators.
  //   * This is a replace for IType.getTypeQualifiedName()
  //   * which uses '$' as separators. As '$' is also a valid character in an id
  //   * this is ambiguous. JavaCore PR: 1GCFUNT
  //   * 
  //   * @deprecated
  //   */
  //  public static String getTypeQualifiedName(IType type) {
  //    StringBuffer buf = new StringBuffer();
  //    getTypeQualifiedName(type, buf);
  //    return buf.toString();
  //  }
  //
  //  private static void getTypeQualifiedName(IType type, StringBuffer buf) {
  //    IType outerType = type.getDeclaringType();
  //    if (outerType != null) {
  //      getTypeQualifiedName(outerType, buf);
  //      buf.append('.');
  //    }
  //    buf.append(type.getElementName());
  //  }

  /**
   * Evaluates if a member (possible from another package) is visible from
   * elements in a package.
   * @param member The member to test the visibility for
   * @param pack The package in focus
   */
  public static boolean isVisible(IMember member, IPackageFragment pack) throws JavaModelException {
    int otherflags = member.getFlags();

    if (Flags.isPublic(otherflags) || Flags.isProtected(otherflags)) {
      return true;
    } else if (Flags.isPrivate(otherflags)) {
      return false;
    }

    IPackageFragment otherpack = (IPackageFragment) findElementOfKind(member, IJavaElement.PACKAGE_FRAGMENT);
    return (pack != null && pack.equals(otherpack));
  }

  public static String formatJavaCode(String sourceString, int initialIndentationLevel, String lineDelim) {
    ICodeFormatter formatter = ToolFactory.createDefaultCodeFormatter(null);
    return formatter.format(sourceString, initialIndentationLevel, null, lineDelim) + lineDelim;
  }

  public static boolean extendsType(IType candidate, IType baseType) throws JavaModelException {
    boolean match = false;
    ITypeHierarchy hierarchy = candidate.newSupertypeHierarchy(null);
    if (hierarchy.exists()) {
      IType[] superClasses = hierarchy.getAllSupertypes(candidate);
      for (int i = 0; i < superClasses.length; i++) {
        if (superClasses[i].equals(baseType)) {
          match = true;
        }
      }
    }
    return match;
  }

  public static boolean implementsInterface(IType candidate, String interfaceName) throws JavaModelException {
    boolean match = false;
    String[] superInterfaces = candidate.getSuperInterfaceNames();
    if (superInterfaces != null && superInterfaces.length > 0) {
      for (int i = 0; i < superInterfaces.length; i++) {
        if (candidate.isBinary() && interfaceName.endsWith(superInterfaces[i])) {
          match = true;
        } else {
          match = interfaceName.equals(superInterfaces[i]);
        }
      }
    } else {
      match = false;
    }
    return match;
  }

  public static void saveModel(ITapestryModel model, IProgressMonitor monitor) {
    InputStream stream = null;
    try {

      stream = new ByteArrayInputStream(model.toXML().getBytes());

      IFile file = (IFile) ((IAdaptable) model.getUnderlyingStorage()).getAdapter(IFile.class);
      //assuming here the file exists!

      file.setContents(stream, true, true, monitor);

      model.reload();

    } catch (CoreException c) {

    } finally {
      try {
        stream.close();
      } catch (IOException e) {
      }
    }
  }

  /**
  * Returns the given file's contents as a byte array.
  */
  public static byte[] getResourceContentsAsByteArray(IFile file) throws CoreException {
    InputStream stream = null;
    try {

      stream = new BufferedInputStream(file.getContents(true));

      return Utils.getInputStreamAsByteArray(stream, -1);

    } catch (IOException e) {

      throw new CoreException(new SpindleStatus(e));

    } finally {

      try {

        stream.close();

      } catch (IOException e) {
      }
    }
  }

  public static byte[] getInputStreamAsByteArray(InputStream stream, int length) throws IOException {
    byte[] contents;
    if (length == -1) {
      contents = new byte[0];
      int contentsLength = 0;
      int bytesRead = -1;
      do {
        int available = stream.available();

        // resize contents if needed
        if (contentsLength + available > contents.length) {
          System.arraycopy(contents, 0, contents = new byte[contentsLength + available], 0, contentsLength);
        }

        // read as many bytes as possible
        bytesRead = stream.read(contents, contentsLength, available);

        if (bytesRead > 0) {
          // remember length of contents
          contentsLength += bytesRead;
        }
      } while (bytesRead > 0);

      // resize contents if necessary
      if (contentsLength < contents.length) {
        System.arraycopy(contents, 0, contents = new byte[contentsLength], 0, contentsLength);
      }
    } else {
      contents = new byte[length];
      int len = 0;
      int readSize = 0;
      while ((readSize != -1) && (len != length)) {
        // See PR 1FMS89U
        // We record first the read size. In this case len is the actual read size.
        len += readSize;
        readSize = stream.read(contents, len, length - len);
      }
    }

    return contents;
  }

  // works for pages and components
  public static IFile findRelatedComponent(IFile templateFile) {

    String componentName = extractComponentName(templateFile);

    try {
      if (componentName != null) {

        IFolder parent = (IFolder) templateFile.getParent();

        IJavaProject jproject = TapestryPlugin.getDefault().getJavaProjectFor(templateFile);

        IPackageFragment fragment = (IPackageFragment) JavaCore.create(parent);

        if (fragment != null) {

          if (fragment.isDefaultPackage()) {

            IResource[] resources = parent.members();
            for (int i = 0; i < resources.length; i++) {

              if (isComponentMatch(componentName, resources[i])) {
                return (IFile) resources[i];
              }
            }
          } else {

            IPackageFragment[] allEqivalent = findAllEquivalentFragments(fragment);

            for (int i = 0; i < allEqivalent.length; i++) {

              Object[] children = allEqivalent[i].getNonJavaResources();
              for (int j = 0; j < children.length; j++) {
                if (children[j] instanceof IResource && isComponentMatch(componentName, (IResource) children[j])) {
                  return (IFile) children[j];
                }
              }
            }
          }
        }
      }
    } catch (JavaModelException e) {
    } catch (CoreException e) {
    }
    return null;
  }

  private static boolean isComponentMatch(String componentName, IResource resource) {
    IPath path = resource.getFullPath();
    String extension = path.getFileExtension();
    if (extension == null || "".equals(extension)) {
      return false;
    }
    String name = path.removeFileExtension().lastSegment();
    return componentName.equals(name) && ("jwc".equals(extension) || "page".equals(extension));
  }

  private static String extractComponentName(IFile file) {

    String name = file.getFullPath().removeFileExtension().lastSegment();

    String languageCode = extractFirstLocalizationCode(name);

    if (languageCode == null) {

      return name;

    }

    if (languageCodes == null) {

      buildLanguageCodes();

    }

    if (!languageCodes.contains(languageCode)) {

      return name;
    }

    return name.substring(0, name.indexOf(languageCode) - 1);

  }

  // find any templates for the supplied file
  // works for jwc or page files
  public static List findTemplatesFor(IFile componentResource) {

    ArrayList templates = new ArrayList();
    IFolder parent = (IFolder) componentResource.getParent();

    String fileName = componentResource.getFullPath().removeFileExtension().lastSegment();
    try {

      IJavaProject project = TapestryPlugin.getDefault().getJavaProjectFor(componentResource);

      IPackageFragment fragment = (IPackageFragment) JavaCore.create(parent);

      if (fragment == null) {
        return templates;
      }

      if (fragment.isDefaultPackage()) {

        // Currently, PackageFragment does not return non java resources
        // from the default package, so we just look in the folder.

        IResource[] resources = parent.members();
        for (int i = 0; i < resources.length; i++) {
          if (isValidTemplate(fileName, resources[i])) {
            templates.add(resources[i]);
          }

        }

      } else {

        IPackageFragment[] allEqivalent = findAllEquivalentFragments(fragment);

        for (int i = 0; i < allEqivalent.length; i++) {
          Object[] children = allEqivalent[i].getNonJavaResources();
          for (int j = 0; j < children.length; j++) {

            if (children[j] instanceof IResource && isValidTemplate(fileName, (IResource) children[j])) {
              templates.add((IResource) children[j]);
            }

          }
        }

      }

    } catch (CoreException e) {
    }

    return templates;
  }

  public static boolean isValidTemplate(String componentName, IResource possibleTemplate) {

    IPath memberPath = possibleTemplate.getFullPath();
    String extension = memberPath.getFileExtension();

    if ("html".equals(extension) || "htm".equals(extension)) {

      String memberName = memberPath.removeFileExtension().lastSegment();

      return templateMatchLocalization(componentName, memberName);

    }
    return false;
  }

  private static boolean templateMatchLocalization(String fileName, String memberName) {

    if (fileName.equals(memberName)) {

      return true;

    } else if (memberName.startsWith(fileName + '_')) {

      String languageString = extractFirstLocalizationCode(memberName);

      if (languageCodes == null) {

        buildLanguageCodes();

      }

      return languageCodes.contains(languageString);

    }

    return false;
  }

  private static String extractFirstLocalizationCode(String candidate) {

    int firstUnderscore = candidate.indexOf('_');
    int secondUnderscore = firstUnderscore + 3;

    String languageString = null;

    if (secondUnderscore < candidate.length()) {

      char next = candidate.charAt(secondUnderscore);

      if (next != '_') {

        return null;

      }

      languageString = candidate.substring(firstUnderscore + 1, secondUnderscore);

    } else {

      languageString = candidate.substring(firstUnderscore + 1);

    }

    return languageString;

  }

  private static List languageCodes = null;

  private static void buildLanguageCodes() {

    languageCodes = Arrays.asList(Locale.getISOLanguages());

  }

  /**
   * Method findComponentClass.
   * @param jwcOrPageStorage
   * @return IType
   */
  public static IType findComponentClass(IStorage jwcOrPageStorage) {

    String fileName = jwcOrPageStorage.getFullPath().lastSegment();

    if (!fileName.endsWith(".jwc") && !fileName.endsWith(".page")) {

      return null;
    }

    IEditorPart part = getEditorFor(jwcOrPageStorage);

    TapestryComponentModel model = null;

    if (part != null) {

      if (part.isDirty()) {

        RequiredSaveEditorAction action = new RequiredSaveEditorAction(part);

        if (!action.save()) {

          return null;
        }
      }

      if (part instanceof SpindleMultipageEditor) {
        model = (TapestryComponentModel) ((SpindleMultipageEditor) part).getModel();
      }

    }
    try {

      if (model == null || !model.isLoaded()) {

        TapestryProjectModelManager manager = TapestryPlugin.getTapestryModelManager(jwcOrPageStorage);

        model = (TapestryComponentModel) manager.getReadOnlyModel(jwcOrPageStorage);

      }

      if (model != null && model.isLoaded()) {

        IJavaProject jproject = TapestryPlugin.getDefault().getJavaProjectFor(jwcOrPageStorage);

        return jproject.findType(model.getComponentSpecification().getComponentClassName());
      }

    } catch (CoreException e) {
    }

    return null;

  }

  public static IPackageFragment[] findAllEquivalentFragments(IPackageFragment fragment) throws JavaModelException {

    IJavaProject jproject = fragment.getJavaProject();
    IPackageFragment[] allFragments = jproject.getPackageFragments();
    ArrayList result = new ArrayList();
    for (int i = 0; i < allFragments.length; i++) {
      if (allFragments[i].isDefaultPackage()) {
        continue;
      }
      if (allFragments[i].getElementName().equals(fragment.getElementName())) {
        result.add(allFragments[i]);
      }
    }
    return (IPackageFragment[]) result.toArray(new IPackageFragment[result.size()]);

  }

  public static String[] getImportOrderPreference() {
    IPreferenceStore prefs = JavaPlugin.getDefault().getPreferenceStore();
    String str = prefs.getString(PreferenceConstants.ORGIMPORTS_IMPORTORDER);
    if (str != null) {
      return unpackOrderList(str);
    }
    return new String[0];
  }

  private static String[] unpackOrderList(String str) {
    StringTokenizer tok = new StringTokenizer(str, ";");
    int nTokens = tok.countTokens();
    String[] res = new String[nTokens];
    for (int i = 0; i < nTokens; i++) {
      res[i] = tok.nextToken();
    }
    return res;
  }

  public static int getImportNumberThreshold() {
    IPreferenceStore prefs = JavaPlugin.getDefault().getPreferenceStore();
    int threshold = prefs.getInt(PreferenceConstants.ORGIMPORTS_ONDEMANDTHRESHOLD);
    if (threshold < 0) {
      threshold = Integer.MAX_VALUE;
    }
    return threshold;
  }

}