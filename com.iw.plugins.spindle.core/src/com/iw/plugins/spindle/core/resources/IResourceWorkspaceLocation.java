package com.iw.plugins.spindle.core.resources;
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

import java.io.InputStream;

import org.apache.tapestry.IResourceLocation;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;

/**
 * Extends <code>org.apache.tapestry.IResourceLocation<code> to record additional
 * bits of information describing a Tapestry artifact found in the workspace.
 * 
 * @author glongman@intelligentworks.com
 * @version $Id$
 * @see org.apache.tapestry.IResourceLocation
 */

public interface IResourceWorkspaceLocation extends IResourceLocation
{
    
    public boolean exists();

    /**
     * return the workspace storage associated with this descriptor
     * <br>
     * Using IStorage here instead of IResource as some things will come from
     * Jar files.
     */
    public IStorage getStorage();

    public boolean isWorkspaceResource();

    /**
     * return the project that contains the artifact
     */
    public IProject getProject();


    /**
     * Returns an open input stream on the contents of this descriptor.
     * The caller is responsible for closing the stream when finished.
     * 
     *   @exception CoreException if the contents of this storage could 
     *		not be accessed.   See any refinements for more information.  
     */
    public InputStream getContents() throws CoreException;

}