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

import java.io.StringWriter;

import org.eclipse.core.resources.IStorage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.graphics.Point;
import org.xmen.xml.XMLNode;

import com.iw.plugins.spindle.PreferenceConstants;
import com.iw.plugins.spindle.UIPlugin;
import com.iw.plugins.spindle.core.parser.validator.DOMValidator;
import com.iw.plugins.spindle.core.util.IndentingWriter;
import com.iw.plugins.spindle.core.util.XMLUtil;
import com.iw.plugins.spindle.editors.Editor;
import com.iw.plugins.spindle.editors.util.CompletionProposal;
import com.iw.plugins.spindle.editors.util.ContentAssistProcessor;

/**
 *  Base class for context assist processors for Tapestry specss
 * 
 * @author glongman@intelligentworks.com
 * @version $Id$
 */
public abstract class SpecCompletionProcessor extends ContentAssistProcessor
{
    
    public SpecCompletionProcessor(Editor editor)
    {
        super(editor);
    }

    /* (non-Javadoc)
     * @see com.iw.plugins.spindle.editors.util.ContentAssistProcessor#connect()
     */
    protected void connect(IDocument document) throws IllegalStateException
    {
        String publicId = null;
        fDTD = null;

        super.connect(document);
        try
        {
            XMLNode root = XMLNode.createTree(document, -1);
            publicId = root.fPublicId;
            fDTD = DOMValidator.getDTD(publicId);
        } catch (BadLocationException e)
        {
            // do nothing
        }

        if (fDTD == null )
            throw new IllegalStateException();
    }

    //    /* (non-Javadoc)
    //     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeContextInformation(org.eclipse.jface.text.ITextViewer, int)
    //     */
    //    public IContextInformation[] computeInformation(ITextViewer viewer, int documentOffset)
    //    {
    //        try
    //        {
    //            IDocument document = fEditor.getDocumentProvider().getDocument(fEditor.getEditorInput());
    //            fAssistParititioner.connect(document);
    //
    //            try
    //            {
    //                XMLNode root = XMLNode.createTree(document, -1);
    //                fPublicId = root.fPublicId;
    //                fDeclaredRootElementName = root.fRootNodeId;
    //                fDTD = DOMValidator.getDTD(fPublicId);
    //
    //            } catch (BadLocationException e)
    //            {
    //                // do nothing
    //            }
    //
    //            if (fDTD == null || fDeclaredRootElementName == null)
    //                return NoInformation;
    //
    //            return doComputeContextInformation(viewer, documentOffset);
    //
    //        } catch (RuntimeException e)
    //        {
    //            UIPlugin.log(e);
    //            throw e;
    //        } finally
    //        {
    //            fAssistParititioner.disconnect();
    //        }
    //    }

    protected ICompletionProposal[] computeEmptyDocumentProposal(ITextViewer viewer, int documentOffset)
    {

        IStorage storage = fEditor.getStorage();
        String extension = storage.getFullPath().getFileExtension();
        if (extension == null || extension.length() == 0)
        {
            return NoProposals;
        }
        String replacement = getSkeletonSpecification(extension);
        return new ICompletionProposal[] {
             new CompletionProposal(
                replacement,
                0,
                viewer.getDocument().getLength(),
                new Point(0, 0),
                UIPlugin.getDefault().getStorageLabelProvider().getImage(storage),
                "insert default skeletion XML",
                null,
                null)};
    }

    private String getSkeletonSpecification(String extension)
    {
        IPreferenceStore store = UIPlugin.getDefault().getPreferenceStore();
        boolean useTabs = store.getBoolean(PreferenceConstants.FORMATTER_USE_TABS_TO_INDENT);
        int tabSize = store.getInt(PreferenceConstants.EDITOR_DISPLAY_TAB_WIDTH);
        StringWriter swriter = new StringWriter();
        IndentingWriter iwriter = new IndentingWriter(swriter, useTabs, tabSize, 0, null);
        if ("jwc".equals(extension))
        {
            XMLUtil.writeComponentSpecification(iwriter, UIPlugin.DEFAULT_COMPONENT_SPEC, 0);
            return swriter.toString();
        } else if ("page".equals(extension))
        {
            XMLUtil.writeComponentSpecification(iwriter, UIPlugin.DEFAULT_PAGE_SPEC, 0);
            return swriter.toString();
        } else if ("application".equals(extension))
        {
            XMLUtil.writeApplicationSpecification(iwriter, UIPlugin.DEFAULT_APPLICATION_SPEC, 0);
            return swriter.toString();
        } else if ("library".equals(extension))
        {
            XMLUtil.writeLibrarySpecification(iwriter, UIPlugin.DEFAULT_LIBRARY_SPEC, 0);
            return swriter.toString();
        }
        return "";
    }

}