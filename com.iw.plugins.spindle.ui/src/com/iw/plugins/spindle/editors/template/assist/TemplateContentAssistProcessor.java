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

package com.iw.plugins.spindle.editors.template.assist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.tapestry.parse.TemplateParser;
import org.eclipse.core.resources.IStorage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.xmen.xml.XMLNode;

import com.iw.plugins.spindle.Images;
import com.iw.plugins.spindle.PreferenceConstants;
import com.iw.plugins.spindle.UIPlugin;
import com.iw.plugins.spindle.editors.Editor;
import com.iw.plugins.spindle.editors.UITapestryAccess;
import com.iw.plugins.spindle.editors.assist.AbstractContentAssistProcessor;
import com.iw.plugins.spindle.editors.assist.DTDProposalGenerator;
import com.iw.plugins.spindle.editors.assist.ProposalFactory;
import com.iw.plugins.spindle.editors.template.TemplateEditor;
import com.wutka.dtd.DTD;

/**
 * Base class for Template completion assist. Basicly here to set a flag if
 * XHTML completions have been requested via the property page.
 * 
 * @author glongman@intelligentworks.com
 */
public abstract class TemplateContentAssistProcessor
    extends
      AbstractContentAssistProcessor
{

  public static List getWebProposals(
      DTD dtd,
      IDocument document,
      int completionOffset,
      int completionLength,
      String tagName,
      List excludeNames,
      HashSet existingAttributeNames,
      String prefix,
      boolean addLeadingSpace)
  {

    List webAttributeNames;

    if (dtd != null && tagName != null)
    {
      webAttributeNames = DTDProposalGenerator.getAttributes(dtd, tagName);
    } else
    {
      return Collections.EMPTY_LIST;
    }

    List result = new ArrayList();

    boolean ignorePrefix = prefix == null || prefix.trim().length() == 0;
    
    List requiredAttributes = DTDProposalGenerator.getRequiredAttributes(dtd, tagName);

    for (Iterator iter = webAttributeNames.iterator(); iter.hasNext();)
    {
      String attrname = ((String) iter.next()).toLowerCase();

      if (existingAttributeNames.contains(attrname))
        continue;

      boolean match = true;
      if (!ignorePrefix)
        match = attrname.startsWith(prefix);

      if (match && !excludeNames.contains(attrname))
      {
        result.add(ProposalFactory.getElementAttributeProposal(
            document,
            attrname,
            completionOffset,
            completionLength,
            addLeadingSpace,
            requiredAttributes.contains(attrname)? Images.getSharedImage("bullet_pink.gif") : Images.getSharedImage("bullet_web.gif"),
            null,
            requiredAttributes.contains(attrname)? 98: 99));
      }
    }
    return result;
  }

  public static List getParameterProposals(
      TemplateEditor editor,
      IDocument document,
      int completionOffset,
      int completionLength,
      String prefix,
      String jwcid,
      HashSet existingAttributeNames,
      List usedNames,
      boolean addLeadingSpace)
  {
    ArrayList result = new ArrayList();
    TemplateTapestryAccess helper = new TemplateTapestryAccess(editor);
    helper.setJwcid(jwcid);

    UITapestryAccess.Result [] infos = null;
    try
    {
      infos = helper.findParameters(prefix, existingAttributeNames);
    } catch (IllegalArgumentException e)
    {
      return Collections.EMPTY_LIST;
    }

    boolean ignorePrefix = prefix == null || prefix.trim().length() == 0;

    for (int i = 0; i < infos.length; i++)
    {
      String parameterName = infos[i].name;

      if (existingAttributeNames.contains(parameterName))
        continue;

      if (!ignorePrefix && !parameterName.toLowerCase().startsWith(prefix))
        continue;

      if (usedNames.contains(parameterName))
        continue;

      usedNames.add(parameterName);

      result.add(ProposalFactory.getElementAttributeProposal(
          document,
          parameterName,
          completionOffset,
          completionLength,
          addLeadingSpace,
          (infos[i].required ? Images.getSharedImage("bullet_pink.gif") : Images
              .getSharedImage("bullet.gif")),
          infos[i].description,
          infos[i].required ? 0 : 1));
    }
    return result;
  }

  private boolean fHasHTMLExtension;
  public TemplateContentAssistProcessor(Editor editor)
  {
    super(editor);
    IEditorInput input = editor.getEditorInput();
    IStorage storage = (IStorage) input.getAdapter(IStorage.class);
    fHasHTMLExtension = storage != null && storage.getName().endsWith(".html");
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.iw.plugins.spindle.editors.util.AbstractContentAssistProcessor#connect(org.eclipse.jface.text.IDocument)
   */
  protected void init(IDocument document) throws IllegalStateException
  {
    fDTD = null;
    IPreferenceStore store = UIPlugin.getDefault().getPreferenceStore();
    if (fHasHTMLExtension)
    {
      String dtdString = store
          .getString(PreferenceConstants.TEMPLATE_EDITOR_HTML_SHOW_XHTML);
      if (dtdString.equals(TemplateEditor.XHTML_NONE_LABEL))
        return;

      if (dtdString.equals(TemplateEditor.XHTML_STRICT_LABEL))
      {
        fDTD = TemplateEditor.XHTML_STRICT;
      } else if (dtdString.equals(TemplateEditor.XHTML_TRANSITIONAL_LABEL))
      {
        fDTD = TemplateEditor.XHTML_TRANSITIONAL;
      } else if (dtdString.equals(TemplateEditor.XHTML_FRAMES_LABEL))
      {
        fDTD = TemplateEditor.XHTML_FRAMESET;
      }

    }
  }

  protected String getJwcid(Map attributeMap)
  {
    XMLNode jwcidArt = (XMLNode) attributeMap.get(TemplateParser.JWCID_ATTRIBUTE_NAME);
    if (jwcidArt != null)
      return jwcidArt.getAttributeValue();

    return null;
  }

}