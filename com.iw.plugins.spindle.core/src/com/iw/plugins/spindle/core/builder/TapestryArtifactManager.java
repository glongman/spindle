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

package com.iw.plugins.spindle.core.builder;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.tapestry.INamespace;
import org.apache.tapestry.spec.IApplicationSpecification;
import org.apache.tapestry.spec.IComponentSpecification;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;

import com.iw.plugins.spindle.core.TapestryCore;
import com.iw.plugins.spindle.core.resources.templates.ITemplateFinderListener;
import com.iw.plugins.spindle.core.spec.BaseSpecification;
import com.iw.plugins.spindle.core.util.Assert;
import com.iw.plugins.spindle.core.util.Markers;

/**
 * The <code>TapestryArtifactManager</code> manages all the Tapestry Artifacts in the workspace.
 * The single instance of <code>TapestryArtifactManager</code> is available from the static method
 * <code>TapestryArtifactManager.getTapestryArtifactManager()</code>. right now the models/build
 * states are not persited between sessions.
 * 
 * @author glongman@gmail.com
 */
public class TapestryArtifactManager implements ITemplateFinderListener
{

    static final Object MANAGER_JOB_FAMILY = new Object();

    static private TapestryArtifactManager instance = new TapestryArtifactManager();

    static public final TapestryArtifactManager getTapestryArtifactManager()
    {

        return instance;
    }

    final ILock fNonJobBuildLock = Platform.getJobManager().newLock();

    Map fProjectBuildStates = new HashMap();

    List fTemplateExtensionListeners;

    private TapestryArtifactManager()
    {
        super();
    }

    /**
     * Sets the last built state for the given project, or null to reset it.
     */
    public void setLastBuildState(IProject project, Object state)
    {
        if (!TapestryCore.hasTapestryNature(project))
            return;

        setProjectState(project, state);
    }

    public void clearBuildState(IProject project)
    {
        removeProjectState(project);
    }

    // get the build state if exists, if not build using context - this one always
    // blocks
    public Object getLastBuildState(final IProject project, final boolean buildIfRequired,
            IRunnableContext context)
    {
        if (project == null)
            return null;

        if (!TapestryCore.hasTapestryNature(project))
            return null;

        Object state = getProjectState(project);

        if (state == null && (buildIfRequired && canBuild(project)))
        {
            IRunnableWithProgress runnable = new IRunnableWithProgress()
            {
                public void run(IProgressMonitor monitor) throws InvocationTargetException,
                        InterruptedException
                {
                    try
                    {
                        fNonJobBuildLock.acquire();
                        Object state = getProjectState(project);

                        if (state == null && (buildIfRequired && canBuild(project)))
                        {
                            project.build(
                                    IncrementalProjectBuilder.FULL_BUILD,
                                    TapestryCore.BUILDER_ID,
                                    new HashMap(),
                                    monitor);
                        }
                    }
                    catch (CoreException e)
                    {
                        TapestryCore.log(e);
                    }
                    finally
                    {
                        fNonJobBuildLock.release();
                    }
                }
            };

            try
            {
                context.run(false, false, runnable);
            }
            catch (Exception e)
            {
                TapestryCore.log(e);
            }
            state = getProjectState(project);
        }
        return state;
    }

    public synchronized void pingProjectState(IProject project)
    {
        Assert.isLegal(project != null);
        getLastBuildState(project, true, false);
    }

    // will block if a build is indicated
    public synchronized Object getLastBuildState(IProject project, boolean buildIfRequired)
    {
        return getLastBuildState(project, buildIfRequired, false);
    }

    // may block if a build is indicated
    public synchronized Object getLastBuildState(IProject project, boolean buildIfRequired,
            boolean block)
    {
        if (project == null)
            return null;

        if (!TapestryCore.hasTapestryNature(project))
            return null;

        Object state = getProjectState(project);
        if (state == null && (buildIfRequired && canBuild(project)))
        {
            buildStateIfPossible(project, block);
            state = getProjectState(project);
        }
        return state;
    }

    boolean canBuild(IProject project)
    {
        if (project == null || !project.isAccessible())
            return false;

        return Markers.getBrokenBuildProblemsFor(project).length == 0;
    }

    // the fsking hashcode on IProjects is never the same twice!

    Object getProjectState(IProject project)
    {
        return fProjectBuildStates.get(project.getFullPath());
    }

    private void setProjectState(IProject project, Object state)
    {
        fProjectBuildStates.put(project.getFullPath(), state);
    }

    private void removeProjectState(IProject project)
    {
        fProjectBuildStates.remove(project);
    }

    private void buildStateIfPossible(final IProject project, boolean block)
    {

        if (project == null || !project.isAccessible())
            return;
        // don't bother building if the last one was busted beyond saving!
        if (project == null || !project.isAccessible()
                || Markers.getBrokenBuildProblemsFor(project).length > 0)
            return;

        Job buildJob = findBuildJob(project);
        try
        {
              if (false)
//            if (block)
                buildJob.join();
        }
        catch (InterruptedException e)
        {
            // eat it
        }
    }

