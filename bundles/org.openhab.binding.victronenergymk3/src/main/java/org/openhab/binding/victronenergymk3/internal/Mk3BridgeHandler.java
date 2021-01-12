/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
 * TODO
 *
 * @author Fabian Wolter - Initial contribution
 */
@NonNullByDefault
public class Mk3BridgeHandler extends BaseBridgeHandler {
    private static final int GRACE_PERIOD_BETWEEN_RECONNECT = 3;
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

        updateStatus(ThingStatus.UNKNOWN);

        try {
            SerialPortIdentifier portId = serialPortManager.getIdentifier(config.serialPort);
            if (portId == null) {
                throw new Mk3Exception("Port not found");
            }

            BufferedSerialPort localSerialPort = serialPort = new BufferedSerialPort(scheduler, portId,
                    getThing().getUID().toString(), this::processMk3FrameL2);

            localSerialPort.startWorking();
            updateStatus(ThingStatus.ONLINE);
        } catch (PortInUseException | UnsupportedCommOperationException | TooManyListenersException | Mk3Exception e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR,
                    "Failed to initialize serial port: " + config.serialPort + ": " + e.getClass().getSimpleName()
                            + ": " + e.getMessage());
            scheduleReconnect();
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // the bridge has no Channels
    }

    private boolean processMk3FrameL2(byte[] frameL2) {
        try {
            Optional<byte[]> optionalFrameL3 = Mk3ProtocolL3.readFrame(frameL2);

            optionalFrameL3.ifPresent(frameL3 -> {
                if (logger.isTraceEnabled()) {
                    logger.trace("Received: {}", HexUtils.bytesToHex(frameL2));
                }

                createChildThingsHandlerStream().forEach(thing -> thing.processMk3FrameL3(frameL3));
            });

            return optionalFrameL3.isPresent();
        } catch (Mk3Exception e) {
            logger.trace("Failed to process frame: {}", e.getMessage());

            resetCurrentFrame();

            return false;
        }
    }

    public void sendBuffer(byte[] buffer) {
        if (getThing().getStatus() == ThingStatus.OFFLINE) {
            return;
        }

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
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR,
                    "IO Error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            scheduleReconnect();
        }
    }

    private void resetCurrentFrame() {
        BufferedSerialPort localSerialPort = serialPort;
        if (localSerialPort != null) {
            localSerialPort.resetCurrentFrame();
        }
    }

    public void scheduleReconnect() {
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
        }, GRACE_PERIOD_BETWEEN_RECONNECT, TimeUnit.SECONDS);
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
