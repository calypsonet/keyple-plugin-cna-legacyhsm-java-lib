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

import com.spirtech.csm.Csm;
import com.spirtech.csm.CsmChannel;
import com.spirtech.csm.CsmException;
import com.spirtech.csm.CsmInfoRecord;
import com.spirtech.csm.CsmKeyInfo;
import com.spirtech.csm.CsmSystem;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import org.eclipse.keyple.core.plugin.PluginIOException;
import org.eclipse.keyple.core.plugin.ReaderIOException;
import org.eclipse.keyple.core.plugin.spi.PoolPluginSpi;
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi;
import org.eclipse.keyple.core.util.HexUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Legacy HSM plugin extension adapter.
 *
 * @since 1.0.0
 */
final class LegacyHsmPluginAdapter implements LegacyHsmPlugin, PoolPluginSpi {

  private static final Logger logger = LoggerFactory.getLogger(LegacyHsmPluginAdapter.class);

  private final CsmSystem csmSystem;
  private final Map<Integer, List<Csm>> keyGroupToCsmsMap =
      Collections.synchronizedMap(new HashMap<Integer, List<Csm>>());

  /**
   * 
   * Do the initialization of the plugin.
   *
   * <p>It initializes the {@link CsmSystem} and get all necessary information from the available
   * CSMs.
   *
   * <p>It records the mapping between available key group references and CSMs in order to speed up
   * the selection of the right CSM for a particular group reference (in the case where multiple CSM
   * are available)
   *
   * <p>According to the current log level, it also prints more or less information about the
   * available keys.
   *
   * @since 1.0.0
   */
  LegacyHsmPluginAdapter() {

    if (logger.isTraceEnabled()) {
      logger.trace("Initializing HSM client...");
    }
    csmSystem = CsmSystem.getInstance();

    try {
      csmSystem.Initialize();
    } catch (CsmException e) {
      tryFree(csmSystem);
      throw new IllegalStateException(
          String.format("Unable to initialize the HSM client: %s", e.getMessage()), e);
    }

    List<Csm> csmList;
    try {
      csmList = csmSystem.getCsmList();
      if (logger.isTraceEnabled()) {
        logger.trace("CSM list size = {}", csmList.size());
      }
      if (csmList.isEmpty()) {
        throw new IllegalStateException("No CSM found retry getCsmList...");
      }
    } catch (CsmException e) {
      tryFree(csmSystem);
      throw new IllegalStateException(
          String.format("Unable to get the list of CSM: %s", e.getMessage()), e);
    }

    for (Csm csm : csmList) {
      try {
        CsmInfoRecord info = csm.getInfos();
        if (info != null) {
          String csmInfo =
              String.format(
                  "Serial number: %s, Version: %d, Structure version: %d, Max channels: %d",
                  HexUtil.toHex(info.getSerialNumber()),
                  info.getCsmVersion(),
                  info.getStructureVersion(),
                  info.getChannelsTotal());
          logger.info(csmInfo);
        } else {
          throw new IllegalStateException(
              "The HSM library returned null while retrieving CsmInfo.");
        }
      } catch (CsmException e) {
        throw new IllegalStateException(
            String.format("An error occurred while getting CsmInfos: %s", e.getMessage()), e);
      }

      // collect the key groups available in this CSM and store it in a map with the associated CSMs
      collectKeyGroups(csm);
    }
  }

  /**
   * Tries to free the HSM library, catching and logging the possible errors.
   *
   * @param csmSystem the csm system handle
   */
  private void tryFree(CsmSystem csmSystem) {
    try {
      if (logger.isDebugEnabled()) {
        logger.debug("Freeing the HSM...");
      }
      csmSystem.Free();
    } catch (CsmException ex) {
      logger.error(
          String.format(
              "HSM Error: Could not free: result=%02X (%s)", ex.getCode(), ex.getMessage()));
    }
  }

