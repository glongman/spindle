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

package com.iw.plugins.spindle.editors.template;

import net.sf.solareclipse.text.TextDoubleClickStrategy;
import net.sf.solareclipse.xml.internal.ui.text.AttValueDoubleClickStrategy;
import net.sf.solareclipse.xml.internal.ui.text.SimpleDoubleClickStrategy;
import net.sf.solareclipse.xml.internal.ui.text.TagDoubleClickStrategy;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;

import com.iw.plugins.spindle.editors.BaseSourceConfiguration;

/**
 *  SourceViewerConfiguration for the TemplateEditor
 * 
 * @author glongman@intelligentworks.com
 * @version $Id$
 */
public class TemplateConfiguration extends BaseSourceConfiguration
{
    public static final boolean DEBUG = false;


    private TemplateTextTools fTextTools;

    private ITextDoubleClickStrategy dcsDefault;
    private ITextDoubleClickStrategy dcsSimple;
    private ITextDoubleClickStrategy dcsTag;
    private ITextDoubleClickStrategy dcsAttValue;

    /**
     * @param colorManager
     * @param editor
     */
    public TemplateConfiguration(TemplateTextTools tools, AbstractTextEditor editor)
    {
        super(editor);
        fTextTools = tools;
        dcsDefault = new TextDoubleClickStrategy();
        dcsSimple = new SimpleDoubleClickStrategy();
        dcsTag = new TagDoubleClickStrategy();
        dcsAttValue = new AttValueDoubleClickStrategy();
    }

