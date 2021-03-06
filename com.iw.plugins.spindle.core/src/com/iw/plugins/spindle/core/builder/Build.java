package com.iw.plugins.spindle.core.builder;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tapestry.IResourceLocation;
import org.apache.tapestry.spec.IApplicationSpecification;
import org.apache.tapestry.spec.IComponentSpecification;
import org.apache.tapestry.spec.ILibrarySpecification;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.w3c.dom.Document;

import com.iw.plugins.spindle.core.ITapestryMarker;
import com.iw.plugins.spindle.core.TapestryCore;
import com.iw.plugins.spindle.core.namespace.CoreNamespace;
import com.iw.plugins.spindle.core.namespace.ICoreNamespace;
import com.iw.plugins.spindle.core.parser.Parser;
import com.iw.plugins.spindle.core.resources.IResourceWorkspaceLocation;
import com.iw.plugins.spindle.core.resources.search.AbstractTapestrySearchAcceptor;
import com.iw.plugins.spindle.core.resources.search.ISearch;
import com.iw.plugins.spindle.core.resources.templates.ITemplateFinderListener;
import com.iw.plugins.spindle.core.scanning.ApplicationScanner;
import com.iw.plugins.spindle.core.scanning.ComponentScanner;
import com.iw.plugins.spindle.core.scanning.IScannerValidator;
import com.iw.plugins.spindle.core.scanning.IScannerValidatorListener;
import com.iw.plugins.spindle.core.scanning.LibraryScanner;
import com.iw.plugins.spindle.core.scanning.ScannerException;
import com.iw.plugins.spindle.core.scanning.SpecificationValidator;
import com.iw.plugins.spindle.core.scanning.TemplateScanner;
import com.iw.plugins.spindle.core.spec.BaseSpecLocatable;
import com.iw.plugins.spindle.core.spec.BaseSpecification;
import com.iw.plugins.spindle.core.spec.PluginApplicationSpecification;
import com.iw.plugins.spindle.core.spec.PluginComponentSpecification;
import com.iw.plugins.spindle.core.spec.PluginLibrarySpecification;
import com.iw.plugins.spindle.core.util.CoreUtils;
import com.iw.plugins.spindle.core.util.Markers;

/**
 * Abstract base class for full and incremental builds
 * 
 * @author glongman@gmail.com
 */
public abstract class Build implements IBuild, IScannerValidatorListener, ITemplateFinderListener
{

    private static ThreadLocal DEPENDENCY_LISTENER_HOLDER;

    protected static void setDependencyListener(BuilderDependencyListener listener)
    {
        if (DEPENDENCY_LISTENER_HOLDER == null)
            DEPENDENCY_LISTENER_HOLDER = new ThreadLocal();

        DEPENDENCY_LISTENER_HOLDER.set(listener);
    }

    public static IDependencyListener getDependencyListener()
    {
        if (DEPENDENCY_LISTENER_HOLDER == null)
            return null;

        return (IDependencyListener) DEPENDENCY_LISTENER_HOLDER.get();
    }

    protected ICoreNamespace fApplicationNamespace;

    protected BuilderQueue fBuildQueue;

    protected List fFoundTypes;

    protected ICoreNamespace fFrameworkNamespace;

    protected IJavaProject fJavaProject;

    protected State fLastState;

    protected List fMissingTypes;

    protected State fNewState;

    protected BuildNotifier fNotifier;

    protected Map fProcessedLocations;

    protected List fSeenTemplateExtensions;

    protected Map fTemplateMap;

    protected Map fFileSpecificationMap;

    protected Map fBinarySpecificationMap; // binary specs (from jars) never

    // change
    protected TapestryBuilder fTapestryBuilder;

    protected List fCleanTemplates; // templates that contain no implicit

    // components

    protected IType fTapestryServletType;

    protected SpecificationValidator.TypeFinder fTypeFinder;

    protected int fBuildMissPriority = TapestryCore.getDefault().getBuildMissPriority();

    private final int IGNORE_BUILD_MISSES = -1;

