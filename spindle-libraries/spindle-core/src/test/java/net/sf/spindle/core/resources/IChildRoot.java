package net.sf.spindle.core.resources;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;

import net.sf.spindle.core.resources.search.ISearchAcceptor;

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
/**
 * Method called by a {@link net.sf.spindle.core.resources.ParentRoot}
 */
public interface IChildRoot extends IRootImplementation
{
    static final int BINARY = 0;

    static final int SOURCE = 1;

    void performSearch(ISearchAcceptor acceptor);

    Object findUnderlier(ResourceImpl resource);

    boolean performlookup(ResourceImpl resource, IResourceAcceptor requestor,
            ArrayList<ICoreResource> seenResources);

    URL buildResourceURL(ResourceImpl resource);

    abstract boolean existsInThisRoot(String path);

    ICoreResource[] getNonJavaResources(ResourceImpl resource);

    int getType();

    File getRootFile();
}
