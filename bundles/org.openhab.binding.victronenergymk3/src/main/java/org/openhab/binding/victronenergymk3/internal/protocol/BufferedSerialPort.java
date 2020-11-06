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
package org.openhab.binding.victronenergymk3.internal.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TooManyListenersException;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.victronenergymk3.internal.Mk3Exception;
import org.openhab.core.io.transport.serial.PortInUseException;
import org.openhab.core.io.transport.serial.SerialPort;
import org.openhab.core.io.transport.serial.SerialPortEvent;
import org.openhab.core.io.transport.serial.SerialPortIdentifier;
import org.openhab.core.io.transport.serial.UnsupportedCommOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates an abstraction layer of the serial port by introducing a concurrent receiving queue.
 *
 * @author Fabian Wolter - Initial contribution
 */
@NonNullByDefault
public class BufferedSerialPort {
    private static final int MINIMUM_FRAME_LENGTH = 3; // includes one payload byte
    private final Logger logger = LoggerFactory.getLogger(BufferedSerialPort.class);
    private final SerialPortIdentifier portId;
    private String owner;
    private @Nullable InputStream inputStream;
    private @Nullable OutputStream outputStream;
    private ByteArrayOutputStream currentFrame = new ByteArrayOutputStream();
    private @Nullable SerialPort serialPort;
    private Function<byte[], Boolean> currentFrameUpdate;
    private Object readLock = new Object();
    private Object writeLock = new Object();

    public BufferedSerialPort(SerialPortIdentifier portId, String owner, Function<byte[], Boolean> currentFrameUpdate) {
        this.portId = portId;
        this.owner = owner;
        this.currentFrameUpdate = currentFrameUpdate;
    }

    public void startWorking()
            throws PortInUseException, UnsupportedCommOperationException, TooManyListenersException, Mk3Exception {
        SerialPort localSerialPort = serialPort = portId.open(owner, 2000);
        localSerialPort.setSerialPortParams(2400, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

        try {
            localSerialPort.enableReceiveThreshold(1);
            localSerialPort.disableReceiveTimeout();
        } catch (UnsupportedCommOperationException e) {
            logger.warn("Serial port does not support TODO"); // TODO
        }

        try {
            outputStream = localSerialPort.getOutputStream();
            inputStream = localSerialPort.getInputStream();

            if (inputStream == null || outputStream == null) {
                throw new IOException("Could not create input or output stream");
            }
        } catch (IOException e) {
            throw new Mk3Exception(e);
        }

        localSerialPort.notifyOnDataAvailable(true);
        localSerialPort.addEventListener(this::onEvent);
    }

    private void onEvent(SerialPortEvent e) {
        synchronized (readLock) {
            InputStream localInputStream = inputStream;
            if (e.getEventType() == SerialPortEvent.DATA_AVAILABLE && localInputStream != null) {
                try {
                    while (localInputStream.available() > 0) {
                        int data = localInputStream.read();
                        if (data == -1) {
                            break;
                        }
                        currentFrame.write(data);
                    }

                    if (currentFrame.size() >= MINIMUM_FRAME_LENGTH) {
                        // byte[] data = new byte[currentFrame.toByteArray().length];
                        // System.arraycopy(currentFrame.toByteArray(), 0, data, 0, data.length);

                        if (currentFrameUpdate.apply(currentFrame.toByteArray())) {
                            currentFrame.reset();
                        }
                    }
                } catch (IOException ex) {
                    logger.warn("Error processing serial port event: {}: {}", ex.getClass().getSimpleName(),
                            ex.getMessage());
                }
            }
        }
    }

    public void write(byte[] data) throws IOException {
        synchronized (writeLock) {
            OutputStream localOutputStream = outputStream;
            if (localOutputStream != null) {
                localOutputStream.write(data);
            }
        }
    }

    public void shutdown() {
        SerialPort localSerialPort = serialPort;
        if (localSerialPort != null) {
            localSerialPort.close();
        }
    }

    public void resetCurrentFrame() {
        synchronized (readLock) {
            currentFrame.reset();
        }
    }
}
