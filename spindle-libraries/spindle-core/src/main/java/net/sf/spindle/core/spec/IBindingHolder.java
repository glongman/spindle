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
import java.util.Collection;

import org.apache.tapestry.spec.BindingSpecification;

/**
 * @author gwl
 * 
 * 
 * Copyright 2002, Geoffrey Longman. All Rights Reserved.
 */
public interface IBindingHolder
{

  public Collection getBindingNames();

  public BindingSpecification getBinding(String name);

  public void setBinding(String name, BindingSpecification binding);

  public void removeBinding(String name);

}