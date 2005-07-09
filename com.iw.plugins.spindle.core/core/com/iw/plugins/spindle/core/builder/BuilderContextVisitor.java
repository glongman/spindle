/***************************************************************************************************
 * ***** BEGIN LICENSE BLOCK Version: MPL 1.1 The contents of this file are subject to the Mozilla
 * Public License Version 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at http://www.mozilla.org/MPL/ Software
 * distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF ANY KIND,
 * either express or implied. See the License for the specific language governing rights and
 * limitations under the License. The Original Code is Spindle, an Eclipse Plugin for Tapestry. The
 * Initial Developer of the Original Code is Geoffrey Longman. Portions created by the Initial
 * Developer are Copyright (C) 2001-2005 the Initial Developer. All Rights Reserved. Contributor(s):
 * glongman@gmail.com ***** END LICENSE BLOCK *****
 */
package com.iw.plugins.spindle.core.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.hivemind.Resource;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import com.iw.plugins.spindle.core.TapestryCore;
import com.iw.plugins.spindle.core.eclipse.TapestryProject;
import com.iw.plugins.spindle.core.resources.eclipse.ContextRoot;



public class BuilderContextVisitor implements IResourceVisitor
{

    static private List knownExtensions = Arrays
            .asList(AbstractBuildInfrastructure.KnownExtensions);

    private List fCollector;

    private IFolder fContextLocation;

    private IFolder fOutputLocation;

    private TapestryProject fTapestryProject;

    private EclipseBuildInfrastructure fInfrastructure;

    private boolean fContextSeen;

    public BuilderContextVisitor(FullBuild build, ArrayList collector)
    {
        this.fCollector = collector;
        fInfrastructure = (EclipseBuildInfrastructure) build.fInfrastructure;
        this.fTapestryProject = (TapestryProject) fInfrastructure.fTapestryProject;
        ContextRoot eclipseCtxRoot = (ContextRoot) fInfrastructure.fContextRoot;
        this.fContextLocation = (IFolder) eclipseCtxRoot.getContainer();
        try
        {

            IPath out = fInfrastructure.fJavaProject.getOutputLocation();
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            fOutputLocation = root.getFolder(out);
        }
        catch (JavaModelException e)
        {
            TapestryCore.log(e);
        }
        fContextSeen = false;
    }

    private boolean isOnContextPath(IResource resource)
    {
        IResource temp = resource;
        while (temp != fTapestryProject.getProject() && !temp.equals(fContextLocation))
        {
            if (temp.equals(fOutputLocation))
                return false;

            temp = temp.getParent();
        }
        return temp.equals(fContextLocation);
    }

    private boolean isOnClasspath(IFolder folder)
    {
        return JavaCore.create(folder) != null;
    }

    public boolean visit(IResource resource) throws CoreException
    {
        if (resource instanceof IProject)
            return true;

        if (resource instanceof IFolder)
        {
            if (resource.equals(fContextLocation))
                return true;

            if (isOnContextPath(resource))
                return true;

            if (isOnClasspath((IFolder) resource))
                return false;

            if (resource.equals(fOutputLocation))
                return false;

            IFolder folder = (IFolder) resource;
            if (folder.getFullPath().segmentCount() >= fContextLocation.getFullPath()
                    .segmentCount())
                return false;

        }
        else if (resource instanceof IFile)
        {
            if (!isOnContextPath(resource))
                return false;

            String extension = resource.getFileExtension();
            if (knownExtensions.contains(extension))

            {
                Resource location = ((ContextRoot) fInfrastructure.fContextRoot)
                        .getRelativeResource(resource);
                fCollector.add(location);
                debug(location);

            }
        }
        return true;
    }

    protected void debug(Resource location)
    {
        if (AbstractBuildInfrastructure.DEBUG)
            System.out.println(location);
    }

}