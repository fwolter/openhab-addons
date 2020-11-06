/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.victronenergymk3.internal;

import java.io.IOException;
import java.util.Optional;
import java.util.TooManyListenersException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.victronenergymk3.internal.protocol.BufferedSerialPort;
import org.openhab.binding.victronenergymk3.internal.protocol.Mk3ProtocolL3;
import org.openhab.core.io.transport.serial.PortInUseException;
import org.openhab.core.io.transport.serial.SerialPortIdentifier;
import org.openhab.core.io.transport.serial.SerialPortManager;
import org.openhab.core.io.transport.serial.UnsupportedCommOperationException;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.types.Command;
import org.openhab.core.util.HexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the bridge handler for the Victron Energy MK3 USB device.
 *
 * @author Fabian Wolter - Initial contribution
 */
@NonNullByDefault
public class Mk3BridgeHandler extends BaseBridgeHandler {
    private static final int GRACE_PERIOD_BETWEEN_RECONNECT_SEC = 3;
    private final Logger logger = LoggerFactory.getLogger(Mk3BridgeHandler.class);
    private SerialPortManager serialPortManager;
    private Mk3Configuration config = new Mk3Configuration();
    private @Nullable BufferedSerialPort serialPort;
    private @Nullable ScheduledFuture<?> reconnector;

    public Mk3BridgeHandler(Bridge bridge, SerialPortManager serialPortManager) {
        super(bridge);
        this.serialPortManager = serialPortManager;
    }

    @Override
    public void initialize() {
        config = getConfigAs(Mk3Configuration.class);

        try {
            SerialPortIdentifier portId = serialPortManager.getIdentifier(config.serialPort);
            if (portId == null) {
                throw new Mk3Exception("Serial port not found: " + config.serialPort);
            }

            BufferedSerialPort localSerialPort = serialPort = new BufferedSerialPort(portId,
                    getThing().getUID().toString(), this::processMk3FrameL2);

            localSerialPort.startWorking();
            updateStatus(ThingStatus.UNKNOWN); // this is necessary to let the framework enable all child Things (only
                                               // if the serial port has been opened successfully)
            updateStatus(ThingStatus.ONLINE);
        } catch (PortInUseException | UnsupportedCommOperationException | TooManyListenersException | Mk3Exception e) {
            scheduleReconnect("Failed to initialize serial port: " + config.serialPort + ": "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // the bridge has no Channels
    }

    private boolean processMk3FrameL2(byte[] frameL2) {
        try {
            Optional<byte[]> optionalFrameL3 = Mk3ProtocolL3.readFrame(frameL2);

            return optionalFrameL3.map(frameL3 -> {
                if (logger.isTraceEnabled()) {
                    logger.trace("Received: {}", HexUtils.bytesToHex(frameL2));
                }

                createChildThingsHandlerStream().forEach(thing -> thing.processMk3FrameL3(frameL3));

                return true;
            }).orElse(false);
        } catch (Mk3Exception e) {
            logger.debug("Failed to process frame: {}: {}", HexUtils.bytesToHex(frameL2), e.getMessage());

            resetCurrentFrame();

            return false;
        }
    }

    public void sendBuffer(byte[] buffer) {
        BufferedSerialPort port = serialPort;
        if (port == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR,
                    "Serial port not available: " + config.serialPort);
            return;
        }

        try {
            port.write(buffer);

            if (logger.isTraceEnabled()) {
                logger.trace("Sending: {}", HexUtils.bytesToHex(buffer));
            }
        } catch (IOException e) {
            scheduleReconnect("IO Error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private void resetCurrentFrame() {
        BufferedSerialPort localSerialPort = serialPort;
        if (localSerialPort != null) {
            localSerialPort.resetCurrentFrame();
        }
    }

    public void scheduleReconnect(String reason) {
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, reason);

        createChildThingsHandlerStream().forEach(InverterThingHandler::dispose);
        dispose();

        reconnector = scheduler.schedule(() -> {
            if (getThing().isEnabled()) {
                logger.debug("Reconnecting ...");

                initialize();

                if (getThing().getStatus() == ThingStatus.ONLINE) {
                    createChildThingsHandlerStream().forEach(InverterThingHandler::initialize);
                }
            }
        }, GRACE_PERIOD_BETWEEN_RECONNECT_SEC, TimeUnit.SECONDS);
    }

    @Override
    public void dispose() {
        ScheduledFuture<?> localReconnector = reconnector;
        if (localReconnector != null) {
            localReconnector.cancel(false);
        }

        BufferedSerialPort localSerialPort = serialPort;
        if (localSerialPort != null) {
            localSerialPort.shutdown();
            serialPort = null;
        }
    }

    @NonNullByDefault({})
    private Stream<InverterThingHandler> createChildThingsHandlerStream() {
        return getThing().getThings().stream().filter(t -> t.isEnabled()).map(t -> t.getHandler())
                .filter(h -> h instanceof InverterThingHandler).map(h -> (InverterThingHandler) h);
    }
}
