package net.sf.spindle.core.builder;

import java.util.Arrays;
import java.util.List;

import net.sf.spindle.core.TapestryCore;
import net.sf.spindle.core.build.AbstractBuildInfrastructure;
import net.sf.spindle.core.build.BuilderException;
import net.sf.spindle.core.build.FullBuild;
import net.sf.spindle.core.build.IIncrementalBuild;
import net.sf.spindle.core.build.State;
import net.sf.spindle.core.extensions.eclipse.IncrementalBuildVetoController;
import net.sf.spindle.core.namespace.ICoreNamespace;
import net.sf.spindle.core.resources.ICoreResource;
import net.sf.spindle.core.resources.eclipse.IEclipseResource;
import net.sf.spindle.core.util.eclipse.EclipseUtils;

import org.apache.hivemind.Resource;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

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

/**
 * Builds a Tapestry Application project incrementally Well, sort of. An incremental build will not
 * reprocess the framework namespace or any libraries found in jar files. Other than that its the
 * same as a full build. TODO Not Used anymore - to be removed
 * 
 * @see core.builder.IncrementalProjectBuild
 * @author glongman@gmail.com
 */
public abstract class AbstractIncrementalEclipseBuild extends FullBuild implements
        IIncrementalBuild
{
    public static int REMOVED_REPLACED = IResourceDelta.REMOVED | IResourceDelta.REPLACED;

    public static int MOVED_OR_SYNCHED_OR_CHANGED_TYPE = IResourceDelta.MOVED_FROM
            | IResourceDelta.MOVED_TO | IResourceDelta.SYNC | IResourceDelta.TYPE;

    protected IResourceDelta projectDelta = null;

    /**
     * Constructor for IncrementalBuilder.
     * 
     * @param builder
     */
    public AbstractIncrementalEclipseBuild(EclipseBuildInfrastructure builder,
            IResourceDelta projectDelta)
    {
        super(builder);
        this.projectDelta = projectDelta;
    }

    /**
     * Basic incremental build check. called by sub implementations in IncrementalBuild classes
     * before thier own checks.
     * <p>
     * An incremental build is possible if:
     * <ul>
     * <li>a file recognized as a Tapestry template was changed, added, or deleted</li>
     * <li>a java type referenced by a Tapestry file was changed, added, or deleted</li>
     * <li>a Tapestry xml file was changed, added, or deleted</li>
     * </ul>
     * Note that before this method is called it has already been determined that an incremental
     * build is indicated (i.e. web.xml has not changed, last build did not fail, etc).
     */
    public boolean needsIncrementalBuild()
    {
        if (projectDelta == null)
            return false;
        lastState = infrastructure.getLastState();
        projectPropertySource = infrastructure
                .installBasePropertySource(lastState.webAppDescriptor);

        // TODO - this is not right - template extension is configurable
        final List knownTapestryExtensions = Arrays
                .asList(AbstractBuildInfrastructure.KnownExtensions);

        // check for java files that changed, or have been added
        try
        {
            projectDelta.accept(new IResourceDeltaVisitor()
            {
                public boolean visit(IResourceDelta delta) throws CoreException
                {
                    IResource resource = delta.getResource();

                    if (resource instanceof IContainer)
                        return true;

                    IPath path = resource.getFullPath();
                    String extension = path.getFileExtension();

                    if (lastState.seenTemplateExtensions.contains(extension))
                        throw new NeedToBuildException();

                    if (lastState.javaTypeDependencies.contains(resource)
                            || knownTapestryExtensions.contains(extension))
                    {
                        throw new NeedToBuildException();
                    }
                    else
                    {

                        if (!"java".equals(extension))
                            return true;

                        String name = path.removeFileExtension().lastSegment();
                        IContainer container = resource.getParent();
                        IJavaElement element = JavaCore.create((IFolder) container);
                        if (element == null)
                            return true;
                        if (element instanceof IPackageFragmentRoot
                                && lastState.missingJavaTypes.contains(name))
                        {
                            throw new NeedToBuildException();
                        }
                        else if (element instanceof IPackageFragment
                                && lastState.missingJavaTypes
                                        .contains(((IPackageFragment) element).getElementName()
                                                + "." + name))
                        {
                            throw new NeedToBuildException();
                        }

                    }
                    return true;
                }
            });
        }
        catch (CoreException e)
        {
            TapestryCore.log(e);
        }
        catch (NeedToBuildException e)
        {
            return true;
        }
        return false;

    }

    /**
     * An exception used to break out of a resource delta scan if an incremental build is indicated.
     * 
     * @see #needsIncrementalBuild(IResourceDelta)
     */
    private static class NeedToBuildException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;

        public NeedToBuildException()
        {
            super();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see core.builder.IIncrementalBuild#canIncrementalBuild()
     */
    public boolean canIncrementalBuild()
    {
        if (projectDelta == null)
            return false;

        lastState = infrastructure.getLastState();
        // ensure the last build didn't fail and that state version sync up.
        if (lastState == null || lastState.fBuildNumber < 0 || lastState.fVersion != State.VERSION)
            return false;

        // The Tapestry framework library must exist in the state
        ICoreResource frameworkLocation = (ICoreResource) classpathRoot
                .getRelativeResource("/org/apache/tapestry/Framework.library");
        if (!lastState.fBinaryNamespaces.containsKey(frameworkLocation))
            return false;

        // ensure the project classpath has not changed
        if (hasClasspathChanged())
            return false;

        // the context root exist and be the same as the one used for the last build.
        IEclipseResource eContextRoot = (IEclipseResource) contextRoot;
        if (eContextRoot != null)
        {
            if (!eContextRoot.equals(lastState.fContextRoot))
            {
                if (AbstractBuildInfrastructure.DEBUG)
                    System.out.println("inc build abort - context root not same in last state");
                return false;
            }

            if (!eContextRoot.exists())
            {
                if (AbstractBuildInfrastructure.DEBUG)
                    System.out.println("inc build abort - context root does not exist"
                            + contextRoot);
                return false;
            }

            // web.xml must exist
            IEclipseResource webXML = (IEclipseResource) contextRoot
                    .getRelativeResource("WEB-INF/web.xml");

            IResource resource = (IResource) webXML.getStorage();
            if (resource == null)
            {
                if (AbstractBuildInfrastructure.DEBUG)
                    System.out.println("inc build abort - web.xml does not exist" + webXML);
                return false;
            }

            // and it must not have changed
            IResourceDelta webXMLDelta = projectDelta.findMember(resource.getProjectRelativePath());

            if (webXMLDelta != null)
            {
                if (AbstractBuildInfrastructure.DEBUG)
                    System.out.println("inc build abort - web.xml changed since last build");
                return false;
            }

            // ensure the .application file did not change
            if (needFullBuildDueToAppSpecChange())
                return false;

        }
        else
        {
            // must have a context root
            if (AbstractBuildInfrastructure.DEBUG)
                System.out.println("inc build abort - no context root found in TapestryBuilder!");
            return false;
        }

        // contrbuted veto-ers must give thier ok to inc build
        IncrementalBuildVetoController vetoController = new IncrementalBuildVetoController();

        if (vetoController.vetoIncrementalBuild(projectDelta))
            return false;

        return true;
    }

    protected boolean hasClasspathChanged()
    {
        IClasspathEntry[] currentEntries = (IClasspathEntry[]) infrastructure.getClasspathMemento();
        IClasspathEntry[] lastStateEntries = (IClasspathEntry[]) lastState.classpathMemento;
        if (currentEntries.length != lastStateEntries.length)
            return true;

        List<IClasspathEntry> old = Arrays.asList(lastStateEntries);
        List<IClasspathEntry> current = Arrays.asList(currentEntries);

        return !current.containsAll(old);
    }

    private boolean needFullBuildDueToAppSpecChange()
    {
        ICoreResource appSpecLocation = lastState.fApplicationServlet.getApplicationSpecLocation();
        if (appSpecLocation != null)
        {
            IResource specResource = EclipseUtils.toResource(appSpecLocation);
            if (specResource == null)
                return false;
            IResourceDelta specDelta = projectDelta.findMember(specResource
                    .getProjectRelativePath());
            if (specDelta != null)
            {
                // can't incremental build if the application specification
                // has been deleted, replaced, moved, or synchonized with a source
                // repository.
                int kind = specDelta.getKind();
                if ((kind & IResourceDelta.NO_CHANGE) == 0)
                {
                    if ((kind & REMOVED_REPLACED) > 0)
                        return true;
                    int flags = specDelta.getFlags();
                    if ((flags & MOVED_OR_SYNCHED_OR_CHANGED_TYPE) > 0)
                        return true;
                }
            }
        }
        else
        {
            // here we check to see if there is an automagic app spec.
            ICoreNamespace last = lastState.primaryNamespace;
            if (last == null)
                return true;

            IResource existingSpecFile = null;
            Resource previousSpecLocation = last.getSpecificationLocation();
            ICoreResource WEB_INF = (ICoreResource) contextRoot.getRelativeResource("WEB-INF");

            if (!previousSpecLocation.equals(WEB_INF))
            {
                existingSpecFile = EclipseUtils.toResource(previousSpecLocation);
            }

            if (existingSpecFile != null)
            {
                IResourceDelta specDelta = projectDelta.findMember(existingSpecFile
                        .getProjectRelativePath());
                if (specDelta != null)
                {
                    // can't incremental build if the application specification
                    // has been deleted, replaced, moved, or synchonized with a source
                    // repository.
                    int kind = specDelta.getKind();
                    if ((kind & IResourceDelta.NO_CHANGE) == 0)
                    {
                        if ((kind & REMOVED_REPLACED) > 0)
                        {
                            if (AbstractBuildInfrastructure.DEBUG)
                                System.out.println("inc build abort - " + existingSpecFile
                                        + "was removed or replaced");
                            return true;
                        }
                        int flags = specDelta.getFlags();
                        if ((flags & MOVED_OR_SYNCHED_OR_CHANGED_TYPE) > 0)
                        {
                            if (AbstractBuildInfrastructure.DEBUG)
                                System.out.println("inc build abort - " + existingSpecFile
                                        + "was moved or synced");
                            return true;
                        }
                    }
                }
            }
            else
            {
                // now we had a synthetic, check to see if a real one has been added.
                try
                {
                    projectDelta.accept(new IResourceDeltaVisitor()
                    {
                        public boolean visit(IResourceDelta delta) throws CoreException
                        {
                            IResource resource = delta.getResource();
                            if (resource instanceof IFolder || resource instanceof IProject)
                                return true;
                            IFile file = (IFile) resource;
                            if ("application".equals(file.getFullPath().getFileExtension()))
                            {
                                if (AbstractBuildInfrastructure.DEBUG)
                                    System.out.println("inc build abort - new app spec found");
                                throw new BuilderException();
                            }
                            return true;
                        }

                    });
                }
                catch (BuilderException e)
                {
                    // an application file exists now where one did not before
                    // force a full build.
                    return true;
                }
                catch (CoreException e)
                {
                    TapestryCore.log(e);
                    return true;
                }
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see core.builder.FullBuild#saveState()
     */
    public void saveState()
    {
        State newState = new State();
        newState.copyAndAdvanceBuildNumber(lastState, infrastructure);
        newState.javaTypeDependencies = foundTypes;
        newState.missingJavaTypes = missingTypes;
        newState.templateMap = templateMap;
        newState.fileSpecificationMap = fileSpecificationMap;
        //FIXME newState.fPrimaryNamespace = appNamespace;
        newState.seenTemplateExtensions = seenTemplateExtensions;
        //FIXME newState.fCleanTemplates = cleanTemplates;

        infrastructure.persistState(newState);
    }

}