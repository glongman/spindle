package com.iw.plugins.spindle.ui.propertysheet;

/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     glongman@intelligentworks.com - spindle customization
 *******************************************************************************/

/**
 * This action hides or shows expert properties in the <code>PropertySheetViewer</code>.
 */
/*package*/ class FilterAction extends PropertySheetAction {
/**
 * Create the Filter action. This action is used to show
 * or hide expert properties.
 */
public FilterAction(PropertySheetViewer viewer, String name) {
	super(viewer, name);
	setToolTipText("Filter Properties");//PropertiesMessages.getString("Filter.toolTip")); //$NON-NLS-1$
//	WorkbenchHelp.setHelp(this, IPropertiesHelpContextIds.FILTER_ACTION);
}
/**
 * Toggle the display of expert properties.
 */

public void run() {
	PropertySheetViewer ps = getPropertySheet();
	ps.deactivateCellEditor();
	if (isChecked()) {
		ps.showExpert();
	} else {
		ps.hideExpert();
	}
}
}
