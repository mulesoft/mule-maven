/*
 * Mule ESB Maven Tools
 * <p>
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * <p>
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.deployment.cloudhub;

import org.mule.tools.client.cloudhub.CloudHubClient;
import org.mule.tools.client.cloudhub.model.Application;
import org.mule.tools.client.cloudhub.model.MuleVersion;
import org.mule.tools.client.core.exception.DeploymentException;
import org.mule.tools.deployment.artifact.ArtifactDeployer;
import org.mule.tools.model.Deployment;
import org.mule.tools.model.anypoint.CloudHubDeployment;
import org.mule.tools.utils.DeployerLog;
import org.mule.tools.verification.DeploymentVerification;
import org.mule.tools.verification.cloudhub.CloudHubDeploymentVerification;

import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Optional.empty;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Deploys mule artifacts to CloudHub using the {@link CloudHubClient}.
 */
public class CloudHubArtifactDeployer implements ArtifactDeployer {

  private static final String DEFAULT_CH_REGION = "us-east-1";
  private static final Long DEFAULT_CLOUDHUB_DEPLOYMENT_TIMEOUT = 600000L;

  private final DeployerLog log;
  private final CloudHubDeployment deployment;

  private CloudHubClient client;
  private DeploymentVerification deploymentVerification;

  public CloudHubArtifactDeployer(Deployment deployment, DeployerLog log) {
    this(deployment, new CloudHubClient((CloudHubDeployment) deployment, log), log);
  }

  public CloudHubArtifactDeployer(Deployment deployment, CloudHubClient cloudHubClient, DeployerLog log) {
    checkArgument(cloudHubClient != null, "The Cloudhub client must not be null.");

    this.log = log;
    this.client = cloudHubClient;
    this.deploymentVerification = new CloudHubDeploymentVerification(client);

    this.deployment = (CloudHubDeployment) deployment;
    if (!this.deployment.getDeploymentTimeout().isPresent()) {
      this.deployment.setDeploymentTimeout(DEFAULT_CLOUDHUB_DEPLOYMENT_TIMEOUT);
    }
  }

  public void setDeploymentVerification(DeploymentVerification deploymentVerification) {
    checkArgument(deploymentVerification != null, "The verificator must not be null.");
    this.deploymentVerification = deploymentVerification;
  }

  @Override
  public void deployDomain() throws DeploymentException {
    throw new DeploymentException("Deployment of domains to CloudHub is not supported");
  }

  @Override
  public void undeployDomain() throws DeploymentException {
    throw new DeploymentException("Undeployment of domains from CloudHub is not supported");
  }

  /**
   * Deploys an application to CloudHub. It creates the application in CloudHub, uploads its contents and triggers its start.
   *
   * @throws DeploymentException
   */
  @Override
  public void deployApplication() throws DeploymentException {
    createOrUpdateApplication();
    startApplication();
    checkApplicationHasStarted();
  }

  /**
   * Deploys an application to CloudHub, stopping it.
   *
   * @throws DeploymentException If the application does not exist or some internal error in CloudHub happens
   */
  @Override
  public void undeployApplication() throws DeploymentException {
    log.info("Stopping application " + deployment.getApplicationName());
    client.stopApplications(deployment.getApplicationName());
  }

  /**
   * Retrieves the application name.
   *
   * @return The application name
   */
  public String getApplicationName() {
    return deployment.getApplicationName();
  }

  /**
   * Creates or update an application in CloudHub.
   * 
   * If the domain name is available it gets created. Otherwise, it tries to update the existent application.
   *
   * @throws DeploymentException If the application is not available and cannot be updated
   */
  protected void createOrUpdateApplication() throws DeploymentException {
    if (client.isDomainAvailable(deployment.getApplicationName())) {
      createApplication();
    } else {
      updateApplication();
    }
  }

  /**
   * Creates the application in CloudHub.
   */
  protected void createApplication() {
    log.info("Creating application: " + deployment.getApplicationName());
    client.createApplications(getApplication(empty()), deployment.getArtifact());
  }

  /**
   * Updates the application in CloudHub.
   *
   * @throws DeploymentException In case the application is not available for the current user or some other internal in CloudHub
   *         happens
   */
  protected void updateApplication() throws DeploymentException {
    Application currentApplication = client.getApplications(deployment.getApplicationName());
    if (currentApplication != null) {
      log.info("Application: " + deployment.getApplicationName() + " already exists, redeploying");
      client.updateApplications(getApplication(Optional.of(currentApplication)), deployment.getArtifact());
    } else {
      log.error("Application name: " + deployment.getApplicationName() + " is not available. Aborting.");
      throw new DeploymentException("Domain " + deployment.getApplicationName() + " is not available. Aborting.");
    }
  }

  /**
   * Starts an application in CloudHub. The application is supposed to be already created and its contents should have already
   * been uploaded.
   */
  protected void startApplication() {
    log.info("Starting application: " + deployment.getApplicationName());
    client.startApplications(deployment.getApplicationName());
  }

  /**
   * Checks if an application in CloudHub has the {@code STARTED_STATUS} status.
   *
   * @throws DeploymentException In case it timeouts while checking for the status
   */
  protected void checkApplicationHasStarted() throws DeploymentException {
    log.info("Checking if application: " + deployment.getApplicationName() + " has started");
    deploymentVerification.assertDeployment(deployment);
  }


  private Application getApplication(Optional<Application> originalApplication) {
    Application application = new Application();
    if (originalApplication.isPresent()) {
      application.setDomain(deployment.getApplicationName());

      MuleVersion muleVersion = new MuleVersion();
      muleVersion.setVersion(deployment.getMuleVersion().get());
      application.setMuleVersion(muleVersion);

      Map<String, String> properties = originalApplication.get().getProperties();
      properties.putAll(deployment.getProperties());
      application.setProperties(properties);

      if (isBlank(deployment.getRegion())) {
        application.setRegion(originalApplication.get().getRegion());
      } else {
        application.setRegion(deployment.getRegion());
      }
    } else {
      application.setDomain(deployment.getApplicationName());

      MuleVersion muleVersion = new MuleVersion();
      muleVersion.setVersion(deployment.getMuleVersion().get());
      application.setMuleVersion(muleVersion);

      application.setProperties(deployment.getProperties());

      String region = isBlank(deployment.getRegion()) ? DEFAULT_CH_REGION : deployment.getRegion();
      application.setRegion(region);
    }

    return application;
  }
}
