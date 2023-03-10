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

import org.calypsonet.keyple.plugin.legacyhsm.LegacyHsmPluginFactoryProvider;
import org.calypsonet.terminal.calypso.WriteAccessLevel;
import org.calypsonet.terminal.calypso.card.CalypsoCard;
import org.calypsonet.terminal.calypso.sam.CalypsoSam;
import org.calypsonet.terminal.calypso.transaction.CardSecuritySetting;
import org.calypsonet.terminal.calypso.transaction.CardTransactionManager;
import org.calypsonet.terminal.reader.CardReader;
import org.calypsonet.terminal.reader.selection.CardSelectionManager;
import org.calypsonet.terminal.reader.selection.CardSelectionResult;
import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.core.service.*;
import org.eclipse.keyple.core.service.resource.CardResource;
import org.eclipse.keyple.core.service.resource.CardResourceServiceProvider;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calypso Card authentication (PC/SC)
 *
 * <p>We demonstrate here the authentication of a Calypso card using a Secure Session in which a
 * file from the card is read. The read is certified by verifying the signature of the card by a
 * virtual Calypso SAM provided by a Legacy HSM.
 *
 * <p>Only one reader is required for this example: a contactless reader for the Calypso Card, the
 * Calypso SAM being provided by the HSM.
 *
 * <p>Scenario:
 *
 * <ul>
 *   <li>Sets up the card resource service to provide a Calypso SAM (C1).
 *   <li>Checks if an ISO 14443-4 card is in the reader, enables the card selection manager.
 *   <li>Attempts to select the specified card (here a Calypso card characterized by its AID) with
 *       an AID-based application selection scenario.
 *   <li>Creates a {@link CardTransactionManager} using {@link CardSecuritySetting} referencing the
 *       SAM profile defined in the card resource service.
 *   <li>Read a file record in Secure Session.
 * </ul>
 *
 * All results are logged with slf4j.
 *
 * <p>Any unexpected behavior will result in runtime exceptions.
 *
 * @since 1.0.0
 */
public class Main_CardAuthentication_PCSC_HSM {
  private static final Logger logger =
      LoggerFactory.getLogger(Main_CardAuthentication_PCSC_HSM.class);

  public static void main(String[] args) {

    // Get the instance of the SmartCardService (singleton pattern)
    SmartCardService smartCardService = SmartCardServiceProvider.getService();

    // Register the PcscPlugin used for cards
    Plugin pcscPlugin = smartCardService.registerPlugin(PcscPluginFactoryBuilder.builder().build());

    // Register the LegacyHsmPlugin used for SAMs
    PoolPlugin hsmPlugin =
        (PoolPlugin) smartCardService.registerPlugin(LegacyHsmPluginFactoryProvider.getFactory());

    // Get and set up the card reader
    // We suppose here, we use an ASK LoGO contactless PC/SC reader as card reader.
    CardReader cardReader =
        ConfigurationUtil.getCardReader(pcscPlugin, ConfigurationUtil.CARD_READER_NAME_REGEX);

    // Configure the card resource service to provide an adequate SAM for future secure operations.
    // We use a Legacy HSM.
    ConfigurationUtil.setupCardResourceService(
        hsmPlugin, CalypsoConstant.HSM_KEY_GROUP, CalypsoConstant.SAM_PROFILE_NAME);

    // Get the Calypso card extension service
    CalypsoExtensionService cardExtension = CalypsoExtensionService.getInstance();

    // Verify that the extension's API level is consistent with the current service.
    smartCardService.checkCardExtension(cardExtension);

    logger.info("=============== Calypso card authentication ==================");

    // Check if a card is present in the reader
    if (!cardReader.isCardPresent()) {
      throw new IllegalStateException("No card is present in the reader.");
    }

    logger.info("= #### Select application with AID = '{}'.", CalypsoConstant.AID);

    // Get the core card selection manager.
    CardSelectionManager cardSelectionManager = smartCardService.createCardSelectionManager();

    // Create a card selection using the Calypso card extension.
    // Prepare the selection by adding the created Calypso card selection to the card selection
    // scenario.
    cardSelectionManager.prepareSelection(
        cardExtension
            .createCardSelection()
            .acceptInvalidatedCard()
            .filterByDfName(CalypsoConstant.AID));

    // Actual card communication: run the selection scenario.
    CardSelectionResult selectionResult =
        cardSelectionManager.processCardSelectionScenario(cardReader);

    // Check the selection result.
    if (selectionResult.getActiveSmartCard() == null) {
      throw new IllegalStateException(
          "The selection of the application " + CalypsoConstant.AID + " failed.");
    }

    // Get the SmartCard resulting of the selection.
    CalypsoCard calypsoCard = (CalypsoCard) selectionResult.getActiveSmartCard();

    logger.info("= SmartCard = {}", calypsoCard);

    // Create security settings that reference the same SAM profile requested from the card resource
    // service.
    CardResource samResource =
        CardResourceServiceProvider.getService().getCardResource(CalypsoConstant.SAM_PROFILE_NAME);

    CardSecuritySetting cardSecuritySetting =
        CalypsoExtensionService.getInstance()
            .createCardSecuritySetting()
            .setControlSamResource(
                samResource.getReader(), (CalypsoSam) samResource.getSmartCard());

    try {
      // Performs file reads using the card transaction manager in non-secure mode.
      cardExtension
          .createCardTransaction(cardReader, calypsoCard, cardSecuritySetting)
          .prepareOpenSecureSession(WriteAccessLevel.DEBIT)
          .prepareReadRecords(
              CalypsoConstant.SFI_ENVIRONMENT_AND_HOLDER,
              CalypsoConstant.RECORD_NUMBER_1,
              CalypsoConstant.RECORD_NUMBER_1,
              CalypsoConstant.RECORD_SIZE)
          .prepareCloseSecureSession()
          .processCommands(true);
    } finally {
      try {
        CardResourceServiceProvider.getService().releaseCardResource(samResource);
      } catch (RuntimeException e) {
        logger.error("Error during the card resource release: {}", e.getMessage(), e);
      }
    }

    logger.info(
        "The Secure Session ended successfully, the card is authenticated and the data read are certified.");

    String sfiEnvHolder = HexUtil.toHex(CalypsoConstant.SFI_ENVIRONMENT_AND_HOLDER);
    logger.info(
        "File {}h, rec 1: FILE_CONTENT = {}",
        sfiEnvHolder,
        calypsoCard.getFileBySfi(CalypsoConstant.SFI_ENVIRONMENT_AND_HOLDER));

    logger.info("= #### End of the Calypso card processing.");

    System.exit(0);
  }
}
