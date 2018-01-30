/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package integration.test.mojo.standalone;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import integration.test.mojo.AbstractDeploymentTest;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.slf4j.LoggerFactory;

import integration.test.util.environment.StandaloneEnvironment;

public class StandaloneDeploymentTest extends AbstractDeploymentTest {

  public static final String VERIFIER_MULE_VERSION = "mule.version";
  public static final String VERIFIER_MULE_TIMEOUT = "mule.timeout";
  public static final String VERIFIER_MULE_HOME_TEST = "mule.home.test";

  @Rule
  public StandaloneEnvironment standaloneEnvironment = new StandaloneEnvironment(getMuleVersion());

  private Verifier verifier;

  private String application;

  public StandaloneDeploymentTest(String application) {
    this.application = application;
  }

  public String getApplication() {
    return application;
  }

  @Before
  public void before() throws VerificationException, InterruptedException, IOException, TimeoutException {
    log = LoggerFactory.getLogger(this.getClass());
    log.info("Initializing context...");

    verifier = buildBaseVerifier();
    verifier.setEnvironmentVariable(VERIFIER_MULE_VERSION, getMuleVersion());
    verifier.setEnvironmentVariable(VERIFIER_MULE_TIMEOUT, System.getProperty("mule.timeout"));
    verifier.setEnvironmentVariable(VERIFIER_MULE_HOME_TEST, standaloneEnvironment.getMuleHome());
  }


  @After
  public void after() throws IOException, InterruptedException {
    verifier.resetStreams();
  }

  protected void deploy() throws VerificationException {
    log.info("Executing mule:deploy goal...");
    verifier.executeGoal(DEPLOY_GOAL);
    assertThat("Standalone should be running ", standaloneEnvironment.isRunning(), is(true));
  }
}
