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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.pde.internal.ui.editor.FormOutlinePage;
import org.eclipse.pde.internal.ui.editor.IPDEEditorPage;
import org.eclipse.pde.internal.ui.editor.FormOutlinePage.BasicLabelProvider;
import org.eclipse.swt.graphics.Image;

import com.iw.plugins.spindle.model.BaseTapestryModel;

public abstract class SpindleFormOutlinePage extends FormOutlinePage {

  /**
   * Constructor for TapestryFormOutlinePage
   */
  public SpindleFormOutlinePage(SpindleFormPage page) {
    super(page);
  }

  protected ILabelProvider createLabelProvider() {
    return new OutlineLabelProvider();
  }

  public String getObjectLabel(Object obj) {
    if (obj instanceof BaseTapestryModel) {
      return ((BaseTapestryModel) obj).getUnderlyingStorage().getFullPath().toString();
    }
    return obj.toString();
  }

  protected Image getObjectImage(Object obj) {
    if (obj instanceof SpindleFormPage) {
      return getPageImage((SpindleFormPage) obj);
    }
    return null;
  }

  protected Image getPageImage(SpindleFormPage page) {
    return null;
  }

  protected Object[] getObjects(Collection labels, SpindleFormPage page) {
    if (labels != null && !labels.isEmpty()) {
      ArrayList result = new ArrayList();
      Iterator iter = new TreeSet(labels).iterator();
      while (iter.hasNext()) {
        Holder holder = new Holder();
        holder.label = (String) iter.next();
        holder.page = page;
        result.add(holder);
      }
      return result.toArray();
    }
    return new Object[0];
  }

  public IPDEEditorPage getParentPage(Object item) {
    if (item instanceof SpindleFormPage) {
      return (SpindleFormPage) item;
    }
    if (item instanceof Holder) {
      return ((Holder) item).page;
    }
    return null;
  }

  public void selectionChanged(Object item) {
  	IPDEEditorPage page = formPage.getEditor().getCurrentPage();    
    SpindleFormPage newPage = (SpindleFormPage)getParentPage(item);
    if (newPage != page) {
      formPage.getEditor().showPage(newPage);
    }
    if (newPage != item) {
      newPage.openTo(((Holder) item).label);
    }
  }

  class OutlineLabelProvider extends BasicLabelProvider {
    public String getText(Object obj) {
      String label = getObjectLabel(obj);
      if (label != null)
        return label;
      return super.getText(obj);
    }

    public Image getImage(Object obj) {
      Image image = getObjectImage(obj);
      if (image != null)
        return image;
      if (obj instanceof SpindleFormPage) {
        image = getPageImage((SpindleFormPage) obj);
        if (image != null)
          return image;
      }
      return super.getImage(obj);
    }
  }

  public class Holder {
    public Holder() {
    }

    public String label;
    public Object element;
    public SpindleFormPage page;
    public String toString() {
      return label;
    }
  }

 
}