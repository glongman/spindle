package org.eclipse.pde.internal.ui.editor;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.Iterator;
import java.util.Vector;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;


public class FormOutlinePage extends ContentOutlinePage implements IModelChangedListener {

	protected PDEFormPage formPage;

	public class BasicContentProvider
		extends DefaultContentProvider
		implements ITreeContentProvider {
		public Object[] getElements(Object obj) {
			return getPages();
		}
		public Object[] getChildren(Object obj) {
			return new Object[0];
		}
		public boolean hasChildren(Object obj) {
			return getChildren(obj).length >0;
		}
		public Object getParent(Object obj) {
			return null;
		}
	}
	public class BasicLabelProvider extends LabelProvider {
		public String getText(Object obj) {
			return obj.toString();
		}
		public Image getImage(Object obj) {
			return null; //PDEPlugin.getDefault().getLabelProvider().getImage(obj);
		}
	}
	protected TreeViewer treeViewer;

public FormOutlinePage(PDEFormPage formPage) {
	this.formPage = formPage;
}
protected ITreeContentProvider createContentProvider() {
	return new BasicContentProvider();
}
public void createControl(Composite parent) {
	Tree widget = new Tree(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
	treeViewer = new TreeViewer(widget);
	treeViewer.addSelectionChangedListener(this);
	treeViewer.setContentProvider(createContentProvider());
	treeViewer.setLabelProvider(createLabelProvider());
	treeViewer.setAutoExpandLevel(999);
	treeViewer.setUseHashlookup(true);
	treeViewer.setInput(formPage.getEditor());

	//tree.setBackground(formPage.getForm().getFactory().getBackgroundColor());
	Object model = formPage.getModel();
	if (model instanceof IModelChangeProvider) {
		((IModelChangeProvider)model).addModelChangedListener(this);
	}
}
protected ILabelProvider createLabelProvider() {
	return new BasicLabelProvider();
}
public void dispose() {
	super.dispose();
	Object model = formPage.getModel();
	if (model instanceof IModelChangeProvider) {
		((IModelChangeProvider) model).removeModelChangedListener(this);
	}
}
public Control getControl() {
	return treeViewer!=null?treeViewer.getControl():null;
}
private Object[] getPages() {
	Vector formPages = new Vector();
	for (Iterator iter = formPage.getEditor().getPages(); iter.hasNext();) {
		IPDEEditorPage page = (IPDEEditorPage) iter.next();
		if (!page.isSource())
			formPages.addElement(page);
	}
	Object [] result = new Object[formPages.size()];
	formPages.copyInto(result);
	return result;
}
public IPDEEditorPage getParentPage(Object item) {
	if (item instanceof IPDEEditorPage)
		return (IPDEEditorPage) item;
	return null;
}
public void modelChanged(IModelChangedEvent event) {
	// a really suboptimal refresh - subclasses should be more selective
	treeViewer.refresh();
	treeViewer.expandAll();
}
public void selectionChanged(Object item) {
	IPDEEditorPage page = formPage.getEditor().getCurrentPage();
	IPDEEditorPage newPage = getParentPage(item);
	if (newPage!=page) formPage.getEditor().showPage(newPage);
	if (newPage != item) newPage.openTo(item);
}
public void selectionChanged(SelectionChangedEvent event) {
	ISelection selection = event.getSelection();
	if (selection.isEmpty() == false
		&& selection instanceof IStructuredSelection) {
		IStructuredSelection ssel = (IStructuredSelection) selection;
		Object item = ssel.getFirstElement();
		selectionChanged(item);
	}
	fireSelectionChanged(selection);
}
public void setFocus() {
	if (treeViewer != null)
		treeViewer.getTree().setFocus();
}
}
