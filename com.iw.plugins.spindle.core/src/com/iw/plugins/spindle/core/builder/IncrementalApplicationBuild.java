package com.iw.plugins.spindle.core.builder;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;

import com.iw.plugins.spindle.core.resources.IResourceWorkspaceLocation;

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

/**
 * Builds a Tapestry Application project incrementally
 * 
 * @version $Id$
 * @author glongman@intelligentworks.com
 */
public class IncrementalApplicationBuild extends Build implements IIncrementalBuild
{

    /**
     * Constructor for IncrementalBuilder.
     * @param builder
     */
    public IncrementalApplicationBuild(TapestryBuilder builder)
    {
        super(builder);
    }

    /* (non-Javadoc)
     * @see com.iw.plugins.spindle.core.builder.IBuild#build()
     */
    public void build() throws BuilderException
    {}

    /* (non-Javadoc)
     * @see com.iw.plugins.spindle.core.builder.IBuild#cleanUp()
     */
    public void cleanUp()
    {}

    /* (non-Javadoc)
     * @see com.iw.plugins.spindle.core.builder.IIncrementalBuild#canIncrementalBuild(org.eclipse.core.resources.IResourceDelta)
     */
    public boolean canIncrementalBuild(IResourceDelta projectDelta)
    {
        IResourceWorkspaceLocation contextRoot = tapestryBuilder.contextRoot;
        if (!contextRoot.exists())
        {
            return false;
        }
        IResourceWorkspaceLocation webXML =
            (IResourceWorkspaceLocation) tapestryBuilder.contextRoot.getRelativeLocation("WEB-INF/web.xml");
        if (!webXML.exists())
        {
            return false;
        }
        IResource resource = (IResource) webXML.getStorage();
        IResourceDelta webXMLDelta = projectDelta.findMember(resource.getProjectRelativePath());
        if (webXMLDelta != null)
        {
            return false;
        }
        
        // TODO incremental not implemented - force full build for now
        return false;
    }

}