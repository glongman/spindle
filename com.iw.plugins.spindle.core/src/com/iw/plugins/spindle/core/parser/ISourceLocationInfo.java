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
 * Portions created by the Initial Developer are Copyright (C) 2003
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * 
 *  glongman@intelligentworks.com
 *
 * ***** END LICENSE BLOCK ***** */

package com.iw.plugins.spindle.core.parser;

import org.apache.tapestry.ILocation;
import org.apache.tapestry.IResourceLocation;

/**
 *  Records all the line and offset information for a chunk of
 *  markup.
 * 
 * @author glongman@intelligentworks.com
 * @version $Id$
 */
public interface ISourceLocationInfo extends ILocation
{
    public abstract boolean hasAttributes();
    public abstract boolean isEmptyTag();
    /** return a location for the element - includes all wrapped by it**/
    public abstract ISourceLocation getSourceLocation();
    /** return a location for all wrapped by the element**/
    public abstract ISourceLocation getContentSourceLocation();
    public abstract ISourceLocation getStartTagSourceLocation();
    public abstract ISourceLocation getEndTagSourceLocation();
    public abstract ISourceLocation getAttributeSourceLocation(String rawname);
    public abstract int getStartTagStartLine();
    public abstract int getStartTagEndLine();
    public abstract int getEndTagStartLine();
    public abstract int getEndTagEndLine();
    public abstract void setResourceLocation(IResourceLocation location);
}