/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation", "Tapestry" 
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache" 
 *    or "Tapestry", nor may "Apache" or "Tapestry" appear in their 
 *    name, without prior written permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE TAPESTRY CONTRIBUTOR COMMUNITY
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package com.iw.plugins.spindle.core.parser.template;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.tapestry.ILocation;

/**
 *  Token representing the open tag for a component.  Components may be either
 *  specified or implicit.  Specified components (the traditional type, dating
 *  back to the origin of Tapestry) are matched by an entry in the
 *  containing component's specification.  Implicit components specify their
 *  type in the component template and must not have an entry in
 *  the containing component's specification.
 *
 *  @see TokenType#OPEN
 *  @author Howard Lewis Ship
 *  @version $Id$
 *  @since 3.0
 *
 **/

public class OpenToken extends TemplateToken
{
    private String _tag;
    private String _id;
    private String _componentType;
    private Map _attributes;

    /**
     *  Creates a new token with the given tag, id and type 
     * 
     *  @param tag the template tag which represents the component, typically "span"
     *  @param id the id for the component, which may be assigned by the template
     *  parser for implicit components
     *  @param  componentType the type of component, if an implicit component, or null for
     *  a specified component
     *  @param location location of tag represented by this token
     * 
     **/

    public OpenToken(String tag, String id, String componentType, ILocation location, TagEventInfo eventInfo)
    {
        super(TokenType.OPEN, location, eventInfo);

        _tag = tag;
        _id = id;
        _componentType = componentType;
    }

    /**
     *  Returns the id for the component.
     * 
     **/
    
    public String getId()
    {
        return _id;
    }

    /**
     *  Returns the tag used to represent the component within the template.
     * 
     **/
    
    public String getTag()
    {
        return _tag;
    }
    
    /**
     *  Returns the specified component type, or null for a component where the type
     *  is not defined in the template.  The type may include a library id prefix.
     * 
     **/
    
    public String getComponentType()
    {
        return _componentType;
    }

	public void addAttribute(String name, AttributeType type, String value)
	{
		TemplateAttribute attribute = new TemplateAttribute(type, value);
		
		if (_attributes == null)
		_attributes = new HashMap();
		
		_attributes.put(name, attribute);
	}
	
	/**
	 *  Returns a Map of attributes.  Key is the attribute name, value
	 *  is an instance of {@link org.apache.tapestry.parse.TemplateAttribute}.
	 *  The caller should not modify the Map.  Returns null if
	 *  this OpenToken contains no attributes.
	 * 
	 **/
	
	public Map getAttributesMap()
	{
		return _attributes;
	}

    protected void extendDescription(ToStringBuilder builder)
    {
        builder.append("id", _id);
        builder.append("componentType", _componentType);
        builder.append("tag", _tag);
        builder.append("attributes", _attributes);
    }

}