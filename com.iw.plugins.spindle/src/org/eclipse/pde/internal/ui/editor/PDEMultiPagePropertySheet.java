package org.eclipse.pde.internal.ui.editor;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.Hashtable;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.views.properties.IPropertySheetPage;

import com.iw.plugins.spindle.ui.propertysheet.PropertySheetPage;


/**
 * changed to use custom property sheet page as default page
 */


public class PDEMultiPagePropertySheet implements IPropertySheetPage {
	private PageBook pagebook;
	private Hashtable recMap = new Hashtable();
	private PropertySheetPage defaultPage;
	private IActionBars actionBars;
	private IPropertySheetPage currentPage;
	private boolean disposed=false;

	class PageRec {
		IPropertySheetPage page;
		SubActionBars bars;
		void setBarsActive(boolean active) {
			if (active)
				bars.activate();
			else
				bars.deactivate();
		}
	}

public PDEMultiPagePropertySheet() {
	defaultPage = new PropertySheetPage();
}
private void activateBars(PageRec rec, boolean activate) {
	rec.setBarsActive(activate);
}
public void createControl(Composite parent) {
	pagebook = new PageBook(parent, SWT.NULL);
	defaultPage.createControl(pagebook);
	if (currentPage!=null) setPageActive(currentPage);
}
private PageRec createPageRec(IPropertySheetPage page) {
	if (actionBars == null)
		return null;
	PageRec rec = new PageRec();
	rec.page = page;

	rec.bars = new SubActionBars(actionBars);
	getPageControl(page);

	page.setActionBars(rec.bars);
	recMap.put(page, rec);
	return rec;
}
public void dispose() {
	updateActionBars();

	if (pagebook != null && !pagebook.isDisposed())
		pagebook.dispose();
	pagebook = null;
	disposed=true;
}

public boolean isDisposed() {
	return disposed;
}

public Control getControl() {
	return pagebook;
}
private Control getPageControl(IPropertySheetPage page) {
	Control control = page.getControl();
	if (control == null || control.isDisposed()) {
		// first time
		page.createControl(pagebook);
		control = page.getControl();
	}
	return control;
}
public void selectionChanged(IWorkbenchPart part, ISelection sel) {
	if (currentPage!=null) currentPage.selectionChanged(part, sel);
}
public void setActionBars(IActionBars bars) {
	this.actionBars = bars;

	createPageRec(defaultPage);

	if (currentPage != null) {
		PageRec rec = createPageRec(currentPage);
		setPageActive(rec);
		updateActionBars();
	}
}
public void setDefaultPageActive() {
	setPageActive(defaultPage);
}
public void setFocus() {
	if (currentPage != null)
		currentPage.setFocus();
}
private void setPageActive(PageRec pageRec) {
	IPropertySheetPage page = pageRec.page;
	Control control = getPageControl(page);
	pagebook.showPage(control);
	pageRec.setBarsActive(true);
}
public void setPageActive(IPropertySheetPage page) {
	IPropertySheetPage oldPage = currentPage;
	this.currentPage = page;
	if (pagebook == null) {
		// still not being made
		return;
	}
	if (oldPage != null) {
		PageRec oldRec = (PageRec) recMap.get(oldPage);
		if (oldRec != null) {
			oldRec.setBarsActive(false);
		}
	}
	PageRec rec = (PageRec) recMap.get(page);
	if (rec == null) {
		rec = createPageRec(page);
	}
	if (rec != null) {
		setPageActive(rec);
		updateActionBars();
	}
}
private void updateActionBars() {
	actionBars.updateActionBars();
}
}
