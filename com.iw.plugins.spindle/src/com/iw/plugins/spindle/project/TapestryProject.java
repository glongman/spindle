package com.iw.plugins.spindle.project;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xerces.dom.DocumentImpl;
import org.apache.xml.serialize.Method;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.Serializer;
import org.apache.xml.serialize.SerializerFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.JavaModelException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.iw.plugins.spindle.TapestryPlugin;
import com.iw.plugins.spindle.model.TapestryApplicationModel;
import com.iw.plugins.spindle.util.Utils;
import com.iw.plugins.spindle.util.lookup.TapestryLookup;

/**
 * @author GWL
 * @version $Id$
 *
 * 
 * Notes:
 * 
 *  this project needs to read/write an xml file defining its application
 */
public class TapestryProject implements IProjectNature, ITapestryProject {

  private String applicationPath;
  private String projectName;
  private IProject project;

  private boolean dirty; // has the application changed?

  /**
   * @see org.eclipse.core.resources.IProjectNature#configure()
   */
  public void configure() throws CoreException {

    // we do nothing - for now
  }

  /**
   * @see org.eclipse.core.resources.IProjectNature#deconfigure()
   */
  public void deconfigure() throws CoreException {
    // we do nothing - for now
  }

  /**
   * @see org.eclipse.core.resources.IProjectNature#getProject()
   */
  public IProject getProject() {
    return project;
  }

  /**
   * @see org.eclipse.core.resources.IProjectNature#setProject(IProject)
   */
  public void setProject(IProject project) {

    this.project = project;
    this.projectName = project.getName();
  }

  /**
   * Returns the applicationPath.
   * @return String
   */
  public String getApplicationPath() {
    return applicationPath;
  }

  /**
   * Returns the dirty.
   * @return boolean
   */
  public boolean isDirty() {
    return dirty;
  }

  public TapestryLookup getLookup() throws JavaModelException {

    TapestryLookup lookup = new TapestryLookup();
    lookup.configure(TapestryPlugin.getDefault().getJavaProjectFor(project));

    return lookup;
  }

  /**
   * Sets the applicationPath.
   * @param applicationPath The applicationPath to set
   */
  public void setApplicationPath(String applicationPath) {
    this.applicationPath = applicationPath;
    dirty = true;
  }

  /**
   * Sets the dirty.
   * @param dirty The dirty to set
   */
  public void setDirty(boolean dirty) {
    this.dirty = dirty;
  }

  protected String readApplicationPath(String xmlSpec) throws IOException {

    IPath projectPath = getProject().getFullPath();
    StringReader reader = new StringReader(xmlSpec);
    Element configElement;

    String path = null;

    try {

      DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      configElement = parser.parse(new InputSource(reader)).getDocumentElement();

    } catch (SAXException e) {

      throw new IOException("bad file format");
    } catch (ParserConfigurationException e) {

      reader.close();
      throw new IOException("bad file format");
    } finally {

      reader.close();
    }

    if (!configElement.getNodeName().equalsIgnoreCase("spindle")) {
      throw new IOException("bad file format");
    }

    NodeList list = configElement.getChildNodes();
    int length = list.getLength();

    for (int i = 0; i < length; ++i) {
      Node node = list.item(i);
      short type = node.getNodeType();
      if (type == Node.ELEMENT_NODE) {
        Element element = (Element) node;

        if (element.getNodeName().equalsIgnoreCase("application")) {

          path = element.getAttribute("path"); //$NON-NLS-1$

        }
      }
    }
    return path;
  }

  protected String saveApplicationPathAsXML(String applicationPath) throws IOException {

    Document doc = new DocumentImpl();
    Element configElement = doc.createElement("spindle");
    doc.appendChild(configElement);

    Element appPathElement = doc.createElement("application");
    appPathElement.setAttribute("path", applicationPath);
    configElement.appendChild(appPathElement);

    ByteArrayOutputStream s = new ByteArrayOutputStream();
    OutputFormat format = new OutputFormat();
    format.setIndenting(true);
    format.setLineSeparator(System.getProperty("line.separator"));

    Serializer serializer =
      SerializerFactory.getSerializerFactory(Method.XML).makeSerializer(
        new OutputStreamWriter(s, "UTF8"),
        format);
    serializer.asDOMSerializer().serialize(doc);
    return s.toString("UTF8");
  }

