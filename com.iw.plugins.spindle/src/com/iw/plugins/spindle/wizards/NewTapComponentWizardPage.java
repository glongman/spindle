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
package com.iw.plugins.spindle.wizards;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.iw.plugins.spindle.MessageUtil;
import com.iw.plugins.spindle.TapestryImages;
import com.iw.plugins.spindle.TapestryPlugin;
import com.iw.plugins.spindle.dialogfields.CheckBoxField;
import com.iw.plugins.spindle.dialogfields.DialogField;
import com.iw.plugins.spindle.dialogfields.IDialogFieldChangedListener;
import com.iw.plugins.spindle.editors.SpindleMultipageEditor;
import com.iw.plugins.spindle.factories.ComponentFactory;
import com.iw.plugins.spindle.model.TapestryApplicationModel;
import com.iw.plugins.spindle.model.TapestryComponentModel;
import com.iw.plugins.spindle.spec.PluginApplicationSpecification;
import com.iw.plugins.spindle.spec.PluginPageSpecification;
import com.iw.plugins.spindle.ui.RequiredSaveEditorAction;
import com.iw.plugins.spindle.util.Utils;
import com.iw.plugins.spindle.wizards.fields.ChooseAutoAddApplicationField;
import com.iw.plugins.spindle.wizards.fields.ComponentNameField;
import com.iw.plugins.spindle.wizards.fields.ContainerDialogField;
import com.iw.plugins.spindle.wizards.fields.PackageDialogField;

public class NewTapComponentWizardPage extends NewTapestryElementWizardPage {

  public static final String P_ADD_TO_APPLICATION = "new.component.default.add.to.application";
  public static final String P_ENABLE_ADD_TO_APPLICATION = "new.component.default.add.to.application.enabled";
  public static final String P_GENERATE_HTML = "new.component.generate.html";

  String PAGE_NAME;

  private static int LABEL_WIDTH = 64;

  String CONTAINER;
  String PACKAGE;
  String SPEC_CLASS;
  String COMPONENTNAME;
  String AUTO_ADD;
  String GENERATE_HTML;

  private ContainerDialogField fContainerDialogField;
  private PackageDialogField fPackageDialogField;
  private ComponentNameField fComponentNameDialog;
  private CheckBoxField fAutoAddLabel;
  private ChooseAutoAddApplicationField fAutoAddField;
  private CheckBoxField fGenerateHTML;
  private DialogField fNextLabel;
  private IFile component = null;

  public static void initializeDefaults(IPreferenceStore pstore) {
    pstore.setDefault(P_ADD_TO_APPLICATION, "");
    pstore.setDefault(P_ENABLE_ADD_TO_APPLICATION, false);
    pstore.setDefault(P_GENERATE_HTML, true);
  }

  /**
   * Constructor for NewTapAppWizardPage1
   */
  public NewTapComponentWizardPage(IWorkspaceRoot root, String pageName) {
    super(MessageUtil.getString(pageName + ".title"));
    PAGE_NAME = pageName;
    CONTAINER = PAGE_NAME + ".container";
    PACKAGE = PAGE_NAME + ".package";
    SPEC_CLASS = PAGE_NAME + ".specclass";
    COMPONENTNAME = PAGE_NAME + ".componentname";
    AUTO_ADD = PAGE_NAME + ".autoadd";

    GENERATE_HTML = PAGE_NAME + ".generateHTML";

    this.setImageDescriptor(ImageDescriptor.createFromURL(TapestryImages.getImageURL("component32.gif")));

    this.setDescription(MessageUtil.getString(PAGE_NAME + ".description"));

    IDialogFieldChangedListener listener = new FieldEventsAdapter();

    fContainerDialogField = new ContainerDialogField(CONTAINER, root, LABEL_WIDTH);
    connect(fContainerDialogField);
    fContainerDialogField.addListener(listener);
    fPackageDialogField = new PackageDialogField(PACKAGE, LABEL_WIDTH);
    connect(fPackageDialogField);
    fPackageDialogField.addListener(listener);
    fComponentNameDialog = new ComponentNameField(COMPONENTNAME);
    connect(fComponentNameDialog);
    fComponentNameDialog.addListener(listener);
    fAutoAddLabel = new CheckBoxField(MessageUtil.getString(AUTO_ADD));
    fAutoAddLabel.addListener(listener);
    fAutoAddField = new ChooseAutoAddApplicationField(null, LABEL_WIDTH, new String[0]);   
    connect(fAutoAddField);
    fAutoAddField.addListener(listener);
    fGenerateHTML = new CheckBoxField(MessageUtil.getString(GENERATE_HTML + ".label"));
    fNextLabel = new DialogField("Choose a class for the specification on the next page...");

  }

