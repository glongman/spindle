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
package com.iw.plugins.spindle.editors.assist;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContext;

import org.eclipse.swt.graphics.Image;

/**
 * SpindleTemplateProposal orderable template proposal.
 * 
 * @author glongman@gmail.com
 *  
 */
public class SpindleTemplateProposal extends TemplateProposal 
{

  
  private String fAdditionalInfo;

  public SpindleTemplateProposal(Template template, TemplateContext context,
      IRegion region, String extraInfo, Image image, int yOrder, int relevance)
  {
    super(template, context, region, image, relevance);
   setYOrder(yOrder);
    fAdditionalInfo = extraInfo;
  }

  public SpindleTemplateProposal(Template template, TemplateContext context,
      IRegion region, String extraInfo, Image image, int yOrder)
  {
    this(template, context, region, extraInfo, image, yOrder, 99);
  }

  public String getDisplayString()
  {
    if (fContext instanceof AttributeTemplateContext)
      return ((AttributeTemplateContext) fContext).getAttributeName();

    if (fContext instanceof TagTemplateContext)
      return ((TagTemplateContext) fContext).getDisplayString();

    return super.getDisplayString();
  }

  public void setAdditionalProposalInfo(String info)
  {
    fAdditionalInfo = info;
  }

  public String getAdditionalProposalInfo()
  {
    if (fAdditionalInfo != null)
      return fAdditionalInfo;

    return super.getAdditionalProposalInfo();
  }

}