/*
 * Mule ESB Maven Tools
 * <p>
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * <p>
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.client.arm;

import groovy.util.ScriptException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.mule.tools.client.AbstractDeployer;
import org.mule.tools.client.exception.ClientException;
import org.mule.tools.client.standalone.exception.DeploymentException;
import org.mule.tools.client.model.TargetType;

import org.mule.tools.model.DeployerLog;
import org.mule.tools.model.DeploymentConfiguration;

import javax.ws.rs.NotFoundException;

public class ArmDeployer extends AbstractDeployer {

  private TargetType targetType;
  private String target;
  private ArmClient armClient;

  public ArmDeployer(DeploymentConfiguration deploymentConfiguration, DeployerLog log) throws DeploymentException {
    super(deploymentConfiguration, log);
  }

  @Override
  public void deploy() throws DeploymentException {
    try {
      armClient.init();
      Integer applicationId = armClient.findApplication(getApplicationName(), targetType, target);
      if (applicationId == null) {
        info("Deploying application " + getApplicationName());
        armClient.deployApplication(getApplicationFile(), getApplicationName(), targetType, target);
      } else {
        String alreadyExistsMessage = "Found application %s on %s %s. Redeploying application...";
        info(String.format(alreadyExistsMessage, getApplicationName(), targetType.toString(), target));
        armClient.redeployApplication(applicationId, getApplicationFile(), getApplicationName(), targetType, target);
      }
    } catch (ClientException e) {
      error("Failed: " + e.getMessage());
      throw new DeploymentException("Failed to deploy application " + getApplicationName(), e);
    }
  }

  @Override
  public void undeploy(MavenProject mavenProject) throws DeploymentException {
    ArmClient armClient =
        new ArmClient(deploymentConfiguration, log);
    armClient.init();
    log.info("Undeploying application " + deploymentConfiguration.getApplicationName());
    try {
      armClient.undeployApplication(deploymentConfiguration.getApplicationName(), deploymentConfiguration.getTargetType(),
                                    deploymentConfiguration.getTarget());
    } catch (NotFoundException e) {
      if (deploymentConfiguration.isFailIfNotExists()) {
        throw e;
      } else {
        log.warn("Application not found: " + deploymentConfiguration.getApplicationName());
      }
    }
  }

  @Override
  protected void initialize() {
    targetType = deploymentConfiguration.getTargetType();
    target = deploymentConfiguration.getTarget();
    armClient = new ArmClient(deploymentConfiguration, log);
  }

  @Override
  public void resolveDependencies(MavenProject mavenProject, ArtifactResolver artifactResolver, ArchiverManager archiverManager,
                                  ArtifactFactory artifactFactory, ArtifactRepository localRepository)
      throws DeploymentException, ScriptException {

  }

}