    public Build(TapestryBuilder builder)
    {
        fTapestryServletType = builder.getType(TapestryCore.getString(TapestryBuilder.STRING_KEY
                + "applicationServletClassname"));

        fTapestryBuilder = builder;
        fNewState = new State(builder);
        fBuildQueue = new BuilderQueue();
        fNotifier = builder.fNotifier;
        fJavaProject = builder.fJavaProject;
        fFoundTypes = new ArrayList();
        fMissingTypes = new ArrayList();
        fProcessedLocations = new HashMap();
        fSeenTemplateExtensions = new ArrayList();
        fTemplateMap = new HashMap();
        fFileSpecificationMap = new HashMap();
        fBinarySpecificationMap = new HashMap();
        TapestryArtifactManager.getTapestryArtifactManager().addTemplateFinderListener(this);
        fTypeFinder = new SpecificationValidator.TypeFinder(builder.fJavaProject);
        fCleanTemplates = new ArrayList();
    }

    public void build() throws BuilderException, CoreException
    {

        Parser parser = new Parser(false);

        preBuild();

        fNotifier.updateProgressDelta(0.1f);

        if (fBuildMissPriority != IGNORE_BUILD_MISSES)
            fNotifier.subTask(TapestryCore.getString(TapestryBuilder.STRING_KEY
                    + "locating-artifacts"));
        else
            fNotifier.subTask("building...");

        fNotifier.updateProgressDelta(0.10f);

        fBuildQueue.addAll(findAllTapestryArtifacts());

        fNotifier.checkCancel();

        resolveFramework(parser);

        try
        {
            doBuild(parser);

            postBuild();

        }
        finally
        {
            setDependencyListener(null);
        }
        
        fNotifier.subTask("cleaning up...");

        fNotifier.updateProgressDelta(0.15f);
        if (fBuildQueue.hasWaiting())
            handleBuildMisses();

        // TODO remove
        System.out.println("template count:" + fTemplateMap.keySet().size());
        System.out.println("file specification count" + fFileSpecificationMap.keySet().size());
        System.out.println("binary specification count" + fBinarySpecificationMap.keySet().size());
        saveState();
    }

    protected void handleBuildMisses()
    {

        fNotifier.setProcessingProgressPer(0.75f / fBuildQueue.getWaitingCount());
        if (fBuildMissPriority == IGNORE_BUILD_MISSES)
            return;

        while (fBuildQueue.getWaitingCount() > 0)
        {
            IResourceWorkspaceLocation location = (IResourceWorkspaceLocation) fBuildQueue
                    .peekWaiting();
            fNotifier.processed(location);
            fBuildQueue.finished(location);

            if (location.isWorkspaceResource())
            {
                IStorage storage = location.getStorage();
                IResource resource = (IResource) storage.getAdapter(IResource.class);
                recordBuildMiss(fBuildMissPriority, resource);
            }
        }
    }

    protected void recordBuildMiss(int missPriority, IResource resource)
    {
        if ("package.html".equals(resource.getName())) // TODO add real filter
            // capability.
            return;

        Markers.addProblemMarkerToResource(
                resource,
                ITapestryMarker.TAPESTRY_PROBLEM_MARKER,
                TapestryCore.getString("builder-missed-file-message", resource.getName()),
                missPriority,
                0,
                0,
                0);
    }

    protected abstract void saveState();

    /*
     * (non-Javadoc)
     * 
     * @see com.iw.plugins.spindle.core.builder.IBuild#cleanUp()
     */
    public void cleanUp()
    {
        TapestryArtifactManager.getTapestryArtifactManager().removeTemplateFinderListener(this);
        fLastState = null;
        fFoundTypes = null;
        fMissingTypes = null;
        fProcessedLocations = null;
        fSeenTemplateExtensions = null;
        fApplicationNamespace = null;
        fFrameworkNamespace = null;
        fBuildQueue = null;
        fNotifier = null;
        fTapestryBuilder = null;
        fProcessedLocations = null;
        fTemplateMap = null;
        fBinarySpecificationMap = null;
        fFileSpecificationMap = null;
        fTypeFinder = null;
        fCleanTemplates = null;
    }

    protected ICoreNamespace getPreBuiltNamespace(IResourceWorkspaceLocation location)
    {
        if (fLastState != null)
        {
            if (location.isBinary() && fLastState.fBinaryNamespaces.containsKey(location))
                return (ICoreNamespace) fLastState.fBinaryNamespaces.get(location);

        }
        return null;
    }

