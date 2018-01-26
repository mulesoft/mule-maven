/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package integration.test.mojo.agent;

import org.junit.Ignore;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@Ignore
public class ApplicationAgentDeploymentTest extends AgentDeploymentTest {

  private static final String APPLICATION = "empty-mule-deploy-application-agent-project";

  public ApplicationAgentDeploymentTest() {
    super(APPLICATION);
  }

  @Override
  public void assertDeployment() {
    assertThat("Failed to deploy: " + APPLICATION, standaloneEnvironment.isDeployed(APPLICATION), is(true));
  }
}
