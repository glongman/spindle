package org.eclipse.pde.internal.ui.editor;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.Hashtable;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.SubMenuManager;
import org.eclipse.jface.action.SubStatusLineManager;
import org.eclipse.jface.action.SubToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.core.IEditable;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.eclipse.ui.texteditor.BasicTextEditorActionContributor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.IUpdate;

import com.iw.plugins.spindle.TapestryPlugin;
import com.iw.plugins.spindle.xpdesupport.ExternalPDESupport;

public abstract class PDEEditorContributor extends EditorActionBarContributor {
	public static final String ACTIONS_SAVE = "EditorActions.save";
	public static final String ACTIONS_CUT = "EditorActions.cut";
	public static final String ACTIONS_COPY = "EditorActions.copy";
	public static final String ACTIONS_PASTE = "EditorActions.paste";
	private SubMenuManager subMenuManager;
	private SubStatusLineManager subStatusManager;
	private SubToolBarManager subToolbarManager;

	private PDEMultiPageEditor editor;
	private IPDEEditorPage page;
	private SaveAction saveAction;
	private ClipboardAction cutAction;
	private ClipboardAction copyAction;
	private ClipboardAction pasteAction;
	private Hashtable globalActions = new Hashtable();
	private String menuName;
	private BasicTextEditorActionContributor sourceContributor;

	class GlobalAction extends Action implements IUpdate {
		private String id;
		public GlobalAction(String id) {
			this.id = id;
		}
		public void run() {
			editor.performGlobalAction(id);
			updateSelectableActions(editor.getSelection());
		}
		public void update() {
			getActionBars().updateActionBars();
		}
	}

	class ClipboardAction extends GlobalAction {
		public ClipboardAction(String id) {
			super(id);
			setEnabled(false);
		}
		public void selectionChanged(ISelection selection) {
		}
		public boolean isEditable() {
			if (editor==null) return false;
			Object model = editor.getModel();
			if (model instanceof IEditable)
				return ((IEditable)model).isEditable();
			return false;
		}
	}

	class CutAction extends ClipboardAction {
		public CutAction() {
			super(ITextEditorActionConstants.CUT);
			setText(ExternalPDESupport.getResourceString(ACTIONS_CUT));
		}
		public void selectionChanged(ISelection selection) {
			setEnabled(isEditable() && selection != null && !selection.isEmpty());
		}
	}

	class CopyAction extends ClipboardAction {
		public CopyAction() {
			super(ITextEditorActionConstants.COPY);
			setText(ExternalPDESupport.getResourceString(ACTIONS_COPY));
		}
		public void selectionChanged(ISelection selection) {
			setEnabled(selection != null && !selection.isEmpty());
		}
	}

	class PasteAction extends ClipboardAction {
		public PasteAction() {
			super(ITextEditorActionConstants.PASTE);
			setText(ExternalPDESupport.getResourceString(ACTIONS_PASTE));
			//selectionChanged(null);
		}
		public void selectionChanged(ISelection selection) {
			boolean enabled = isEditable();
			if (enabled) {
				boolean knownType = hasKnownTypes(editor.getClipboard());
				enabled = knownType;
				if (knownType) {
					enabled = editor.canPasteFromClipboard();
				}
			}
			setEnabled(enabled);
		}
	}

	class SaveAction extends Action implements IUpdate {
		public SaveAction() {
		}
		public void run() {
			if (editor != null)
				TapestryPlugin.getDefault().getActivePage().saveEditor(editor, false);
		}
		public void update() {
			if (editor != null) {
				setEnabled(editor.isDirty());
			} else
				setEnabled(false);
		}
	}

	public PDEEditorContributor(String menuName) {
		this.menuName = menuName;
		sourceContributor = new BasicTextEditorActionContributor();
		makeActions();
	}
	private void addGlobalAction(String id) {
		GlobalAction action = new GlobalAction(id);
		addGlobalAction(id, action);
	}
	private void addGlobalAction(String id, Action action) {
		globalActions.put(id, action);
	}
	public void addClipboardActions(IMenuManager mng) {
		mng.add(cutAction);
		mng.add(copyAction);
		mng.add(pasteAction);
	}
	
	protected boolean hasKnownTypes(Clipboard clipboard) {
		// defect 18146
		try {
			Object data =
				clipboard.getContents(ModelDataTransfer.getInstance());
			return (data != null);
		} catch (SWTError e) {
			return false;
		}
	}
	