    protected ICoreNamespace createNamespace(Parser parser, String id, IStorage storage,
            IResourceWorkspaceLocation location, String encoding)
    {

        ICoreNamespace result = null;

        ILibrarySpecification lib = null;

        String name = location.getName();
        if (name.endsWith(".application"))
        {
            lib = resolveApplication(parser, storage, location, encoding);
        }
        else if (name.endsWith(".library"))
        {
            lib = resolveLibrarySpecification(parser, storage, location, encoding);
        }
        if (lib != null)
            result = new CoreNamespace(id, lib);

        ((BaseSpecLocatable) lib).setNamespace(result);

        return result;
    }

    /**
     * Perform the build.
     */
    protected abstract void doBuild(Parser parser);

    /**
     * Find and add all files with Tapestry extensions found in the classpath to a List.
     */
    protected void findAllArtifactsInClasspath(final ArrayList found)
    {
        if (TapestryCore.getDefault().getBuildMissPriority() == -1)
            return;
        
        fTapestryBuilder.fNotifier.checkCancel();

        ISearch searcher = null;
        try
        {
            searcher = fTapestryBuilder.fClasspathRoot.getSearch();
        }
        catch (CoreException e)
        {
            TapestryCore.log(e);
        }
        
        fTapestryBuilder.fNotifier.checkCancel();
        
        if (searcher != null)
        {
            searcher.search(new ArtifactCollector()
            {
                public boolean acceptTapestry(Object parent, IStorage storage)
                {
                    fTapestryBuilder.fNotifier.checkCancel();
                    IPackageFragment fragment = (IPackageFragment) parent;
                    IResourceWorkspaceLocation location = fTapestryBuilder.fClasspathRoot
                            .getRelativeLocation(fragment, storage);
                    found.add(location);

                    return keepGoing();
                }
            });
        }
        
        fTapestryBuilder.fNotifier.checkCancel();
    }

    /**
     * Find and add all files with Tapestry extensions found in the web context to a List.
     */
    protected void findAllArtifactsInWebContext(final ArrayList found)
    {
        
        fTapestryBuilder.fNotifier.checkCancel();
        
        ISearch searcher = null;
        try
        {
            searcher = fTapestryBuilder.fContextRoot.getSearch();
        }
        catch (CoreException e)
        {
            TapestryCore.log(e);
        }
        
        fTapestryBuilder.fNotifier.checkCancel();
        
        if (searcher != null)
        {
            searcher.search(new ArtifactCollector()
            {
                public boolean acceptTapestry(Object parent, IStorage storage)
                {
                    fTapestryBuilder.fNotifier.checkCancel();
                    IResource resource = (IResource) storage;
                    IResourceWorkspaceLocation location = fTapestryBuilder.fContextRoot
                            .getRelativeLocation(resource);

                    if (!fTapestryBuilder.conflictsWithJavaOutputDirectory(resource))
                        found.add(location);

                    return keepGoing();

                }
            });
        }
    }

    /**
     * Find all files in the project that have Tapestry extensions.
     * 
     * @return List a list containing all the Tapestry files in the project
     */
    protected List findAllTapestryArtifacts()
    {
        if (fBuildMissPriority == IGNORE_BUILD_MISSES)
            return Collections.EMPTY_LIST;

        long start = System.currentTimeMillis();
        try
        {
            ArrayList found = new ArrayList();
            findAllArtifactsInWebContext(found);
            findAllArtifactsInClasspath(found);
            return found;
        }
        finally
        {
            if (TapestryBuilder.DEBUG)
                System.out.println("findall:" + (System.currentTimeMillis() - start));
        }
    }

