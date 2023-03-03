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

import com.spirtech.csm.CsmChannel;
import com.spirtech.csm.CsmException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.eclipse.keyple.core.plugin.ReaderIOException;
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi;
import org.eclipse.keyple.core.util.HexUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Legacy HSM reader extension adapter.
 *
 * @since 1.0.0
 */
final class LegacyHsmReaderAdapter implements LegacyHsmReader, ReaderSpi {

  private static final Logger logger = LoggerFactory.getLogger(LegacyHsmReaderAdapter.class);

  private final String name;

  /** CsmChannel object from the Spirtech library */
  private final CsmChannel csmChannel;

  private boolean isPhysicalChannelOpen;

  /** Virtual ATR */
  private final byte[] atr = {
    /* ISO header */
    (byte) 0x3B,
    (byte) 0x3F,
    (byte) 0x96,
    (byte) 0x00,
    /* historical bytes */
    (byte) 0x80,
    (byte) 0x5A,
    /* platform [6] */
    (byte) 0x00,
    /* Application type: SAM [7] */
    (byte) 0x80,
    /* Application subtype: C1 [8] */
    (byte) 0xC1,
    /* Software issuer Spirtech [9] */
    (byte) 0x08,
    /* Software version [10] */
    (byte) 0x00,
    /* Software revision [11] */
    (byte) 0x00,
    /* Serial number [12-15] */
    (byte) 0x00,
    (byte) 0x00,
    (byte) 0x00,
    (byte) 0x00,
    /* ATR status */
    (byte) 0x82,
    (byte) 0x90,
    (byte) 0x00
  };

  /**
   * 
   * This constructor should only be called by allocateReader from {@link LegacyHsmPluginAdapter}
   *
   * @param csmChannel the {@link CsmChannel}
   * @since 1.0.0
   */
  LegacyHsmReaderAdapter(CsmChannel csmChannel) throws CsmException {
    this.name =
        csmChannel.getCsm().toString()
            + " Ch. #"
            + csmChannel.getId()
            + " "
            + System.currentTimeMillis();
    this.csmChannel = csmChannel;
    this.isPhysicalChannelOpen = true;
    // fill virtual ATR with CSM infos
    // version
    this.atr[10] = (byte) csmChannel.getCsm().getInfos().fCsmVersion;
    // serial number
    this.atr[12] = (byte) (csmChannel.getCsm().getInfos().fSerialNumber >> 24);
    this.atr[13] = (byte) (csmChannel.getCsm().getInfos().fSerialNumber >> 16);
    this.atr[14] = (byte) (csmChannel.getCsm().getInfos().fSerialNumber >> 8);
    this.atr[15] = (byte) csmChannel.getCsm().getInfos().fSerialNumber;
    if (logger.isTraceEnabled()) {
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream(os);
      csmChannel.getInfo().dump("", ps);
      if (logger.isTraceEnabled()) {
        logger.trace(
            "Creation of a HSM SAM reader. CSMCHANNEL = {}, VIRTUAL ATR = {}",
            os.toString().replaceAll("\\n|[\\s]{1,20}", " "),
            HexUtil.toHex(atr));
      }
    }
  }

  /**
   * 
   * Release the current CsmChannel.
   *
   * <p>Since the {@link CsmChannel} is released, this reader is unusable after this method has been
   * called.
   *
   * @throws ReaderIOException if an {@link CsmException} occurs
   * @since 1.0.0
   */
  void freeReaderChannel() throws ReaderIOException {
    if (logger.isTraceEnabled()) {
      logger.trace("Free reader channel request.");
    }
    if (csmChannel != null) {
      try {
        csmChannel.close();
        isPhysicalChannelOpen = false;
      } catch (CsmException e) {
        throw new ReaderIOException(e.getMessage(), e);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0.0
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Do not nothing since the physical channel opening is implicit through the CsmChannel
   * allocation process.
   *
   * @since 1.0.0
   */
  @Override
  public void openPhysicalChannel() {
    if (logger.isTraceEnabled()) {
      logger.trace("Open physical channel requested.");
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>Do not nothing since the physical channel closing is implicit through the CsmChannel
   * de-allocation process.
   *
   * @since 1.0.0
   */
  @Override
  public void closePhysicalChannel() {
    if (logger.isTraceEnabled()) {
      logger.trace("Close physical channel requested.");
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0.0
   */
  @Override
  public boolean isPhysicalChannelOpen() {
    return isPhysicalChannelOpen;
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0.0
   */
  @Override
  public boolean checkCardPresence() {
    if (logger.isTraceEnabled()) {
      logger.trace("Check card presence requested.");
    }
    return true;
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0.0
   */
  @Override
  public String getPowerOnData() {
    String powerOnData = HexUtil.toHex(atr);
    if (logger.isTraceEnabled()) {
      logger.trace("Get power on requested. ATR = {}", powerOnData);
    }
    return powerOnData;
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0.0
   */
  @Override
  public byte[] transmitApdu(byte[] apduIn) throws ReaderIOException {
    if (logger.isTraceEnabled()) {
      logger.trace("APDU_REQ = {}", HexUtil.toHex(apduIn));
    }
    byte[] apduOut;
    try {
      apduOut = csmChannel.apduExchange(apduIn);
      if (logger.isTraceEnabled()) {
        logger.trace("APDU_RSP = {}", HexUtil.toHex(apduOut));
      }
    } catch (CsmException e) {
      throw new ReaderIOException(
          String.format(
              "CsmException raised while doing apduExchange. result=%02X (%s)",
              e.getCode(), e.getMessage()));
    }
    return apduOut == null ? null : apduOut.clone();
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0.0
   */
  @Override
  public boolean isContactless() {
    return false;
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
