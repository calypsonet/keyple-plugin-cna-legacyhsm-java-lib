/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.calypsonet.keyple.plugin.legacyhsm;

/**
 * Legacy HSM plugin factory provider.
 *
 * @since 1.0.0
 */
public final class LegacyHsmPluginFactoryProvider {

  /**
   * Returns the factory to register to the Keyple core service.
   *
   * @return A new instance.
   * @since 1.0.0
   */
  public static LegacyHsmPluginFactory getFactory() {
    return new LegacyHsmPluginFactoryAdapter();
  }
}
