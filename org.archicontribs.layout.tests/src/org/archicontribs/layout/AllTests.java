/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.layout;

import org.archicontribs.layout.elk.ArchimateDiagramLayoutSetupTests;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;


@Suite
@SelectClasses({
    // ELKConfig
	ArchimateDiagramLayoutSetupTests.class
})

@SuiteDisplayName("All Layout Tests")
public class AllTests {
}