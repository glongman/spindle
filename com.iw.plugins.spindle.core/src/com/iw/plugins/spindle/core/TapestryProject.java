package com.iw.plugins.spindle.core;

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

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import com.iw.plugins.spindle.core.builder.TapestryArtifactManager;
import com.iw.plugins.spindle.core.metadata.DefaultTapestryMetadata;
import com.iw.plugins.spindle.core.metadata.ProjectExternalMetadataLocator;
import com.iw.plugins.spindle.core.resources.ClasspathRootLocation;
import com.iw.plugins.spindle.core.resources.ContextRootLocation;
import com.iw.plugins.spindle.core.util.Markers;
import com.iw.plugins.spindle.core.util.SpindleStatus;

/**
 * The Tapestry project nature. Configures and Deconfigures the builder
 * 
 * @author glongman@gmail.com
 */
public class TapestryProject implements IProjectNature, ITapestryProject
{
	static public IStatus addTapestryNature(IJavaProject project, boolean forceOrder)
    {
    	return addTapestryNature(project.getProject(), forceOrder);      
    }
	
    static public IStatus addTapestryNature(IProject project, boolean forceOrder)
    {
    	
        try
        {
            TapestryCore.addNatureToProject(
                    project,
                    TapestryCore.NATURE_ID,
                    forceOrder);
        }
        catch (CoreException ex)
        {
        	
            TapestryCore.log(ex.getMessage());
            return ex.getStatus();
        }
        return  SpindleStatus.OK_STATUS;
    }

    static public IStatus removeTapestryNature(IJavaProject project)
    {
        try
        {
            TapestryCore.removeNatureFromProject(project.getProject(), TapestryCore.NATURE_ID);
        }
        catch (CoreException ex)
        {
            TapestryCore.log(ex.getMessage());
            return ex.getStatus();
        }
        return  SpindleStatus.OK_STATUS;
    }

    /**
     * @return a TapestryProject if this javaProject has the tapestry nature or null if Project has
     *         not tapestry nature
     */
    static public TapestryProject create(IJavaProject javaProject)
    {
        TapestryProject result = null;
        try
        {
            result = (TapestryProject) javaProject.getProject().getNature(TapestryCore.NATURE_ID);
        }
        catch (CoreException ex)
        {
            TapestryCore.log(ex.getMessage());
        }
        return result;
    }

    /**
     * @return a TapestryProject if this Project has the tapestry nature or null if Project doen't
     *         have the tapestry nature
     */
    static public ITapestryProject create(IProject project)
    {
        IJavaProject javaProject = JavaCore.create(project);
        if (javaProject != null)
        {
            return TapestryProject.create(javaProject);
        }
        else
        {
            return null;
        }
    }

    /**
     * The platform project this <code>TapestryProject</code> is based on
     */
    protected IProject fProject;

    protected IFolder fWebContextFolder;

    protected boolean fValidateWebXML = true;

    protected boolean fUsingExternalMetadata = false;

    protected boolean fMetadataLoaded = false;

    /** needed for project nature creation * */
    public TapestryProject()
    {
        super();
    }

    public void clearMetadata()
    {
        fMetadataLoaded = false;
        fUsingExternalMetadata = false;
    }

    public synchronized void checkMetadata()
    {
        if (fMetadataLoaded)
            return;
        try
        {
            
            fUsingExternalMetadata = loadMetadataFromExtensions();
            if (!fUsingExternalMetadata) {
                //load from default file (.tapestryplugin)
                DefaultTapestryMetadata meta = new DefaultTapestryMetadata(getProject(), false);
                fWebContextFolder = meta.getWebContextFolder();
                fValidateWebXML = meta.isValidatingWebXML();
            }
        }
        finally
        {
            fMetadataLoaded = true;
        }
    }

