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
 * Intelligent Works Incorporated.
 * Portions created by the Initial Developer are Copyright (C) 2003
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * 
 *  glongman@intelligentworks.com
 *
 * ***** END LICENSE BLOCK ***** */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IType;
import org.w3c.dom.Element;

import com.iw.plugins.spindle.core.processing.ILookupRequestor;
import com.iw.plugins.spindle.core.processing.TapestryLookup;

/**
 * Builds a Tapestry project from scratch.
 * 
 * @version $Id$
 * @author glongman@intelligentworks.com
 */
/*package*/
class FullBuild extends Build {

  protected IType tapestryServletType;
  protected Map knownValidServlets;
  protected Map infoCache;
  protected TapestryProjectInfo projectInfo;

  BuilderQueue applicationQueue;
  BuilderQueue libraryQueue;
  BuilderQueue pageQueue;
  BuilderQueue componentQueue;
  BuilderQueue htmlQueue;
  BuilderQueue scriptQueue;

  /**
   * Constructor for FullBuilder.
   */
  public FullBuild(TapestryBuilder builder) {
    super(builder);
    this.tapestryServletType = getType("org.apache.tapestry.ApplicationServlet");

  }

  public void build() {
    if (TapestryBuilder.DEBUG)
      System.out.println("FULL Tapestry build");

    try {
      notifier.subTask("Tapestry builder starting");
      Markers.removeProblemsFor(tapestryBuilder.currentProject);

      // clear out the infos - we'll be building new ones!
      tapestryBuilder.tapestryProject.close();

      if (tapestryServletType == null) {
        Markers.addTapestryProblemMarkerToResource(
          tapestryBuilder.currentProject,
          "ignoring applications in project because '"
            + tapestryBuilder.currentProject.getName()
            + "', type 'org.apache.tapestry.ApplicationServlet' not found in project build path!",
          IMarker.SEVERITY_WARNING,
          0,
          0,
          0);
      } else {
        findDeclaredApplications();
      }
      notifier.updateProgressDelta(0.1f);

      notifier.subTask("locating Tapestry artifacts");
      applicationQueue = new BuilderQueue();
      libraryQueue = new BuilderQueue();
      pageQueue = new BuilderQueue();
      componentQueue = new BuilderQueue();
      htmlQueue = new BuilderQueue();
      scriptQueue = new BuilderQueue();

      List found = findAllTapestryArtifacts();
      notifier.updateProgressDelta(0.15f);
      //      int total = getTotalWaitingCount();
      int total = found.size();
      if (total > 0) {
        //        String[] allSourceFiles = new String[locations.size()];
        //        locations.toArray(allSourceFiles);
        //        String[] initialTypeNames = new String[typeNames.size()];
        //        typeNames.toArray(initialTypeNames);
        //
        notifier.setProcessingProgressPer(0.75f / (total * 2));
        projectInfo = new TapestryProjectInfo();

      }
    } catch (CoreException e) {
      TapestryCore.log(e);
    } finally {
      cleanUp();
    }
  }

  /**
   * Method findAllTapestryArtifacts.
   */
  protected List findAllTapestryArtifacts() throws CoreException {
    TapestryLookup lookup = new TapestryLookup();
    Set names = knownValidServlets.keySet();
    String[] servletNames = (String[]) names.toArray(new String[names.size()]);
    lookup.configure(tapestryBuilder.tapestryProject, servletNames);
    lookup.findAll(new ILookupRequestor() {

      public boolean isCancelled() {
        try {
          tapestryBuilder.notifier.checkCancel();
        } catch (OperationCanceledException e) {
          return true;
        }
        return false;
      }

      public void accept(IStorage storage, Object parent) {
        System.out.println(storage);
      }

      /**
       * @see com.iw.plugins.spindle.core.processing.ILookupRequestor#markBadLocation(IStorage, Object, ILookupRequestor, String)
       */
      public void markBadLocation(
        IStorage s,
        Object parent,
        ILookupRequestor requestor,
        String message) {
        System.err.println(s + " " + message);
      }
    });
    ArrayList found = new ArrayList();
    //    findAllArtifactsInProjectProper(found);
    //    findAllArtifactsInBinaryClasspath(found);
    return found;
  }

  /**
   * Method findAllArtifactsInBinaryClasspath.
   */
  private void findAllArtifactsInBinaryClasspath(ArrayList found) {
  }

  /**
   * Method findAllArtifactsInProjectProper.
   */
  private void findAllArtifactsInProjectProper(ArrayList found) {
    try {
      tapestryBuilder.getProject().accept(
        new TapestryResourceVisitor(this, found),
        IResource.DEPTH_INFINITE,
        false);
    } catch (CoreException e) {
      TapestryCore.log(e);
    }
  }

  public void cleanUp() {
  }

  protected int getTotalWaitingCount() {
    return applicationQueue.getWaitingCount()
      + libraryQueue.getWaitingCount()
      + pageQueue.getWaitingCount()
      + componentQueue.getWaitingCount()
      + htmlQueue.getWaitingCount()
      + scriptQueue.getWaitingCount();
  }

  protected void findDeclaredApplications() throws CoreException {
    if (tapestryBuilder.webXML != null && tapestryBuilder.webXML.exists()) {
      Element wxmlElement = parseToElement(tapestryBuilder.webXML);
      if (wxmlElement == null) {
        return;
      }
      ServletInfo[] servletInfos = new WebXMLProcessor(this).getServletInformation(wxmlElement);
      if (servletInfos.length > 0) {
        knownValidServlets = new HashMap();
        for (int i = 0; i < servletInfos.length; i++) {
          knownValidServlets.put(servletInfos[i].name, servletInfos[i]);
        }
      }

    } else {
      String definedWebRoot = tapestryBuilder.tapestryProject.getWebContext();
      if (definedWebRoot != null && !"".equals(definedWebRoot)) {
        Markers.addTapestryProblemMarkerToResource(
          tapestryBuilder.getProject(),
          "Ignoring applications: " + definedWebRoot + " does not exist",
          IMarker.SEVERITY_WARNING,
          0,
          0,
          0);
      }
    }

  }

  public class ServletInfo {
    String name;
    String classname;
    Map parameters = new HashMap();
    boolean isServletSubclass;
    public String toString() {
      StringBuffer buffer = new StringBuffer("ServletInfo(");
      buffer.append(name);
      buffer.append(")::");
      buffer.append("classname = ");
      buffer.append(classname);
      buffer.append(", params = ");
      buffer.append(parameters);
      return buffer.toString();
    }
  }

}
