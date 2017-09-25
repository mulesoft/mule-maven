/*
 * Mule ESB Maven Tools
 * <p>
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * <p>
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.api.packager.archiver;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

import org.codehaus.plexus.archiver.dir.DirectoryArchiver;
import org.junit.Before;
import org.junit.Test;

public class MuleExplodedArchiverTest {

  public MuleExplodedArchiver archiver;

  @Before
  public void setUp() {
    archiver = new MuleExplodedArchiver();
  }

  @Test
  public void validateArchiverType() {
    assertThat("The archiver type is not as expected", archiver.archiver, instanceOf(DirectoryArchiver.class));
  }

}
