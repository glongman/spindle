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
package com.iw.plugins.spindle.ui.descriptors;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import com.iw.plugins.spindle.TapestryPlugin;
import com.iw.plugins.spindle.model.ITapestryModel;
import com.iw.plugins.spindle.ui.TypeDialogCellEditor;
import com.iw.plugins.spindle.util.lookup.TapestryLookup;

public class TypeDialogPropertyDescriptor extends PropertyDescriptor implements INeedsModelInitialization {

  private IPackageFragmentRoot root;
  private String hierarchyRoot;

  private int searchFlags;

  public TypeDialogPropertyDescriptor(Object id, String displayName, int searchFlags) {
    super(id, displayName);
    this.searchFlags = searchFlags;
  }

  public TypeDialogPropertyDescriptor(Object id, String displayName, int searchFlags, String hierarchyRoot) {
    this(id, displayName, searchFlags);
    this.hierarchyRoot = hierarchyRoot;

  }

  /**
   * @see com.iw.plugins.spindle.ui.descriptors.INeedsModelInitialization#initialize(ITapestryModel)
   */
  public void initialize(ITapestryModel model) {
    Assert.isNotNull(model);
    this.root = getRoot(model);
  }

  private IPackageFragmentRoot getRoot(ITapestryModel model) {

    IStorage storage = model.getUnderlyingStorage();
    TapestryLookup lookup = new TapestryLookup();

    try {

      lookup.configure(TapestryPlugin.getDefault().getJavaProjectFor(storage));

      IPackageFragment fragment = lookup.findPackageFragment(storage);

      Object possibleRoot = fragment.getParent();

      return (IPackageFragmentRoot) possibleRoot;

    } catch (CoreException e) {

      Shell shell = TapestryPlugin.getDefault().getActiveWorkbenchWindow().getShell();

      ErrorDialog.openError(shell, "Spindle error", "can't find java project root", e.getStatus());
    }
    return null;
  }

  public CellEditor createPropertyEditor(Composite parent) {
    return new TypeDialogCellEditor(parent, root, searchFlags, hierarchyRoot);
  }

}