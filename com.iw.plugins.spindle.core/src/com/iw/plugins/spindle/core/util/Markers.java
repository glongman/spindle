package com.iw.plugins.spindle.core.util;

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

import java.util.ArrayList;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import com.iw.plugins.spindle.core.ITapestryMarker;
import com.iw.plugins.spindle.core.TapestryCore;
import com.iw.plugins.spindle.core.builder.TapestryBuilder;
import com.iw.plugins.spindle.core.resources.IResourceWorkspaceLocation;
import com.iw.plugins.spindle.core.source.IProblem;

/**
 * Marker utililties
 * 
 * @author glongman@gmail.com
 */
public class Markers
{

    public static final String TAPESTRY_MARKER_TAG = ITapestryMarker.TAPESTRY_PROBLEM_MARKER;

    public static final String TAPESTRY_BUILBROKEN_TAG = ITapestryMarker.TAPESTRY_BUILDBROKEN_MARKER;

    public static final String TAPESTRY_FATAL = ITapestryMarker.TAPESTRY_FATAL_PROBLEM_MARKER;

    public static final String TAPESTRY_SOURCE = ITapestryMarker.TAPESTRY_SOURCE_PROBLEM_MARKER;

    public static final String TAPESTRY_INTERESTING = ITapestryMarker.TAPESTRY_INTERESTING_PROJECT_MARKER;

    public static final String MARKER_CODE = "code";

    /**
     * Method addBuildBrokenProblemMarkerToResource.
     * 
     * @param iProject
     * @param string
     */
    public static void addBuildBrokenProblemMarkerToResource(IProject iProject, String message)
    {
        addProblemMarkerToResource(
                iProject,
                TAPESTRY_BUILBROKEN_TAG,
                message,
                IMarker.SEVERITY_ERROR,
                0,
                0,
                0);
    }

    public static void recordProblems(IStorage storage, IProblem[] problems)
    {
        IResource res = (IResource) storage.getAdapter(IResource.class);
        boolean workspace = res != null;
        for (int i = 0; i < problems.length; i++)
        {
            if (workspace)
            {
                Markers.addTapestryProblemMarkerToResource(res, problems[i]);
            }
            else
            {
                TapestryCore.logProblem(storage, problems[i]);
            }
        }
    }

    public static void recordProblems(IResourceWorkspaceLocation location, IProblem[] problems)
    {
        IResource res = CoreUtils.toResource(location);
        boolean workspace = res != null;
        for (int i = 0; i < problems.length; i++)
        {
            if (workspace)
            {
                addTapestryProblemMarkerToResource(res, problems[i]);
            }
            else
            {
                TapestryCore.logProblem(location.getStorage(), problems[i]);
            }
        }
    }

    public static void addTapestryProblemMarkersToResource(IResource resource, IProblem[] problems)
    {
        if (problems.length > 0)
            for (int i = 0; i < problems.length; i++)
                addTapestryProblemMarkerToResource(resource, problems[i]);

    }

    public static void addTapestryProblemMarkerToResource(IResource resource, IProblem problem)
    {
        try
        {
            IMarker marker = resource.createMarker(problem.getType());

            marker.setAttributes(
                    new String[]
                    { IMarker.MESSAGE, IMarker.SEVERITY, IMarker.LINE_NUMBER, IMarker.CHAR_START,
                            IMarker.CHAR_END, ITapestryMarker.TEMPORARY_FLAG,
                            ITapestryMarker.PROBLEM_CODE },
                    new Object[]
                    { problem.getMessage(), new Integer(problem.getSeverity()),
                            new Integer(problem.getLineNumber()),
                            new Integer(problem.getCharStart()), new Integer(problem.getCharEnd()),
                            new Boolean(problem.isTemporary()), new Integer(problem.getCode()) });

        }
        catch (CoreException e)
        {
            TapestryCore.log(e);
        }

    }

    // public static void addTapestryProblemMarkerToResource(
    // IResource resource,
    // String message,
    // int severity,
    // ISourceLocation source)
    // {
    //
    // addTapestryProblemMarkerToResource(
    // resource,
    // message,
    // severity,
    // source.getLineNumber(),
    // source.getCharStart(),
    // source.getCharEnd());
    //
    // }

