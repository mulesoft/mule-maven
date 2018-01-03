/*
 * Mule ESB Maven Tools
 * <p>
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * <p>
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.api.validation;

import static com.vdurmont.semver4j.Semver.SemverType.LOOSE;
import static de.skuzzle.semantic.Version.create;
import static de.skuzzle.semantic.Version.isValidVersion;
import static de.skuzzle.semantic.Version.parseVersion;

import com.vdurmont.semver4j.Semver;

import org.apache.commons.lang3.StringUtils;
import org.mule.tools.api.exception.ValidationException;

import java.util.Optional;

/**
 * @author Mulesoft Inc.
 * @since 2.0.0
 */
public class VersionUtils {

  private static final String NULL_VERSION = "0.0.0";

  /**
   * Validates if a version complies with semantic versioning specification
   *
   * @param version the version to validate
   * @return false if the version does not comply with semantic versioning, true otherwise
   */
  public static Boolean isVersionValid(String version) {
    return !StringUtils.equals(version, NULL_VERSION) && isValidVersion(version);
  }

  /**
   * Validates if {@code version1} is greater or equal than {@code version2}
   *
   * @param version1
   * @param version2
   * @return false if version1 is lesser than version2
   */
  public static Boolean isVersionGreaterOrEquals(String version1, String version2) throws ValidationException {
    String v1 = completeIncremental(version1);
    String v2 = completeIncremental(version2);
    return parseVersion(v1).compareTo(parseVersion(v2)) >= 0;
  }

  /**
   * It completes the incremental version number with 0 in the event the version provided has the form x to become x.0.0 or x.y to
   * become x.y.0
   *
   * @param version the version to be completed
   * @return The completed version x.y.z with no qualifier
   */
  public static String completeIncremental(String version) throws ValidationException {
    Semver semver = new Semver(version, LOOSE);
    Optional<Integer> minor = Optional.ofNullable(semver.getMinor());
    Optional<Integer> patch = Optional.ofNullable(semver.getPatch());
    if (!minor.isPresent() || !patch.isPresent()) {
      version = create(semver.getMajor(), minor.orElse(0), patch.orElse(0)).toString();
    }
    if (!isVersionValid(version)) {
      throw new ValidationException("Version is invalid: " + version);
    }
    return getBaseVersion(version);
  }

  /**
   * Returns the base version, i.e., in the format major.minor.patch.
   * 
   * @param version
   * @return the base part of the version.
   */
  public static String getBaseVersion(String version) {
    return new Semver(version).withClearedSuffixAndBuild().getValue();
  }

  /**
   * Returns the version major.
   * 
   * @param version
   * @return the major part of the version.
   */
  public static String getMajor(String version) {
    return String.valueOf(new Semver(version).getMajor());
  }
}
