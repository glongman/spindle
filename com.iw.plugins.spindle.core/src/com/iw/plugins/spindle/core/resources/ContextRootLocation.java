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

package com.iw.plugins.spindle.core.resources;

import org.apache.tapestry.IResourceLocation;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.iw.plugins.spindle.core.resources.search.ISearch;

/**
 *  Used for the roots
 * 
 * @author glongman@intelligentworks.com
 * @version $Id$
 */
public class ContextRootLocation extends AbstractRootLocation
{

    IFolder rootFolder;
    ContextSearch search;

    public ContextRootLocation(IFolder folder)
    {
        rootFolder = folder;
    }

    /* (non-Javadoc)
     * @see com.iw.plugins.spindle.core.resources.IResourceWorkspaceLocation#exists()
     */
    public boolean exists()
    {
        return rootFolder.exists();
    }

    /* (non-Javadoc)
     * @see com.iw.plugins.spindle.core.resources.IResourceWorkspaceLocation#isWorkspaceResource()
     */
    public boolean isWorkspaceResource()
    {
        return true;
    }

    public IContainer getContainer()
    {
        return rootFolder;
    }

    /* (non-Javadoc)
     * @see com.iw.plugins.spindle.core.resources.IResourceWorkspaceLocation#getProject()
     */
    public IProject getProject()
    {
        return rootFolder.getProject();
    }

    public IResourceWorkspaceLocation getRelativeLocation(IResource resource)
    {
        return new ContextResourceWorkspaceLocation(this, resource);
    }

    /* (non-Javadoc)
     * @see org.apache.tapestry.IResourceLocation#getRelativeLocation(java.lang.String)
     */
    public IResourceLocation getRelativeLocation(String name)
    {
        if (name.startsWith("/"))
        {
            if (getPath().equals(name))
            {
                return this;
            } else
            {
                return new ContextResourceWorkspaceLocation(this, new Path(name).makeRelative().toString());
            }
        }
        return new ContextResourceWorkspaceLocation(this, name);
    }

    public void lookup(IResourceLocationAcceptor requestor) throws CoreException
    {
        IResource[] members = rootFolder.members(false);
        for (int i = 0; i < members.length; i++)
        {
            if (members[i] instanceof IContainer)
            {
                continue;
            }
            if (!requestor.accept((IResourceWorkspaceLocation) getRelativeLocation(members[i].getName())))
            {
                break;
            }
        }
    }

    public String findRelativePath(IResource resource)
    {
        IPath rootPath = rootFolder.getFullPath();
        IPath resourcePath = resource.getFullPath();
        if (!rootPath.isPrefixOf(resourcePath))
        {
            throw new RuntimeException("not relative to this root!");
        }
        IPath resultPath = resourcePath.removeFirstSegments(rootPath.segmentCount()).makeAbsolute();
        if (resource instanceof IContainer && resultPath.segmentCount() > 0)
        {
            resultPath = resultPath.addTrailingSeparator();
        }
        return resultPath.toString();
    }

    protected IContainer getContainer(ContextResourceWorkspaceLocation location)
    {
        IFolder folder = rootFolder.getFolder(location.getPath());
        if (folder != null && folder.exists())
        {
            return folder;
        }
        return null;
    }

    /* (non-Javadoc)
     * @see com.iw.plugins.spindle.core.resources.IResourceWorkspaceLocation#getSearch()
     */
    public ISearch getSearch() throws CoreException
    {
        if (search == null)
        {
            search = new ContextSearch();
            search.configure(rootFolder);
        }
        return search;
    }

    /* (non-Javadoc)
     * @see com.iw.plugins.spindle.core.resources.IResourceWorkspaceLocation#isOnClasspath()
     */
    public boolean isOnClasspath()
    {
        return false;
    }

    public String toString()
    {
        return "ctx(" + rootFolder.getFullPath() + ")/";
    }

}