  /**
   * Should be called from the wizard with the input element. 
   */
  public void init(IJavaElement jelem) {

    WizardDialog container = (WizardDialog) getWizard().getContainer();
    IRunnableContext context = (IRunnableContext) container;

    fContainerDialogField.init(jelem, context);
    fPackageDialogField.init(fContainerDialogField, context);
    IPackageFragment pack = null;
    if (jelem != null) {
      pack = (IPackageFragment) Utils.findElementOfKind(jelem, IJavaElement.PACKAGE_FRAGMENT);
    }
    fPackageDialogField.setPackageFragment(pack);
    fComponentNameDialog.setTextValue("");
    fComponentNameDialog.init(fPackageDialogField);
    fAutoAddField.init(jelem, fComponentNameDialog, getWizard().getClass() == NewTapComponentWizard.class);
  }

  /**
   * @see DialogPage#createControl(Composite)
   */
  public void createControl(Composite container) {

    Composite composite = new Composite(container, SWT.NONE);

    FormLayout layout = new FormLayout();
    layout.marginWidth = 4;
    layout.marginHeight = 4;
    composite.setLayout(layout);

    FormData formData = new FormData();
    formData.top = new FormAttachment(0, 0);
    formData.left = new FormAttachment(0, 0);
    formData.width = 400;
    composite.setLayoutData(formData);

    Control nameFieldControl = fComponentNameDialog.getControl(composite);
    Control containerFieldControl = fContainerDialogField.getControl(composite);
    Control packageFieldControl = fPackageDialogField.getControl(composite);
    Control autoAddLabelControl = fAutoAddLabel.getControl(composite);
    Control autoAddControl = fAutoAddField.getControl(composite);
    Control genHTML = fGenerateHTML.getControl(composite);
    Control labelControl = fNextLabel.getControl(composite);

    addControl(nameFieldControl, composite, 10);
    Control separator = createSeparator(composite, nameFieldControl);

    addControl(containerFieldControl, separator, 4);
    addControl(packageFieldControl, containerFieldControl, 4);

    separator = createSeparator(composite, packageFieldControl);

    addControl(autoAddLabelControl, separator, 4);
    addControl(autoAddControl, autoAddLabelControl, 4);

    separator = createSeparator(composite, autoAddControl);

    addControl(genHTML, separator, 10);

    addControl(labelControl, genHTML, 50);

    setControl(composite);
    setFocus();

    IPreferenceStore pstore = TapestryPlugin.getDefault().getPreferenceStore();
    boolean autoAdd = pstore.getBoolean(P_ENABLE_ADD_TO_APPLICATION);
    fAutoAddLabel.setCheckBoxValue(autoAdd);
    fAutoAddField.setEnabled(autoAdd);
    fAutoAddField.updateStatus();
    fGenerateHTML.setCheckBoxValue(pstore.getBoolean(P_GENERATE_HTML));
    updateStatus();

  }