    /**
     * Completely process an application specification file, recording any problems encountered.
     * <p>
     * If the entity referred to by the location is really a file in the workspace, the problems are
     * recorded as resource markers. Otherwise, the problem is logged.
     */
    protected IApplicationSpecification resolveApplication(Parser parser, IStorage storage,
            IResourceLocation location, String encoding)
    {
        IResourceWorkspaceLocation useLocation = (IResourceWorkspaceLocation) location;
        IApplicationSpecification result = null;
        try
        {
            Document document = parseToDocument(
                    parser,
                    storage,
                    useLocation,
                    encoding == null ? "UTF-8" : encoding);
            
            fTapestryBuilder.fNotifier.checkCancel();
            // Node node = parseToNode(location);
            if (document != null)
            {
                ApplicationScanner scanner = new ApplicationScanner();
                scanner.setResourceInformation(storage, useLocation);
                scanner.setFactory(TapestryCore.getSpecificationFactory());

                IScannerValidator useValidator = new BuilderValidator(this, fTypeFinder, true);
                useValidator.addListener(this);

                try
                {
                    result = (IApplicationSpecification) scanner.scan(document, useValidator);
                    Markers.recordProblems(useLocation, scanner.getProblems());
                }
                finally
                {
                    fTapestryBuilder.fNotifier.checkCancel();
                    useValidator.removeListener(this);
                }

            }
            else
            {
                PluginApplicationSpecification dummy = new PluginApplicationSpecification();
                dummy.makePlaceHolder();
                dummy.setSpecificationLocation(location);
                result = dummy;
            }
            rememberSpecification(storage, (BaseSpecification) result);

        }
        catch (IOException e)
        {
            TapestryCore.log(e);
        }
        catch (CoreException e)
        {
            TapestryCore.log(e);
        }
        catch (ScannerException e)
        {
            recordFatalProblem(location, e);
        }
        finally
        {
            finished(useLocation);
        }
        return result;
    }

    protected void rememberSpecification(IStorage storage, BaseSpecification result)
    {
        if (storage == null)
        {
            Throwable t = new Throwable();
            t.fillInStackTrace();
            TapestryCore.log("unexpected null storage", t);
            return;
        }
        if (storage instanceof IFile)
        {
            fFileSpecificationMap.put(storage, result);
        }
        else
        {
            fBinarySpecificationMap.put(storage, result);
        }
    }

    protected void recordFatalProblem(IResourceLocation location, ScannerException e)
    {
        IResource resource = CoreUtils.toResource(location);
        if (resource != null)
            Markers.addProblemMarkerToResource(
                    resource,
                    ITapestryMarker.TAPESTRY_FATAL_PROBLEM_MARKER,
                    e.getMessage(),
                    IMarker.SEVERITY_ERROR,
                    1,
                    0,
                    0);
    }

    /**
     * Completely process a library specification file, recording any problems encountered.
     * <p>
     * If the entity referred to by the location is really a resource in the workspace, the problems
     * are recorded as resource markers. Otherwise, the problem is logged.
     */
    protected ILibrarySpecification resolveLibrarySpecification(Parser parser, IStorage storage,
            IResourceLocation location, String encoding)
    {
        fTapestryBuilder.fNotifier.checkCancel();
        IResourceWorkspaceLocation useLocation = (IResourceWorkspaceLocation) location;
        ILibrarySpecification result = null;
        if (fProcessedLocations.containsKey(useLocation))
            return (ILibrarySpecification) fProcessedLocations.get(useLocation);

        if (storage instanceof IFile) {
            Markers.removeProblemsFor((IFile) storage);
            fTapestryBuilder.fNotifier.checkCancel();
        }

        try
        {
            Document document = parseToDocument(
                    parser,
                    storage,
                    location,
                    encoding == null ? "UTF-8" : encoding);
            
            fTapestryBuilder.fNotifier.checkCancel();
            // Node node = parseToNode(location);
            if (document != null)
            {
                LibraryScanner scanner = new LibraryScanner();
                scanner.setResourceInformation(storage, useLocation);
                scanner.setFactory(TapestryCore.getSpecificationFactory());

                IScannerValidator useValidator = new BuilderValidator(this, fTypeFinder, true);
                try
                {
                    useValidator.addListener(this);
                    result = (ILibrarySpecification) scanner.scan(document, useValidator);

                }
                finally
                {
                    fTapestryBuilder.fNotifier.checkCancel();
                    useValidator.removeListener(this);
                }
                rememberSpecification(storage, (BaseSpecification) result);

                Markers.recordProblems(useLocation, scanner.getProblems());

            }
            else
            {
                PluginLibrarySpecification dummy = new PluginLibrarySpecification();
                dummy.setSpecificationLocation(location);
                dummy.makePlaceHolder();
                result = dummy;
            }
            rememberSpecification(storage, (BaseSpecification) result);
        }
        catch (IOException e)
        {
            TapestryCore.log(e);
        }
        catch (CoreException e)
        {
            TapestryCore.log(e);
        }
        catch (ScannerException e)
        {
            recordFatalProblem(location, e);
        }
        finally
        {
            finished(useLocation);
        }
        
        fTapestryBuilder.fNotifier.checkCancel();
        
        if (result != null)
            fProcessedLocations.put(useLocation, result);

        return result;
    }

