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
 * Portions created by the Initial Developer are Copyright (C) 2002
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * 
 *  glongman@intelligentworks.com
 *
 * ***** END LICENSE BLOCK ***** */
package com.iw.plugins.spindle.editorlib;

import java.util.Iterator;

import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;

import com.iw.plugins.spindle.TapestryImages;
import com.iw.plugins.spindle.editors.BasicLinksSection;
import com.iw.plugins.spindle.editors.HyperLinkAdapter;
import com.iw.plugins.spindle.editors.SpindleFormPage;
import com.iw.plugins.spindle.model.TapestryLibraryModel;
import com.iw.plugins.spindle.spec.IPluginLibrarySpecification;

public class OverviewPageSection extends BasicLinksSection {

	/**
	 * Constructor for OverviewPageSection
	 */
	public OverviewPageSection(SpindleFormPage page) {
		super(
			page,
			"Pages",
			"This section lists the pages defined in this file");

	}

	protected SpindleFormPage getGotoPage() {
		return (SpindleFormPage)getFormPage().getEditor().getPage(LibraryMultipageEditor.PAGES);
	}

	public void update(boolean removePrevious) {
		if (removePrevious) {
			removeAll();
		}
		TapestryLibraryModel model =
			(TapestryLibraryModel) getFormPage().getModel();
		IPluginLibrarySpecification spec = (IPluginLibrarySpecification)model.getSpecification();
		Iterator i = spec.getPageNames().iterator();
		while (i.hasNext()) {
			String pageName = (String) i.next();
			Image image = TapestryImages.getSharedImage("page16.gif");
			addHyperLink(pageName, pageName, image, new PagesHyperLinkAdapter());
		}
		super.update(removePrevious);
	}

	public void modelChanged(IModelChangedEvent event) {
		int eventType = event.getChangeType();
		if (eventType == IModelChangedEvent.WORLD_CHANGED) {
			updateNeeded = true;
			return;
		}
		if (eventType == IModelChangedEvent.CHANGE) {
			updateNeeded = event.getChangedProperty().equals("pageMap");
		}
	}

	protected class PagesHyperLinkAdapter extends HyperLinkAdapter {
		public void linkActivated(Control parent) {
			final SpindleFormPage targetPage = getGotoPage();
			if (targetPage == null) {
				return;
			}
			getFormPage().getEditor().showPage(targetPage);
			targetPage.openTo(parent.getData());
		}
	}

}