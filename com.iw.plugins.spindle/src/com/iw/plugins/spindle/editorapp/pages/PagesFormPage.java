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
package com.iw.plugins.spindle.editorapp.pages;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.update.ui.forms.internal.AbstractSectionForm;
import org.eclipse.update.ui.forms.internal.SectionChangeManager;

import com.iw.plugins.spindle.MessageUtil;
import com.iw.plugins.spindle.editors.SpindleForm;
import com.iw.plugins.spindle.editors.SpindleFormPage;
import com.iw.plugins.spindle.editors.SpindleMultipageEditor;
import com.iw.plugins.spindle.model.TapestryApplicationModel;
import com.iw.plugins.spindle.util.IStimulatable;

public class PagesFormPage extends SpindleFormPage {

  private IStimulatable form;

  public PagesFormPage(SpindleMultipageEditor editor, String title) {
    super(editor, title);
  }

  /**
   * @see PDEFormPage#createForm()
   */
  protected AbstractSectionForm createForm() {
    PagesForm pform = new PagesForm(this);
    form = (IStimulatable) pform;
    return pform;
  }

  /**
   * @see PDEFormPage#createContentOutlinePage()
   */
  public IContentOutlinePage createContentOutlinePage() {
    return null;
  }

  public void openTo(Object object) {
    form.stimulate(object);
  }

  public class PagesForm extends SpindleForm implements IStimulatable {

    PagesSection pageSection;
    PagesSummarySection summarySection;

    public PagesForm(SpindleFormPage page) {
      super(page);
    }

    public void initialize(Object modelObject) {
      super.initialize(modelObject);
      if (hasBeenInitialized()) {
        TapestryApplicationModel model = (TapestryApplicationModel) modelObject;
        String name = model.getApplicationSpec().getName();
        if (model.isEditable() == false) {
          name = MessageUtil.getFormattedString("TapistryComponentsForm.readonly", name);
        }
        setHeadingText("Application Pages");
        ((Composite) getControl()).layout(true);
      }
    }

    public void stimulate(Object stimulus) {
      pageSection.setSelection((String) stimulus);
    }

    protected void createFormClient(Composite parent) {
      GridLayout layout = new GridLayout();
      layout.numColumns = 2;
      layout.marginWidth = 10;
      layout.horizontalSpacing = 15;
      //layout.makeColumnsEqualWidth=true;
      parent.setLayout(layout);

      GridData gd;
      Control control;

      Composite leftColumn = getFactory().createComposite(parent);
      gd = new GridData(GridData.FILL_VERTICAL);
      leftColumn.setLayoutData(gd);
      GridLayout leftLayout = new GridLayout();
      leftLayout.verticalSpacing = 10;
      leftLayout.marginWidth = 0;
      leftColumn.setLayout(leftLayout);

      Composite rightColumn = getFactory().createComposite(parent);
      gd = new GridData(GridData.FILL_BOTH);
      rightColumn.setLayoutData(gd);
      GridLayout rightLayout = new GridLayout();
      rightLayout.verticalSpacing = 10;
      rightLayout.marginWidth = 0;
      rightColumn.setLayout(rightLayout);

      pageSection = new PagesSection((SpindleFormPage) page);
      control = pageSection.createControl(leftColumn, getFactory());
      gd = new GridData(GridData.FILL_BOTH | GridData.VERTICAL_ALIGN_BEGINNING);
      gd.verticalSpan = 75;
      control.setLayoutData(gd);

      summarySection = new PagesSummarySection((SpindleFormPage) page);
      control = summarySection.createControl(rightColumn, getFactory());
      gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
      control.setLayoutData(gd);

      registerSection(pageSection);
      registerSection(summarySection);

      SectionChangeManager manager = new SectionChangeManager();
      manager.linkSections(pageSection, summarySection);

    }

  }

}