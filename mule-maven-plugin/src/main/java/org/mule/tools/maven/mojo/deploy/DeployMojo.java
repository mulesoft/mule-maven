/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.maven.mojo.deploy;

import static org.mule.tools.validation.DeploymentValidatorFactory.createDeploymentValidator;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.mule.tools.client.standalone.controller.MuleProcessController;
import org.mule.tools.client.standalone.exception.DeploymentException;
import org.mule.tools.deployment.DefaultDeployer;
import org.mule.tools.deployment.Deployer;

/**
 * Maven plugin to deploy Mule applications to different kind of servers: Standalone (both Community and Enterprise), Clustered,
 * Anypoint Runtime Manager and CloudHub. Main uses are running integration tests and deploying applications. Some of the features
 * are: Download Mule Standalone from a Maven Repository and install it locally. Deploy a Mule application to a server. Undeploy a
 * Mule appliction. Assemble a Mule cluster and deploy applications.
 *
 * @see UndeployMojo
 * @see MuleProcessController
 * @since 1.0
 */
@Mojo(name = "deploy", requiresProject = true)
public class DeployMojo extends AbstractMuleDeployerMojo {

  @Override
  public void doExecute() throws MojoFailureException, MojoExecutionException {
    try {
      createDeploymentValidator(deploymentConfiguration).validateMuleVersionAgainstEnvironment();
      Deployer deployer = new DefaultDeployer(deploymentConfiguration, log);
      deployer.deploy();
    } catch (DeploymentException e) {
      getLog().error("Failed to deploy " + deploymentConfiguration.getApplicationName() + ": " + e.getMessage(), e);
      throw new MojoFailureException("Failed to deploy [" + deploymentConfiguration.getArtifact() + "]");
    }
  }

  // @Override
  // public String getPreviousRunPlaceholder() {
  // return "MULE_MAVEN_PLUGIN_DEPLOY_PREVIOUS_RUN_PLACEHOLDER";
  // }
}
