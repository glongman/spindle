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

import org.eclipse.jface.action.Action;

/**
 * This is the base class of all the local actions used
 * in the PropertySheet.
 */
/*package*/ abstract class PropertySheetAction extends Action {
	protected PropertySheetViewer viewer;
	private String id;
/**
 * Create a PropertySheetViewer action.
 */
protected PropertySheetAction(PropertySheetViewer viewer, String name) {
	super (name);
	this.id = name;
	this.viewer = viewer;
}
/**
 * Return the unique action ID that will be
 * used in contribution managers.
 */
public String getId() {
	return id;
}
/**
 * Return the PropertySheetViewer
 */
public PropertySheetViewer getPropertySheet() {
	return viewer;
}
/**
 * Set the unique ID that should be used
 * in the contribution managers.
 */
public void setId(String newId) {
	id = newId;
}
}
