package org.eclipse.pde.internal.ui.editor;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.core.IEditable;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.core.IWritable;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.update.ui.forms.internal.CustomWorkbook;
import org.eclipse.update.ui.forms.internal.IFormPage;
import org.eclipse.update.ui.forms.internal.IFormSelectionListener;
import org.eclipse.update.ui.forms.internal.IFormWorkbook;

import com.iw.plugins.spindle.MessageUtil;
import com.iw.plugins.spindle.TapestryPlugin;

public abstract class PDEMultiPageEditor
	extends EditorPart
	implements ISelectionProvider {
	public static final String WRONG_EDITOR = "MultiPageEditor.wrongEditor";
	public static final String TAG_TYPE = "input_type";
	public static final String TYPE_WORKBENCH = "workbench_file";
	public static final String TYPE_SYSTEM = "system_file";
	public static final String TAG_PATH = "input_path";

	protected IFormWorkbook formWorkbook;
	private SelectionProvider selectionProvider = new SelectionProvider();
	protected Object model;
	protected IModelChangedListener modelListener;
	private Vector pages;
	protected String firstPageId;
	private PDEMultiPageContentOutline contentOutline =
		new PDEMultiPageContentOutline(this);
	private PDEMultiPagePropertySheet propertySheet =
		new PDEMultiPagePropertySheet();
	private Hashtable table = new Hashtable();
	private Menu contextMenu;
	private IDocumentProvider documentProvider;
	private boolean disposed;
	protected IModelUndoManager undoManager;
	protected Clipboard clipboard;
	private boolean validated;

	public PDEMultiPageEditor() {
		formWorkbook = new CustomWorkbook();
		formWorkbook.setFirstPageSelected(false);
		pages = new Vector();
//		PDEPlugin.getDefault().getLabelProvider().connect(this);
		createPages();
		undoManager = createModelUndoManager();
	}

	void updateUndo(IAction undoAction, IAction redoAction) {
		undoManager.setActions(undoAction, redoAction);
	}

	public void addPage(String id, IPDEEditorPage page) {
		table.put(id, page);
		pages.addElement(page);
	}

	public void addPage(String id, IPDEEditorPage page, int index) {
		table.put(id, page);
		pages.add(index, page);
	}

	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		selectionProvider.addSelectionChangedListener(listener);
	}
	public void commitFormPages(boolean onSave) {
		for (Iterator iter = getPages(); iter.hasNext();) {
			IPDEEditorPage page = (IPDEEditorPage) iter.next();
			if (page instanceof PDEFormPage) {
				PDEFormPage formPage = (PDEFormPage) page;
				formPage.getForm().commitChanges(onSave);
			}
		}
	}
	protected IDocumentPartitioner createDocumentPartitioner() {
		return null;
	}
	protected IDocumentProvider createDocumentProvider(Object input) {
		IDocumentProvider documentProvider = null;
		if (input instanceof IFile)
			documentProvider = new FileDocumentProvider() {
			public IDocument createDocument(Object element)
				throws CoreException {
				IDocument document = super.createDocument(element);
				if (document != null) {
					IDocumentPartitioner partitioner =
						createDocumentPartitioner();
					if (partitioner != null) {
						partitioner.connect(document);
						document.setDocumentPartitioner(partitioner);
					}
				}
				return document;
			}
		};
		else if (input instanceof File) {
			documentProvider =
				new SystemFileDocumentProvider(createDocumentPartitioner());
		} else if (input instanceof IStorage) {
			documentProvider =
				new StorageDocumentProvider(createDocumentPartitioner());
		}
		return documentProvider;
	}
	protected abstract Object createModel(Object input) throws CoreException;
	protected abstract void createPages();
	protected IModelUndoManager createModelUndoManager() {
		return new NullUndoManager();
	}

	public void createPartControl(Composite parent) {
		clipboard = new Clipboard(parent.getDisplay());
		formWorkbook.createControl(parent);
		formWorkbook.addFormSelectionListener(new IFormSelectionListener() {
			public void formSelected(IFormPage page) {
				updateSynchronizedViews((IPDEEditorPage) page);
				getContributor().setActivePage((IPDEEditorPage) page);
				if (page instanceof PDEFormPage) {
					PDEFormPage formPage = (PDEFormPage) page;
					if (formPage.getSelection() != null)
						setSelection(formPage.getSelection());
				}
				((IPDEEditorPage) page).setFocus();
			}
		});
		MenuManager manager = new MenuManager();
		IMenuListener listener = new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				editorContextMenuAboutToShow(manager);
			}
		};
		manager.setRemoveAllWhenShown(true);
		manager.addMenuListener(listener);
		contextMenu = manager.createContextMenu(formWorkbook.getControl());
		formWorkbook.getControl().setMenu(contextMenu);

		for (Iterator iter = pages.iterator(); iter.hasNext();) {
			IFormPage page = (IFormPage) iter.next();
			formWorkbook.addPage(page);
		}
		if (firstPageId != null)
			showPage(firstPageId);
	}
	public void dispose() {
		setSelection(new StructuredSelection());
		for (int i = 0; i < pages.size(); i++) {
			IWorkbenchPart part = (IWorkbenchPart) pages.elementAt(i);
			part.dispose();
		}
		IEditorInput input = getEditorInput();
		IAnnotationModel amodel = documentProvider.getAnnotationModel(input);
		if (amodel != null)
			amodel.disconnect(documentProvider.getDocument(input));
		documentProvider.disconnect(input);
		if (modelListener != null && model instanceof IModelChangeProvider) {
			((IModelChangeProvider) model).removeModelChangedListener(
				modelListener);
			undoManager.disconnect((IModelChangeProvider) model);
		}
//		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		disposed = true;
	}
	public void doSave(IProgressMonitor monitor) {
		final IEditorInput input = getEditorInput();
		commitFormPages(true);
		updateDocument();
		WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
			public void execute(final IProgressMonitor monitor)
				throws CoreException {
				documentProvider.saveDocument(
					monitor,
					input,
					documentProvider.getDocument(input),
					true);
			}
		};

		try {
			documentProvider.aboutToChange(input);
			op.run(monitor);
			documentProvider.changed(input);
			fireSaveNeeded();
		} catch (InterruptedException x) {
		} catch (InvocationTargetException x) {
			TapestryPlugin.getDefault().logException(x);
		}
	}
	public void doSaveAs() {
		getCurrentPage().doSaveAs();
	}
	public void editorContextMenuAboutToShow(IMenuManager menu) {
		PDEEditorContributor contributor = getContributor();
		getCurrentPage().contextMenuAboutToShow(menu);
		if (contributor != null)
			contributor.contextMenuAboutToShow(menu);

	}
	public void fireSaveNeeded() {
		firePropertyChange(PROP_DIRTY);
		PDEEditorContributor contributor = getContributor();
		if (contributor != null)
			contributor.updateActions();
		validateEdit();
	}
	
	private void validateEdit() {
		if (!isDirty()) return;
		if (!validated) {
			IEditorInput input = getEditorInput();
			if (input instanceof IFileEditorInput) {
				IFile file = ((IFileEditorInput) input).getFile();
				Shell shell = formWorkbook.getControl().getShell();
				IStatus validateStatus =
					TapestryPlugin.getDefault().getWorkspace().validateEdit(
						new IFile[] { file },
						shell);
				if (validateStatus.getCode()!=IStatus.OK)
					ErrorDialog.openError(shell, getTitle(), null, validateStatus);
			}
			validated = true;
		}
	}
	public IAction getAction(String id) {
		return getContributor().getGlobalAction(id);
	}
	public Object getAdapter(Class key) {
		if (key.equals(IContentOutlinePage.class)) {
			return getContentOutline();
		}
		if (key.equals(IPropertySheetPage.class)) {
			return getPropertySheet();
		}
		return super.getAdapter(key);
	}
	public PDEMultiPageContentOutline getContentOutline() {
		if (contentOutline == null || contentOutline.isDisposed()) {
			contentOutline = new PDEMultiPageContentOutline(this);
			updateContentOutline(getCurrentPage());
		}
		return contentOutline;
	}
	public Menu getContextMenu() {
		return contextMenu;
	}
	public PDEEditorContributor getContributor() {
		return (PDEEditorContributor) getEditorSite().getActionBarContributor();
	}
	public IPDEEditorPage getCurrentPage() {
		return (IPDEEditorPage) formWorkbook.getCurrentPage();
	}
	public IDocumentProvider getDocumentProvider() {
		return documentProvider;
	}
	public abstract IPDEEditorPage getHomePage();
	public Object getModel() {
		return model;
	}
	public IPDEEditorPage getPage(String pageId) {
		return (IPDEEditorPage) table.get(pageId);
	}
	public Iterator getPages() {
		return pages.iterator();
	}
	public PDEMultiPagePropertySheet getPropertySheet() {
		if (propertySheet == null || propertySheet.isDisposed()) {
			propertySheet = new PDEMultiPagePropertySheet();
			updatePropertySheet(getCurrentPage());
		}
		return propertySheet;
	}

	public Clipboard getClipboard() {
		return clipboard;
	}

	public ISelection getSelection() {
		return selectionProvider.getSelection();
	}
	protected abstract String getSourcePageId();
	public IStatusLineManager getStatusLineManager() {
		PDEEditorContributor contributor = getContributor();
		if (contributor != null)
			return contributor.getStatusLineManager();
		return null;
	}
	public void gotoMarker(IMarker marker) {
		showPage(getPage(getSourcePageId())).gotoMarker(marker);
	}
	public void init(IEditorSite site, IEditorInput input)
		throws PartInitException {

		if (isValidContentType(input) == false) {
			String message =
				MessageUtil.getFormattedString(WRONG_EDITOR, input.getName());
			IStatus s =
				new Status(
					IStatus.ERROR,
					TapestryPlugin.getDefault().getPluginId(),
					IStatus.OK,
					message,
					null);
			throw new PartInitException(s);
		}

		setSite(site);
		setInput(input);

		Object inputObject = null;
		if (input instanceof SystemFileEditorInput) {
			inputObject = input.getAdapter(File.class);
		} else if (input instanceof IFileEditorInput) {
			inputObject = input.getAdapter(IFile.class);
		} else if (input instanceof IStorageEditorInput) {
			try {
				inputObject = ((IStorageEditorInput) input).getStorage();
			} catch (CoreException e) {
				throw new PartInitException(e.getStatus());
			}
		}
		site.setSelectionProvider(this);
		try {
			initializeModels(inputObject);
		} catch (CoreException e) {
			throw new PartInitException(e.getStatus());
		}

		for (Iterator iter = pages.iterator(); iter.hasNext();) {
			IEditorPart part = (IEditorPart) iter.next();
			part.init(site, input);
		}
		if (inputObject instanceof IFile)
			setTitle(((IFile) inputObject).getName());
		else if (inputObject instanceof java.io.File)
			setTitle("system:" + ((java.io.File) inputObject).getName());
		else
			setTitle(input.toString());
	}
	protected void initializeModels(Object input) throws CoreException {
		documentProvider = createDocumentProvider(input);
		if (documentProvider == null)
			return;
		// create document provider
		model = createModel(input);

		if (model instanceof IModelChangeProvider) {
			modelListener = new IModelChangedListener() {
				public void modelChanged(IModelChangedEvent e) {
					if (e.getChangeType() != IModelChangedEvent.WORLD_CHANGED)
						fireSaveNeeded();
				}
			};
			((IModelChangeProvider) model).addModelChangedListener(
				modelListener);
			undoManager.connect((IModelChangeProvider) model);
		}

		try {
			IEditorInput editorInput = getEditorInput();
			documentProvider.connect(editorInput);
			IAnnotationModel amodel =
				documentProvider.getAnnotationModel(editorInput);
			if (amodel != null)
				amodel.connect(documentProvider.getDocument(editorInput));
		} catch (CoreException e) {
			TapestryPlugin.getDefault().logException(e);
		}
		if (isModelCorrect(model) == false) {
			firstPageId = getSourcePageId();
		}
	}
	public boolean isDirty() {
		if (isModelDirty(model))
			return true;
		if (documentProvider != null)
			return documentProvider.canSaveDocument(getEditorInput());
		return false;
	}
	public boolean isDisposed() {
		return disposed;
	}
	public boolean isEditable() {
		if (model instanceof IModel) {
			return ((IModel) model).isEditable();
		}
		return true;
	}
	protected boolean isModelCorrect(Object model) {
		return true;
	}
	protected abstract boolean isModelDirty(Object model);
	public boolean isSaveAsAllowed() {
		return false;
	}
	protected boolean isValidContentType(IEditorInput input) {
		return true;
	}
	protected void performGlobalAction(String id) {
		// preserve selection
		ISelection selection = getSelection();
		boolean handled = getCurrentPage().performGlobalAction(id);

		if (!handled) {
			IPDEEditorPage page = getCurrentPage();
			if (page instanceof PDEFormPage) {
				if (id.equals(ITextEditorActionConstants.UNDO)) {
					undoManager.undo();
					return;
				}
				if (id.equals(ITextEditorActionConstants.REDO)) {
					undoManager.redo();
					return;
				}
				if (id.equals(ITextEditorActionConstants.CUT)
					|| id.equals(ITextEditorActionConstants.COPY)) {
					copyToClipboard(selection);
					return;
				}
			}
		}
	}
	public void registerContentOutline(IPDEEditorPage page) {
		IContentOutlinePage outlinePage = page.getContentOutlinePage();
		outlinePage.createControl(contentOutline.getPagebook());
	}
	public void removePage(IPDEEditorPage page) {
		formWorkbook.removePage(page);
		pages.removeElement(page);
	}
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		selectionProvider.removeSelectionChangedListener(listener);
	}
	public void setFocus() {
		getCurrentPage().setFocus();
	}
	public void setSelection(ISelection selection) {
		selectionProvider.setSelection(selection);
		getContributor().updateSelectableActions(selection);
	}
	public IPDEEditorPage showPage(String id) {
		return showPage(getPage(id));
	}
	public void showPage(String id, Object openToObject) {
		IPDEEditorPage page = showPage(getPage(id));
		if (page != null)
			page.openTo(openToObject);
	}
	public IPDEEditorPage showPage(final IPDEEditorPage page) {
		formWorkbook.selectPage(page);
		return page;
	}
	void updateDocument() {
		// if model is dirty, flush its content into
		// the document so that the source editor will
		// pick up the changes.
		if (!(model instanceof IEditable))
			return;
		IEditable editableModel = (IEditable) model;
		if (editableModel.isDirty() == false)
			return;
		try {
			// need to update the document
			IDocument document = documentProvider.getDocument(getEditorInput());
			StringWriter swriter = new StringWriter();
			PrintWriter writer = new PrintWriter(swriter);
			editableModel.save(writer);
			writer.flush();
			swriter.close();
			document.set(swriter.toString());
		} catch (IOException e) {
			TapestryPlugin.getDefault().logException(e);
		}
	}

	protected abstract boolean updateModel();

	void updateSynchronizedViews(IPDEEditorPage page) {
		updateContentOutline(page);
		updatePropertySheet(page);
	}

	void updateContentOutline(IPDEEditorPage page) {
		IContentOutlinePage outlinePage = page.getContentOutlinePage();
		if (outlinePage != null) {
			contentOutline.setPageActive(outlinePage);
		}
	}

	void updatePropertySheet(IPDEEditorPage page) {
		IPropertySheetPage propertySheetPage = page.getPropertySheetPage();
		if (propertySheetPage != null) {
			propertySheet.setPageActive(propertySheetPage);
		} else {
			propertySheet.setDefaultPageActive();
		}
	}

	public void close(final boolean save) {
		Display display = getSite().getShell().getDisplay();

		display.asyncExec(new Runnable() {
			public void run() {
				getSite().getPage().closeEditor(PDEMultiPageEditor.this, save);
			}
		});
	}
	private void copyToClipboard(ISelection selection) {
		IStructuredSelection ssel = (IStructuredSelection) selection;
		Object[] objects = ssel.toArray();
		StringWriter writer = new StringWriter();
		PrintWriter pwriter = new PrintWriter(writer);

		Class objClass = null;

		for (int i = 0; i < objects.length; i++) {
			Object obj = objects[i];
			if (objClass == null)
				objClass = obj.getClass();
			else if (objClass.equals(obj.getClass()) == false)
				return;
			if (obj instanceof IWritable) {
				((IWritable) obj).write("", pwriter);
			}
		}
		pwriter.flush();
		String textVersion = writer.toString();
		try {
			pwriter.close();
			writer.close();
		} catch (IOException e) {
		}
		// set the clipboard contents
		clipboard.setContents(
			new Object[] { objects, textVersion },
			new Transfer[] {
				ModelDataTransfer.getInstance(),
				TextTransfer.getInstance()});
	}

	public boolean canPasteFromClipboard() {
		IPDEEditorPage page = getCurrentPage();
		if (page instanceof PDEFormPage) {
			return page.canPaste(getClipboard());
		}
		return false;
	}
}