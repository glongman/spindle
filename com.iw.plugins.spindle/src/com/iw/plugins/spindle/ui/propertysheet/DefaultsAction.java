package com.iw.plugins.spindle.ui.propertysheet;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.ui.help.WorkbenchHelp;

/**
 * This action resets the <code>PropertySheetViewer</code> values back
 * to the default values.
 *
 * [Issue: should listen for selection changes in the viewer and set enablement]
 */
/*package*/ class DefaultsAction extends PropertySheetAction {
/**
 * Create the Defaults action. This action is used to set
 * the properties back to their default values.
 */
public DefaultsAction(PropertySheetViewer viewer, String name) {
	super(viewer, name);
	setToolTipText("Restore Default Value");//PropertiesMessages.getString("DefaultAction.toolTip")); //$NON-NLS-1$
//	WorkbenchHelp.setHelp(this, IPropertiesHelpContextIds.DEFAULTS_ACTION);
}
/**
 * Reset the properties to their default values.
 */
public void run() {
	getPropertySheet().deactivateCellEditor();
	getPropertySheet().resetProperties();
}
}
