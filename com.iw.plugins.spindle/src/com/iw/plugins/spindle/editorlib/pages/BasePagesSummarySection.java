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
package com.iw.plugins.spindle.editorlib.pages;

import net.sf.tapestry.spec.ILibrarySpecification;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.update.ui.forms.internal.FormEntry;
import org.eclipse.update.ui.forms.internal.FormSection;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;

import com.iw.plugins.spindle.TapestryPlugin;
import com.iw.plugins.spindle.editors.SpindleFormPage;
import com.iw.plugins.spindle.editors.SpindleFormSection;
import com.iw.plugins.spindle.editors.SummaryHTMLViewer;
import com.iw.plugins.spindle.editors.SummarySourceViewer;
import com.iw.plugins.spindle.model.ITapestryModel;
import com.iw.plugins.spindle.model.manager.TapestryProjectModelManager;
import com.iw.plugins.spindle.spec.IIdentifiable;
import com.iw.plugins.spindle.util.lookup.TapestryLookup;

public abstract class BasePagesSummarySection extends SpindleFormSection implements IResourceChangeListener {

  private FormEntry pageName;
  private FormEntry specText;
  private Label resolveFailedLabel;
  private SummarySourceViewer sourceViewer;
  private SummarySourceViewer htmlViewer;
  private Composite container;

  private String selectedPage;

  /**
   * Constructor for ComponentAliasSummarySection
   */
  public BasePagesSummarySection(SpindleFormPage page) {
    super(page);
    setHeaderText("Page Summary  (READ ONLY)");
    setDescription("This section will show a summary of the selected Page (if it can be resolved).");
  }

