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
package com.iw.plugins.spindle.model;

import java.beans.PropertyChangeListener;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;

import net.sf.tapestry.parse.SpecificationParser;
import net.sf.tapestry.util.xml.DocumentParseException;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;

import com.iw.plugins.spindle.TapestryPlugin;
import com.iw.plugins.spindle.spec.PluginComponentSpecification;
import com.iw.plugins.spindle.spec.PluginContainedComponent;
import com.iw.plugins.spindle.util.SourceWriter;
import com.iw.plugins.spindle.util.lookup.TapestryLookup;

public class TapestryComponentModel extends BaseTapestryModel implements PropertyChangeListener {

  private PluginComponentSpecification componentSpec;

  /**
   * Constructor for TapestryComponentModel
   */
  public TapestryComponentModel(IStorage storage) {
    super(storage);
  }

  public PluginComponentSpecification getComponentSpecification() {
    if (loaded) {
      return componentSpec;
    } else {
      return null;
    }
  }

  /**
   * @see AbstractModel#load()
   */
  public void load(final InputStream source) throws CoreException {

    final TapestryComponentModel thisModel = this;

    TapestryPlugin.getDefault().getWorkspace().run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) {

        removeAllProblemMarkers();
        if (componentSpec != null) {
          componentSpec.removePropertyChangeListener(TapestryComponentModel.this);
          componentSpec.setParent(null);
          componentSpec = null;
        }
        try {
          IStorage element = getUnderlyingStorage();

          String extension = element.getFullPath().getFileExtension();

          SpecificationParser parser = (SpecificationParser) TapestryPlugin.getParserFor(extension);

          if (extension.equals("jwc")) {

            componentSpec =
              (PluginComponentSpecification) TapestryPlugin
                .getParser()
                .parseComponentSpecification(
                source,
                element.getName());

          } else if (extension.equals("page")) {

            componentSpec =
              (PluginComponentSpecification) TapestryPlugin.getParser().parsePageSpecification(
                source,
                element.getName());

          }
          componentSpec.setIdentifier(element.getName());
          componentSpec.setParent(thisModel);
          componentSpec.addPropertyChangeListener(TapestryComponentModel.this);
          loaded = true;
          editable = !element.isReadOnly();
          fireModelObjectChanged(componentSpec, "componentSpec");
        } catch (DocumentParseException dpex) {
        	
          addProblemMarker(dpex);
          loaded = false;
        }
      }
    }, null);

  }

  /**
   * @see IEditable#save()
   */
  public void save(PrintWriter writer) {
    PluginComponentSpecification spec = (PluginComponentSpecification) getComponentSpecification();
    StringWriter stringwriter = new StringWriter();
    spec.write(new SourceWriter(stringwriter));
    writer.print(stringwriter.toString());
  }

  public void setDescription(String description) {
    PluginComponentSpecification spec = getComponentSpecification();
    if (spec != null) {
      spec.setDescription(description);
      fireModelObjectChanged(this, "description");
    }
  }

  public String getDescription() {
    PluginComponentSpecification spec = getComponentSpecification();
    if (spec != null) {
      return spec.getDescription();
    }
    return "";
  }

  public ReferenceInfo resolveReferences(boolean reverse) {
    //    if (!reverse) {
    //      ArrayList containedComponents = new ArrayList();
    //      PluginComponentSpecification spec = getComponentSpecification();
    //      Iterator iter = spec.getComponentIds().iterator();
    //      while (iter.hasNext()) {
    //        String id = (String) iter.next();
    //        PluginContainedComponent contained = (PluginContainedComponent) spec.getComponent(id);
    //        if (contained.getCopyOf() != null) {
    //          continue;
    //        }
    //        ReferenceInfo.ReferenceHolder holder = new ReferenceInfo.ReferenceHolder();
    //        holder.name = contained.getType();
    //        holder.description = "[component id =" + id + " type=" + holder.name + "] ";
    //        containedComponents.add(holder);
    //      }
    //
    //      return resolveForwardReferences(containedComponents);
    //    } else {
    //      return resolveReverseReferences();
    //    }
    return null;
  }

  public boolean containsReference(String name) {
    try {
      if (!isLoaded()) {
        load(((IStorage) getUnderlyingStorage()).getContents());
      }
      PluginComponentSpecification componentSpec = getComponentSpecification();
      if (componentSpec != null) {
        Iterator containedComponentIds = componentSpec.getComponentIds().iterator();
        while (containedComponentIds.hasNext()) {
          String id = (String) containedComponentIds.next();
          if (((PluginContainedComponent) componentSpec.getComponent(id)).getType().equals(name)) {
            return true;
          }
        }
      }
    } catch (CoreException corex) {
      // do nothing
    }

    return false;

  }

  //  private ReferenceInfo resolveForwardReferences(List containedComponents) {
  //    ArrayList resolved = new ArrayList();
  //    ArrayList unresolved = new ArrayList();
  //    Iterator iter = containedComponents.iterator();
  //    while (iter.hasNext()) {
  //      ReferenceInfo.ReferenceHolder holder = (ReferenceInfo.ReferenceHolder) iter.next();
  //      holder.isAlias = !holder.name.endsWith(".jwc");
  //      IStorage[] resolvedArray =
  //        TapestryPlugin.getDefault().resolveTapestryComponent(getUnderlyingStorage(), holder.name);
  //      if (resolvedArray.length == 0) {
  //        holder.description += "unresolved";
  //        unresolved.add(holder);
  //        continue;
  //      }
  //      IStorage resolvedFile = resolvedArray[0];
  //      if (resolvedFile instanceof JarEntryFile) {
  //        holder.name = ((JarEntryFile) resolvedFile).getName();
  //
  //      }
  //      if (holder.isAlias) {
  //        holder.description += "default = /" + resolvedFile.getFullPath().toString();
  //      }
  //      holder.model =
  //        (BaseTapestryModel) TapestryPlugin.getTapestryModelManager().getModel(resolvedFile);
  //      resolved.add(holder);
  //    }
  //    return new ReferenceInfo(resolved, unresolved, false);
  //  }

  // private ReferenceInfo resolveReverseReferences() {
  //    ArrayList resolved = new ArrayList();
  //    IStorage underlier = getUnderlyingStorage();
  //    String useName = getSpecificationLocation();
  //    Iterator components = ModelUtils.getComponentModels();
  //    while (components.hasNext()) {
  //      TapestryComponentModel cmodel = (TapestryComponentModel) components.next();
  //      if (resolved.contains(cmodel)) {
  //        continue;
  //      }
  //      if (cmodel.containsReference(useName)) {
  //        ReferenceInfo.ReferenceHolder holder = new ReferenceInfo.ReferenceHolder();
  //        holder.description = cmodel.getUnderlyingStorage().getName();
  //        holder.name = cmodel.getUnderlyingStorage().getFullPath().toString();
  //        holder.model = cmodel;
  //        resolved.add(holder);
  //        break;
  //      }
  //    }
  //    Iterator applications = ModelUtils.getApplicationModels();
  //    while (applications.hasNext()) {
  //      TapestryApplicationModel amodel = (TapestryApplicationModel) applications.next();
  //      if (resolved.contains(amodel)) {
  //        continue;
  //      }
  //      if (amodel.containsReference(useName)) {
  //        ReferenceInfo.ReferenceHolder holder = new ReferenceInfo.ReferenceHolder();
  //        holder.description = amodel.getUnderlyingStorage().getName();
  //        holder.name = amodel.getUnderlyingStorage().getFullPath().toString();
  //        holder.model = amodel;
  //        resolved.add(holder);
  //        break;
  //      }
  //    }
  //    return new ReferenceInfo(resolved, new ArrayList(), true);
  //  }

  public String getSpecificationLocation() {
    IStorage underlier = getUnderlyingStorage();
    IPackageFragment fragment = null;
    TapestryLookup lookup = new TapestryLookup();
    try {

      IJavaProject jproject = TapestryPlugin.getDefault().getJavaProjectFor(underlier);
      lookup.configure(jproject);
      fragment = lookup.findPackageFragment(underlier);

    } catch (CoreException jmex) {
    }
    String tapestryName = "";
    if (fragment != null) {
      tapestryName = fragment.getElementName();
      tapestryName = tapestryName.replace('.', '/');
      tapestryName = "/" + tapestryName;
    }
    return tapestryName + "/" + underlier.getName();
  }

  public List getPropertyNames() {
    return getComponentSpecification().getPropertyNames();
  }

  public String getProperty(String name) {
    return getComponentSpecification().getProperty(name);
  }

  public void setProperty(String name, String value) {
    if (isEditable()) {
      getComponentSpecification().setProperty(name, value);
    }
  }

  public void removeProperty(String name) {

    if (isEditable()) {

      getComponentSpecification().removeProperty(name);
    }

  }

  /**
   * @see com.iw.plugins.spindle.model.ITapestryModel#getPublicId()
   */
  public String getPublicId() {
    if (componentSpec != null) {
      return componentSpec.getPublicId();
    }
    return "";
  }

  /**
   * @see com.iw.plugins.spindle.model.ITapestryModel#setPublicId(String)
   */
  public void setPublicId(String value) {

    if (componentSpec != null) {

      componentSpec.setPublicId(value);
    }
  }

}
