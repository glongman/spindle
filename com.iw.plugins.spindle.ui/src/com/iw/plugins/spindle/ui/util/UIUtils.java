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

package com.iw.plugins.spindle.ui.util;

import org.eclipse.core.resources.IStorage;
import org.eclipse.jdt.internal.ui.javaeditor.JarEntryEditorInput;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.rules.DefaultPartitioner;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.xmen.internal.ui.text.ITypeConstants;
import org.xmen.internal.ui.text.XMLDocumentPartitioner;

import com.iw.plugins.spindle.UIPlugin;
import com.iw.plugins.spindle.core.resources.IResourceWorkspaceLocation;
import com.iw.plugins.spindle.editors.XMLContentFormatter;
import com.iw.plugins.spindle.editors.XMLFormattingStrategy;

/**
 * Access to features exposed by the JDT UI plugin
 * 
 * @author glongman@intelligentworks.com
 *  
 */
public class UIUtils
{

  public static XMLDocumentPartitioner createXMLStructurePartitioner()
  {
    return new XMLDocumentPartitioner(
        XMLDocumentPartitioner.createScanner(),
        ITypeConstants.TYPES);
  }

  public static IEditorPart getEditorFor(IResourceWorkspaceLocation location)
  {
    IStorage storage = location.getStorage();
    if (storage != null)
      return UIUtils.getEditorFor(storage);

    return null;
  }

  public static IEditorPart getEditorFor(IStorage storage)
  {

    IWorkbench workbench = UIPlugin.getDefault().getWorkbench();
    IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();

    for (int i = 0; i < windows.length; i++)
    {
      IWorkbenchPage[] pages = windows[i].getPages();
      for (int x = 0; x < pages.length; x++)
      {

        IEditorReference[] editors = pages[x].getEditorReferences();

        for (int z = 0; z < editors.length; z++)
        {

          IEditorReference ref = editors[z];
          IEditorPart editor = ref.getEditor(true);

          if (editor == null)
          {
            continue;
          }

          IEditorInput input = editor.getEditorInput();
          IStorage editorStorage;
          if (input instanceof JarEntryEditorInput)
          {
            editorStorage = ((JarEntryEditorInput) input).getStorage();
          } else
          {
            editorStorage = (IStorage) input.getAdapter(IStorage.class);
          }

          if (editorStorage != null && editorStorage.equals(storage))
            return editor;

        }
      }
    }
    return null;
  }

  public static String findWordString(IDocument document, int offset)
  {
    try
    {
      IRegion region = findWord(document, offset);
      if (region != null)
        return document.get(region.getOffset(), region.getLength());
    } catch (BadLocationException e)
    {
      //do nothing
    }
    return null;
  }

  public static IRegion findWord(IDocument document, int offset)
  {

    int start = -1;
    int end = -1;

    try
    {

      int pos = offset;
      char c;

      while (pos >= 0)
      {
        c = document.getChar(pos);
        if (!Character.isJavaIdentifierPart(c))
          break;
        --pos;
      }

      start = pos;

      pos = offset;
      int length = document.getLength();

      while (pos < length)
      {
        c = document.getChar(pos);
        if (!Character.isJavaIdentifierPart(c))
          break;
        ++pos;
      }

      end = pos;

    } catch (BadLocationException x)
    {}

    if (start > -1 && end > -1)
    {
      if (start == offset && end == offset)
        return new Region(offset, 0);
      else if (start == offset)
        return new Region(start, end - start);
      else
        return new Region(start + 1, end - start - 1);
    }
    return null;
  }

  //TODO remove and replace
  /**
   * @deprecated
   */
  public static void XMLFormatDocument(IDocument document)
  {
    XMLContentFormatter formatter = new XMLContentFormatter(
        new XMLFormattingStrategy(),
        new String[] { DefaultPartitioner.CONTENT_TYPES_CATEGORY },
        UIPlugin.getDefault().getPreferenceStore());
    formatter.format(document, new Region(0, document.getLength()));
  }

  /**
   * Used to layout controls vertically in a FormLayout. Sets the layout data.
   * 
   * @param toBeAdded the Control to be added
   * @param parent the parent Control
   * @param verticalOffset an int hint for spacing.
   */
  public static void addFormControl(Control toBeAdded, Control parent, int verticalOffset)
  {
    FormData formData = new FormData();
    formData.top = new FormAttachment(parent, verticalOffset);
    formData.left = new FormAttachment(0, 0);
    formData.right = new FormAttachment(100, 0);
    toBeAdded.setLayoutData(formData);
  }

  /**
   * Used to layout controls vertically in a FormLayout. Sets the layout data.
   * 
   * @param toBeAdded the Control to be added
   * @param parent the parent Control
   */
  public static void addFormControl(Control toBeAdded, Control parent)
  {
    addFormControl(toBeAdded, parent, 0);
  }

  /**
   * Adds a new separator (next in vertical layout) in a composite that uses
   * FormLayout
   * 
   * @param container the container
   * @param parent the parent Control we will place the separator after.
   * @return the separator with correct layout data.
   */
  public static Control createFormSeparator(Composite container, Control parent)
  {
    Label separator = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
    FormData formData = new FormData();
    if (parent != null)
    {
      formData.top = new FormAttachment(parent, 10);
    }
    formData.left = new FormAttachment(0, 0);
    formData.right = new FormAttachment(100, 0);
    separator.setLayoutData(formData);
    return separator;
  }

}