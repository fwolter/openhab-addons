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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.victronenergymk3.internal.protocol.Mk3ProtocolL3;
import org.openhab.binding.victronenergymk3.internal.protocol.RamVariable;
import org.openhab.binding.victronenergymk3.internal.protocol.Request;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.BridgeHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link InverterThingHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * The Mk3 USB device drops frames when a request is sent by this binding while another request is processed by the Mk3,
 * although the protocol would allow this.
 * A state machine ensures that we never send a request when waiting for a response to a previous request.
 *
 * @author Fabian Wolter - Initial contribution
 */
@NonNullByDefault
public class InverterThingHandler extends BaseThingHandler {
    private static final int READ_RETRY_COUNT = 3;
    private static final List<Request> OPERATING_REQUESTS = new ArrayList<>();
    private final List<Request> initRequests = new ArrayList<>();
    private final Logger logger = LoggerFactory.getLogger(InverterThingHandler.class);
    private InverterConfiguration config = new InverterConfiguration();
    private Map<RamVariable, @Nullable Function<Short, ? extends State>> scalingFunctions = new HashMap<>();
    private Request currentRequest;
    private int currentSendDelayMs;
    private @Nullable ScheduledFuture<?> nextRequest;
    private @Nullable Future<?> receiveTimeout;
    private @Nullable ScheduledFuture<?> sender;
    private int readTries;
    private Mk3State state = Mk3State.IDLE;
    private boolean pollPending;
    private volatile Optional<byte[]> sendingBuffer = Optional.empty();

    static {
        OPERATING_REQUESTS.add(new Request.DeviceState());

        for (RamVariable variable : RamVariable.values()) {
            OPERATING_REQUESTS.add(new Request.VariableValue(variable));
        }
    }

    enum Mk3State {
        IDLE,
        READING,
        WRITING
    }

    public InverterThingHandler(Thing thing) {
        super(thing);

        currentRequest = OPERATING_REQUESTS.get(0);
    }

    @Override
    public void initialize() {
        config = getConfigAs(InverterConfiguration.class);

        synchronized (this) {
            initRequests.clear();
            initRequests.add(new Request.Address(config.address));
            initRequests.add(new Request.SetWinmonMode());

            for (RamVariable variable : RamVariable.values()) {
                initRequests.add(new Request.VariableInfo(variable));
            }

            readTries = 0;
            currentRequest = initRequests.get(0);
            currentSendDelayMs = 0;
            pollPending = false;
            state = Mk3State.IDLE;

            updateStatus(ThingStatus.UNKNOWN);

            sendReadRequestIfIdleOtherwiseSchedule();

            // scheduler.scheduleWithFixedDelay(() -> {
            // try {
            // handleDecimalCommand(RamVariable.GRID_POWER_SETPOINT, 5);
            // } catch (Mk3Exception e) {
            // logger.warn(e.getMessage());
            // }
            // }, 100, 100, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (getThing().getStatus() != ThingStatus.ONLINE) {
            return;
        }

        String groupId = channelUID.getGroupId();

        if (command instanceof RefreshType) {
            currentSendDelayMs = 0;
            if (!initRequests.contains(currentRequest)) {
                currentRequest = OPERATING_REQUESTS.get(0);
            }
        } else if (groupId != null && groupId.equals(VictronEnergyMk3BindingConstants.RAM_GROUP)) {
            try {
                RamVariable ramVariable = RamVariable.valueOf(channelUID.getIdWithoutGroup().toUpperCase());

                if (command instanceof QuantityType<?>) {
                    QuantityType<?> quantity = (QuantityType<?>) command;
                    Unit<?> localUnit = ramVariable.getUnit();

                    if (localUnit == null) {
                        logger.warn("No unit defined for RAM variable: {}", ramVariable);
                        return;
                    }

                    quantity = quantity.toUnit(localUnit);
                    if (quantity == null) {
                        logger.warn("Could not convert QuantityType {} for RAM variable: {}", command, ramVariable);
                        return;
                    }

                    handleDecimalCommand(ramVariable, quantity.doubleValue());
                } else if (command instanceof DecimalType) {
                    handleDecimalCommand(ramVariable, ((DecimalType) command).doubleValue());
                } else {
                    logger.warn("Incompatible command type: {}: {}", command.getClass().getSimpleName(), command);
                }
            } catch (IllegalArgumentException e) {
                logger.warn("Unknown RAM variable: {}", channelUID.getIdWithoutGroup());
            } catch (Mk3Exception e) {
                logger.debug("{}: {}: {}", e.getMessage(), channelUID, command);
            }
        } else {
            logger.warn("Failed to handle command: {}: {}", channelUID, command);
        }
    }

