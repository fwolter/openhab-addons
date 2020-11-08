/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.victronenergymk3.internal.protocol.Mk3ProtocolL3;
import org.openhab.binding.victronenergymk3.internal.protocol.RamVariable;
import org.openhab.binding.victronenergymk3.internal.protocol.Request;
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
import org.openhab.core.util.HexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link InverterThingHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * The Mk3 USB device drops frames when a request is sent by this binding while another request is processed by the Mk3.
 * A state machine ensures that we never send a request when waiting for a response to a previous request.
 *
 * @author Fabian Wolter - Initial contribution
 */
@NonNullByDefault
public class InverterThingHandler extends BaseThingHandler {
    private static final int READ_RETRY_COUNT = 5;
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
    private AtomicReference<Mk3State> state = new AtomicReference<>(Mk3State.IDLE);
    private AtomicBoolean pollPending = new AtomicBoolean();
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

        if (config.refreshInterval <= 0) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.CONFIGURATION_ERROR,
                    "Refresh interval invalid: " + config.refreshInterval);
            return;
        }

        initRequests.clear();
        initRequests.add(new Request.Address(config.address));
        initRequests.add(new Request.SetWinmonMode());

        for (RamVariable variable : RamVariable.values()) {
            initRequests.add(new Request.VariableInfo(variable));
        }

        readTries = 0;
        currentRequest = initRequests.get(0);
        currentSendDelayMs = 0;
        state.set(Mk3State.IDLE);

        updateStatus(ThingStatus.UNKNOWN);

        // byte[] frameL3 = concat(Mk3ProtocolL3.createWriteRamVarRequest(RamVariable.GRID_POWER_EN),
        // Mk3ProtocolL3.createWriteDataRequest((short) 0));
        //
        // sendBuffer(frameL3);
        // try {
        // Thread.sleep(500);
        // } catch (InterruptedException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }

        sendCurrentRequest();

        // sender = scheduler.scheduleAtFixedRate(() -> {
        // System.out.print((System.nanoTime() - t) / 1e6 + "ms");
        // byte[] frameL3 = concat(Mk3ProtocolL3.createWriteRamVarRequest(RamVariable.GRID_POWER_SETPOINT),
        // Mk3ProtocolL3.createWriteDataRequest((short) -3000));
        //
        // if (state.compareAndSet(Mk3State.IDLE, Mk3State.WRITING)) {
        // sendBuffer(frameL3, true);
        // } else {
        // if (sendingBuffer.isPresent()) {
        // logger.debug("Command dropped: Buffer overflow");
        // }
        // sendingBuffer = Optional.of(frameL3);
        // }
        //
        // t = System.nanoTime();
        // System.out.println(" FINISHED");
        // }, 0, 3000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
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

                    byte[] frameL3 = concat(Mk3ProtocolL3.createWriteRamVarRequest(ramVariable),
                            Mk3ProtocolL3.createWriteDataRequest(quantity.shortValue()));

                    if (state.compareAndSet(Mk3State.IDLE, Mk3State.WRITING)) {
                        System.out.println(HexUtils.bytesToHex(frameL3));
                        sendBuffer(frameL3, false);
                    } else {
                        if (sendingBuffer.isPresent()) {
                            logger.debug("Command dropped: {}: {}: Buffer overflow", channelUID, command);
                        }
                        sendingBuffer = Optional.of(frameL3);
                    }
                } else {
                    logger.warn("Incompatible command type: {}: {}", command.getClass().getSimpleName(), command);
                }
            } catch (IllegalArgumentException e) {
                logger.warn("Unknown RAM variable: {}", channelUID.getIdWithoutGroup());
            }
        } else {
            logger.warn("Failed to handle command: {}: {}", channelUID, command);
        }
    }

    public void processMk3FrameL3(byte[] frameL3) {
        ByteBuffer data = ByteBuffer.wrap(frameL3).order(ByteOrder.LITTLE_ENDIAN);

        try {
            switch (Byte.toUnsignedInt(data.get(0))) {
                case Mk3ProtocolL3.ADDRESS_CMD:
                case Mk3ProtocolL3.STATE_CMD:
                    responseSuccessfullyReceived();
                    scheduleNextRequest();
                    break;
                case Mk3ProtocolL3.WINMON_X_CMD:
                    System.out.println(HexUtils.bytesToHex(frameL3));
                    responseSuccessfullyReceived();
                    break;
                case Mk3ProtocolL3.WINMON_W_CMD:
                    switch (Byte.toUnsignedInt(data.get(1))) {
                        case Mk3ProtocolL3.RAM_VARIABLE_INFO_RESPONSE:
                            if (frameL3.length < 6) {
                                throw new Mk3Exception("RAM variable info response length:" + frameL3.length);
                            }

                            if (currentRequest instanceof Request.VariableInfo) {
                                RamVariable variable = ((Request.VariableInfo) currentRequest).getVariable();

                                scalingFunctions.put(variable, Mk3ProtocolL3.createRamVarScalingFunction(variable,
                                        data.getShort(2), data.getShort(5)));

                                responseSuccessfullyReceived();
                                scheduleNextRequest();
                            }
                            break;
                        case Mk3ProtocolL3.RAM_VARIABLE_VALUE_RESPONSE_VALID:
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

                                responseSuccessfullyReceived();
                                scheduleNextRequest();
                            }
                            break;
                        case Mk3ProtocolL3.RAM_VARIABLE_VALUE_RESPONSE_INVALID:
                            responseSuccessfullyReceived();
                            scheduleNextRequest();
                            break;
                        case Mk3ProtocolL3.DEVICE_STATE_RESPONSE:
                            if (frameL3.length < 4) {
                                throw new Mk3Exception("Device state response length:" + frameL3.length);
                            }

                            if (currentRequest instanceof Request.DeviceState) {
                                updateState(VictronEnergyMk3BindingConstants.CHANNEL_DEVICE_STATE, new StringType(
                                        Mk3ProtocolL3.convertDeviceState(data.get(2), data.get(3)).toString()));

                                responseSuccessfullyReceived();
                                scheduleNextRequest();
                            }
                            break;
                        case Mk3ProtocolL3.RAM_VARIABLE_WRITE_SUCCESSFUL_RESPONSE:
                            responseSuccessfullyReceived();
                            break;
                    }
                    break;
            }
        } catch (Mk3Exception e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Protocol error: " + e.getMessage());
        }
    }

    private void scheduleNextRequest() {
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
            pollPending.set(false);

            nextRequest = scheduler.schedule(this::sendCurrentRequest, currentSendDelayMs, TimeUnit.MILLISECONDS);
        }
    }

    private void sendBuffer(byte[] frameL2, boolean startAnswerTimeout) {
        if (getThing().isEnabled()) { // prevent race condition
            getMk3BridgeHandler().sendBuffer(frameL2);

            if (startAnswerTimeout) {
                receiveTimeout = scheduler.schedule(this::receiveTimeout, Mk3ProtocolL3.RECEIVE_TIMEOUT_MS,
                        TimeUnit.MILLISECONDS);
            }
        }
    }

    private void receiveTimeout() {
        if (state.compareAndSet(Mk3State.READING, Mk3State.IDLE)) {
            if (readTries++ > READ_RETRY_COUNT) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Request failed: " + currentRequest);

                getMk3BridgeHandler().scheduleReconnect();
            } else {
                scheduler.execute(this::sendCurrentRequest);
            }
        } else if (state.compareAndSet(Mk3State.WRITING, Mk3State.IDLE)) {
            logger.debug("Command dropped: Device didn't respond after {} tries: {}", readTries + 1, currentRequest);
            sendingBuffer = Optional.empty();
            scheduleNextRequest();
        }
    }

    private void sendCurrentRequest() {
        if (state.compareAndSet(Mk3State.IDLE, Mk3State.READING)) {
            sendBuffer(currentRequest.getFrame(), true);
        } else {
            pollPending.set(true);
        }
    }

    private void responseSuccessfullyReceived() {
        state.set(Mk3State.IDLE);

        if (getThing().getStatus() != ThingStatus.ONLINE) {
            updateStatus(ThingStatus.ONLINE);
        }

        Future<?> localReceiveTimeout = receiveTimeout;
        if (localReceiveTimeout != null) {
            localReceiveTimeout.cancel(true);
        }

        sendingBuffer.ifPresent(buffer -> {
            if (state.compareAndSet(Mk3State.IDLE, Mk3State.WRITING)) {
                sendBuffer(buffer, true);
                sendingBuffer = Optional.empty();
            }
        });

        if (pollPending.get() && state.compareAndSet(Mk3State.IDLE, Mk3State.READING)) {
            pollPending.set(false);
            sendBuffer(currentRequest.getFrame(), true);
        }
    }

    private Mk3BridgeHandler getMk3BridgeHandler() {
        Bridge localBridge = getBridge();
        if (localBridge != null) {
            BridgeHandler handler = localBridge.getHandler();
            if (handler instanceof Mk3BridgeHandler) {
                return ((Mk3BridgeHandler) handler);
            }
        }
        throw new IllegalStateException();
    }

    private void updateRamValue(RamVariable ramVariable, State state) {
        updateState(VictronEnergyMk3BindingConstants.RAM_GROUP + "#" + ramVariable.toString().toLowerCase(), state);
    }

    @Override
    public void dispose() {
        if (sender != null) {
            sender.cancel(false);
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