    /**
     * Process completely all the templates found for a component or page. Problems encountered are
     * recorded.
     * <p>
     * If the entity referred to by the specification location is really a resource in the
     * workspace, the problems are recorded as resource markers. Otherwise, the problem is logged.
     */
    protected void parseTemplates(PluginComponentSpecification spec)
    {
        TemplateScanner scanner = new TemplateScanner();
        scanner.setFactory(TapestryCore.getSpecificationFactory());

        for (Iterator iter = spec.getTemplateLocations().iterator(); iter.hasNext();)
        {
            IResourceWorkspaceLocation location = (IResourceWorkspaceLocation) iter.next();

            if (fProcessedLocations.containsKey(location))
                continue;

            IStorage storage = location.getStorage();
            if (storage != null)
            {
                if (storage instanceof IFile)
                    Markers.removeProblemsFor((IFile) storage);

                fTemplateMap.put(storage, spec);

                try
                {
                    IResourceWorkspaceLocation specLoc = (IResourceWorkspaceLocation) spec
                            .getSpecificationLocation();
                    if (shouldParseTemplate(specLoc.getStorage(), storage))
                    {
                        IScannerValidator useValidator = new BuilderValidator(this, fTypeFinder,
                                true);
                        try
                        {
                            useValidator.addListener(this);
                            scanner.scanTemplate(spec, location, useValidator);
                        }
                        finally
                        {
                            useValidator.removeListener(this);
                        }

                        Markers.recordProblems(location, scanner.getProblems());

                        if (!scanner.containsImplicitComponents() && storage instanceof IResource) // we
                            // don't
                            // care
                            // if
                            // specs
                            // in
                            // jars
                            // are
                            // clean
                            fCleanTemplates.add(storage);
                    }
                    fProcessedLocations.put(location, null);
                }
                catch (ScannerException e)
                {
                    TapestryCore.log(e);
                }
                catch (CoreException e)
                {
                    TapestryCore.log(e);
                }
                catch (RuntimeException e)
                {
                    TapestryCore.log(e);
                    throw e;
                }
                finally
                {
                    finished(location);
                }

            }
        }
    }

    /**
     * Always returns true here. Incremental builds may override to check if the template really
     * needs to be parsed.
     * 
     * @param ownerSpec
     *            the IStorage for the specification that owns this template
     * @param template
     *            the IStorage for the template
     * @return true iff the template should be parsed (expensive)
     */
    protected boolean shouldParseTemplate(IStorage ownerSpec, IStorage template)
    {
        return true;
    }

    /**
     * Invoke the Parser on an xml file given a resource location. Problems encountered by the
     * Parser are recorded.
     * <p>
     * If the entity referred to by the location is really a resource in the workspace, the problems
     * are recorded as resource markers. Otherwise, the problem is logged.
     */
    protected Document parseToDocument(Parser parser, IStorage storage, IResourceLocation location,
            String encoding) throws IOException, CoreException
    {
        if (storage != null)
        {
            return parseToDocument(parser, storage, encoding);
        }
        else
        {
            throw new IOException(TapestryCore.getString("core-resource-does-not-exist", location));
        }
    }

    /**
     * Invoke the Parser on an xml file given an instance of IStorage. Problems encountered by the
     * Parser are recorded.
     * <p>
     * If the IStorage is really a resource in the workspace, the problems are recorded as resource
     * markers. Otherwise, the problem is logged.
     */
    protected Document parseToDocument(Parser parser, IStorage storage, String encoding)
            throws IOException, CoreException
    {
        Document result = null;
        try
        {
            result = parser.parse(storage, encoding);

        }
        catch (CoreException e)
        {
            TapestryCore.log(e);
            throw e;
        }
        catch (IOException e)
        {
            TapestryCore.log(e);
            throw e;
        }

        Markers.recordProblems(storage, parser.getProblems());

        if (parser.getHasFatalErrors())
            return null;

        return result;
    }

