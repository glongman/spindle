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
 * Geoffrey Longman.
 * Portions created by the Initial Developer are Copyright (C) 2001-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * 
 *  glongman@gmail.com
 *
 * ***** END LICENSE BLOCK ***** */
package com.iw.plugins.spindle.ui.dialogfields;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.iw.plugins.spindle.core.util.Assert;

/**
 * @author GWL
 * @version Copyright 2002, Intelligent Works Incoporated All Rights Reserved
 */
public class StringField extends DialogField
{

  private Text textControl;

  public StringField(String label)
  {
    super(label);
  }

  public StringField(String label, int labelWidth)
  {
    super(label, labelWidth);
  }

  public boolean isVisible()
  {
    return super.isVisible() && textControl.isVisible();
  }

  public void setVisible(boolean flag)
  {
    super.setVisible(flag);
    if (textControl != null && !textControl.isDisposed())
      textControl.setVisible(flag);
  }

  public Control getControl(Composite parent)
  {

    Composite container = new Composite(parent, SWT.NULL);
    FormLayout layout = new FormLayout();
    container.setLayout(layout);

    Label labelControl = getLabelControl(container);
    FormData formData = new FormData();
    formData.width = getLabelWidth();
    formData.top = new FormAttachment(0, 5);
    formData.left = new FormAttachment(0, 0);
    labelControl.setLayoutData(formData);

    Text textControl = getTextControl(container);
    formData = new FormData();
    formData.top = new FormAttachment(0, 3);
    formData.left = new FormAttachment(labelControl, 4);
    formData.right = new FormAttachment(100, 0);
    textControl.setLayoutData(formData);

    return container;
  }

  public void fillIntoGrid(Composite parent, int numcols)
  {
    super.fillIntoGrid(parent, numcols);
    numcols -= 1;
    Assert.isTrue(numcols >= 1);

    Text textControl = getTextControl(parent);
    GridData data = new GridData(GridData.FILL_HORIZONTAL);
    data.horizontalSpan = numcols;
    textControl.setLayoutData(data);
  }

  public Text getTextControl(Composite parent)
  {
    if (textControl == null)
    {
      final DialogField field = this;
      textControl = new Text(parent, SWT.BORDER);
      textControl.setFont(parent.getFont());
      textControl.addModifyListener(new ModifyListener()
      {

        public void modifyText(ModifyEvent e)
        {
          fireDialogFieldChanged(field);
        }

      });

    }
    return textControl;
  }

  public void setTextValue(String value)
  {
    setTextValue(value, true);
  }

  public void setTextValue(String value, boolean update)
  {
    if (textControl != null && !textControl.isDisposed())
    {
      textControl.setText(value == null ? "" : value);
      if (update)
      {
        fireDialogFieldChanged(this);
      }
    }
  }

  public String getTextValue()
  {
    if (textControl != null && !textControl.isDisposed())
    {
      return textControl.getText();
    }
    return null;
  }

  public void setEnabled(boolean flag)
  {
    if (textControl != null && !textControl.isDisposed())
    {
      textControl.setEnabled(flag);
    }
    super.setEnabled(flag);
  }

  public boolean setFocus()
  {
    if (textControl != null && !textControl.isDisposed())
      return textControl.setFocus();

    return false;
  }

}