	public void contextMenuAboutToShow(IMenuManager mng) {
		contextMenuAboutToShow(mng, true);
	}
	public void contextMenuAboutToShow(IMenuManager mng, boolean addClipboard) {
		if (addClipboard) {
			addClipboardActions(mng);
			mng.add(new Separator());
		}
		mng.add(saveAction);
	}
	public void contributeToMenu(IMenuManager mm) {
		subMenuManager = new SubMenuManager(mm);
		sourceContributor.contributeToMenu(subMenuManager);
	}
	public void contributeToStatusLine(IStatusLineManager slm) {
		subStatusManager = new SubStatusLineManager(slm);
		sourceContributor.contributeToStatusLine(subStatusManager);
	}
	public void contributeToToolBar(IToolBarManager tbm) {
		subToolbarManager = new SubToolBarManager(tbm);
		sourceContributor.contributeToToolBar(subToolbarManager);
	}
	public PDEMultiPageEditor getEditor() {
		return editor;
	}
	public IAction getGlobalAction(String id) {
		return (IAction) globalActions.get(id);
	}

	public IAction getSaveAction() {
		return saveAction;
	}
	public IStatusLineManager getStatusLineManager() {
		return getActionBars().getStatusLineManager();
	}

	protected void makeActions() {
		// clipboard actions
		cutAction = new CutAction();
		copyAction = new CopyAction();
		pasteAction = new PasteAction();
		addGlobalAction(ITextEditorActionConstants.CUT, cutAction);
		addGlobalAction(ITextEditorActionConstants.COPY, copyAction);
		addGlobalAction(ITextEditorActionConstants.PASTE, pasteAction);

		addGlobalAction(ITextEditorActionConstants.DELETE);
		addGlobalAction(ITextEditorActionConstants.UNDO);
		addGlobalAction(ITextEditorActionConstants.REDO);
		addGlobalAction(ITextEditorActionConstants.SELECT_ALL);
		addGlobalAction(ITextEditorActionConstants.FIND);
		addGlobalAction(ITextEditorActionConstants.BOOKMARK);

		saveAction = new SaveAction();
		saveAction.setText(ExternalPDESupport.getResourceString(ACTIONS_SAVE));

	}
	public void setActiveEditor(IEditorPart targetEditor) {
		if (editor != null)
			editor.updateUndo(null, null);
		if (targetEditor instanceof PDESourcePage) {
			// Fixing the 'goto line' problem -
			// the action is thinking that source page
			// is a standalone editor and tries to activate it
			// #19361
			PDESourcePage page = (PDESourcePage)targetEditor; 
			TapestryPlugin.getDefault().getActivePage().activate(page.getEditor());
			return;
		}
	    this.editor = (PDEMultiPageEditor) targetEditor;
		editor.updateUndo(
			getGlobalAction(ITextEditorActionConstants.UNDO),
			getGlobalAction(ITextEditorActionConstants.REDO));
		IPDEEditorPage page = editor.getCurrentPage();
		setActivePage(page);
		updateSelectableActions(editor.getSelection());
	}
	public void setActivePage(IPDEEditorPage newPage) {
		IPDEEditorPage oldPage = page;
		this.page = newPage;
		if (newPage == null)
			return;
		updateActions();
		if (oldPage != null
			&& oldPage.isSource() == false
			&& newPage.isSource() == false)
			return;

		IActionBars bars = getActionBars();
		PDESourcePage sourcePage = null;
		
		if (newPage instanceof PDESourcePage)
			sourcePage = (PDESourcePage)newPage;
		
		subMenuManager.setVisible(sourcePage!=null);
		subStatusManager.setVisible(sourcePage!=null);
		subToolbarManager.setVisible(sourcePage!=null);

		sourceContributor.setActiveEditor(sourcePage);
		// update global actions
		bars.setGlobalActionHandler(
			IWorkbenchActionConstants.DELETE,
			page.getAction(ITextEditorActionConstants.DELETE));
		bars.setGlobalActionHandler(
			IWorkbenchActionConstants.UNDO,
			page.getAction(ITextEditorActionConstants.UNDO));
		bars.setGlobalActionHandler(
			IWorkbenchActionConstants.REDO,
			page.getAction(ITextEditorActionConstants.REDO));
		bars.setGlobalActionHandler(
			IWorkbenchActionConstants.CUT,
			page.getAction(ITextEditorActionConstants.CUT));
		bars.setGlobalActionHandler(
			IWorkbenchActionConstants.COPY,
			page.getAction(ITextEditorActionConstants.COPY));
		bars.setGlobalActionHandler(
			IWorkbenchActionConstants.PASTE,
			page.getAction(ITextEditorActionConstants.PASTE));
		bars.setGlobalActionHandler(
			IWorkbenchActionConstants.SELECT_ALL,
			page.getAction(ITextEditorActionConstants.SELECT_ALL));
		bars.setGlobalActionHandler(
			IWorkbenchActionConstants.FIND,
			page.getAction(ITextEditorActionConstants.FIND));
		bars.setGlobalActionHandler(
			IWorkbenchActionConstants.BOOKMARK,
			page.getAction(ITextEditorActionConstants.BOOKMARK));
		bars.updateActionBars();
	}
	public void updateActions() {
		saveAction.update();
	}

	public void updateSelectableActions(ISelection selection) {
		cutAction.selectionChanged(selection);
		copyAction.selectionChanged(selection);
		pasteAction.selectionChanged(selection);
	}
}