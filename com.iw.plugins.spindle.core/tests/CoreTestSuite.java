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

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 *  RUNS ALL THE TESTS!
 * 
 * @author glongman@gmail.com
 * @version $Id$
 */
public class CoreTestSuite
{

    public static Test suite()
    {
        TestSuite suite = new TestSuite("Test for eveything");
        //$JUnit-BEGIN$
        suite.addTest(tests.TapestryPullParser.AllTests.suite());
        suite.addTest(tests.TapestryDOMParser.AllTests.suite());
        suite.addTest(tests.util.AllTests.suite());
        suite.addTest(tests.multirun.AllTests.suite());
        suite.addTest(tests.Parser.AllTests.suite());
        suite.addTest(tests.Scanners.AllTests.suite());
        suite.addTest(tests.perl.AllTests.suite());
        
        //$JUnit-END$
        return suite;
    }
}
