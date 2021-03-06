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

package net.sf.spindle.core.builder;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.spindle.core.ITapestryProject;
import net.sf.spindle.core.TapestryCore;
import net.sf.spindle.core.builder.EclipseBuildInfrastructure.EclipseState;
import net.sf.spindle.core.eclipse.TapestryCorePlugin;
import net.sf.spindle.core.eclipse.TapestryProject;
import net.sf.spindle.core.spec.BaseSpecification;
import net.sf.spindle.core.spec.PluginComponentSpecification;
import net.sf.spindle.core.util.Assert;
import net.sf.spindle.core.util.eclipse.EclipsePluginUtils;
import net.sf.spindle.core.util.eclipse.Markers;

import org.apache.tapestry.INamespace;
import org.apache.tapestry.engine.IPropertySource;
import org.apache.tapestry.spec.IApplicationSpecification;
import org.apache.tapestry.spec.IComponentSpecification;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;

/**
 * The <code>TapestryArtifactManager</code> manages all the Tapestry Artifacts in the workspace.
 * The single instance of <code>TapestryArtifactManager</code> is available from the static method
 * <code>TapestryArtifactManager.getTapestryArtifactManager()</code>. right now the models/build
 * states are not persited between sessions.
 * 
 * @author glongman@gmail.com
 */
public class TapestryArtifactManager
{

    static final Object MANAGER_JOB_FAMILY = new Object();

    static private TapestryArtifactManager INSTANCE = new TapestryArtifactManager();

    static public final TapestryArtifactManager getTapestryArtifactManager()
    {
        return INSTANCE;
    }

    private final ILock fNonJobBuildLock = Platform.getJobManager().newLock();

    private Map<IProject, EclipseState> fProjectBuildStates = new HashMap<IProject, EclipseState>();

    private TapestryArtifactManager()
    {
        super();
    }

    /**
     * Sets the last built state for the given project, or null to reset it.
     */
    public void setLastBuildState(IProject project, EclipseState state)
    {
        if (!EclipsePluginUtils.hasTapestryNature(project))
            return;

        setProjectState(project, state);
    }

    public void clearBuildState(IProject project)
    {
        fProjectBuildStates.remove(project);
    }

    // get the build state if exists, if not build using context - this one always
    // blocks
    public Object getLastBuildState(final IProject project, final boolean buildIfRequired,
            IRunnableContext context)
    {
        if (project == null)
            return null;

        if (!EclipsePluginUtils.hasTapestryNature(project))
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
                                    TapestryCorePlugin.BUILDER_ID,
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
        getLastBuildState(project, true);
    }

    // may block if a build is indicated
    public synchronized EclipseState getLastBuildState(IProject project, boolean buildIfRequired)
    {
        if (project == null)
            return null;

        if (!EclipsePluginUtils.hasTapestryNature(project))
            return null;

        EclipseState state = getProjectState(project);
        if (state == null && (buildIfRequired && canBuild(project)))
        {
            try
            {
                buildStateIfPossible(project);
                state = getProjectState(project);
            }
            catch (CoreException e)
            {
                TapestryCore.log(e);
            }
        }
        return state;
    }

    private boolean canBuild(IProject project)
    {
        if (project == null || !project.isAccessible())
            return false;

        return Markers.getBrokenBuildProblemsFor(project).length == 0;
    }

    // the fsking hashcode on IProjects is never the same twice!

    private EclipseState getProjectState(IProject project)
    {
        return fProjectBuildStates.get(project.getFullPath());
    }

    private void setProjectState(IProject project, EclipseState state)
    {
        fProjectBuildStates.put(project, state);
    }

    private void buildStateIfPossible(final IProject project) throws CoreException
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
    }

    public Map getTemplateMap(IProject project)
    {
        return getTemplateMap(project, true);
    }

    public Map<IStorage, PluginComponentSpecification> getTemplateMap(IProject project,
            boolean buildIfRequired)
    {
        EclipseState state = (EclipseState) getLastBuildState(project, buildIfRequired);
        if (state != null)
            return state.templateMap;
        return null;
    }

    public void invalidateBuildStates()
    {
        fProjectBuildStates.clear();
    }

    public Map<IStorage, ? extends BaseSpecification> getSpecMap(IProject project)
    {
        return getSpecMap(project, true);
    }

    public Map<IStorage, ? extends BaseSpecification> getSpecMap(IProject project,
            boolean buildIfRequired)
    {
        EclipseState state = getLastBuildState(project, buildIfRequired);
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
        EclipseState state = getLastBuildState(project, buildIfRequired);
        if (state != null)
            return state.primaryNamespace;
        return null;
    }

    public IPropertySource getPropertySource(ITapestryProject tproject)
    {
        IProject project = ((TapestryProject) tproject).getProject();
        EclipseState state = getLastBuildState(project, false);
        if (state != null)
            return state.webAppDescriptor;
        return null;
    }

    public INamespace getFrameworkNamespace(IProject project)
    {
        return getFrameworkNamespace(project, true);
    }

    public INamespace getFrameworkNamespace(IProject project, boolean buildIfRequired)
    {
        EclipseState state = getLastBuildState(project, buildIfRequired);
        if (state != null)
            return state.frameworkNamespace;
        return null;
    }

    /**
     * This will not build the project if its not built
     * 
     * @param fProject
     * @param string
     * @return the Specification objects that refer to the type.
     */
    public List<? extends BaseSpecification> findTypeRefences(IProject project,
            String fullyQualifiedTypeName)
    {
        Assert.isNotNull(project);
        if (fullyQualifiedTypeName == null || fullyQualifiedTypeName.trim().length() == 0)
            return Collections.emptyList();
        EclipseState buildState = getLastBuildState(project, false);
        if (buildState == null)
            return Collections.emptyList();
        List<BaseSpecification> result = new ArrayList<BaseSpecification>();
        Map<IStorage, ? extends BaseSpecification> specMap = buildState.getSpecificationMap();
        for (IStorage key : specMap.keySet())
        {
            BaseSpecification spec = specMap.get(key);
            if (spec == null)
                continue;
            switch (spec.getSpecificationType())
            {
                case APPLICATION_SPEC:
                    String engineClassname = ((IApplicationSpecification) spec)
                            .getEngineClassName();
                    if (engineClassname != null && engineClassname.equals(fullyQualifiedTypeName))
                        result.add(spec);
                    break;
                case COMPONENT_SPEC:
                    String componentClassname = ((IComponentSpecification) spec)
                            .getComponentClassName();
                    if (componentClassname != null
                            && componentClassname.equals(fullyQualifiedTypeName))
                        result.add(spec);
                    break;
                default:
                    break;
            }
        }
        return result;
    }
}