  /**
   * @see NewElementWizardPage#getRunnable()
   */
  public IRunnableWithProgress getRunnable(IType specClass) {
    final IType useClass = specClass;
    return new IRunnableWithProgress() {
      public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        try {
          if (monitor == null) {
            monitor = new NullProgressMonitor();
          }
          createComponentResource(new SubProgressMonitor(monitor, 1), useClass);
          if (fGenerateHTML.getCheckBoxValue()) {
            createHTMLResource(new SubProgressMonitor(monitor, 1));
          }
          monitor.done();
        } catch (CoreException e) {
          throw new InvocationTargetException(e);
        }
      }
    };
  }

  /**
  * Method getAutoAddRunnable.
  * @return IRunnableWithProgress
  */
  public IRunnableWithProgress getAutoAddRunnable() {
    IPackageFragment frag = fPackageDialogField.getPackageFragment();
    String componentName = fComponentNameDialog.getTextValue();
    String componentTapestryPath = null;
    if (frag.isDefaultPackage()) {
      componentTapestryPath = "/" + componentName + ".jwc";
    } else {
      componentTapestryPath = ("/" + frag.getElementName() + "/").replace('.', '/') + componentName + ".jwc";
    }
	final boolean addingNewComponent = getWizard().getClass() == NewTapComponentWizard.class;
    final boolean doAutoAdd = fAutoAddLabel.getCheckBoxValue();
    final TapestryApplicationModel useSelectedModel = fAutoAddField.getSelectedModel();
    final String useTapestryPath = componentTapestryPath;
    final String useComponentName = componentName;
    final Shell shell = this.getShell();
    return new IRunnableWithProgress() {
      public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        if (monitor == null) {
          monitor = new NullProgressMonitor();
        }
        if (doAutoAdd) {
          SpindleMultipageEditor targetEditor = (SpindleMultipageEditor) Utils.getEditorFor(useSelectedModel);

          if (!checkSaveEditor(targetEditor)) {
            MessageDialog.openInformation(shell, MessageUtil.getString(PAGE_NAME + ".autoAddNotPossible"),
            //"AutoAdd not possible",
            MessageUtil.getFormattedString(PAGE_NAME + ".autoAddParseError", useSelectedModel.getUnderlyingStorage().getName())
            //              "A parse error occured while saving "
            //                + useSelectedModel.getUnderlyingStorage().getName()                          
            //                + ".\n The component will be created without adding it to the app."
            );
            return;
          }

          PluginApplicationSpecification spec = useSelectedModel.getApplicationSpec();
          boolean exists = false;
          if (addingNewComponent) {
          	exists = spec.getComponentAlias(useComponentName) != null;
          } else {
          	exists = spec.getPageSpecification(useComponentName) != null;
          }
          if (exists) {
            MessageDialog
              .openInformation(
                shell,
                MessageUtil.getString(PAGE_NAME + ".autoAddNotPossible"),
                MessageUtil.getFormattedString(
                  PAGE_NAME + "autoAddAlreadyExisits",
                  new Object[] { useComponentName, useSelectedModel.getUnderlyingStorage().getName()})
            //              "The component "
            //                + useComponentName
            //                + " already exists in "
            //                + useSelectedModel.getUnderlyingStorage().getName()
            //                + ".\n The component will be created without adding it to the app."
            );
            return;
          }
          if (addingNewComponent) {
            spec.setComponentAlias(useComponentName, useTapestryPath);
          } else {
            spec.setPageSpecification(useComponentName, new PluginPageSpecification(useTapestryPath));
          }
          if (targetEditor == null) {
            TapestryPlugin.openTapestryEditor(useSelectedModel);
          }
          if (targetEditor != null) {
            useSelectedModel.setOutOfSynch(true);
            targetEditor.showPage(SpindleMultipageEditor.SOURCE_PAGE);
            targetEditor.fireSaveNeeded();
          }

        }
      };
    };
  }

  private boolean checkSaveEditor(SpindleMultipageEditor targetEditor) throws InterruptedException {
    TapestryComponentModel model = null;
    if (targetEditor != null && targetEditor.isDirty()) {

      RequiredSaveEditorAction saver = new RequiredSaveEditorAction(targetEditor);
      if (!saver.save()) {
        throw new InterruptedException();
      }
      model = (TapestryComponentModel) targetEditor.getModel();

      if (!model.isLoaded()) {
        return false;
      }
    }
    return true;
  }

  public boolean performFinish() {
    IPreferenceStore pstore = TapestryPlugin.getDefault().getPreferenceStore();
    boolean autoAdd = fAutoAddLabel.getCheckBoxValue();
    pstore.setValue(P_ENABLE_ADD_TO_APPLICATION, autoAdd);
    pstore.setValue(P_GENERATE_HTML, fGenerateHTML.getCheckBoxValue());
    if (autoAdd) {
      TapestryApplicationModel model = fAutoAddField.getSelectedModel();
      IPackageFragment spackage = fAutoAddField.getSelectedPackage();
      if (model != null) {
        String packageName = "." + spackage.getElementName() + ".";
        packageName = packageName.replace('.', '/');
        pstore.setValue(P_ADD_TO_APPLICATION, packageName + model.getUnderlyingStorage().getName());
      }
    }
    return true;
  }

  public void createComponentResource(IProgressMonitor monitor, IType specClass) throws InterruptedException, CoreException {

    IPackageFragmentRoot root = fContainerDialogField.getPackageFragmentRoot();
    IPackageFragment pack = fPackageDialogField.getPackageFragment();
    String compname = fComponentNameDialog.getTextValue();
    component = ComponentFactory.createComponent(root, pack, compname, specClass, monitor);
  }

  public void createHTMLResource(IProgressMonitor monitor) throws InterruptedException, CoreException {
    IPackageFragmentRoot root = fContainerDialogField.getPackageFragmentRoot();
    IPackageFragment pack = fPackageDialogField.getPackageFragment();
    String componentName = fComponentNameDialog.getTextValue();

    IContainer container = (IContainer) pack.getUnderlyingResource();
    IFile file1 = container.getFile(new Path(componentName + ".html"));
    IFile file2 = container.getFile(new Path(componentName + ".htm"));

    if (file1.exists() || file2.exists()) {
      return;
    }

    monitor.beginTask("", 10);
    if (pack == null) {
      pack = root.getPackageFragment("");
    }
    if (!pack.exists()) {
      String packName = pack.getElementName();
      pack = root.createPackageFragment(packName, true, null);
      pack.save(new SubProgressMonitor(monitor, 1), true);
    }
    monitor.worked(1);

    InputStream contents = new ByteArrayInputStream(MessageUtil.getString(PAGE_NAME + ".genHTMLSource").getBytes());
    file1.create(contents, false, new SubProgressMonitor(monitor, 1));
    monitor.worked(1);
    monitor.done();

  }

  public IFile getComponent() {
    return component;
  }

  protected void setFocus() {
    fComponentNameDialog.setFocus();
  }

  private void checkEnabled(IStatus status) {
    boolean flag = status.isOK();
    fContainerDialogField.setEnabled(flag);
    fPackageDialogField.setEnabled(flag);
    boolean autoAdd = fAutoAddLabel.getCheckBoxValue();
    fAutoAddField.setEnabled(autoAdd);   

  }

  public void updateStatus() {
    super.updateStatus();
    checkEnabled(fComponentNameDialog.getStatus());
  }

  private class FieldEventsAdapter implements IDialogFieldChangedListener {

    public void dialogFieldChanged(DialogField field) {
      updateStatus();
      if (field == fAutoAddLabel) {
      	System.out.println("Boo");
      }
    }
    /**
     * @see IDialogFieldChangedListener#dialogFieldButtonPressed(DialogField)
     */
    public void dialogFieldButtonPressed(DialogField field) {
    }

    /**
     * @see IDialogFieldChangedListener#dialogFieldStatusChanged(IStatus, DialogField)
     */
    public void dialogFieldStatusChanged(IStatus status, DialogField field) {       
    	if (field == fAutoAddField) {
    		updateStatus();
    	}
    	
    }

  }

  /**
   * @see IWizardPage#canFlipToNextPage()
   */
  public boolean canFlipToNextPage() {
    return getCurrentStatus().isOK();

  }

  public String getChosenComponentName() {
    return fComponentNameDialog.getTextValue();
  }

  public DialogField getComponentNameField() {
    return fComponentNameDialog;
  }

  /**
   * Method getComponentContainerField.
   * @return DialogField
   */
  public DialogField getComponentContainerField() {
    return fContainerDialogField;
  }

  /**
   * Method getChosenContainer.
   * @return String
   */
  public String getChosenContainer() {
    return fContainerDialogField.getTextValue();
  }

  /**
   * Method getChoosenPackage.
   * @return String
   */
  public String getChoosenPackage() {
    return fPackageDialogField.getTextValue();
  }

  /**
   * Method getComponentPackageField.
   * @return DialogField
   */
  public DialogField getComponentPackageField() {
    return fPackageDialogField;
  }

}