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

package com.iw.plugins.spindle.editors.spec.assist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Point;

import com.iw.plugins.spindle.Images;
import com.iw.plugins.spindle.editors.Editor;
import com.iw.plugins.spindle.editors.util.CompletionProposal;
import com.iw.plugins.spindle.editors.util.DocumentArtifact;
import com.iw.plugins.spindle.editors.util.DocumentArtifactPartitioner;

/**
 *  Processor for default declType type - only works to insert comments within the
 *  body of the XML
 * 
 * @author glongman@intelligentworks.com
 * @version $Id$
 */
public class TagCompletionProcessor extends SpecCompletionProcessor
{

    /**
     * @param editor
     */
    public TagCompletionProcessor(Editor editor)
    {
        super(editor);
    }

    /* (non-Javadoc)
     * @see com.iw.plugins.spindle.editors.util.ContentAssistProcessor#doComputeCompletionProposals(org.eclipse.jface.text.ITextViewer, int)
     */
    protected ICompletionProposal[] doComputeCompletionProposals(ITextViewer viewer, int documentOffset)
    {
        DocumentArtifact tag = DocumentArtifact.getArtifactAt(viewer.getDocument(), documentOffset);

        if (tag.getOffset() + tag.getLength() == documentOffset)
            tag = tag.getNextArtifact();

        boolean atStart = tag.getOffset() == documentOffset;

        int baseState = tag.getStateAt(documentOffset);
        String tagName = tag.getName();
        if ((tag.getType() == DocumentArtifactPartitioner.ENDTAG && !atStart)
            || baseState == DocumentArtifact.IN_TERMINATOR)
            return NoSuggestions;

        boolean addLeadingSpace = false;
        List proposals = new ArrayList();

        if (baseState == DocumentArtifact.TAG)
        {
            if (atStart && tag.getType() == DocumentArtifactPartitioner.ENDTAG)
            {
                DocumentArtifact parentTag = tag.getCorrespondingNode();
                String parentName = parentTag.getName();
                if (parentTag == null || parentName == null)
                    return NoSuggestions;
                DocumentArtifact prevSib = parentTag.findLastChild();
                String sibName = null;
                if (prevSib != null)
                    sibName = prevSib.getName();

                List candidates = getRawNewTagProposals(fDTD, parentName, sibName);
                if (candidates.isEmpty())
                    return NoSuggestions;

                for (Iterator iter = candidates.iterator(); iter.hasNext();)
                {
                    CompletionProposal proposal = (CompletionProposal) iter.next();
                    proposal.setReplacementOffset(tag.getOffset());
                    proposal.setReplacementLength(0);
                    proposals.add(proposal);
                }
                proposals.add(SpecAssistHelper.getDefaultInsertCommentProposal(documentOffset, 0));
                return (ICompletionProposal[]) proposals.toArray(new ICompletionProposal[proposals.size()]);

            } else if (atStart || (tag.getAttributes().isEmpty() && !tag.isTerminated()))
            {
                String content = tag.getContent();
                int length = tag.getLength();
                List candidates = findRawNewTagProposals(fDTD, tag, documentOffset);
                if (candidates.isEmpty())
                    return NoSuggestions;

                int i = 0;
                if (!atStart)
                {
                    for (; i < length; i++)
                    {
                        char character = content.charAt(i);
                        if (character == '\r' || character == '\n')
                            break;
                    }
                }

                int replacementLength = i;

                if (length > 1 && documentOffset > tag.getOffset() + 1)
                {
                    String match = tag.getContentTo(documentOffset, true).trim().toLowerCase();
                    for (Iterator iter = candidates.iterator(); iter.hasNext();)
                    {
                        CompletionProposal proposal = (CompletionProposal) iter.next();
                        if (proposal.getDisplayString().startsWith(match))
                        {
                            proposal.setReplacementOffset(tag.getOffset());
                            proposal.setReplacementLength(replacementLength);
                            proposals.add(proposal);
                        }
                    }
                    if (proposals.isEmpty())
                    {
                        return NoSuggestions;
                    }

                } else
                {
                    for (Iterator iter = candidates.iterator(); iter.hasNext();)
                    {
                        CompletionProposal proposal = (CompletionProposal) iter.next();
                        proposal.setReplacementOffset(tag.getOffset());
                        proposal.setReplacementLength(replacementLength);
                        proposals.add(proposal);
                    }
                }
                return (ICompletionProposal[]) proposals.toArray(new ICompletionProposal[proposals.size()]);
            } else if (!atStart && tagName != null && documentOffset < tag.getOffset() + tagName.length())
            {
                return NoProposals;
            }

            addLeadingSpace = true;

        } else if (baseState == DocumentArtifact.ATT_VALUE)
        {
            return new ICompletionProposal[] {
                 new CompletionProposal(
                    "'" + CompletionProposal.DEFAULT_ATTR_VALUE + "'",
                    documentOffset,
                    0,
                    new Point(1, CompletionProposal.DEFAULT_ATTR_VALUE.length()))};
        } else
        {
            //ensure that we are in a legal position to insert. ie. not inside another attribute name!
            addLeadingSpace = baseState == DocumentArtifact.AFTER_ATT_VALUE;
        }

        Map attrmap = tag.getAttributesMap();

        DocumentArtifact existingAttr = tag.getAttributeAt(documentOffset);
        if (existingAttr != null)
        {
            if (baseState != DocumentArtifact.AFTER_ATT_VALUE
                && existingAttr != null
                && existingAttr.getOffset() < documentOffset)
            {
                computeAttributeNameReplacements(documentOffset, existingAttr, tagName, attrmap.keySet(), proposals);
            } else
            {
                computeAttributeProposals(documentOffset, addLeadingSpace, tagName, attrmap.keySet(), proposals);
            }
        } else {
            computeAttributeProposals(documentOffset, addLeadingSpace, tagName, attrmap.keySet(), proposals);
        }

        if (proposals.isEmpty())
            return NoSuggestions;

        Collections.sort(proposals, CompletionProposal.PROPOSAL_COMPARATOR);

        return (ICompletionProposal[]) proposals.toArray(new ICompletionProposal[proposals.size()]);

    }

