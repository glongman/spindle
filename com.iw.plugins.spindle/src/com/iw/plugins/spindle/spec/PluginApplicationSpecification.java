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
 * Portions created by the Initial Developer are Copyright (C) 2002
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * 
 *  glongman@intelligentworks.com
 *
 * ***** END LICENSE BLOCK ***** */
package com.iw.plugins.spindle.spec;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.sf.tapestry.parse.SpecificationParser;
import net.sf.tapestry.spec.ApplicationSpecification;
import net.sf.tapestry.spec.PageSpecification;

import com.iw.plugins.spindle.MessageUtil;
import com.iw.plugins.spindle.model.TapestryApplicationModel;
import com.iw.plugins.spindle.util.Indenter;

public class PluginApplicationSpecification
  extends ApplicationSpecification
  implements IIdentifiable, PropertyChangeListener {

  private PropertyChangeSupport propertySupport;

  private String identifier;
  private TapestryApplicationModel parent;

  public PluginApplicationSpecification() {
    propertySupport = new PropertyChangeSupport(this);
  }

  public Set getPageNamesSorted() {
    return new TreeSet(pageMap.keySet());
  }

  public Set getComponentMapAliases() {
    if (componentMap == null) {
      return new HashSet();
    }
    return componentMap.keySet();
  }

  public void setName(String name) {
    String old = super.getName();
    super.setName(name);
    propertySupport.firePropertyChange("name", old, name);
  }

  public void setService(String name, String classname) {
    if (serviceMap == null) {
      super.addService(name, classname);
    } else {
      serviceMap.put(name, classname);
    }
    propertySupport.firePropertyChange("services", null, null);
  }

  public void removeService(String name) {
    if (serviceMap != null) {
      serviceMap.remove(name);
      propertySupport.firePropertyChange("services", null, null);
    }
  }

  public boolean canRevertService(String name) {
    String useName = name.toLowerCase();
    if (serviceMap != null
      && serviceMap.containsKey(useName)
      && getDefaultServiceMap().containsKey(useName)) {
      return true;
    }
    return false;
  }

  public boolean isDefaultService(String name) {
    return getDefaultServiceMap().containsKey(name.toLowerCase());
  }

  public boolean canDeleteService(String name) {
    String useName = name.toLowerCase();
    if (getDefaultServiceMap().containsKey(useName)) {
      return false;
    }
    return serviceMap != null && serviceMap.containsKey(useName);
  }

  public void setProperty(String name, String value) {
    String old = super.getProperty(name);
    super.setProperty(name, value);
    propertySupport.firePropertyChange("properties", name, value);
  }

  public void removeProperty(String name) {
    String old = super.getProperty(name);
    super.removeProperty(name);
    propertySupport.firePropertyChange("properties", old, null);
  }

  public void setEngineClassName(String name) {
    String old = engineClassName;
    engineClassName = name;
    propertySupport.firePropertyChange("engineClassName", old, name);
  }

  public void setComponentAlias(String alias, String resourceName) {
    if (componentMap == null) {
      super.setComponentAlias(alias, resourceName);
    } else {
      componentMap.put(alias, resourceName);
    }
    propertySupport.firePropertyChange("componentMap", null, componentMap);
  }

  public void removeComponentAlias(String alias) {
    if (componentMap.containsKey(alias)) {
      componentMap.remove(alias);
      propertySupport.firePropertyChange("componentMap", null, componentMap);
    }
  }

  public Collection getNonDefaultPageNames() {
    if (pageMap == null) {
      return new HashSet();
    }
    return pageMap.keySet();
  }

  public void setPageSpecification(String name, PageSpecification spec) {
    if (pageMap == null) {
      super.setPageSpecification(name, spec);
    } else {
      pageMap.put(name, spec);
    }

    PluginPageSpecification pageSpec = (PluginPageSpecification) spec;
    pageSpec.setIdentifier(name);
    pageSpec.setParent(this);
    pageSpec.addPropertyChangeListener(this);
    propertySupport.firePropertyChange("pageMap", null, pageMap);
  }

  public void removePageSpecification(String name) {
    if (pageMap.containsKey(name)) {
      pageMap.remove(name);
      propertySupport.firePropertyChange("pageMap", null, pageMap);
    }
  }

  public String getPageName(String componentSpecLocation) {
    String useName = componentSpecLocation;
    //if (useName.indexOf("/") >= 0) {
    // useName = useName.substring(1).replace('/', '.');
    //}
    String pageName = null;
    if (pageMap != null) {
      pageName = findKeyInPageMap(useName, pageMap);
    }
    if (pageName == null) {
      pageName = findKeyInPageMap(useName, getDefaultPageMap());
    }
    return pageName;
  }

  private String findKeyInPageMap(String componentLocation, Map map) {
    Iterator i = map.entrySet().iterator();
    while (i.hasNext()) {
      Map.Entry entry = (Map.Entry) i.next();
      PageSpecification pageSpec = (PageSpecification) entry.getValue();
      if (pageSpec.getSpecificationPath().equals(componentLocation)) {
        return (String) entry.getKey();
      }
    }
    return null;
  }

  public String findAliasFor(String componentSpecLocation) {
    String result = null;
    if (componentMap != null) {
      result = findKeyInComponentMap(componentSpecLocation, componentMap);
    }
    if (result == null) {
      result = findKeyInComponentMap(componentSpecLocation, getDefaultComponentMap());
    }
    return result;
  }

  private String findKeyInComponentMap(String componentLocation, Map map) {
    Iterator i = map.entrySet().iterator();
    while (i.hasNext()) {
      Map.Entry entry = (Map.Entry) i.next();
      if (entry.getValue().equals(componentLocation)) {
        return (String) entry.getKey();
      }
    }
    return null;
  }

  public void addPropertyChangeListener(PropertyChangeListener listener) {
    propertySupport.addPropertyChangeListener(listener);
  }

  public void removePropertyChangeListener(PropertyChangeListener listener) {
    propertySupport.removePropertyChangeListener(listener);
  }

  public void write(PrintWriter writer) {
    int indent = 0;
    writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    writer.println("<!DOCTYPE application ");
    writer.print("      PUBLIC \"");
    String DTDversion = getDTDVersion();
    if (DTDversion == null || "1.2".equals(DTDversion)) {
      writer.print(SpecificationParser.TAPESTRY_DTD_1_2_PUBLIC_ID);
      writer.println("\"");
      writer.println("      \"http://tapestry.sf.net/dtd/Tapestry_1_2.dtd\">");
    } else {
      writer.print(SpecificationParser.TAPESTRY_DTD_1_1_PUBLIC_ID);
      writer.println("\"");
      writer.println("      \"http://tapestry.sf.net/dtd/Tapestry_1_1.dtd\">");
    }
    writer.println(MessageUtil.getString("TAPESTRY.xmlComment"));

    writer.println();
    writer.print("<application name=\"");
    writer.print(getName());
    writer.print("\" engine-class=\"");
    writer.print(getEngineClassName());
    writer.println("\" >");

    String description = getDescription();
    if (description != null && !"".equals(description.trim())) {
      writer.println();
      writeDescription(description.trim(), writer, indent + 1);
    }

    Collection properties = getPropertyNames();
    if (properties != null) {
      writer.println();
      Iterator propertyNames = new TreeSet(properties).iterator();
      while (propertyNames.hasNext()) {
        String propertyName = (String) propertyNames.next();
        writeProperty(propertyName, getProperty(propertyName), writer, indent + 1);
      }
    }

    if (pageMap != null) {
      Iterator pageNames = new TreeSet(pageMap.keySet()).iterator();
      if (pageNames.hasNext()) {
        writer.println();
      }
      while (pageNames.hasNext()) {
        String pname = (String) pageNames.next();
        ((PluginPageSpecification) getPageSpecification(pname)).write(pname, writer, indent + 1);
      }
      if (componentMap != null) {
        Iterator componentAliases = new TreeSet(componentMap.keySet()).iterator();
        if (componentAliases.hasNext()) {
          writer.println();
        }
        while (componentAliases.hasNext()) {
          String alias = (String) componentAliases.next();
          Indenter.printIndented(writer, indent + 1, "<component-alias type=\"");
          writer.print(alias);
          writer.print("\" specification-path=\"");
          writer.print(componentMap.get(alias));
          writer.println("\" />");
        }
      }
    }

    if (serviceMap != null) {
      Iterator serviceNames = new TreeSet(serviceMap.keySet()).iterator();
      if (serviceNames.hasNext()) {
        writer.println();
      }
      while (serviceNames.hasNext()) {
        String serviceName = (String) serviceNames.next();
        String classname = getServiceClassName(serviceName);
        if (classname != null) {
          Indenter.printIndented(writer, indent + 1, "<service name=\"");
          writer.print(serviceName);
          writer.print("\" class=\"");
          writer.print(classname);
          writer.println("\" />");
        }
      }
    }

    writer.println();
    writer.println("</application>");
  }

  static public void writeDescription(String description, PrintWriter writer, int indent) {
    boolean tooLong = description.length() > 40;
    boolean singleLine = description.indexOf("\r") <= 0 && description.indexOf("\n") <= 0;
    Indenter.printIndented(writer, indent, "<description>");
    if (singleLine && !tooLong) {
      writer.print("<![CDATA[   " + description + "   ]]>");
      writer.println("</description>");
    } else if (singleLine && tooLong) {
      writer.println();
      Indenter.printlnIndented(writer, indent + 1, "<![CDATA[   " + description + "   ]]>");
      Indenter.printlnIndented(writer, indent, "</description>");
    } else {
      writer.println();
      writer.println("<![CDATA[");
      writeMultiLine(writer, description);
      writer.println("]]>");
      Indenter.printlnIndented(writer, indent, "</description>");
    }
  }

  static public void writeMultiLine(PrintWriter writer, String message) {
    BufferedReader reader =
      new BufferedReader(new InputStreamReader(new ByteArrayInputStream(message.getBytes())));
    try {
      String line = reader.readLine();
      while (line != null) {
        writer.println(line);
        line = reader.readLine();
      }
    } catch (IOException e) {
    }
  }

  static public void writeProperty(String name, String value, PrintWriter writer, int indent) {
    Indenter.printIndented(writer, indent, "<property name=\"" + name);
    if (value == null || "".equals(value)) {
      writer.println("\"/>");
    } else {
      writer.println("\">");
      Indenter.printlnIndented(writer, indent + 1, value);
      Indenter.printlnIndented(writer, indent, "</property>");
    }
  }

  /**
   * @see net.sf.tapestry.spec.ApplicationSpecification#setDTDVersion(String)
   */
  public void setDTDVersion(String dtdVersion) {
    super.setDTDVersion(dtdVersion);
    propertySupport.firePropertyChange("dtd", null, dtdVersion);
  }

  /**
   * Returns the identifier.
   * @return String
   */
  public String getIdentifier() {
    return identifier;
  }

  /**
   * Returns the parent.
   * @return TapestryApplicationModel
   */
  public Object getParent() {
    return parent;
  }

  /**
   * Sets the identifier.
   * @param identifier The identifier to set
   */
  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  /**
   * Sets the parent.
   * @param parent The parent to set
   */
  public void setParent(Object parent) {
    this.parent = (TapestryApplicationModel) parent;
  }

  
  public void propertyChange(PropertyChangeEvent event) {
    propertySupport.firePropertyChange(event);
  }

}