  /**
   * @see FormSection#createClient(Composite, FormWidgetFactory)
   */
  public Composite createClientContainer(Composite parent, FormWidgetFactory factory) {

    container = factory.createComposite(parent);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.verticalSpacing = 7;
    layout.horizontalSpacing = 6;
    container.setLayout(layout);

    String labelName = "Page Name";
    pageName = new FormEntry(createText(container, labelName, factory));
    Text text = (Text) pageName.getControl();
    text.setEditable(false);
    text.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));

    labelName = "Specification";
    specText = new FormEntry(createText(container, labelName, factory));
    text = (Text) specText.getControl();
    text.setEnabled(false);
    text.setEditable(false);
    text.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));

    resolveFailedLabel = factory.createLabel(container, "empty", SWT.WRAP);
    GridData data = new GridData();
    data.horizontalAlignment = GridData.FILL;
    data.horizontalSpan = 2;
    resolveFailedLabel.setLayoutData(data);
    resolveFailedLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
    resolveFailedLabel.setVisible(false);

    SashForm sashForm = new SashForm(container, SWT.NO_TRIM);
    sashForm.setOrientation(SWT.VERTICAL);
    data = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
    data.horizontalSpan = 2;
    data.verticalSpan = 75;
    sashForm.setLayoutData(data);
    sashForm.SASH_WIDTH = 4;

    SpindleFormSection sourceSection = new SourceWidget((SpindleFormPage) getFormPage());

    sourceSection.createControl(sashForm, factory);

    SpindleFormSection templateSection = new HTMLWidget((SpindleFormPage) getFormPage());

    templateSection.createControl(sashForm, factory);



    sashForm.setWeights(new int[] { 1, 1 });

    factory.paintBordersFor(container);
    TapestryPlugin.getDefault().getWorkspace().addResourceChangeListener(this);
    return container;
  }

  class SourceWidget extends SpindleFormSection {

    /**
     * Constructor for SourceWidget.
     * @param page
     */
    public SourceWidget(SpindleFormPage page) {
      super(page);
      setCollapsable(false);
      setHeaderPainted(true);
      setDescriptionPainted(true);
      setHeaderText("Specification Source (read only)");
      setDescription(" ");
    }
    /**
     * @see com.iw.plugins.spindle.ui.SectionWidget#createClient(Composite)
     */
    public Composite createClientContainer(Composite parent, FormWidgetFactory factory) {

      Composite composite = new Composite(parent, SWT.BORDER);
      GridData gd;
      GridLayout layout = new GridLayout();
      layout.marginHeight = 0;
      layout.marginWidth = 0;
      composite.setLayout(layout);
      gd = new GridData(GridData.FILL_BOTH);
      composite.setLayoutData(gd);

      sourceViewer = new SummarySourceViewer(composite);
      gd = new GridData(gd.FILL_BOTH | gd.GRAB_HORIZONTAL);
      gd.horizontalSpan = 2;
      sourceViewer.getControl().setLayoutData(gd);
      sourceViewer.setPopupListener(new IMenuListener() {
        public void menuAboutToShow(IMenuManager manager) {
          manager.removeAll();
          Action openAction = new Action("Open") {
            public void run() {
              openTapestryEditor(sourceViewer.getCurrentStorage());
            }
          };
          openAction.setEnabled(sourceViewer.getCurrentStorage() != null);
          manager.add(openAction);
        }
      });

      return composite;
    }

  }

  class HTMLWidget extends SpindleFormSection {

    /**
     * Constructor for SourceWidget.
     * @param page
     */
    public HTMLWidget(SpindleFormPage page) {
      super(page);
      setCollapsable(false);
      setHeaderPainted(true);
      setDescriptionPainted(true);
      setHeaderText("Template Source (read only)");
      setDescription(" ");
    }
    /**
     * @see com.iw.plugins.spindle.ui.SectionWidget#createClient(Composite)
     */
    public Composite createClientContainer(Composite parent, FormWidgetFactory factory) {

      Composite composite = new Composite(parent, SWT.BORDER);
      GridData gd;
      GridLayout layout = new GridLayout();
      layout.marginHeight = 0;
      layout.marginWidth = 0;
      composite.setLayout(layout);
      gd = new GridData(GridData.FILL_BOTH);
      composite.setLayoutData(gd);

      htmlViewer = new SummaryHTMLViewer(composite);
      gd = new GridData(gd.FILL_BOTH | gd.GRAB_HORIZONTAL);

      htmlViewer.getControl().setLayoutData(gd);
      htmlViewer.setPopupListener(new IMenuListener() {
        public void menuAboutToShow(IMenuManager manager) {
          manager.removeAll();
          Action openAction = new Action("Open") {
            public void run() {
              openHtmlEditor(htmlViewer.getCurrentStorage());
            }
          };
          openAction.setEnabled(htmlViewer.getCurrentStorage() != null);
          manager.add(openAction);
        }
      });

      return composite;
    }

  }

  private void openTapestryEditor(IStorage storage) {
    if (storage != null) {
      TapestryPlugin.openTapestryEditor(storage);
    }
  }

  private void openHtmlEditor(IStorage storage) {
    if (storage != null) {
      TapestryPlugin.openNonTapistryEditor(storage);
    }
  }

  protected ITapestryModel getModel() {
    return ((ITapestryModel) getFormPage().getModel());
  }

  public void dispose() {
    TapestryPlugin.getDefault().getWorkspace().removeResourceChangeListener(this);
    super.dispose();
  }

  protected abstract ILibrarySpecification getSpec();

  public void sectionChanged(FormSection source, int changeType, Object changeObject) {

    if (changeObject instanceof IIdentifiable) {

      selectedPage = ((IIdentifiable) changeObject).getIdentifier();

    } else {

      selectedPage = (String) changeObject;
    }
    updateSummary();
  }

  private void updateSummary() {

    resolveFailedLabel.setVisible(false);

    String specificationPath = "";

    if (selectedPage != null) {

      specificationPath = getSpec().getPageSpecificationPath(selectedPage);

      if (specificationPath == null) {

        // we must resolve it from Framework.library
        TapestryProjectModelManager mgr;
        try {
          mgr = TapestryPlugin.getDefault().getTapestryModelManager(getModel().getUnderlyingStorage());

          ILibrarySpecification framework = mgr.getDefaultLibrary().getSpecification();

          specificationPath = framework.getPageSpecificationPath(selectedPage);
        } catch (CoreException e) {
        }
      }
    }

    if (selectedPage == null) {

      pageName.setValue("", false);
      specText.setValue("", false);
      sourceViewer.updateNotFound();
      htmlViewer.updateNotFound();

    } else {

      TapestryLookup lookup = new TapestryLookup();

      pageName.setValue(selectedPage, false);

      specText.setValue(specificationPath, false);

      if (specificationPath == null || !(specificationPath.endsWith(".jwc") || specificationPath.endsWith(".page"))) {

        resolveFailed(specificationPath);
        return;
      }

      ITapestryModel model = (ITapestryModel) getModel();
      IStorage thisStorage = model.getUnderlyingStorage();
      try {

        IJavaProject jproject = TapestryPlugin.getDefault().getJavaProjectFor(thisStorage);
        if (jproject == null) {

          resolveFailed(specificationPath);

        }

        lookup.configure(jproject);

      } catch (CoreException jmex) {

        resolveFailed(specificationPath);

      }

      IStorage[] found = lookup.findComponent(specificationPath);

      if (found == null || found.length == 0) {

        found = lookup.findPage(specificationPath);

      }

      if (found == null || found.length != 1) {

        resolveFailed(specificationPath);
        return;

      }
      sourceViewer.update(null, found[0]);
      IStorage[] htmlStorage = lookup.findHtmlFor(specificationPath);

      if (htmlStorage.length != 1) {

        htmlViewer.updateNotFound();
        return;

      }
      htmlViewer.update(null, htmlStorage[0]);
      container.layout(true);
      container.redraw();
    }
  }

  private void resolveFailed(String name) {
    resolveFailedLabel.setText("Could not resolve: " + name);
    resolveFailedLabel.setVisible(true);
    sourceViewer.updateNotFound();
    htmlViewer.updateNotFound();
  }

  /**
  * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(IResourceChangeEvent)
  */
  public void resourceChanged(IResourceChangeEvent event) {
    if (selectedPage == null)
      return;

    updateSummary();
  }

}