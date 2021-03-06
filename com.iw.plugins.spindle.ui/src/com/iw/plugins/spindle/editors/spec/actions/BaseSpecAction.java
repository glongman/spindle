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

package com.iw.plugins.spindle.editors.spec.actions;

import org.apache.tapestry.INamespace;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.xmen.internal.ui.text.XMLReconciler;
import org.xmen.xml.XMLNode;

import com.iw.plugins.spindle.UIPlugin;
import com.iw.plugins.spindle.core.parser.validator.DOMValidator;
import com.iw.plugins.spindle.core.util.SpindleStatus;
import com.iw.plugins.spindle.editors.actions.BaseEditorAction;
import com.iw.plugins.spindle.editors.documentsAndModels.IXMLModelProvider;
import com.wutka.dtd.DTD;

/**
 * Base class for spec actions that need the xml partitioning.
 * 
 * @author glongman@gmail.com
 */
public abstract class BaseSpecAction extends BaseEditorAction
{

    protected String fDeclaredRootElementName;

    protected String fPublicId;

    protected DTD fDTD;

    protected INamespace fNamespace;

    protected IDocument fDocument;

    public BaseSpecAction()
    {
        super();
    }

    public BaseSpecAction(String text)
    {
        super(text);
    }

    public BaseSpecAction(String text, ImageDescriptor image)
    {
        super(text, image);
    }

    public BaseSpecAction(String text, int style)
    {
        super(text, style);
    }

    protected IStatus doGetStatus(SpindleStatus status)
    {
        if (getDocumentOffset() >= 0)
        {
            fDeclaredRootElementName = null;
            fPublicId = null;
            fDTD = null;

            try
            {

                IEditorInput editorInput = fEditor.getEditorInput();
                IDocumentProvider documentProvider = getTextEditor().getDocumentProvider();
                fDocument = documentProvider.getDocument(editorInput);
                if (fDocument.getLength() == 0 && fDocument.get().trim().length() == 0)
                    return null;

                IXMLModelProvider modelProvider = UIPlugin.getDefault().getXMLModelProvider();

                XMLReconciler model = (modelProvider).getModel(fDocument);
                if (model == null)
                    return null;

                XMLNode root = model.getRoot();
                fPublicId = model.getPublicId();
                fDeclaredRootElementName = model.getRootNodeId();
                fDTD = DOMValidator.getDTD(fPublicId);

                if (fDTD == null || fDeclaredRootElementName == null)
                    return null;

            }
            catch (RuntimeException e)
            {
                UIPlugin.log(e);
                throw e;
            }
        }
        return status;
    }
}