    /**
     * @param documentOffset
     * @param existingAttr
     * @param tagName
     * @param set
     * @param proposals
     */
    private void computeAttributeNameReplacements(
        int documentOffset,
        DocumentArtifact existingAttribute,
        String tagName,
        Set existingAttributeNames,
        List proposals)
    {
        String name = existingAttribute.getName();
        String value = existingAttribute.getAttributeValue();
        //get index of whitespace
        String matchString = existingAttribute.getContentTo(documentOffset, false).toLowerCase();
        if (matchString.length() > name.length())
            return;

        int replacementOffset = existingAttribute.getOffset();
        int replacementLength = name.length();

        int matchLength = matchString.length();
        if (matchLength == 0 || matchLength > name.length())
            matchString = null;

        try
        {
            List attrs = SpecAssistHelper.getAttributes(fDTD, tagName);

            if (!attrs.isEmpty())
            {
                List requiredAttributes = SpecAssistHelper.getRequiredAttributes(fDTD, tagName);
                for (Iterator iter = attrs.iterator(); iter.hasNext();)
                {
                    String attrName = (String) iter.next();
                    if (existingAttributeNames.contains(attrName) || (matchString != null && !attrName.startsWith(matchString)))
                        continue;

                    CompletionProposal proposal;
                    if (value == null)
                    {
                        proposal =
                            new CompletionProposal(
                                attrName + "=\"\"",
                                replacementOffset,
                                replacementLength,
                                new Point(attrName.length(), 0),
                                requiredAttributes.contains(attrName)
                                    ? Images.getSharedImage("bullet_pink.gif")
                                    : Images.getSharedImage("bullet.gif"),
                                null,
                                null,
                                null);
                    } else
                    {
                        proposal =
                            new CompletionProposal(
                                attrName,
                                replacementOffset,
                                replacementLength,
                                new Point(attrName.length(), 0),
                                requiredAttributes.contains(attrName)
                                    ? Images.getSharedImage("bullet_pink.gif")
                                    : Images.getSharedImage("bullet.gif"),
                                null,
                                null,
                                null);
                    }

                    proposals.add(proposal);
                }
            }

        } catch (IllegalArgumentException e)
        {
            //do nothing
        }

    }

    protected void computeAttributeProposals(
        int documentOffset,
        boolean addLeadingSpace,
        String tagName,
        Set existingAttributeNames,
        List proposals)
    {

        List attrs = SpecAssistHelper.getAttributes(fDTD, tagName);

        if (!attrs.isEmpty())
        {
            List requiredAttributes = SpecAssistHelper.getRequiredAttributes(fDTD, tagName);
            for (Iterator iter = attrs.iterator(); iter.hasNext();)
            {
                String attrname = (String) iter.next();
                if (!existingAttributeNames.contains(attrname))
                {
                    CompletionProposal proposal =
                        CompletionProposal.getAttributeProposal(attrname, addLeadingSpace, documentOffset);

                    if (requiredAttributes.contains(attrname))
                        proposal.setImage(Images.getSharedImage("bullet_pink.gif"));
                    proposals.add(proposal);
                }

            }
        }

    }

    /* (non-Javadoc)
     * @see com.iw.plugins.spindle.editors.util.ContentAssistProcessor#doComputeContextInformation(org.eclipse.jface.text.ITextViewer, int)
     */
    public IContextInformation[] doComputeContextInformation(ITextViewer viewer, int documentOffset)
    {
        DocumentArtifact tag = DocumentArtifact.getArtifactAt(viewer.getDocument(), documentOffset);
        int baseState = tag.getStateAt(documentOffset);
        String name = null;
        if (tag.getType() == DocumentArtifactPartitioner.ENDTAG)
        {
            DocumentArtifact start = tag.getCorrespondingNode();
            if (start != null)
                name = start.getName();
        } else
        {
            name = tag.getName();
        }

        if (name == null)
            return NoInformation;

        if (documentOffset - tag.getOffset() <= name.length() + 1)
        {
            String comment = SpecAssistHelper.getElementComment(fDTD, name);
            return new IContextInformation[] {
                 new ContextInformation(name, comment.length() == 0 ? "No Information" : comment)};
        }

        return NoInformation;
    }

}