    public static void addTapestryProblemMarkerToResource(IResource resource, String message,
            int severity, int lineNumber, int charStart, int charEnd)
    {

        addProblemMarkerToResource(
                resource,
                TAPESTRY_MARKER_TAG,
                message,
                new Integer(severity),
                new Integer(lineNumber),
                new Integer(charStart),
                new Integer(charEnd));
    }

    public static void addProblemMarkerToResource(IResource resource, String markerTag,
            String message, int severity, int lineNumber, int charStart, int charEnd)
    {

        addProblemMarkerToResource(
                resource,
                markerTag,
                message,
                new Integer(severity),
                new Integer(lineNumber),
                new Integer(charStart),
                new Integer(charEnd));
    }

    private static void addProblemMarkerToResource(IResource resource, String markerTag,
            String message, Integer severity, Integer lineNumber, Integer charStart, Integer charEnd)
    {
        try
        {
            IMarker marker = resource.createMarker(markerTag);

            marker.setAttributes(new String[]
            { IMarker.MESSAGE, IMarker.SEVERITY, IMarker.LINE_NUMBER, IMarker.CHAR_START,
                    IMarker.CHAR_END }, new Object[]
            { message, severity, lineNumber, charStart, charEnd });
        }
        catch (CoreException e)
        {
            TapestryCore.log(e);
        }

    }

    public static IMarker[] getBrokenBuildProblemsFor(IProject project)
    {
        try
        {
            if (project != null && project.exists())
            {

                return project.findMarkers(
                        Markers.TAPESTRY_BUILBROKEN_TAG,
                        true,
                        IResource.DEPTH_ZERO);
            }
        }
        catch (CoreException e)
        {
        } // assume there were no problems
        return new IMarker[0];
    }

    public static IMarker[] getProblemsFor(IResource resource)
    {
        try
        {
            if (resource != null && resource.exists())
                return resource.findMarkers(TAPESTRY_MARKER_TAG, false, IResource.DEPTH_INFINITE);
        }
        catch (CoreException e)
        {
        } // assume there are no problems
        return new IMarker[0];
    }

    public static void removeInterestingProjectMarkers(IProject homeProject, IResource resource,
            int depth)
    {
        long start = System.currentTimeMillis();
        try
        {
            String homePath = homeProject.getFullPath().toString();
            try
            {
                IMarker[] interestings = getInterestingProjectMarkers(resource, depth);

                if (interestings == null)
                    return;

                for (int i = 0; i < interestings.length; i++)
                {
                    String iHomePath = interestings[i].getAttribute("HOME", null);
                    if (iHomePath != null && homePath.equals(iHomePath))
                        interestings[i].delete();
                }
            }
            catch (CoreException e)
            {
                // assume there are no interestings
            }
        }
        finally
        {
            System.out.println("removeInteresting:" + (System.currentTimeMillis() - start));
        }
    }

    public static IMarker[] getInterestingProjectMarkers(IResource resource, int depth)
    {
        IMarker[] interestings = new IMarker[0];
        try
        {

            if (resource != null && resource.isAccessible())
                interestings = resource.findMarkers(TAPESTRY_INTERESTING, false, depth);
            return interestings;
        }
        catch (CoreException e)
        {
            // assume no interestings
        }

        return interestings;
    }

    public static IProject[] getHomeProjects(IProject interestingProject)
    {
        ArrayList result = new ArrayList();
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        if (root != null && root.isAccessible())
        {
            IMarker[] markers = getInterestingProjectMarkers(
                    interestingProject,
                    IResource.DEPTH_ZERO);

            for (int i = 0; i < markers.length; i++)
            {
                String mPath = markers[i].getAttribute("HOME", null);
                if (mPath == null)
                    continue;
                IPath path = new Path(mPath);
                IProject project = root.getProject(path.lastSegment());

                if (project.exists())
                    result.add(project);
            }
        }
        return (IProject[]) result.toArray(new IProject[result.size()]);
    }

