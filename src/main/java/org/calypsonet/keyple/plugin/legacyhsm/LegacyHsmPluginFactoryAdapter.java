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

import org.eclipse.keyple.core.common.CommonApiProperties;
import org.eclipse.keyple.core.plugin.PluginApiProperties;
import org.eclipse.keyple.core.plugin.spi.PoolPluginFactorySpi;
import org.eclipse.keyple.core.plugin.spi.PoolPluginSpi;

/**
 * Legacy HSM plugin factory extension adapter.
 *
 * @since 1.0.0
 */
final class LegacyHsmPluginFactoryAdapter implements LegacyHsmPluginFactory, PoolPluginFactorySpi {

  /**
   * The unique name of the plugin.
   *
   * @since 1.0.0
   */
  static final String PLUGIN_NAME = "LegacyHsmPlugin";

  /**
   * {@inheritDoc}
   *
   * @since 1.0.0
   */
  @Override
  public String getPluginApiVersion() {
    return PluginApiProperties.VERSION;
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0.0
   */
  @Override
  public String getCommonApiVersion() {
    return CommonApiProperties.VERSION;
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0.0
   */
  @Override
  public String getPoolPluginName() {
    return PLUGIN_NAME;
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0.0
   */
  @Override
  public PoolPluginSpi getPoolPlugin() {
    return new LegacyHsmPluginAdapter();
  }
}