    /*
     * @see SourceViewerConfiguration#getDoubleClickStrategy(ISourceViewer, String)
     */
    public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType)
    {
        if (TemplatePartitionScanner.XML_COMMENT.equals(contentType))
            return dcsSimple;

        if (TemplatePartitionScanner.XML_PI.equals(contentType))
            return dcsSimple;

        if (TemplatePartitionScanner.XML_TAG.equals(contentType))
            return dcsTag;

        if (TemplatePartitionScanner.XML_ATTRIBUTE.equals(contentType))
            return dcsAttValue;

        if (TemplatePartitionScanner.XML_CDATA.equals(contentType))
            return dcsSimple;

        if (TemplatePartitionScanner.TAPESTRY_JWCID_ATTRIBUTE.equals(contentType))
            return dcsAttValue;
        //TODO need a refined strategy for values like "id@Insert" or "@contrib.Table"

        if (contentType.startsWith(TemplatePartitionScanner.DTD_INTERNAL))
            return dcsSimple;

        return dcsDefault;
    }

    /*
     * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getConfiguredContentTypes(ISourceViewer)
     */
    public String[] getConfiguredContentTypes(ISourceViewer sourceViewer)
    {
        return new String[] {
            IDocument.DEFAULT_CONTENT_TYPE,
            TemplatePartitionScanner.XML_PI,
            TemplatePartitionScanner.XML_COMMENT,
            TemplatePartitionScanner.XML_DECL,
            TemplatePartitionScanner.XML_TAG,
            TemplatePartitionScanner.XML_ATTRIBUTE,
            TemplatePartitionScanner.XML_CDATA,
            TemplatePartitionScanner.TAPESTRY_JWCID_ATTRIBUTE,
            TemplatePartitionScanner.DTD_INTERNAL,
            TemplatePartitionScanner.DTD_INTERNAL_PI,
            TemplatePartitionScanner.DTD_INTERNAL_COMMENT,
            TemplatePartitionScanner.DTD_INTERNAL_DECL,
            };
    }

    /*
     * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getPresentationReconciler(ISourceViewer)
     */
    public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer)
    {
        PresentationReconciler reconciler = new PresentationReconciler();

        DefaultDamagerRepairer dr;

        dr = new DefaultDamagerRepairer(fTextTools.getXMLTextScanner());
        reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
        reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

        dr = new DefaultDamagerRepairer(fTextTools.getDTDTextScanner());
        reconciler.setDamager(dr, TemplatePartitionScanner.DTD_INTERNAL);
        reconciler.setRepairer(dr, TemplatePartitionScanner.DTD_INTERNAL);

        dr = new DefaultDamagerRepairer(fTextTools.getXMLPIScanner());

        reconciler.setDamager(dr, TemplatePartitionScanner.XML_PI);
        reconciler.setRepairer(dr, TemplatePartitionScanner.XML_PI);
        reconciler.setDamager(dr, TemplatePartitionScanner.DTD_INTERNAL_PI);
        reconciler.setRepairer(dr, TemplatePartitionScanner.DTD_INTERNAL_PI);

        dr = new DefaultDamagerRepairer(fTextTools.getXMLCommentScanner());

        reconciler.setDamager(dr, TemplatePartitionScanner.XML_COMMENT);
        reconciler.setRepairer(dr, TemplatePartitionScanner.XML_COMMENT);
        reconciler.setDamager(dr, TemplatePartitionScanner.DTD_INTERNAL_COMMENT);
        reconciler.setRepairer(dr, TemplatePartitionScanner.DTD_INTERNAL_COMMENT);

        dr = new DefaultDamagerRepairer(fTextTools.getXMLDeclScanner());

        reconciler.setDamager(dr, TemplatePartitionScanner.XML_DECL);
        reconciler.setRepairer(dr, TemplatePartitionScanner.XML_DECL);
        reconciler.setDamager(dr, TemplatePartitionScanner.DTD_INTERNAL_DECL);
        reconciler.setRepairer(dr, TemplatePartitionScanner.DTD_INTERNAL_DECL);

        dr = new DefaultDamagerRepairer(fTextTools.getTemplateTagScanner());

        reconciler.setDamager(dr, TemplatePartitionScanner.XML_TAG);
        reconciler.setRepairer(dr, TemplatePartitionScanner.XML_TAG);

        dr = new DefaultDamagerRepairer(fTextTools.getJwcidAttributeScanner());

        reconciler.setDamager(dr, TemplatePartitionScanner.TAPESTRY_JWCID_ATTRIBUTE);
        reconciler.setRepairer(dr, TemplatePartitionScanner.TAPESTRY_JWCID_ATTRIBUTE);

        reconciler.setDamager(dr, TemplatePartitionScanner.XML_ATTRIBUTE);
        reconciler.setRepairer(dr, TemplatePartitionScanner.XML_ATTRIBUTE);

        dr = new DefaultDamagerRepairer(fTextTools.getXMLAttributeScanner());

        reconciler.setDamager(dr, TemplatePartitionScanner.XML_ATTRIBUTE);
        reconciler.setRepairer(dr, TemplatePartitionScanner.XML_ATTRIBUTE);

        dr = new DefaultDamagerRepairer(fTextTools.getXMLCDATAScanner());

        reconciler.setDamager(dr, TemplatePartitionScanner.XML_CDATA);
        reconciler.setRepairer(dr, TemplatePartitionScanner.XML_CDATA);

        return reconciler;
    }
    /* (non-Javadoc)
     * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getTextHover(org.eclipse.jface.text.source.ISourceViewer, java.lang.String)
     */
    public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType)
    {
        if (DEBUG)
        {

            // TODO Auto-generated method stub
            return new ITextHover()
            {
                public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion)
                {
                    try
                    {
                        IDocumentProvider provider = getEditor().getDocumentProvider();
                        IDocument doc = provider.getDocument(getEditor().getEditorInput());
                        return doc.getPartition(hoverRegion.getOffset()).getType();
                    } catch (BadLocationException e)
                    {
                        return "bad location: " + hoverRegion;
                    }
                }

                public IRegion getHoverRegion(ITextViewer textViewer, int offset)
                {
                    return new Region(offset, 1);
                }
            };
        }
        return super.getTextHover(sourceViewer, contentType);
    }

}