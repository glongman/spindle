package net.sf.spindle.core.spec;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tapestry.engine.IPropertySource;

/**
 * Base class for Spec classes that have properties
 * 
 * @author glongman@gmail.com
 */
public abstract class BasePropertyHolder extends DescribableSpecification
		implements IPluginPropertyHolder, IPropertySource {
	Map<String, PluginPropertyDeclaration> fProperties;

	/**
	 * The locations and values of all reserved property declarations in a the
	 * document for this holder. Immutable after a parse/scan episode.
	 */
	List<PluginPropertyDeclaration> fPropertyDeclarations;

	/**
	 * 
	 */
	public BasePropertyHolder(SpecType type) {
		super(type);

	}

	public void addPropertyDeclaration(PluginPropertyDeclaration declaration) {
		if (fPropertyDeclarations == null) {
			fPropertyDeclarations = new ArrayList<PluginPropertyDeclaration>();
			fProperties = new HashMap<String, PluginPropertyDeclaration>();
		}

		fPropertyDeclarations.add(declaration);

		if (!fProperties.containsKey(declaration.getKey()))
			fProperties.put(declaration.getKey(), declaration);

	}

	public List getPropertyDeclarations() {
		if (fPropertyDeclarations == null)
			return Collections.EMPTY_LIST;
		return fPropertyDeclarations;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.tapestry.util.IPropertyHolder#getPropertyNames()
	 */
	public List<String> getPropertyNames() {
		if (fProperties == null)
			return Collections.emptyList();

		List<String> result = new ArrayList<String>(fProperties.keySet());

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.tapestry.util.IPropertyHolder#setProperty(java.lang.String,
	 *      java.lang.String)
	 */
	public void setProperty(String name, String value) {
		throw new IllegalStateException("not used in SPindle!");
		// if (value == null)
		// {
		// removeProperty(name);
		// return;
		// }
		//
		// if (fProperties == null)
		// fProperties = new PropertyFiringMap(this, "properties");
		//
		// fProperties.put(name, value);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.tapestry.util.IPropertyHolder#removeProperty(java.lang.String)
	 */
	public void removeProperty(String name) {
		throw new IllegalStateException("not used in SPindle!");
		// remove(fProperties, name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.tapestry.util.IPropertyHolder#getProperty(java.lang.String)
	 */
	public String getProperty(String name) {
		if (fProperties == null)
			return null;

		PluginPropertyDeclaration declaration = (PluginPropertyDeclaration) fProperties
				.get(name);
		if (declaration == null)
			return null;
		return declaration.getValue();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.tapestry.engine.IPropertySource#getPropertyValue(java.lang.String)
	 */
	public String getPropertyValue(String name) {
		return getProperty(name);
	}

	PluginPropertyDeclaration getPropertyDeclaration(String name) {
		if (fProperties == null)
			return null;

		return (PluginPropertyDeclaration) fProperties.get(name);
	}

}