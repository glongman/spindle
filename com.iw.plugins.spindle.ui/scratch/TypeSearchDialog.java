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

package com.iw.plugins.spindle.ui.widgets;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.iw.plugins.spindle.Images;

/**
 *  TODO Add Type comment
 * 
 * @author glongman@gmail.com
 * @version $Id$
 */
public class TypeSearchDialog extends TitleAreaDialog implements ISelectionChangedListener, IDoubleClickListener
{

    private TypeChooserWidget searchWidget;
    private String title;
    private String description;
    private IRunnableContext fContext;

    public TypeSearchDialog(
        Shell shell,
        IRunnableContext context,
        IJavaProject project,
        IType hierarchyRoot,
        String windowTitle,
        String description)
    {
        super(shell);
        this.title = windowTitle == null ? "" : windowTitle;
        this.description = description == null ? "" : description;
        searchWidget = new TypeChooserWidget(project, hierarchyRoot, context);

        searchWidget.addSelectionChangedListener(this);
        searchWidget.addDoubleClickListener(this);
    }

    public void create()
    {
        super.create();
        setTitle(title);
        setMessage(description, IMessageProvider.NONE);
        searchWidget.setFocus();
        updateOkState();
    }

    /**
     * @see AbstractDialog#performCancel()
     */
    protected boolean performCancel()
    {
        setReturnCode(CANCEL);
        return true;

    }

    protected void okPressed()
    {
        setReturnCode(OK);
        hardClose();
    }

    protected boolean hardClose()
    {
        // dispose any contained stuff
        searchWidget.dispose();
        return super.close();
    }

    protected Control createDialogArea(Composite parent)
    {
        Composite container = (Composite) super.createDialogArea(parent);
        container.setFont(parent.getFont());
        Composite useContainer = new Composite(container, SWT.NONE);
        FillLayout layout = new FillLayout();
        useContainer.setLayout(layout);
        useContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
        searchWidget.createControl(useContainer);
        setTitleImage(Images.getSharedImage("applicationDialog.gif"));
        return container;
    }

    private void updateOkState()
    {
        Button okButton = getButton(IDialogConstants.OK_ID);
        if (okButton != null)
        {
            ISelection selection = searchWidget.getSelection();
            okButton.setEnabled(selection != null && !selection.isEmpty());
        }
    }

    public IType getResultType()
    {
        return searchWidget.getResultType();
    }

    /**
     * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(SelectionChangedEvent)
     */
    public void selectionChanged(SelectionChangedEvent event)
    {
        updateOkState();
    }

    /**
     * @see org.eclipse.jface.viewers.IDoubleClickListener#doubleClick(DoubleClickEvent)
     */
    public void doubleClick(DoubleClickEvent event)
    {
        ISelection selection = searchWidget.getSelection();
        if (selection != null && !selection.isEmpty())
        {
            buttonPressed(IDialogConstants.OK_ID);
        }
    }

}