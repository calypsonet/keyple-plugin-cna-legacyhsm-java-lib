/* **************************************************************************************
 * Copyright (c) 2018 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.calypsonet.keyple.plugin.legacyhsm.example;

/**
 * Helper class to provide specific constants to manipulate Calypso cards from the Keyple demo kit.
 *
 * <ul>
 *   <li>AID application selection (default Calypso AID)
 *   <li>File
 *   <li>File definitions and identifiers (SFI)
 *   <li>Sample data
 *   <li>Security settings
 * </ul>
 */
public final class CalypsoConstant {

  /** Constructor. */
  private CalypsoConstant() {}

  // Application
  /** AID: Keyple test kit profile 1, Application 2 */
  public static final String AID = "315449432E49434131";

  // File structure
  public static final int RECORD_SIZE = 29;

  public static final byte RECORD_NUMBER_1 = 1;

  // File identifiers
  public static final byte SFI_ENVIRONMENT_AND_HOLDER = (byte) 0x07;

  // Security settings
  public static final String SAM_PROFILE_NAME = "SAM C1";
  public static final String HSM_KEY_GROUP = "1";
}