    private boolean loadMetadataFromExtensions()
    {
        ProjectExternalMetadataLocator locator = TapestryCore.getDefault()
                .getExternalMetadataLocator();
        IProject project = getProject();
        try
        {
            String[] natureIds = project.getDescription().getNatureIds();
            for (int i = 0; i < natureIds.length; i++)
            {
                try
                {
                    IFolder folder = locator.getWebContextRootFolder(natureIds[i], project);
                    if (folder != null && folder.exists())
                    {
                        fWebContextFolder = folder;
                        fValidateWebXML = false;
                        return true;
                    }
                }
                catch (CoreException e1)
                {
                    // failed - skip to the next one
                    e1.printStackTrace();
                }
            }
        }
        catch (CoreException e)
        {
            TapestryCore.log(e);
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.core.resources.IProjectNature#getProject()
     */
    public IProject getProject()
    {
        return fProject;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.core.resources.IProjectNature#setProject(org.eclipse.core.resources.IProject)
     */
    public void setProject(IProject project)
    {
        this.fProject = project;
    }

    /*
     * @see IProjectNature#configure()
     */
    public void configure() throws CoreException
    {
        addToBuildSpec(TapestryCore.BUILDER_ID);
    }

    /*
     * @see IProjectNature#deconfigure()
     */
    public void deconfigure() throws CoreException
    {
        removeFromBuildSpec(TapestryCore.BUILDER_ID);
        Markers.cleanProblemsForProject(getProject());

        TapestryArtifactManager.getTapestryArtifactManager().clearBuildState(getProject());

        if (!fUsingExternalMetadata)
        {
            DefaultTapestryMetadata meta = new DefaultTapestryMetadata(getProject(), false);
            meta.clearProperties();
        }
    }

    public boolean isOnOutputPath(IPath candidate)
    {
        try
        {
            IPath output = getJavaProject().getOutputLocation();
            return pathCheck(output, candidate);
        }
        catch (CoreException e)
        {
            TapestryCore.log(e);
        }
        return false;
    }

    public boolean isOnSourcePath(IPath candidate)
    {
        try
        {
            IPackageFragmentRoot[] roots = getJavaProject().getPackageFragmentRoots();
            for (int i = 0; i < roots.length; i++)
            {
                if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE)
                {
                    IPath rootpath = roots[i].getUnderlyingResource().getFullPath();
                    if (pathCheck(rootpath, candidate))
                        return true;
                }

            }
        }
        catch (CoreException e)
        {
            TapestryCore.log(e);
        }
        return false;
    }

    private boolean pathCheck(IPath existing, IPath candidate)
    {
        if (existing.equals(candidate))
            return true;

        if (candidate.segmentCount() < existing.segmentCount())
            return false;

        return existing.matchingFirstSegments(candidate) == existing.segmentCount();
    }

    public IJavaProject getJavaProject() throws CoreException
    {
        return (IJavaProject) getProject().getNature(JavaCore.NATURE_ID);
    }

    public boolean isValidatingWebXML()
    {
        if (fUsingExternalMetadata)
            return false;
        checkMetadata();
        return fValidateWebXML;
    }
    
    public boolean isUsingExternalMetadata() {
       return fUsingExternalMetadata; 
    }

    public ClasspathRootLocation getClasspathRoot() throws CoreException
    {
        return new ClasspathRootLocation(getJavaProject());
    }

    public IFolder getWebContextFolder()
    {
        checkMetadata();
        return fWebContextFolder;
    }

    public ContextRootLocation getWebContextLocation()
    {
        IFolder folder = getWebContextFolder();
        if (folder == null || !folder.exists())
            return null;

        return new ContextRootLocation(folder);
    }

    protected void addToBuildSpec(String builderID) throws CoreException
    {
        IProjectDescription description = getProject().getDescription();
        ICommand javaCommand = getTapestryCommand(description);

        if (javaCommand == null)
        {
            ICommand command = description.newCommand();
            command.setBuilderName(builderID);
            setTapestryCommand(description, command);
        }
    }

    protected void removeFromBuildSpec(String builderID) throws CoreException
    {
        IProjectDescription description = getProject().getDescription();
        ICommand[] commands = description.getBuildSpec();
        for (int i = 0; i < commands.length; ++i)
        {
            if (commands[i].getBuilderName().equals(builderID))
            {
                ICommand[] newCommands = new ICommand[commands.length - 1];
                System.arraycopy(commands, 0, newCommands, 0, i);
                System.arraycopy(commands, i + 1, newCommands, i, commands.length - i - 1);
                description.setBuildSpec(newCommands);
                getProject().setDescription(description, null);
                return;
            }
        }
    }

    private ICommand getTapestryCommand(IProjectDescription description) 
    {
        ICommand[] commands = description.getBuildSpec();
        for (int i = 0; i < commands.length; ++i)
        {
            if (commands[i].getBuilderName().equals(TapestryCore.BUILDER_ID))
                return commands[i];

        }
        return null;
    }

    private void setTapestryCommand(IProjectDescription description, ICommand newCommand)
            throws CoreException
    {
        ICommand[] oldCommands = description.getBuildSpec();
        ICommand oldTapestryCommand = getTapestryCommand(description);
        ICommand[] newCommands;

        if (oldTapestryCommand == null)
        {
            // Add a Tapestry build spec to the end of the command list
            newCommands = new ICommand[oldCommands.length + 1];
            System.arraycopy(oldCommands, 0, newCommands, 0, oldCommands.length);
            newCommands[newCommands.length - 1] = newCommand;
        }
        else
        {
            for (int i = 0, max = oldCommands.length; i < max; i++)
            {
                if (oldCommands[i] == oldTapestryCommand)
                {
                    oldCommands[i] = newCommand;
                    break;
                }
            }
            newCommands = oldCommands;
        }

        // Commit the spec change into the project
        description.setBuildSpec(newCommands);
        getProject().setDescription(description, null);
    }

}