/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.validation.cloudhub;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mule.tools.client.cloudhub.CloudHubClient;
import org.mule.tools.client.cloudhub.model.Application;
import org.mule.tools.client.cloudhub.model.MuleVersion;
import org.mule.tools.client.cloudhub.model.SupportedVersion;
import org.mule.tools.model.anypoint.CloudHubDeployment;
import org.mule.tools.utils.DeployerLog;
import org.mule.tools.validation.AbstractDeploymentValidator;
import org.mule.tools.validation.EnvironmentSupportedVersions;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class CloudHubDeploymentValidatorTest {

  private static final String MULE_VERSION1 = "4.0.0";
  private static final String MULE_VERSION2 = "4.1.0";
  private static final String BASE_URI = "https://anypoint.mulesoft.com";
  private Application application;
  private MuleVersion muleVersion;

  private List<SupportedVersion> supportedVersions;

  private EnvironmentSupportedVersions expectedEnvironmentSupportedVersions;


  private final CloudHubDeployment cloudHubDeployment = new CloudHubDeployment();
  private static final DeployerLog LOG_MOCK = mock(DeployerLog.class);
  private AbstractDeploymentValidator validatorSpy;

  @BeforeEach
  public void setUp() {
    application = new Application();
    muleVersion = new MuleVersion();
    muleVersion.setVersion("4.3.0");
    muleVersion.setJavaVersion("17");
    muleVersion.setReleaseChannel("LTS");
    muleVersion.setUpdateId("updateId");
    muleVersion.setState("state");
    muleVersion.setLatestUpdateId("updateId");
    muleVersion.setLog4j1Used(true);
    muleVersion.setMonitoringSupported(true);
    muleVersion.setPersistentQueuesSupported(true);
    muleVersion.setVpnSupported("vpn");
    application.setIpAddresses(new ArrayList<>()).setLogLevels(new ArrayList<>()).setPersistentQueuesEncryptionEnabled(false)
        .setUserId("userId").setPersistentQueuesEncrypted(false).setStatus("STARTED").setProperties(new HashMap<>())
        .setMuleVersion(muleVersion).setDomain("domain").setId("id").setFullDomain("fullDomain").setDescription("Description")
        .setLastUpdateTime(new Long(1)).setFilename("fileName").setTentants(1).setUserName("name").setMonitoringEnabled(true)
        .setStaticIPsEnabled(true).setMultitenanted(true).setHasFile(true).setSecureDataGatewayEnabled(true)
        .setTrackingSettings(new HashMap<>()).setPreviousMuleVersion(muleVersion).setSupportedVersions(new ArrayList<>())
        .setVpnConfig(new HashMap<>()).setVpnEnabled(true);

    SupportedVersion sv1 = new SupportedVersion();
    sv1.setVersion(MULE_VERSION1);

    SupportedVersion sv2 = new SupportedVersion();
    sv1.setVersion(MULE_VERSION2);

    supportedVersions = asList(sv1, sv2);
    expectedEnvironmentSupportedVersions = new EnvironmentSupportedVersions(supportedVersions.stream().map(sv -> sv.getVersion())
        .collect(Collectors.toSet()));
  }

  @Test
  public void getEnvironmentSupportedVersionsTest() throws Exception {
    cloudHubDeployment.setUri(BASE_URI);

    validatorSpy = spy(new CloudHubDeploymentValidator(cloudHubDeployment));

    CloudHubClient clientSpy = spy(new CloudHubClient(cloudHubDeployment, LOG_MOCK));
    doReturn(clientSpy).when((CloudHubDeploymentValidator) validatorSpy).getCloudHubClient();

    doReturn(supportedVersions).when(clientSpy).getSupportedMuleVersions();

    assertThat(validatorSpy.getEnvironmentSupportedVersions())
        .describedAs("Supported version that was generated is not the expected").isEqualTo(expectedEnvironmentSupportedVersions);
  }

  @Test
  public void testCloudhubClient() {
    CloudHubDeployment cloudhubDeployment = new CloudHubDeployment();
    DeployerLog log = mock(DeployerLog.class);
    cloudhubDeployment.setUri("anypoint.baseUri");
    CloudHubClient client = new CloudHubClientTest(cloudhubDeployment, log);
    File file = mock(File.class);
    when(file.getName()).thenReturn("name");
    when(file.exists()).thenReturn(false);
    Application response = client.createApplication(application, file);
    assertThat(response.getStatus()).isEqualTo("STARTED");
    assertThat(response.getMuleVersion()).isEqualTo(muleVersion);
    assertThat(response.getUserId()).isEqualTo("userId");
    response = client.updateApplication(application, file);
    assertThat(response.getStatus()).isEqualTo("STARTED");
    assertThat(response.getMuleVersion()).isEqualTo(muleVersion);
    assertThat(response.getUserId()).isEqualTo("userId");
    assertThat(response.getDomain()).isEqualTo("domain");
    assertThat(response.getId()).isEqualTo("id");
    assertThat(response.getFullDomain()).isEqualTo("fullDomain");
    assertThat(response.getDescription()).isEqualTo("Description");
    assertThat(response.getLastUpdateTime()).isEqualTo(new Long(1));
    assertThat(response.getFilename()).isEqualTo("fileName");
    assertThat(response.getTentants()).isEqualTo(1);
    assertThat(response.getUserName()).isEqualTo("name");
    assertThat(response.getMonitoringEnabled()).isEqualTo(true);
    assertThat(response.getStaticIPsEnabled()).isEqualTo(true);
    assertThat(response.getMultitenanted()).isEqualTo(true);
    assertThat(response.getHasFile()).isEqualTo(true);
    assertThat(response.getSecureDataGatewayEnabled()).isEqualTo(true);
    assertThat(response.getVpnEnabled()).isEqualTo(true);
    assertThat(response.getMuleVersion().getVersion()).isEqualTo(muleVersion.getVersion());
    assertThat(response.getMuleVersion().getJavaVersion()).isEqualTo(muleVersion.getJavaVersion());
    assertThat(response.getMuleVersion().getReleaseChannel()).isEqualTo(muleVersion.getReleaseChannel());
    assertThat(response.getMuleVersion().getUpdateId()).isEqualTo(muleVersion.getUpdateId());
    assertThat(response.getMuleVersion().getState()).isEqualTo(muleVersion.getState());
    assertThat(response.getMuleVersion().getLatestUpdateId()).isEqualTo(muleVersion.getLatestUpdateId());
    assertThat(response.getMuleVersion().getLog4j1Used()).isEqualTo(muleVersion.getLog4j1Used());
    assertThat(response.getMuleVersion().getMonitoringSupported()).isEqualTo(muleVersion.getMonitoringSupported());
    assertThat(response.getMuleVersion().getPersistentQueuesSupported()).isEqualTo(muleVersion.getPersistentQueuesSupported());
    assertThat(response.getMuleVersion().getVpnSupported()).isEqualTo(muleVersion.getVpnSupported());
    client.deleteApplications("domain");
  }


  public class CloudHubClientTest extends CloudHubClient {

    public CloudHubClientTest(CloudHubDeployment cloudhubDeployment, DeployerLog log) {
      super(cloudhubDeployment, log);
    }

    @Override
    protected Response post(String uri, String path, Entity entity) {
      Response response = mock(Response.class);
      when(response.getStatus()).thenReturn(200);
      when(response.readEntity(Application.class)).thenReturn(application);
      return response;
    }

    @Override
    public Response put(String uri, String path, Entity entity) {
      Response response = mock(Response.class);
      when(response.getStatus()).thenReturn(200);
      when(response.readEntity(Application.class)).thenReturn(application);
      return response;
    }

    @Override
    protected Response delete(String uri, String path) {
      Response response = mock(Response.class);
      when(response.getStatus()).thenReturn(200);
      return response;
    }

  }
}
