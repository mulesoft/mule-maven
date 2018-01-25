/*
 * Mule ESB Maven Tools
 * <p>
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * <p>
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.verification.agent;

import org.apache.commons.lang3.StringUtils;
import org.mule.tools.client.agent.AgentClient;
import org.mule.tools.client.agent.model.Application;
import org.mule.tools.client.core.exception.DeploymentException;
import org.mule.tools.model.Deployment;
import org.mule.tools.verification.DefaultDeploymentVerification;
import org.mule.tools.verification.DeploymentVerification;
import org.mule.tools.verification.DeploymentVerificationStrategy;

import java.util.function.Consumer;
import java.util.function.Predicate;


public class AgentDeploymentVerification implements DeploymentVerification {

  private static final String STARTED_STATUS = "STARTED";

  private final AgentClient client;
  private DeploymentVerification verification;

  public AgentDeploymentVerification(AgentClient client) {
    this.client = client;
    this.verification = new DefaultDeploymentVerification(new AgentDeploymentVerificationStrategy());
  }

  @Override
  public void assertDeployment(Deployment deployment) throws DeploymentException {
    verification.assertDeployment(deployment);
  }

  private class AgentDeploymentVerificationStrategy implements DeploymentVerificationStrategy {

    @Override
    public Predicate<Deployment> isDeployed() {
      return (deployment) -> {
        Application application = client.getApplication(deployment.getApplicationName());
        return application != null && StringUtils.equals(application.state, STARTED_STATUS);
      };
    }

    @Override
    public Consumer<Deployment> onTimeout() {
      return deployment -> client.undeployApplication(deployment.getApplicationName());
    }
  }
}
