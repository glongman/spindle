package net.sf.spindle.core.extensions;
/*
The contents of this file are subject to the Mozilla Public License
Version 1.1 (the "License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
License for the specific language governing rights and limitations
under the License.

The Original Code is __Spindle, an Eclipse Plugin For Tapestry__.

The Initial Developer of the Original Code is _____Geoffrey Longman__.
Portions created by _____Initial Developer___ are Copyright (C) _2004, 2005, 2006__
__Geoffrey Longman____. All Rights Reserved.

Contributor(s): __glongman@gmail.com___.
*/
import net.sf.spindle.core.resources.ICoreResource;

import org.apache.tapestry.spec.IComponentSpecification;

//TODO javadoc is out of date
/**
 * Interface for resolving a Tapestry page/component's type.
 * <p>
 * Spindle assumes that pages and components have a Java type. However, other
 * languages like Groovy may also be used.
 * <p>
 * Plugins may contribute implementations of this interface using the:
 * <ul>
 * <li>core.componentTypeResolver - extension point</li>
 * </ul>
 * <p>
 * How it works.
 * <p>
 * One instance of each contributed resolver is created. When appropriate, each
 * resolver in turn is asked:
 * <ul>
 * <li><code>canResolve(IType type)</code> the first one that returns true
 * wins
 * </ul>
 * <p>
 * Then <code>doResolve()</code> is called. The instance should return a
 * status of OK if the resolution was successful. Spindle will not proceed to
 * the next step unless the returned status's <code>isOK()</code> returns true
 * If not OK, Spindle will use the message in the status to inform the user.
 * <p>
 * Once an OK status is recieved, <code>getStorage()</code> is called to
 * retrieve the resolved object.
 * <p>
 * The order that contributed extensions are invoked is fixed but is arbitrary
 * in that the order that extensions are added to the extension point is not
 * determinable before hand..
 * 
 * @author glongman@gmail.com
 *  
 */
public interface IComponentTypeResourceResolver
{

  /**
   * Give the contribution a chance to check and see if it is capable of
   * resolving based on the type.
   * <p>
   * Called first
   * 
   * @param type IType the type found in the component spec xml.
   * @return true if this instance can proceed to resolve, false otherwise.
   */
  boolean canResolve(Object typeObject);

  /**
   * resolve the component type's IStorage object and throw and Exception on failure
   * <p>
   * Called second
   * @param specificationLocation the location of the specification we are trying
   *                     to resolve a type for
   * @param componentSpec the parsed IComponentSpecification object found at the
   *                     above location (may be null for various reasons. one reason would
   *                     be if the spec xml is not well formed).
   * 
   * @return an instance of IStatus. <code>getStorage()</code> will only be
   *                 called if the status returned is OK.
   */
  void doResolve(
      ICoreResource specificationLocation,
      IComponentSpecification componentSpec) throws SpindleExtensionException;

  /**
   * Called only if <code>doResolve()</code> did not throw an Exception.
   * <p>
   * called last
   * 
   * @return the component type's resolve storage object. Must not be null!
   */
  Object getStorage();

}