    private void handleDecimalCommand(RamVariable ramVariable, double value) throws Mk3Exception {
        double convertedValue = ramVariable.getConverter().apply(value);

        byte[] frameL3 = concat(Mk3ProtocolL3.createWriteRamVarRequest(ramVariable),
                Mk3ProtocolL3.createWriteDataRequest((short) convertedValue));

        synchronized (this) {
            if (state == Mk3State.IDLE) {
                sendBuffer(frameL3);
                state = Mk3State.WRITING;
            } else {
                if (sendingBuffer.isPresent()) {
                    throw new Mk3Exception("Command dropped");
                }
                sendingBuffer = Optional.of(frameL3);
            }
        }
    }

    public synchronized void processMk3FrameL3(byte[] frameL3) {
        ByteBuffer data = ByteBuffer.wrap(frameL3).order(ByteOrder.LITTLE_ENDIAN);

        try {
            switch (Byte.toUnsignedInt(data.get(0))) {
                case Mk3ProtocolL3.ADDRESS_CMD:
                case Mk3ProtocolL3.STATE_CMD:
                    if (state == Mk3State.READING) {
                        prepareNextTransaction();
                        scheduleNextReadRequest();
                        setOnline();
                    }
                    break;
                case Mk3ProtocolL3.WINMON_X_CMD:
                    if (state == Mk3State.WRITING) {
                        prepareNextTransaction();
                        setOnline();
                    }
                    break;
                case Mk3ProtocolL3.WINMON_W_CMD:
                    switch (Byte.toUnsignedInt(data.get(1))) {
                        case Mk3ProtocolL3.RAM_VARIABLE_INFO_RESPONSE:
                            if (state == Mk3State.READING) {
                                if (frameL3.length < 6) {
                                    throw new Mk3Exception("RAM variable info response length:" + frameL3.length);
                                }

                                if (currentRequest instanceof Request.VariableInfo) {
                                    RamVariable variable = ((Request.VariableInfo) currentRequest).getVariable();

                                    scalingFunctions.put(variable, Mk3ProtocolL3.createRamVarScalingFunction(variable,
                                            data.getShort(2), data.getShort(5)));

                                    prepareNextTransaction();
                                    scheduleNextReadRequest();
                                    setOnline();
                                } else {
                                    logger.warn("Expected {}, but was variable info", currentRequest);
                                }
                            }
                            break;
                        case Mk3ProtocolL3.RAM_VARIABLE_VALUE_RESPONSE_VALID:
                            if (state == Mk3State.READING) {
                                if (frameL3.length < 4) {
                                    throw new Mk3Exception("RAM variable value response length:" + frameL3.length);
                                }

                                if (currentRequest instanceof Request.VariableValue) {
                                    RamVariable variable = ((Request.VariableValue) currentRequest).getVariable();

                                    Function<Short, ? extends State> scaleFunction = scalingFunctions.get(variable);
                                    if (scaleFunction != null) {
                                        updateRamValue(variable, scaleFunction.apply(data.getShort(2)));
                                    } else {
                                        updateRamValue(variable, UnDefType.UNDEF);
                                    }

                                    prepareNextTransaction();
                                    scheduleNextReadRequest();
                                    setOnline();
                                } else {
                                    logger.warn("Expected {}, but was variable value", currentRequest);
                                }
                            }
                            break;
                        case Mk3ProtocolL3.RAM_VARIABLE_VALUE_RESPONSE_INVALID:
                            if (state == Mk3State.READING) {
                                prepareNextTransaction();
                                scheduleNextReadRequest();
                                setOnline();
                            }
                            break;
                        case Mk3ProtocolL3.DEVICE_STATE_RESPONSE:
                            if (state == Mk3State.READING) {
                                if (frameL3.length < 4) {
                                    throw new Mk3Exception("Device state response length:" + frameL3.length);
                                }

                                if (currentRequest instanceof Request.DeviceState) {
                                    updateState(VictronEnergyMk3BindingConstants.CHANNEL_DEVICE_STATE, new StringType(
                                            Mk3ProtocolL3.convertDeviceState(data.get(2), data.get(3)).toString()));

                                    prepareNextTransaction();
                                    scheduleNextReadRequest();
                                    setOnline();
                                } else {
                                    logger.warn("Expected {}, but was device state", currentRequest);
                                }
                            }
                            break;
                        case Mk3ProtocolL3.RAM_VARIABLE_WRITE_SUCCESSFUL_RESPONSE:
                            if (state == Mk3State.WRITING) {
                                prepareNextTransaction();
                                setOnline();
                            }
                            break;
                    }
                    break;
            }
        } catch (Mk3Exception e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Protocol error: " + e.getMessage());
        }
    }

    private void setOnline() {
        if (getThing().getStatus() != ThingStatus.ONLINE) {
            updateStatus(ThingStatus.ONLINE);
        }
    }

