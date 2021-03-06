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

package com.iw.plugins.spindle.editors.multi.application;

import org.eclipse.swt.graphics.Image;

import com.iw.plugins.spindle.Images;
import com.iw.plugins.spindle.UIPlugin;
import com.iw.plugins.spindle.editors.multi.IMultiPage;
import com.iw.plugins.spindle.editors.multi.MultiPageSpecEditor;

/**
 *  MultiPage editor for applications.
 * 
 * @author glongman@gmail.com
 * @version $Id$
 */
public class MultiPageApplicationEditor extends MultiPageSpecEditor
{
    public MultiPageApplicationEditor()
    {
        super();
    }

    /* (non-Javadoc)
     * @see com.iw.plugins.spindle.editors.spec.multipage.MultiPageSpecEditor#createOverview()
     */
    public IMultiPage createOverview()
    {
        // TODO Auto-generated method stub
        return new OverviewApplicationFormPage(this, UIPlugin.getString("OVERVIEW_TAB_LABEL"));
    }

    /* (non-Javadoc)
     * @see com.iw.plugins.spindle.editors.multi.MultiPageSpecEditor#getDefaultHeadingImage()
     */
    public Image getDefaultHeadingImage()
    {
        return Images.getSharedImage("application_banner.gif");
    }

}
