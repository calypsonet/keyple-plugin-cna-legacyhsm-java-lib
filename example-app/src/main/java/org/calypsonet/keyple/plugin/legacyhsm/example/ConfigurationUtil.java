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
package org.calypsonet.keyple.plugin.legacyhsm.example;

import org.calypsonet.terminal.calypso.sam.CalypsoSamSelection;
import org.calypsonet.terminal.reader.CardReader;
import org.calypsonet.terminal.reader.ConfigurableCardReader;
import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.core.service.Plugin;
import org.eclipse.keyple.core.service.PoolPlugin;
import org.eclipse.keyple.core.service.resource.*;
import org.eclipse.keyple.core.service.resource.spi.CardResourceProfileExtension;
import org.eclipse.keyple.plugin.pcsc.PcscReader;
import org.eclipse.keyple.plugin.pcsc.PcscSupportedContactlessProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class providing methods for configuring readers and the card resource service used across
 * several examples.
 */
public class ConfigurationUtil {
  private static final Logger logger = LoggerFactory.getLogger(ConfigurationUtil.class);

  // Common contactless reader identifiers
  public static final String CARD_READER_NAME_REGEX = ".*ASK LoGO.*|.*Contactless.*";
  public static final String ISO_CARD_PROTOCOL = "ISO_14443_4_CARD";

  /** Constructor. */
  private ConfigurationUtil() {}

  /**
   * Retrieves the first available reader in the provided plugin whose name matches the provided
   * regular expression.
   *
   * @param plugin The plugin to which the reader belongs.
   * @param readerNameRegex A regular expression matching the targeted reader.
   * @return A not null reference.
   * @throws IllegalStateException If the reader is not found.
   */
  public static CardReader getCardReader(Plugin plugin, String readerNameRegex) {
    for (String readerName : plugin.getReaderNames()) {
      if (readerName.matches(readerNameRegex)) {
        // Configure the reader with parameters suitable for contactless operations.
        plugin
            .getReaderExtension(PcscReader.class, readerName)
            .setContactless(true)
            .setIsoProtocol(PcscReader.IsoProtocol.T1)
            .setSharingMode(PcscReader.SharingMode.SHARED);
        ConfigurableCardReader reader = (ConfigurableCardReader) plugin.getReader(readerName);
        reader.activateProtocol(
            PcscSupportedContactlessProtocol.ISO_14443_4.name(), ISO_CARD_PROTOCOL);
        logger.info("Card reader, plugin; {}, name: {}", plugin.getName(), reader.getName());
        return reader;
      }
    }
    throw new IllegalStateException(
        String.format("Reader '%s' not found in plugin '%s'", readerNameRegex, plugin.getName()));
  }

  /**
   * Set up the {@link CardResourceService} to provide a Calypso SAM C1 resource when requested.
   *
   * @param poolPlugin The plugin to which the SAM reader belongs.
   * @param samProfileName A string defining the SAM profile.
   * @throws IllegalStateException If the expected card resource is not found.
   */
  public static void setupCardResourceService(
      PoolPlugin poolPlugin, String readerGroupReference, String samProfileName) {

    // Create a card resource extension expecting a SAM "C1".
    CalypsoSamSelection samSelection = CalypsoExtensionService.getInstance().createSamSelection();
    CardResourceProfileExtension samCardResourceExtension =
        CalypsoExtensionService.getInstance().createSamResourceProfileExtension(samSelection);

    // Get the service
    CardResourceService cardResourceService = CardResourceServiceProvider.getService();

    // Create a minimalist configuration (no plugin/reader observation)
    cardResourceService
        .getConfigurator()
        .withPoolPlugins(PoolPluginsConfigurator.builder().addPoolPlugin(poolPlugin).build())
        .withCardResourceProfiles(
            CardResourceProfileConfigurator.builder(samProfileName, samCardResourceExtension)
                .withReaderGroupReference(readerGroupReference)
                .build())
        .configure();

    // Start the service
    cardResourceService.start();

    // verify the resource availability
    CardResource cardResource = cardResourceService.getCardResource(samProfileName);

    if (cardResource == null) {
      throw new IllegalStateException(
          String.format(
              "Unable to retrieve a SAM card resource for profile '%s' in plugin '%s'",
              samProfileName, poolPlugin.getName()));
    }

    // release the resource
    cardResourceService.releaseCardResource(cardResource);
  }
}
