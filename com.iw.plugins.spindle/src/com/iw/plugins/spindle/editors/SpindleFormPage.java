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
package com.iw.plugins.spindle.editors;

import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.update.ui.forms.internal.AbstractSectionForm;
import org.eclipse.update.ui.forms.internal.IFormPage;

import com.iw.plugins.spindle.model.BaseTapestryModel;

public class SpindleFormPage extends PDEFormPage {

  /**
   * Constructor for TapestryFormPage
   */
  public SpindleFormPage(SpindleMultipageEditor arg0, String arg1) {
    super(arg0, arg1);
  }
 
 /** must override
   * @see PDEFormPage#createForm()
   */
  protected AbstractSectionForm createForm() {
  	return null;
  }
  
 /** 
   * @see PDEFormPage#createContentOutlinePage()
   */
  public IContentOutlinePage createContentOutlinePage() {
    return null;
  }  

  public void update() {
  	if (((BaseTapestryModel)getEditor().getModel()).isLoaded()) {
  		super.update();
  	}
  } 
  
  /**
   * @see PDEFormPage#becomesInvisible(IFormPage)
   */
  public boolean becomesInvisible(IFormPage arg0) {
  	super.becomesInvisible(arg0);
  	((SpindleMultipageEditor)getEditor()).resynchDocument(false);
  	BaseTapestryModel model = (BaseTapestryModel)getModel();
   	model.setDirty(false);
  	return true;
  }

}