    /**
     * Perform any work that needs to be done before the build proper can begin. For example the
     * application builder parses web.xml here.
     */
    protected abstract void preBuild() throws CoreException;

    /**
     * Perform any work that need to happen after doBuild()
     * 
     * @param parser
     */
    protected abstract void postBuild();

    /**
     * Resolve the Tapestry Framework namespace here. Library builds check to see if the library
     * being built *is* the framework library. If so it skips this step.
     */
    protected abstract void resolveFramework(Parser parser);

    protected IComponentSpecification resolveIComponentSpecification(Parser parser,
            ICoreNamespace namespace, IStorage storage, IResourceWorkspaceLocation location,
            String encoding)

    {

        fTapestryBuilder.fNotifier.checkCancel();
        // to avoid double parsing specs that are accessible
        // by multiple means in Tapestry
        if (fProcessedLocations.containsKey(location))
            return (IComponentSpecification) fProcessedLocations.get(location);

        if (TapestryBuilder.DEBUG)
            System.out.println("parsing: " + location);

        PluginComponentSpecification result = null;
        if (storage != null) {
            fTapestryBuilder.fNotifier.checkCancel();
            if (storage instanceof IFile)
                Markers.removeProblemsFor((IFile) storage);
        }

        try
        {
            Document document = parseToDocument(
                    parser,
                    storage,
                    location,
                    encoding == null ? "UTF-8" : encoding);
            // while editor reconcilers can re-use the scanner, we can't here
            // because scanning may invoke the scanning of another, nested,
            // component.
            
            fTapestryBuilder.fNotifier.checkCancel();
            
            ComponentScanner scanner = new ComponentScanner();

            if (document != null)
            {
                scanner.setResourceInformation(storage, location);
                scanner.setNamespace(namespace);
                scanner.setFactory(TapestryCore.getSpecificationFactory());

                IScannerValidator useValidator = new BuilderValidator(this, fTypeFinder, true);
                useValidator.addListener(this);
                try
                {
                    result = (PluginComponentSpecification) scanner.scan(document, useValidator);
                    if (result != null)
                        result.setTemplateLocations(TapestryBuilder.scanForTemplates(
                                result,
                                scanner));

                }
                finally
                {
                    fTapestryBuilder.fNotifier.checkCancel();
                    useValidator.removeListener(this);
                }

                Markers.recordProblems(location, scanner.getProblems());

                rememberSpecification(storage, result);
            }
            else
            {
                PluginComponentSpecification dummy = new PluginComponentSpecification();
                dummy.makePlaceHolder();
                dummy.setSpecificationLocation(location);
                dummy.setNamespace(namespace);
                dummy.setTemplateLocations(TapestryBuilder.scanForTemplates(dummy, null));
                result = dummy;
            }

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (CoreException e)
        {
            e.printStackTrace();
        }
        catch (ScannerException e)
        {
            recordFatalProblem(location, e);

        }
        finally
        {
            finished(location);

        }

        fTapestryBuilder.fNotifier.checkCancel();

        if (result != null)
            fProcessedLocations.put(location, result);
        return result;
    }

    protected void finished(IResourceLocation location)
    {
        if (fBuildQueue.isWaiting(location))
            fBuildQueue.finished(location);

        fNotifier.processed(location);
    }

    /**
     * Template extensions seen during a build are stored in the State Used by Incremental builds.
     */
    public void templateExtensionSeen(String extension)
    {
        if (!fSeenTemplateExtensions.contains(extension))
            fSeenTemplateExtensions.add(extension);

    }

    /**
     * Trigger fired whenever a type check occurs during a build. Type names are stored in the State
     * and used to by incremental builds
     */
    public void typeChecked(String fullyQualifiedName, IType result)
    {
        if (result == null)
        {
            if (!fMissingTypes.contains(fullyQualifiedName))
                fMissingTypes.add(fullyQualifiedName);
        }
        else if (!result.isBinary())
        {
            try
            {
                IResource resource = result.getUnderlyingResource();
                if (!fFoundTypes.contains(resource))
                    fFoundTypes.add(resource);
            }
            catch (JavaModelException e)
            {
                TapestryCore.log(e);
            }
        }
    }

    /**
     * A search acceptor that is used to find all the Tapestry artifacts in the web context or the
     * classpath.
     */
    private abstract class ArtifactCollector extends AbstractTapestrySearchAcceptor
    {

        public ArtifactCollector()
        {
            super(AbstractTapestrySearchAcceptor.ACCEPT_ANY);
        }

        public boolean keepGoing()
        {
            try
            {
                fTapestryBuilder.fNotifier.checkCancel();
            }
            catch (OperationCanceledException e)
            {
                return false;
            }
            return true;
        }
    }

    /**
     * A depenency listener that does nothing.
     * 
     * @author glongman@gmail.com
     */
    class NullDependencyListener implements IDependencyListener
    {
        /*
         * (non-Javadoc)
         * 
         * @see com.iw.plugins.spindle.core.builder.IDependencyListener#foundResourceDependency(com.iw.plugins.spindle.core.resources.IResourceWorkspaceLocation,
         *      com.iw.plugins.spindle.core.resources.IResourceWorkspaceLocation)
         */
        public void foundResourceDependency(IResourceWorkspaceLocation dependant,
                IResourceWorkspaceLocation dependancy)
        {
            // do nothing
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.iw.plugins.spindle.core.builder.IDependencyListener#foundTypeDependency(com.iw.plugins.spindle.core.resources.IResourceWorkspaceLocation,
         *      java.lang.String)
         */
        public void foundTypeDependency(IResourceWorkspaceLocation dependant,
                String fullyQualifiedTypeName)
        {
            // do nothing
        }
    }

    protected class BuilderDependencyListener implements IDependencyListener
    {

        Map dependencyMap;

        /*
         * (non-Javadoc)
         * 
         * @see com.iw.plugins.spindle.core.builder.IDependencyListener#foundResourceDependency(com.iw.plugins.spindle.core.resources.IResourceWorkspaceLocation,
         *      com.iw.plugins.spindle.core.resources.IResourceWorkspaceLocation)
         */
        public void foundResourceDependency(IResourceWorkspaceLocation dependant,
                IResourceWorkspaceLocation dependancy)
        {
            if (dependant.isBinary())
                return;

            if (dependencyMap == null)
                dependencyMap = new HashMap();

            getInfo(dependant).resourceDeps.add(dependancy);

        }

        private DependencyInfo getInfo(IResourceWorkspaceLocation dependant)
        {
            DependencyInfo info = (DependencyInfo) dependencyMap.get(dependant);
            if (info == null)
            {
                info = new DependencyInfo();
                dependencyMap.put(dependant, info);
            }
            return info;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.iw.plugins.spindle.core.builder.IDependencyListener#foundTypeDependency(com.iw.plugins.spindle.core.resources.IResourceWorkspaceLocation,
         *      java.lang.String)
         */
        public void foundTypeDependency(IResourceWorkspaceLocation dependant,
                String fullyQualifiedTypeName)
        {
            if (dependant.isBinary())
                return;

            if (dependencyMap == null)
                dependencyMap = new HashMap();

            getInfo(dependant).typeDeps.add(fullyQualifiedTypeName);
        }

        public void clear()
        {
            if (dependencyMap != null)
                dependencyMap.clear();
        }

        public void dump()
        {
            if (dependencyMap == null)
            {
                System.out.println("no deps found");
            }
            else
            {
                for (Iterator iter = dependencyMap.keySet().iterator(); iter.hasNext();)
                {
                    IResourceWorkspaceLocation element = (IResourceWorkspaceLocation) iter.next();
                    System.out.println("Deps for: " + element);

                    DependencyInfo info = getInfo(element);
                    for (Iterator iterator = info.resourceDeps.iterator(); iterator.hasNext();)
                    {
                        System.err.println(iterator.next());
                    }
                    for (Iterator iterator = info.typeDeps.iterator(); iterator.hasNext();)
                    {
                        System.err.println(iterator.next());
                    }
                }
            }
        }
    }

    protected class DependencyInfo
    {
        Set resourceDeps = new HashSet();

        Set typeDeps = new HashSet();
    }

}