    private Job findBuildJob(IProject project)
    {
        Assert.isLegal(project != null);
        Job buildJob = null;
        Job[] jobs = Platform.getJobManager().find(MANAGER_JOB_FAMILY);
        for (int i = 0; i < jobs.length; i++)
        {
            if (((BuildJob) jobs[i]).getProject().equals(project))
            {
                buildJob = jobs[i];
                break;
            }
        }
        if (buildJob == null)
        {
            buildJob = new BuildJob(project);
            buildJob.setRule(project.getParent());
            buildJob.setUser(true);
            buildJob.schedule();
        }
        return buildJob;

    } /*
         * (non-Javadoc)
         * 
         * @see com.iw.plugins.spindle.core.scanning.IScannerValidator#addListener(com.iw.plugins.spindle.core.scanning.IScannerValidatorListener)
         */

    public void addTemplateFinderListener(ITemplateFinderListener listener)
    {
        if (fTemplateExtensionListeners == null)
            fTemplateExtensionListeners = new ArrayList();

        if (!fTemplateExtensionListeners.contains(listener))
            fTemplateExtensionListeners.add(listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iw.plugins.spindle.core.scanning.IScannerValidator#removeListener(com.iw.plugins.spindle.core.scanning.IScannerValidatorListener)
     */
    public void removeTemplateFinderListener(ITemplateFinderListener listener)
    {
        if (fTemplateExtensionListeners != null)
            fTemplateExtensionListeners.remove(listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iw.plugins.spindle.core.resources.templates.ITemplateFinderListener#templateExtensionSeen(java.lang.String)
     */
    public void templateExtensionSeen(String extension)
    {
        if (fTemplateExtensionListeners == null)
            return;

        for (Iterator iter = fTemplateExtensionListeners.iterator(); iter.hasNext();)
        {
            ITemplateFinderListener listener = (ITemplateFinderListener) iter.next();
            listener.templateExtensionSeen(extension);
        }

    }

    public Map getTemplateMap(IProject project)
    {
        return getTemplateMap(project, true);
    }

    public Map getTemplateMap(IProject project, boolean buildIfRequired)
    {
        State state = (State) getLastBuildState(project, buildIfRequired);
        if (state != null)
            return state.fTemplateMap;
        return null;
    }

    public void invalidateBuildStates()
    {
        fProjectBuildStates.clear();
    }

    public Map getSpecMap(IProject project)
    {
        return getSpecMap(project, true);
    }

    public Map getSpecMap(IProject project, boolean buildIfRequired)
    {
        State state = (State) getLastBuildState(project, buildIfRequired);
        if (state != null)
            return state.getSpecificationMap();
        return null;
    }

    public INamespace getProjectNamespace(IProject project)
    {
        return getProjectNamespace(project, true);
    }

    public INamespace getProjectNamespace(IProject project, boolean buildIfRequired)
    {
        State state = (State) getLastBuildState(project, buildIfRequired);
        if (state != null)
            return state.fPrimaryNamespace;
        return null;
    }

    public INamespace getFrameworkNamespace(IProject project)
    {
        return getFrameworkNamespace(project, true);
    }

    public INamespace getFrameworkNamespace(IProject project, boolean buildIfRequired)
    {
        State state = (State) getLastBuildState(project, buildIfRequired);
        if (state != null)
            return state.fFrameworkNamespace;
        return null;
    }

    /**
     * This will not build the project if its not built
     * 
     * @param fProject
     * @param string
     * @return the Specification objects that refer to the type.
     */
    public List findTypeRefences(IProject project, String fullyQualifiedTypeName)
    {
        Assert.isNotNull(project);
        if (fullyQualifiedTypeName == null || fullyQualifiedTypeName.trim().length() == 0)
            return Collections.EMPTY_LIST;
        State buildState = (State) getLastBuildState(project, false, false);
        if (buildState == null)
            return Collections.EMPTY_LIST;
        List result = new ArrayList();
        Map specMap = buildState.getSpecificationMap();
        for (Iterator iter = specMap.keySet().iterator(); iter.hasNext();)
        {
            Object key = iter.next();
            BaseSpecification spec = (BaseSpecification) specMap.get(key);
            if (spec != null)
            {

                switch (spec.getSpecificationType())
                {
                    case BaseSpecification.APPLICATION_SPEC:
                        String engineSpec = ((IApplicationSpecification) spec).getEngineClassName();
                        if (engineSpec != null && engineSpec.equals(fullyQualifiedTypeName))
                            result.add(spec);
                        break;
                    case BaseSpecification.COMPONENT_SPEC:
                        String componentSpec = ((IComponentSpecification) spec)
                                .getComponentClassName();
                        if (componentSpec != null && componentSpec.equals(fullyQualifiedTypeName))
                            result.add(spec);
                        break;

                    default:
                        break;
                }
            }
        }
        return result;
    }

}