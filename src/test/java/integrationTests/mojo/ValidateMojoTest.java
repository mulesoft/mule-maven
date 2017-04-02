/*
 * Mule ESB Maven Tools
 * <p>
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * <p>
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package integrationTests.mojo;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.junit.Test;

public class ValidateMojoTest extends MojoTest {

  protected static final String VALIDATE = "validate";
  private static final String MISSING_DECLARED_SHARED_LIBRARIES_PROJECT = "missing-declared-shared-libraries-project";
  private static final String INVALID_PACKAGE_PROJECT = "invalid-package-project";
  private static final String VALIDATE_SHARED_LIBRARIES_PROJECT = "validate-shared-libraries-project";
  private static final String DEPENDENCY_A_GROUP_ID = "group.id.a";
  private static final String DEPENDENCY_A_ARTIFACT_ID = "artifact-id-a";
  private static final String DEPENDENCY_A_VERSION = "1.0.0-SNAPSHOT";
  private static final String DEPENDENCY_A_TYPE = "jar";
  private static final String DEPENDENCY_B_GROUP_ID = "group.id.b";
  private static final String DEPENDENCY_B_ARTIFACT_ID = "artifact-id-b";
  private static final String DEPENDENCY_B_VERSION = "1.0.0";
  private static final String DEPENDENCY_B_TYPE = "jar";
  private static final String DEPENDENCY_A_PROJECT_NAME = "dependency-a";
  private static final String DEPENDENCY_B_PROJECT_NAME = "dependency-b";

  public ValidateMojoTest() {
    this.goal = VALIDATE;
  }

  @Test
  public void testFailOnEmptyProject() throws Exception {
    projectBaseDirectory = builder.createProjectBaseDir(EMPTY_PROJECT_NAME, this.getClass());
    verifier = new Verifier(projectBaseDirectory.getAbsolutePath());
    try {
      verifier.executeGoal(VALIDATE);
    } catch (VerificationException e) {
    }
    verifier.verifyTextInLog("Invalid Mule project. Missing src/main/mule folder. This folder is mandatory");
  }

  @Test
  public void testFailOnInvalidPackageType() throws Exception {
    projectBaseDirectory = builder.createProjectBaseDir(INVALID_PACKAGE_PROJECT, this.getClass());
    verifier = new Verifier(projectBaseDirectory.getAbsolutePath());
    try {
      verifier.executeGoal(VALIDATE);
    } catch (VerificationException e) {
    }
    verifier.verifyTextInLog("Unknown packaging: mule-invalid");
  }

  @Test
  public void testFailOnEmptyPolicyProject() throws Exception {
    projectBaseDirectory = builder.createProjectBaseDir(EMPTY_POLICY_NAME, this.getClass());
    verifier = new Verifier(projectBaseDirectory.getAbsolutePath());
    try {
      verifier.executeGoal(VALIDATE);
    } catch (VerificationException e) {
    }
    verifier.verifyTextInLog("Invalid Mule project. Missing src/main/policy folder. This folder is mandatory");
  }

  @Test
  public void testFailOnEmptyDomainProject() throws Exception {
    projectBaseDirectory = builder.createProjectBaseDir(EMPTY_DOMAIN_NAME, this.getClass());
    verifier = new Verifier(projectBaseDirectory.getAbsolutePath());
    try {
      verifier.executeGoal(VALIDATE);
    } catch (VerificationException e) {
    }
    verifier.verifyTextInLog("Invalid Mule project. Missing src/main/mule folder. This folder is mandatory");
  }

  @Test
  public void testFailOnMissingSharedLibrariesProject() throws Exception {
    projectBaseDirectory = builder.createProjectBaseDir(MISSING_DECLARED_SHARED_LIBRARIES_PROJECT, this.getClass());
    verifier = new Verifier(projectBaseDirectory.getAbsolutePath());

    try {
      verifier.executeGoal(VALIDATE);
    } catch (VerificationException e) {
    }
    verifier.verifyTextInLog("The mule application does not contain the following shared libraries: ");
  }

  @Test
  public void testValidateSharedLibrariesProject() throws Exception {
    installThirdPartyArtifact(DEPENDENCY_A_GROUP_ID, DEPENDENCY_A_ARTIFACT_ID, DEPENDENCY_A_VERSION, DEPENDENCY_A_TYPE,
                              DEPENDENCY_A_PROJECT_NAME);
    installThirdPartyArtifact(DEPENDENCY_B_GROUP_ID, DEPENDENCY_B_ARTIFACT_ID, DEPENDENCY_B_VERSION, DEPENDENCY_B_TYPE,
                              DEPENDENCY_B_PROJECT_NAME);

    projectBaseDirectory = builder.createProjectBaseDir(VALIDATE_SHARED_LIBRARIES_PROJECT, this.getClass());
    verifier = new Verifier(projectBaseDirectory.getAbsolutePath());
    try {
      verifier.executeGoal(VALIDATE);
    } catch (VerificationException e) {
    }
    verifier.verifyErrorFreeLog();
  }

  @Test
  public void testFailMissingJsonOnPolicyProject() throws Exception {
    String artifactId = "missing-json-policy-project";
    projectBaseDirectory = builder.createProjectBaseDir(artifactId, this.getClass());
    verifier = new Verifier(projectBaseDirectory.getAbsolutePath());
    try {
      verifier.executeGoal(VALIDATE);
    } catch (VerificationException e) {
    }
    verifier.verifyTextInLog("Invalid Mule project. Missing mule-policy.json file, it must be present in the root of application");
  }

  @Test
  public void testFailMissingJsonOnDomainProject() throws Exception {
    String artifactId = "missing-json-domain-project";
    projectBaseDirectory = builder.createProjectBaseDir(artifactId, this.getClass());
    verifier = new Verifier(projectBaseDirectory.getAbsolutePath());
    try {
      verifier.executeGoal(VALIDATE);
    } catch (VerificationException e) {
    }
    verifier.verifyTextInLog("Invalid Mule project. Missing mule-application.json file, it must be present in the root of application");
  }

  @Test
  public void testFailMissingJsonAppProject() throws Exception {
    String artifactId = "missing-json-project";
    projectBaseDirectory = builder.createProjectBaseDir(artifactId, this.getClass());
    verifier = new Verifier(projectBaseDirectory.getAbsolutePath());
    try {
      verifier.executeGoal(VALIDATE);
    } catch (VerificationException e) {
    }
    verifier.verifyTextInLog("Invalid Mule project. Missing mule-application.json file, it must be present in the root of application");
  }

}
