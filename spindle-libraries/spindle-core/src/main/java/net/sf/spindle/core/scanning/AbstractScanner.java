package net.sf.spindle.core.scanning;
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
import java.util.List;

import net.sf.spindle.core.TapestryCore;
import net.sf.spindle.core.source.DefaultProblem;
import net.sf.spindle.core.source.IProblem;
import net.sf.spindle.core.source.IProblemCollector;
import net.sf.spindle.core.source.ISourceLocation;
import net.sf.spindle.core.source.SourceLocation;
import net.sf.spindle.core.types.IJavaTypeFinder;
import net.sf.spindle.core.util.Assert;




/**
 * Base Class for Node processors
 * <p>
 * Node Processors can find problems, but these problems do not represent a list of *all* the
 * problems with this document.
 * </p>
 * <p>
 * i.e.The Parser will hold problems for things like well-formedness and dtd validation!
 * </p>
 * 
 * @author glongman@gmail.com
 */
public abstract class AbstractScanner implements IProblemCollector
{

    protected IProblemCollector fExternalProblemCollector;

    protected List<IProblem> fProblems = new ArrayList<IProblem>();

    protected IScannerValidator fValidator;

    protected IJavaTypeFinder fJavaTypeFinder;

    protected boolean isCachingJavaTypes = false;

    protected Object fSource;

    protected Object fResultObject;

    public Object scan(Object source, IScannerValidator validator) throws ScannerException
    {
        Assert.isNotNull(source);
        Assert.isNotNull(validator);
        fSource = source;
        beginCollecting();
        try
        {

            fValidator = validator;
            fValidator.setProblemCollector(this);
            fResultObject = beforeScan();
            if (fResultObject == null)
                return null;

            doScan();
            return afterScan();
        }
        catch (ScannerException scex)
        {

            if (scex.getLocation() != null)
            {
                addProblem(IProblem.ERROR, scex.getLocation(), scex.getMessage(), scex
                        .isTemporary(), scex.getCode());
            }
            else
            {
                addProblem(new DefaultProblem(IProblem.ERROR, scex.getMessage(),
                        SourceLocation.FILE_LOCATION, false, scex.getCode()));
            }
            return null;
        }
        catch (RuntimeException e)
        {
            TapestryCore.log(e);
            throw e;

        }
        finally
        {
            cleanup();
            endCollecting();
        }

    }

    protected abstract void doScan() throws ScannerException;

    protected abstract Object beforeScan() throws ScannerException;

    protected abstract void cleanup();

    protected Object afterScan() throws ScannerException
    {
        return fResultObject;
    }

    public void beginCollecting()
    {
        if (fExternalProblemCollector != null)
            fExternalProblemCollector.beginCollecting();

        fProblems.clear();
    }

    public void endCollecting()
    {
        if (fExternalProblemCollector != null)
            fExternalProblemCollector.endCollecting();
    }

    public void addProblem(IProblem problem)
    {
        if (fExternalProblemCollector != null)
        {
            fExternalProblemCollector.addProblem(problem);
        }
        else
        {
            fProblems.add(problem);
        }
    }

    public void addProblem(int severity, ISourceLocation location, String message,
            boolean isTemporary, int code)
    {
        addProblem(new DefaultProblem(severity, message, location, isTemporary, code));
    }

    public void addProblems(IProblem[] problems)
    {
        if (problems != null)
            for (int i = 0; i < problems.length; i++)
            {
                addProblem(problems[i]);
            }
    }

    public IProblem[] getProblems()
    {
        if (fExternalProblemCollector != null)
            return fExternalProblemCollector.getProblems();
        return (IProblem[]) fProblems.toArray(new IProblem[fProblems.size()]);
    }

    protected boolean isDummyString(String value)
    {
        if (value != null)
            return value.startsWith(fValidator.getDummyStringPrefix());

        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see core.scanning.IScannerValidator#getNextDummyString()
     */
    protected String getNextDummyString()
    {
        return fValidator.getDummyStringPrefix();
    }

    /*
     * (non-Javadoc)
     * 
     * @see core.scanning.IScannerValidator#getDummyStringPrefix()
     */
    protected String getDummyStringPrefix()
    {
        return fValidator.getDummyStringPrefix();
    }

    /**
     * @return
     */
    public IProblemCollector getExternalProblemCollector()
    {
        return fExternalProblemCollector;
    }

    /**
     * @param collector
     */
    public void setExternalProblemCollector(IProblemCollector collector)
    {
        fExternalProblemCollector = collector;
    }

}