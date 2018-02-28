/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.maven.utils;

public class DefaultExclusion extends org.apache.maven.model.Exclusion
    implements org.mule.tools.api.packager.packaging.Exclusion {

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("<");
    buf.append(getClass().getSimpleName());
    buf.append(" ");
    buf.append(getGroupId());
    buf.append(":");
    buf.append(getArtifactId());
    buf.append(">");
    return buf.toString();
  }

  public String asFilter() {
    StringBuilder buf = new StringBuilder();
    buf.append(getGroupId());
    buf.append(":");
    buf.append(getArtifactId());
    return buf.toString();
  }
}