  public void saveProperties(String xmlValue) throws CoreException {

    String filename = ".spindle";
    IFile rscFile = getProject().getFile(filename);
    InputStream inputStream = new ByteArrayInputStream(xmlValue.getBytes());
    // update the resource content
    if (rscFile.exists()) {
      rscFile.setContents(inputStream, IResource.FORCE, null);
    } else {
      rscFile.create(inputStream, IResource.FORCE, null);
    }
  }

  public String readProperties() throws CoreException, IOException {

    String property = null;
    String propertyFileName = ".spindle";
    IFile rscFile = getProject().getFile(propertyFileName);
    if (rscFile.exists()) {
      property = new String(getResourceContentsAsByteArray(rscFile));
    }
    return property;
  }

  /**
   * Returns the given file's contents as a byte array.
   */
  public byte[] getResourceContentsAsByteArray(IFile file) throws CoreException {
    InputStream stream = null;
    try {

      stream = new BufferedInputStream(file.getContents(true));

      return getInputStreamAsByteArray(stream, -1);

    } catch (IOException e) {

      throw new CoreException(new TempStatus(e));

    } finally {

      try {

        stream.close();

      } catch (IOException e) {
      }
    }
  }

  public byte[] getInputStreamAsByteArray(InputStream stream, int length)
    throws IOException {
    byte[] contents;
    if (length == -1) {
      contents = new byte[0];
      int contentsLength = 0;
      int bytesRead = -1;
      do {
        int available = stream.available();

        // resize contents if needed
        if (contentsLength + available > contents.length) {
          System.arraycopy(
            contents,
            0,
            contents = new byte[contentsLength + available],
            0,
            contentsLength);
        }

        // read as many bytes as possible
        bytesRead = stream.read(contents, contentsLength, available);

        if (bytesRead > 0) {
          // remember length of contents
          contentsLength += bytesRead;
        }
      } while (bytesRead > 0);

      // resize contents if necessary
      if (contentsLength < contents.length) {
        System.arraycopy(contents, 0, contents = new byte[contentsLength], 0, contentsLength);
      }
    } else {
      contents = new byte[length];
      int len = 0;
      int readSize = 0;
      while ((readSize != -1) && (len != length)) {
        // See PR 1FMS89U
        // We record first the read size. In this case len is the actual read size.
        len += readSize;
        readSize = stream.read(contents, len, length - len);
      }
    }

    return contents;
  }

  public class TempStatus implements IStatus {

    Throwable exception;

    public TempStatus(Throwable e) {
      this.exception = e;
    }
    /**
    * @see org.eclipse.core.runtime.IStatus#getChildren()
    */
    public IStatus[] getChildren() {
      return new IStatus[0];
    }

    /**
     * @see org.eclipse.core.runtime.IStatus#getCode()
     */
    public int getCode() {
      return IStatus.ERROR;
    }

    /**
     * @see org.eclipse.core.runtime.IStatus#getException()
     */
    public Throwable getException() {
      return exception;
    }

    /**
     * @see org.eclipse.core.runtime.IStatus#getMessage()
     */
    public String getMessage() {
      return exception.getMessage();
    }

    /**
     * @see org.eclipse.core.runtime.IStatus#getPlugin()
     */
    public String getPlugin() {
      return TapestryPlugin.ID_PLUGIN;
    }

    /**
     * @see org.eclipse.core.runtime.IStatus#getSeverity()
     */
    public int getSeverity() {
      return IStatus.ERROR;
    }

    /**
     * @see org.eclipse.core.runtime.IStatus#isMultiStatus()
     */
    public boolean isMultiStatus() {
      return false;
    }

    /**
     * @see org.eclipse.core.runtime.IStatus#isOK()
     */
    public boolean isOK() {
      return false;
    }

    /**
     * @see org.eclipse.core.runtime.IStatus#matches(int)
     */
    public boolean matches(int severityMask) {
      return false;
    }

  }

  /**
   * @see com.iw.plugins.spindle.project.ITapestryProject#getApplicationModel()
   */
  public TapestryApplicationModel getApplicationModel() {
    return null;
  }

}
