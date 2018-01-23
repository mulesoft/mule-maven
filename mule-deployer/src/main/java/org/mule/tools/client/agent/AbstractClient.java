/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.client.agent;


import static com.google.common.net.HttpHeaders.USER_AGENT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.Family.familyOf;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.glassfish.jersey.client.HttpUrlConnectorProvider.SET_METHOD_WORKAROUND;
import static org.mule.tools.client.authentication.AuthenticationServiceClient.LOGIN;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import com.sun.net.httpserver.Headers;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import org.mule.tools.client.exception.ClientException;
import org.mule.tools.utils.DeployerLog;

public abstract class AbstractClient {

  private static final String USER_AGENT_MULE_DEPLOYER = "mule-deployer%s";

  protected DeployerLog log;

  public AbstractClient() {}

  public AbstractClient(DeployerLog log) {
    this.log = log;
  }

  protected WebTarget getTarget(String uri, String path) {
    ClientBuilder builder = ClientBuilder.newBuilder();
    configureSecurityContext(builder);
    Client client = builder.build().register(MultiPartFeature.class);
    if (log != null && log.isDebugEnabled() && !isLoginRequest(path)) {
      client.register(new ClientLoggingFilter(log));
    }

    return client.target(uri).path(path);
  }

  private boolean isLoginRequest(String path) {
    return LOGIN.equals(path);
  }

  protected void validateStatusSuccess(Response response) {
    if (familyOf(response.getStatus()) != SUCCESSFUL) {
      throw new ClientException(response);
    }
  }

  protected void configureSecurityContext(ClientBuilder builder) {
    // Implemented in concrete classes
  }

  protected Response post(String uri, String path, Entity entity) {
    return builder(uri, path).post(entity);
  }

  protected Response post(String uri, String path, Object entity) {
    return post(uri, path, Entity.entity(entity, APPLICATION_JSON_TYPE));
  }

  protected Response put(String uri, String path, Entity entity) {
    return builder(uri, path).put(entity);
  }

  protected Response put(String uri, String path, Object entity) {
    return put(uri, path, Entity.entity(entity, APPLICATION_JSON_TYPE));
  }

  protected Response delete(String uri, String path) {
    return builder(uri, path).delete();
  }

  protected Response get(String uri, String path) {
    return builder(uri, path).get();
  }

  protected <T> T get(String uri, String path, Class<T> clazz) {
    return get(uri, path).readEntity(clazz);
  }

  protected Response patch(String uri, String path, Entity entity) {
    Invocation.Builder builder = builder(uri, path);
    builder.property(SET_METHOD_WORKAROUND, true);
    return builder.method("PATCH", entity);
  }

  private Invocation.Builder builder(String uri, String path) {
    WebTarget target = getTarget(uri, path);
    Invocation.Builder builder = target.request(APPLICATION_JSON_TYPE).header(USER_AGENT, getUserAgentMuleDeployer());
    configureRequest(builder);
    return builder;
  }

  protected String getUserAgentMuleDeployer() {
    Package classPackage = AbstractClient.class.getPackage();
    String implementationVersion = classPackage != null ? classPackage.getImplementationVersion() : EMPTY;

    String version = isNotBlank(implementationVersion) ? "-[" + implementationVersion + "]" : EMPTY;
    return String.format(USER_AGENT_MULE_DEPLOYER, version);
  }

  /**
   * Template method to allow subclasses to configure the request (adding headers for example).
   * 
   * @param builder The invocation builder for the request.
   */
  protected void configureRequest(Invocation.Builder builder) {

  }

}