  /**
   * Parses all the keys of the provided Csm.
   *
   * <p>Update the keyGroupsCsmMap to list which Csm has which keyReaderGroupReference.
   *
   * <p>A key group reference may be available in several CSM. That's why a list of CSM is
   * associated with each key group reference in the keyGroupsCsmMap
   *
   * <p>Also print the keys details in the log flow (debug level).
   *
   * @param csm the Csm
   */
  private void collectKeyGroups(Csm csm) {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(os);

    try {
      if (logger.isDebugEnabled()) {
        logger.debug("Reading the keys of the HSM {}", csm);
      }
      List<CsmKeyInfo> keys = csm.getKeyList();

      if (logger.isDebugEnabled()) {
        CsmKeyInfo.dumpHeader("", ps);
        logger.debug(os.toString().replace("\n", ""));
      }

      for (CsmKeyInfo key : keys) {
        if (logger.isDebugEnabled()) {
          os.reset();
          key.dump("", ps);
          logger.debug(os.toString().replace("\n", ""));
        }
        List<Csm> csmList = keyGroupToCsmsMap.get(key.getKeyGroup());
        if (csmList == null) {
          csmList = new ArrayList<Csm>();
        }
        if (!csmList.contains(csm)) {
          csmList.add(csm);
          keyGroupToCsmsMap.put(key.getKeyGroup(), csmList);
        }
      }

      if (logger.isDebugEnabled()) {
        logger.debug("     Total: {} keys", keys.size());
        logger.debug("     End of HSM key(s)");
      }
    } catch (CsmException ex) {
      logger.error(
          String.format(
              "HSM Error: Could not get csm keys: result=%02X (%s)%n",
              ex.getCode(), ex.getMessage()));
      tryFree(csmSystem);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0.0
   */
  @Override
  public String getName() {
    return LegacyHsmPluginFactoryAdapter.PLUGIN_NAME;
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0.0
   */
  @Override
  public SortedSet<String> getReaderGroupReferences() {
    SortedSet<String> readerGroupReferences = new ConcurrentSkipListSet<String>();
    for (Map.Entry<Integer, List<Csm>> entry : keyGroupToCsmsMap.entrySet()) {
      readerGroupReferences.add(Integer.toString(entry.getKey()));
    }
    return readerGroupReferences;
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0.0
   */
  @Override
  public ReaderSpi allocateReader(String readerGroupReference) throws PluginIOException {

    if (logger.isTraceEnabled()) {
      logger.trace("Reader allocation requested. GROUP_REFERENCE = {}", readerGroupReference);
    }

    LegacyHsmReaderAdapter hsmReader = null;
    int reference;

    // convert the group reference string to an integer: null or malformed strings give the
    // reference 0
    try {
      if (readerGroupReference == null) {
        reference = 0;
      } else {
        reference = Integer.parseInt(readerGroupReference);
      }
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("Bad group reference string: " + readerGroupReference);
    }
    List<Csm> csmList = keyGroupToCsmsMap.get(reference);
    if (csmList.isEmpty()) {
      throw new PluginIOException(
          "The request key group reference "
              + reference
              + " is not available in the configuration.");
    }
    // loop on all available CSMs with the requested key group reference until a channel is opened
    for (Csm csm : csmList) {
      try {
        CsmChannel csmChannel = csm.channelOpen(reference);
        if (csmChannel == null) {
          throw new PluginIOException("No channel available at the moment.");
        }
        hsmReader = new LegacyHsmReaderAdapter(csmChannel);
        if (logger.isTraceEnabled()) {
          logger.trace("Reader {} allocated.", hsmReader.getName());
        }
        break;
      } catch (CsmException e) {
        /* let the caller handle key group related exceptions */
        if (e.getCode() != CsmException.kHsmErrKeyGroup) {
          logger.error(
              "Unable to allocate a new CSM channel for CSM {}. result={} ({})",
              csm,
              e.getCode(),
              e.getMessage());
        }
        throw new PluginIOException("HSM library exception:" + e.getMessage(), e);
      }
    }
    return hsmReader;
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0.0
   */
  @Override
  public void releaseReader(ReaderSpi readerSpi) throws PluginIOException {
    if (logger.isTraceEnabled()) {
      logger.trace("Reader release request READER_NAME = {}.", readerSpi.getName());
    }
    try {
      if (readerSpi != null) {
        ((LegacyHsmReaderAdapter) readerSpi).freeReaderChannel();
        if (logger.isTraceEnabled()) {
          logger.trace("Reader {} released.", readerSpi.getName());
        }
      } else {
        logger.error("Reader not released. reader object is null.");
      }
    } catch (ReaderIOException e) {
      throw new PluginIOException("A reader error occurred", e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0.0
   */
  @Override
  public void onUnregister() {
    // NOP
  }
}
