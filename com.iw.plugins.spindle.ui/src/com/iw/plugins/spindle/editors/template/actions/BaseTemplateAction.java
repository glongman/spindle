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
 * Portions created by the Initial Developer are Copyright (C) 2003
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * 
 *  glongman@intelligentworks.com
 *
 * ***** END LICENSE BLOCK ***** */

package com.iw.plugins.spindle.editors.template.actions;

import org.apache.tapestry.INamespace;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IDocument;

import com.iw.plugins.spindle.UIPlugin;
import com.iw.plugins.spindle.editors.actions.BaseEditorAction;

/**
 * Base class for spec actions that need the xml partitioning.
 * 
 * @author glongman@intelligentworks.com
 * @version $Id: BaseTemplateAction.java,v 1.4.2.1 2004/06/22 12:23:46 glongman
 *          Exp $
 */
public abstract class BaseTemplateAction extends BaseEditorAction
{

  protected INamespace fNamespace;
  protected INamespace fFrameworkNamespace;

  protected IDocument fDocument;

  public BaseTemplateAction()
  {
    super();
  }

  public BaseTemplateAction(String text)
  {
    super(text);
  }

  public BaseTemplateAction(String text, ImageDescriptor image)
  {
    super(text, image);
  }

  public BaseTemplateAction(String text, int style)
  {
    super(text, style);
  }

  public final void run()
  {
    super.run();

    if (fDocumentOffset < 0)
      return;

    try
    {
      fDocument = fEditor.getDocumentProvider().getDocument(fEditor.getEditorInput());
      if (fDocument.getLength() == 0 || fDocument.get().trim().length() == 0)
        return;
      doRun();

    } catch (RuntimeException e)
    {
      UIPlugin.log(e);
      throw e;
    }

  }

  protected abstract void doRun();

}