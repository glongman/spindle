package org.eclipse.pde.internal.ui.editor;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.update.ui.forms.internal.IFormPage;

public abstract class PDESourcePage extends AbstractTextEditor implements IPDEEditorPage {
	public static final String PAGE_TITLE = "SourcePage.title";
	public static final String ERROR_MESSAGE = "SourcePage.errorMessage";
	public static final String ERROR_TITLE = "SourcePage.errorTitle";
	private IContentOutlinePage outlinePage;
	private boolean errorMode;
	private PDEMultiPageEditor editor;
	private boolean modelNeedsUpdating=false;
	private Control control;
	private DocumentListener documentListener = new DocumentListener();

	class DocumentListener implements IDocumentListener {
		public void documentAboutToBeChanged(DocumentEvent e) {
		}
		public void documentChanged(DocumentEvent e) {
			if (isVisible()) {
				modelNeedsUpdating = true;
			}
		}
	}

public PDESourcePage(PDEMultiPageEditor editor) {
	this.editor = editor;
}
public boolean becomesInvisible(IFormPage newPage) {
	if (errorMode || modelNeedsUpdating) {
		boolean cleanModel = getEditor().updateModel();
		if (cleanModel==false) {
			warnErrorsInSource();
			errorMode=true;
			return false;
		}
		modelNeedsUpdating = false;
		errorMode = false;
	}
	getSite().setSelectionProvider(getEditor());
	return true;
}
public void becomesVisible(IFormPage oldPage) {
	modelNeedsUpdating=false;
	getSite().setSelectionProvider(getSelectionProvider());
}
public boolean contextMenuAboutToShow(IMenuManager manager) {
	return false;
}
public abstract IContentOutlinePage createContentOutlinePage();
public void createControl(Composite parent) {
	createPartControl(parent);
}
public void createPartControl(Composite parent) {
	super.createPartControl(parent);
	Control[] children = parent.getChildren();
	control = children[children.length - 1];

	IDocument document = getDocumentProvider().getDocument(getEditorInput());
	document.addDocumentListener(documentListener);
	errorMode = !getEditor().isModelCorrect(getEditor().getModel());
}
public void dispose() {
	IDocument document = getDocumentProvider().getDocument(getEditorInput());
	if (document != null)
		document.removeDocumentListener(documentListener);
	super.dispose();
}
protected void firePropertyChange(int type) {
	if (type == PROP_DIRTY) {
		getEditor().fireSaveNeeded();
	} else
		super.firePropertyChange(type);
}
public IContentOutlinePage getContentOutlinePage() {
	if (outlinePage == null) {
		outlinePage = createContentOutlinePage();
	}
	return outlinePage;
}
public Control getControl() {
	return control;
}
public PDEMultiPageEditor getEditor() {
	return editor;
}
public String getLabel() {
	return getTitle();
}
public IPropertySheetPage getPropertySheetPage() {
	return null;
}
public String getTitle() {
	return "Source";//PDEPlugin.getResourceString(PAGE_TITLE);
}
public void init(IEditorSite site, IEditorInput input)
	throws PartInitException {
	setDocumentProvider(getEditor().getDocumentProvider());
	super.init(site, input);
}
public boolean isEditable() {
	return getEditor().isEditable();
}
public boolean isSource() {
	return true;
}
public boolean isVisible() {
	return editor.getCurrentPage()==this;
}
public void openTo(Object object) {
}
public boolean performGlobalAction(String id) { return true; }
public String toString() {
	return getTitle();
}
public void update() {}
private void warnErrorsInSource() {
//	Display.getCurrent().beep();
//	String title = editor.getSite().getRegisteredName();
//	MessageDialog.openError(
//		PDEPlugin.getActiveWorkbenchShell(),
//		title,
//		PDEPlugin.getResourceString(ERROR_MESSAGE));
}

protected void createActions () {
	PDEEditorContributor contributor = getEditor().getContributor();
	super.createActions();
	setAction(ITextEditorActionConstants.SAVE, contributor.getSaveAction());
}

public void close(boolean save) {
	editor.close(save);
}

public boolean canPaste(Clipboard clipboard) {
	return true;
}
}
