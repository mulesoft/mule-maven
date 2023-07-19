/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.tools.model.anypoint;

import org.junit.Before;
import org.junit.Test;
import org.mule.tools.client.core.exception.DeploymentException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.spy;

public class CloudHubDeploymentTest {

  private CloudHubDeployment deploymentSpy;

  @Before
  public void setUp() {
    deploymentSpy = spy(CloudHubDeployment.class);
  }

  @Test
  public void setCloudHubDeploymentDefaultValuesCloudHubWorkersSetSystemPropertyTest() throws DeploymentException {
    String cloudHubWorkers = "10";
    System.setProperty("cloudhub.workers", cloudHubWorkers);
    deploymentSpy.setEnvironmentSpecificValues();
    assertThat("The cloudhub workers was not resolved by system property",
               deploymentSpy.getWorkers(), equalTo(Integer.valueOf(cloudHubWorkers)));
    System.clearProperty("cloudhub.workers");
  }

  @Test
  public void setCloudHubDeploymentDefaultValuesCloudHubWorkerTypeSetSystemPropertyTest() throws DeploymentException {
    String cloudHubWorkerType = "worker-type";
    System.setProperty("cloudhub.workerType", cloudHubWorkerType);
    deploymentSpy.setEnvironmentSpecificValues();
    assertThat("The cloudhub worker type property was not resolved by system property",
               deploymentSpy.getWorkerType(), equalTo(cloudHubWorkerType));
    System.clearProperty("cloudhub.workerType");
  }

  @Test
  public void defaultOsV2ValueIsNull() {
    assertThat("The default value for Object Store v2 property is not null",
               deploymentSpy.getObjectStoreV2(), equalTo(null));
  }

  @Test
  public void defaultPersistentQueuesValueIsFalse() {
    assertThat("The default value for Persistent Queues property is not false",
               deploymentSpy.getPersistentQueues(), equalTo(false));
  }

  @Test
  public void defaultDisableCloudHubLogsValueIsFalse() {
    assertThat("The default value for Custom Log4J property is not false",
               deploymentSpy.getDisableCloudHubLogs(), equalTo(false));
  }

  @Test
  public void defaultWaitBeforeValidationIsZero() {
    assertThat("The default value for pepe is not zero",
               deploymentSpy.getWaitBeforeValidation(), equalTo(6000));
  }

  @Test
  public void defaultApplyLatestRuntimePatch() {
    assertThat("The default value for apply patch property must be false",
               deploymentSpy.getApplyLatestRuntimePatch(), equalTo(false));
  }
}