    public static boolean hasHomeProject(IProject interestingProject, IProject homeProject)
    {
        IMarker[] interestings = getInterestingProjectMarkers(
                interestingProject,
                IResource.DEPTH_ZERO);
        if (interestings == null || interestings.length == 0)
            return false;

        String homePath = homeProject.getFullPath().toString();

        for (int i = 0; i < interestings.length; i++)
        {
            String iHomePath = interestings[i].getAttribute("HOME", null);
            if (iHomePath != null && homePath.equals(iHomePath))
                return true;
        }

        return false;
    }

    public static void addInterestingProjectMarkers(IProject homeProject,
            IProject[] interestingProjects)
    {
        for (int i = 0; i < interestingProjects.length; i++)
        {
            addInterestingProjectMarker(homeProject, interestingProjects[i]);
        }
    }

    public static void addInterestingProjectMarker(IProject homeProject, IProject interestingProject)
    {
        if (hasHomeProject(interestingProject, homeProject))
            return;

        String homePath = homeProject.getFullPath().toString();
        try
        {
            IMarker marker = interestingProject.createMarker(TAPESTRY_INTERESTING);

            marker.setAttributes(new String[]
            { "HOME" }, new Object[]
            { homePath });
        }
        catch (CoreException e)
        {
            TapestryCore.log(e);
        }
    }

    public static IMarker[] getFatalProblemsFor(IResource resource)
    {
        try
        {
            if (resource != null && resource.exists())
                return resource.findMarkers(TAPESTRY_FATAL, false, IResource.DEPTH_INFINITE);
        }
        catch (CoreException e)
        {
        } // assume there are no problems
        return new IMarker[0];
    }

    public static void removeProblemsFor(IResource resource)
    {
        try
        {
            if (resource != null && resource.exists())
            {
                resource
                        .deleteMarkers(Markers.TAPESTRY_MARKER_TAG, false, IResource.DEPTH_INFINITE);
                resource.deleteMarkers(Markers.TAPESTRY_FATAL, false, IResource.DEPTH_INFINITE);
                resource.deleteMarkers(Markers.TAPESTRY_SOURCE, false, IResource.DEPTH_INFINITE);
            }
        }
        catch (CoreException e)
        {
        } // assume there were no problems
    }

    /**
     * Method removeProblemsForProject.
     * 
     * @param project
     */
    public static void removeProblemsForProject(IProject project)
    {
        try
        {
            if (project != null && project.isAccessible())
            {

                project.deleteMarkers(Markers.TAPESTRY_MARKER_TAG, false, IResource.DEPTH_INFINITE);

                project.deleteMarkers(Markers.TAPESTRY_FATAL, false, IResource.DEPTH_INFINITE);

                project.deleteMarkers(Markers.TAPESTRY_BUILBROKEN_TAG, false, IResource.DEPTH_ZERO);
            }
        }
        catch (CoreException e)
        {
        } // assume there were no problems
    }

    /**
     * @param project
     */
    public static void cleanProblemsForProject(IProject project)
    {
        if (project != null && project.isAccessible())
        {
            try
            {
                removeProblemsForProject(project);
                project.deleteMarkers(Markers.TAPESTRY_SOURCE, false, IResource.DEPTH_INFINITE);

                IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

                removeInterestingProjectMarkers(project, root, IResource.DEPTH_ONE);
            }
            catch (CoreException e)
            {
                TapestryCore.log(e);
            }
        }

    }

    /**
     * @param fCurrentProject
     */
    public static void removeBuildProblemsForProject(IProject iProject)
    {
        try
        {
            if (iProject != null && iProject.exists())
            {
                iProject
                        .deleteMarkers(Markers.TAPESTRY_BUILBROKEN_TAG, false, IResource.DEPTH_ZERO);
            }
        }
        catch (CoreException e)
        {
        } // assume there were no problems

    }

    public static void removeTemporaryProblemsForResource(IResource resource)
    {
        IMarker[] markers = getProblemsFor(resource);
        for (int i = 0; i < markers.length; i++)
        {
            if (markers[i].getAttribute(ITapestryMarker.TEMPORARY_FLAG, false))
            {
                try
                {
                    markers[i].delete();
                }
                catch (CoreException e)
                {
                    TapestryCore.log(e);
                }
            }

        }

    }

}