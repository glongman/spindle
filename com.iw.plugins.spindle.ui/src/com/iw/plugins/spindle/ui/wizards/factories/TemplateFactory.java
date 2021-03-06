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
 * Portions created by the Initial Developer are Copyright (C) 2004
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * 
 *  glongman@gmail.com
 *
 * ***** END LICENSE BLOCK ***** */
package com.iw.plugins.spindle.ui.wizards.factories;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.TemplateTranslator;
import org.eclipse.jface.text.templates.TemplateVariable;
import org.eclipse.jface.text.templates.TemplateVariableResolver;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.RangeMarker;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import com.iw.plugins.spindle.core.util.XMLUtil;
import com.iw.plugins.spindle.editors.assist.usertemplates.UserTemplateAccess;
import com.iw.plugins.spindle.editors.assist.usertemplates.XMLFileContextType;

/**
 * TemplateFactory factory for creating file contents from Templates.
 * 
 * @author glongman@gmail.com
 *  
 */
public class TemplateFactory
{

  class DefaultResolver extends TemplateVariableResolver
  {
    public DefaultResolver(String type, String description)
    {
      super(type, description);
    }
  }

  final class DefaultContext extends TemplateContext
  {
    public DefaultContext()
    {
      super(null);
    }
    //not called
    public boolean canEvaluate(Template template)
    {
      return true;
    }
    //not called
    public TemplateBuffer evaluate(Template template) throws BadLocationException,
        TemplateException
    {
      if (!fTemplateContextId.equals(template.getContextTypeId()))
        throw new TemplateException("wrong context!");

      TemplateTranslator translator = new TemplateTranslator();
      TemplateBuffer buffer = translator.translate(template);

      resolve(buffer, this);
      return buffer;
    }
  }

  public static List getAllTemplates(TemplateFactory factory)
  {
    return getAllTemplates(factory.fTemplateContextId);
  }

  public static List getAllTemplates(String templateContextId)
  {
    return Arrays.asList(UserTemplateAccess.getDefault().getTemplateStore().getTemplates(
        templateContextId));
  }

  private String fTemplateContextId;
  /** Variable resolvers used by this content factory. */
  private final Map fResolvers = new HashMap();

  TemplateFactory(String templateContextId)
  {
    fTemplateContextId = templateContextId;
  }

  public String getTemplateContextId()
  {
    return fTemplateContextId;
  }

  protected String getGeneratedContent(
      Template template,
      TemplateContext context,
      boolean format) throws BadLocationException, TemplateException
  {
    TemplateBuffer buffer = context.evaluate(template);
    String content = buffer.getString();
    if (format)
      content = XMLUtil.fomat(content);

    return content;
  }

  protected TemplateContext createTemplateContext()
  {
    return new DefaultContext();
  }

  private void resolve(TemplateBuffer buffer, TemplateContext context) throws MalformedTreeException,
      BadLocationException
  {

    TemplateVariable[] variables = buffer.getVariables();

    List positions = variablesToPositions(variables);
    List edits = new ArrayList(5);

    // iterate over all variables and try to resolve them
    for (int i = 0; i != variables.length; i++)
    {
      TemplateVariable variable = variables[i];

      if (variable.isUnambiguous())
        continue;

      // remember old values
      int[] oldOffsets = variable.getOffsets();
      int oldLength = variable.getLength();
      String oldValue = variable.getDefaultValue();

      String type = variable.getType();
      TemplateVariableResolver resolver = (TemplateVariableResolver) fResolvers.get(type);
      if (resolver == null)
        resolver = new DefaultResolver(type, "");
      resolver.resolve(variable, context);

      String value = variable.getDefaultValue();

      if (!oldValue.equals(value))
        // update buffer to reflect new value
        for (int k = 0; k != oldOffsets.length; k++)
          edits.add(new ReplaceEdit(oldOffsets[k], oldLength, value));

    }

    IDocument document = new Document(buffer.getString());
    MultiTextEdit edit = new MultiTextEdit(0, document.getLength());
    edit.addChildren((TextEdit[]) positions.toArray(new TextEdit[positions.size()]));
    edit.addChildren((TextEdit[]) edits.toArray(new TextEdit[edits.size()]));
    edit.apply(document, TextEdit.UPDATE_REGIONS);

    positionsToVariables(positions, variables);

    buffer.setContent(document.get(), variables);

  }

  public void addResolver(TemplateVariableResolver resolver)
  {
    Assert.isNotNull(resolver);
    fResolvers.put(resolver.getType(), resolver);
  }

  protected void addDefaultResolvers()
  {
    addResolver(new GlobalTemplateVariables.Cursor());
    addResolver(new GlobalTemplateVariables.WordSelection());
    addResolver(new GlobalTemplateVariables.LineSelection());
    addResolver(new GlobalTemplateVariables.Dollar());
    addResolver(new GlobalTemplateVariables.Date());
    addResolver(new GlobalTemplateVariables.Year());
    addResolver(new GlobalTemplateVariables.Time());
    addResolver(new GlobalTemplateVariables.User());
  }

  protected void addXMLFileResolvers()
  {
    addResolver(new XMLFileContextType.PublicId());
    addResolver(new XMLFileContextType.PublicIdUrl());
    addResolver(new XMLFileContextType.Encoding());
  }

  private static List variablesToPositions(TemplateVariable[] variables)
  {
    List positions = new ArrayList(5);
    for (int i = 0; i != variables.length; i++)
    {
      int[] offsets = variables[i].getOffsets();
      for (int j = 0; j != offsets.length; j++)
        positions.add(new RangeMarker(offsets[j], 0));
    }

    return positions;
  }

  private static void positionsToVariables(List positions, TemplateVariable[] variables)
  {
    Iterator iterator = positions.iterator();

    for (int i = 0; i != variables.length; i++)
    {
      TemplateVariable variable = variables[i];

      int[] offsets = new int[variable.getOffsets().length];
      for (int j = 0; j != offsets.length; j++)
        offsets[j] = ((TextEdit) iterator.next()).getOffset();

      variable.setOffsets(offsets);
    }
  }

}