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

package com.iw.plugins.spindle.editors.template.actions;

import java.util.Map;

import org.apache.tapestry.parse.TemplateParser;
import org.apache.tapestry.spec.IComponentSpecification;
import org.apache.tapestry.spec.IContainedComponent;
import org.apache.tapestry.spec.IParameterSpecification;
import org.eclipse.core.resources.IStorage;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.AbstractTextEditor;

import com.iw.plugins.spindle.UIPlugin;
import com.iw.plugins.spindle.core.resources.IResourceWorkspaceLocation;
import com.iw.plugins.spindle.editors.template.assist.TemplateTapestryAccess;
import com.iw.plugins.spindle.editors.util.DocumentArtifact;
import com.iw.plugins.spindle.editors.util.DocumentArtifactPartitioner;
import com.iw.plugins.spindle.ui.util.UIUtils;

/**
 *  Open an interesting thing, if possible.
 * 
 * @author glongman@intelligentworks.com
 * @version $Id$
 */
public class OpenDeclarationAction extends BaseTemplateAction
{
    public static final String ACTION_ID = UIPlugin.PLUGIN_ID + ".template.openDeclaration";

    private TemplateTapestryAccess fAccess;

    public OpenDeclarationAction()
    {
        super();
        //      TODO I10N
        setText("Open Declaration");
        setId(ACTION_ID);
    }

    protected void doRun()
    {
        DocumentArtifact artifact = DocumentArtifact.getArtifactAt(fDocument, fDocumentOffset);
        if (artifact == null)
            return;
        String type = artifact.getType();
        if (type == DocumentArtifactPartitioner.TEXT
            || type == DocumentArtifactPartitioner.COMMENT
            || type == DocumentArtifactPartitioner.PI
            || type == DocumentArtifactPartitioner.DECL
            || type == DocumentArtifactPartitioner.ENDTAG)
        {
            return;
        }

        DocumentArtifact attrAtOffset = artifact.getAttributeAt(fDocumentOffset);
        if (attrAtOffset == null)
            return;

        Map attrs = artifact.getAttributesMap();

        DocumentArtifact jwcidAttribute = (DocumentArtifact) attrs.get(TemplateParser.JWCID_ATTRIBUTE_NAME);

        if (jwcidAttribute == null)
            return;

        IComponentSpecification componentSpec = resolveComponentSpec(jwcidAttribute.getAttributeValue());

        if (componentSpec == null)
            return;

        if (attrAtOffset.equals(jwcidAttribute))
        {
            handleComponentLookup(componentSpec);
        } else
        {
            handleBinding(componentSpec, attrAtOffset.getName());
        }
    }

    /**
     * @param componentSpec
     */
    private void handleComponentLookup(IComponentSpecification componentSpec)
    {
        IResourceWorkspaceLocation location = null;
        IContainedComponent contained = fAccess.getContainedComponent();
        if (contained != null)
        {
            String simpleId = fAccess.getSimpleId();
            location = (IResourceWorkspaceLocation) fAccess.getBaseSpecification().getSpecificationLocation();
            if (location != null && location.exists())
                foundResult(location.getStorage(), simpleId, contained);
        } else
        {
            location = (IResourceWorkspaceLocation) componentSpec.getSpecificationLocation();
            if (location == null || !location.exists())
                return;

            foundResult(location.getStorage(), null, null);
        }

    }

    /**
     * @param componentSpec
     * @param string
     */
    private void handleBinding(IComponentSpecification componentSpec, String parameterName)
    {
        IParameterSpecification parameterSpec = (IParameterSpecification) componentSpec.getParameter(parameterName);
        if (parameterSpec != null)
        {
            IResourceWorkspaceLocation location = (IResourceWorkspaceLocation) componentSpec.getSpecificationLocation();
            if (location == null || !location.exists())
                return;

            foundResult(location.getStorage(), parameterName, parameterSpec);
        }

    }

    /**
     * @param string
     * @return
     */
    private IComponentSpecification resolveComponentSpec(String jwcid)
    {
        if (jwcid == null)
            return null;

        try
        {
            fAccess = new TemplateTapestryAccess(fEditor);
            fAccess.setJwcid(jwcid);
            return fAccess.getResolvedComponent();
        } catch (IllegalArgumentException e)
        {
            // do nothing
        }
        return null;
    }

    protected void foundResult(Object result, String key, Object moreInfo)
    {
        if (result instanceof IType)
        {
            try
            {
                JavaUI.openInEditor((IType) result);
            } catch (PartInitException e)
            {
                UIPlugin.log(e);
            } catch (JavaModelException e)
            {
                UIPlugin.log(e);
            }
        } else if (result instanceof IStorage)
        {
            UIPlugin.openTapestryEditor((IStorage) result);
            IEditorPart editor = UIUtils.getEditorFor((IStorage) result);
            if (editor != null && (editor instanceof AbstractTextEditor) || moreInfo != null)
            {
                if (moreInfo instanceof IParameterSpecification && key != null)
                {
                    reveal((AbstractTextEditor) editor, "parameter", "name", key);
                } else if (moreInfo instanceof IContainedComponent && key != null)
                {
                    reveal((AbstractTextEditor) editor, "component", "id", key);
                }

            }
        }
    }

    private void reveal(AbstractTextEditor editor, String elementName, String attrName, String attrValue)
    {
        IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        DocumentArtifactPartitioner partitioner =
            new DocumentArtifactPartitioner(DocumentArtifactPartitioner.SCANNER, DocumentArtifactPartitioner.TYPES);
        try
        {
            DocumentArtifact reveal = null;
            partitioner.connect(document);
            Position[] pos = null;
            pos = document.getPositions(partitioner.getPositionCategory());
            for (int i = 0; i < pos.length; i++)
            {
                DocumentArtifact artifact = (DocumentArtifact) pos[i];
                if (artifact.getType() == DocumentArtifactPartitioner.ENDTAG)
                    continue;
                String name = artifact.getName();
                if (name == null)
                    continue;

                if (!elementName.equals(name.toLowerCase()))
                    continue;

                Map attributesMap = artifact.getAttributesMap();
                DocumentArtifact attribute = (DocumentArtifact) attributesMap.get(attrName);
                if (attribute == null)
                    continue;

                String value = attribute.getAttributeValue();
                if (value != null && value.equals(attrValue))
                {
                    reveal = artifact;
                    break;
                }
            }
            if (reveal != null)
                editor.setHighlightRange(reveal.getOffset(), reveal.getLength(), true);

        } catch (Exception e)
        {
            UIPlugin.log(e);
        } finally
        {
            partitioner.disconnect();
        }
    }

}