    private void scheduleNextReadRequest() {
        ScheduledFuture<?> localNextRequest = nextRequest;
        if (localNextRequest == null || localNextRequest.isDone()) {
            if (initRequests.contains(currentRequest)) {
                int index = initRequests.indexOf(currentRequest);
                if (index >= initRequests.size() - 1) {
                    // initialization requests have finished, continue with operating requests
                    currentRequest = OPERATING_REQUESTS.get(0);
                } else {
                    currentRequest = initRequests.get(index + 1);
                }
            } else {
                int index = OPERATING_REQUESTS.indexOf(currentRequest);

                if (index < 0) {
                    throw new IllegalStateException("Invalid current request");
                }

                index++;

                if (index >= OPERATING_REQUESTS.size()) {
                    currentRequest = OPERATING_REQUESTS.get(0);
                    currentSendDelayMs = config.refreshInterval / OPERATING_REQUESTS.size();
                } else {
                    currentRequest = OPERATING_REQUESTS.get(index);
                }
            }

            readTries = 0;
            pollPending = false;

            nextRequest = scheduler.schedule(this::sendReadRequestIfIdleOtherwiseSchedule, currentSendDelayMs,
                    TimeUnit.MILLISECONDS);
        }
    }

    private void sendBuffer(byte[] frameL2) {
        getMk3BridgeHandler().sendBuffer(frameL2);

        receiveTimeout = scheduler.schedule(this::receiveTimeout, Mk3ProtocolL3.RECEIVE_TIMEOUT_MS,
                TimeUnit.MILLISECONDS);
    }

    private synchronized void receiveTimeout() {
        Future<?> localReceiveTimeout = receiveTimeout;

        if (localReceiveTimeout != null && !localReceiveTimeout.isCancelled()) {
            if (state == Mk3State.READING) {
                logger.debug("Read request timed out after {} tries: {}", currentRequest, (readTries + 1));

                if (readTries++ > READ_RETRY_COUNT) {
                    getMk3BridgeHandler().scheduleReconnect("Read request failed finally: " + currentRequest);
                } else {
                    state = Mk3State.IDLE;
                    sendReadRequest();
                }
            } else if (state == Mk3State.WRITING) {
                logger.debug("Write request timed out");

                sendingBuffer = Optional.empty();

                state = Mk3State.IDLE;

                sendReadRequest();
            } else {
                logger.error("Unexpected state: {}", state);
            }
        }
    }

    private synchronized void sendReadRequestIfIdleOtherwiseSchedule() {
        if (state == Mk3State.IDLE) {
            sendReadRequest();
        } else {
            pollPending = true;
        }
    }

    private void prepareNextTransaction() {
        Future<?> localReceiveTimeout = receiveTimeout;
        if (localReceiveTimeout != null) {
            localReceiveTimeout.cancel(true);
        }

        // avoid starvation of either read requests or write requests
        Mk3State priorityState = null;
        if (pollPending && sendingBuffer.isPresent()) {
            // both, read and write requests are pending.
            // if the last request was reading, the next will be writing and vice versa.

            if (state == Mk3State.READING) {
                priorityState = Mk3State.WRITING;
            } else if (state == Mk3State.WRITING) {
                priorityState = Mk3State.READING;
            }
        }

        // the if-expressions cannot be combined with OR, because the order of if statements is important.
        if (priorityState == Mk3State.READING) {
            sendReadRequest();
        } else if (priorityState == Mk3State.WRITING) {
            sendWriteRequest();
        } else if (pollPending) {
            sendReadRequest();
        } else if (sendingBuffer.isPresent()) {
            sendWriteRequest();
        } else {
            state = Mk3State.IDLE;
        }
    }

    private void sendReadRequest() {
        logger.trace("Sending read request: {}", currentRequest);
        sendBuffer(currentRequest.getFrame());
        pollPending = false;
        state = Mk3State.READING;
    }

    private void sendWriteRequest() {
        logger.trace("Write request");
        sendBuffer(sendingBuffer.get());
        sendingBuffer = Optional.empty();
        state = Mk3State.WRITING;
    }

    private Mk3BridgeHandler getMk3BridgeHandler() {
        Bridge localBridge = getBridge();
        if (localBridge != null) {
            BridgeHandler handler = localBridge.getHandler();
            if (handler instanceof Mk3BridgeHandler) {
                return ((Mk3BridgeHandler) handler);
            }
        }
        throw new IllegalStateException("Could not retrieve Bridge or Bridge handler");
    }

    private void updateRamValue(RamVariable ramVariable, State state) {
        updateState(VictronEnergyMk3BindingConstants.RAM_GROUP + "#" + ramVariable.toString().toLowerCase(), state);
    }

    @Override
    public void dispose() {
        Future<?> localSender = sender;
        if (localSender != null) {
            localSender.cancel(false);
        }

        Future<?> localReceiveTimeout = receiveTimeout;
        if (localReceiveTimeout != null) {
            localReceiveTimeout.cancel(false);
        }

        ScheduledFuture<?> localNextRequest = nextRequest;
        if (localNextRequest != null) {
            localNextRequest.cancel(false);
        }
    }

    public static byte[] concat(byte[] first, byte[] second) {
        byte[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
