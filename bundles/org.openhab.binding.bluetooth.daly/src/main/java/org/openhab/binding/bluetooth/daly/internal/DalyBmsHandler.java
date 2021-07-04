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
package org.openhab.binding.bluetooth.daly.internal;

import static org.openhab.binding.bluetooth.daly.internal.Channels.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.bluetooth.BluetoothCharacteristic;
import org.openhab.binding.bluetooth.ConnectedBluetoothHandler;
import org.openhab.binding.bluetooth.notification.BluetoothScanNotification;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.kaitai.struct.ByteBufferKaitaiStream;

/**
 * Handles commands and messages for Daly BMS.
 *
 * @author Fabian Wolter - Initial contribution
 *
 */
@NonNullByDefault
public class DalyBmsHandler extends ConnectedBluetoothHandler {

    private static final UUID SERVICE_UUID = UUID.fromString("494e5445-4c4c-495f-524f-434b535f4857");
    private static final UUID PROTOCOL_CHAR_UUID = UUID.fromString("494e5445-4c4c-495f-524f-434b535f2011");

    private static final byte[] SCAN_HEADER = { (byte) 0xFF, (byte) 0x88, (byte) 0xEC };

    private final Logger logger = LoggerFactory.getLogger(DalyBmsHandler.class);

    private Future<?> scanJob = CompletableFuture.completedFuture(null);
    private Properties errorMessages = new Properties();;

    public DalyBmsHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (thing.getStatus() == ThingStatus.OFFLINE) {
            // something went wrong in super.initialize() so we shouldn't initialize further here either
            return;
        }

        scanJob = scheduler.scheduleWithFixedDelay(() -> {
            // TODO
            disconnect();
            updateStatus(ThingStatus.ONLINE);
        }, 0, 10, TimeUnit.SECONDS);

        try {
            errorMessages.load(getClass().getResourceAsStream("errorMessages.properties"));
        } catch (IOException e) {
            logger.warn("Could not load error messages", e);
        }
    }

    @Override
    public void dispose() {
        scanJob.cancel(false);
        super.dispose();
    }

    private int scanPacketSize() {
        return 0; // TODO
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        super.handleCommand(channelUID, command);

        if (command == RefreshType.REFRESH) {
            // TODO
        }
    }

    @Override
    public void onScanRecordReceived(BluetoothScanNotification scanNotification) {
        super.onScanRecordReceived(scanNotification);
        byte[] scanData = scanNotification.getData();

    }

    @Override
    public void onCharacteristicUpdate(BluetoothCharacteristic characteristic, byte[] value) {
        super.onCharacteristicUpdate(characteristic, value);

        Daly daly = new Daly(new ByteBufferKaitaiStream(value));
        for (Daly.Frame frame : daly.frame()) {
            Object data = frame.data();

            if (data instanceof Daly.SocVoltageCurrent) {
                Daly.SocVoltageCurrent d = (Daly.SocVoltageCurrent) data;

                update(PRESSURE, d.pressureV());
                update(ACQUISITION, d.acquisitionV());
                update(CURRENT, d.currentA());
                update(STATE_OF_CHARGE, d.socPercent());
            } else if (data instanceof Daly.MinMaxVoltage) {
                Daly.MinMaxVoltage d = (Daly.MinMaxVoltage) data;

                update(MAX_VOLTAGE, d.maxVoltageV());
                update(MAX_VOLTAGE_CELL_NO, d.maxCellNo());
                update(MIN_VOLTAGE, d.minVoltageV());
                update(MIN_VOLTAGE_CELL_NO, d.minCellNo());
            } else if (data instanceof Daly.MinMaxTemperature) {
                Daly.MinMaxTemperature d = (Daly.MinMaxTemperature) data;

                update(MAX_TEMPERATURE, d.maxTemperatureCelsius());
                update(MAX_TEMPERATURE_CELL_NO, d.maxCellNo());
                update(MIN_TEMPERATURE, d.minTemperatureCelsius());
                update(MIN_TEMPERATURE_CELL_NO, d.minCellNo());
            } else if (data instanceof Daly.ChargeDischargeStatus) {
                Daly.ChargeDischargeStatus d = (Daly.ChargeDischargeStatus) data;

                update(CHARGE_DISCHARGE_STATUS, d.chargeDischargeStatusRaw().ordinal());
                update(CHARGING_MOS_TUBE_STATUS, d.chargingMosTubeStatus());
                update(DISCHARGING_MOS_TUBE_STATUS, d.dischargeMosTubeState());
                update(BMS_LIFE, d.bmsLife());
                update(RESIDUAL_CAPACITY, d.residualCapacityAh());
            } else if (data instanceof Daly.StatusInformation) {
                Daly.StatusInformation d = (Daly.StatusInformation) data;

                update(STRING_SIZE, d.batteryString());
                update(TEMPERATURE, d.temperature());
                update(CHARGER_STATUS, d.chargerStatus().ordinal());
                update(LOAD_STATUS, d.loadStatus().ordinal());
                update(DI_STATE, 1, d.di1());
                update(DI_STATE, 2, d.di2());
                update(DI_STATE, 3, d.di3());
                update(DI_STATE, 4, d.di4());
                update(DO_STATE, 1, d.do1());
                update(DO_STATE, 2, d.do2());
                update(DO_STATE, 3, d.do3());
                update(DO_STATE, 4, d.do4());
            } else if (data instanceof Daly.CellVoltage) {
                Daly.CellVoltage d = (Daly.CellVoltage) data;

                int i = 0;
                for (int voltage : d.voltageMv()) {
                    update(CELL_VOLTAGE, d.frameNumber() * 3 + i, voltage);
                    i++;
                }
            } else if (data instanceof Daly.MonomerTemperature) {
                Daly.MonomerTemperature d = (Daly.MonomerTemperature) data;

                int i = 0;
                for (int temperature : d.temperature()) {
                    update(CELL_TEMPERATURE, d.frameNumber() * 8 + i, temperature - 40);
                    i++;
                }
            } else if (data instanceof Daly.MonomerEquilibriumState) {
                Daly.MonomerEquilibriumState d = (Daly.MonomerEquilibriumState) data;

                int i = 0;
                for (boolean state : d.state()) {
                    update(CELL_EQUILIBRIUM_STATE, i, state);
                    i++;
                }
            } else if (data instanceof Daly.BatteryFailureStatus) {
                Daly.BatteryFailureStatus d = (Daly.BatteryFailureStatus) data;

                String failureStatus = IntStream.range(0, 6 * 8 + 4) //
                        .filter(bit -> ((d.error().get(bit / 8) >> (bit % 8)) & 1) == 1) //
                        .mapToObj(bit -> (String) errorMessages.get(bit)).collect(Collectors.joining(", "));

                updateState(FAILURE_STATUS.name().toLowerCase(), new StringType(failureStatus));
            }
        }
    }

    private void update(Channels channel, Number value) {
        String name = channel.name().toLowerCase();

        Unit<?> unit = channel.getUnit();
        if (unit == null) {
            updateState(name, new DecimalType(new BigDecimal(value.toString())));
        } else {
            updateState(name, QuantityType.valueOf(value.doubleValue(), unit));
        }
    }

    private void update(Channels channel, int number, Number value) {
        String name = channel.name().toLowerCase();

        Unit<?> unit = channel.getUnit();
        if (unit == null) {
            updateState(name + "#" + number, new DecimalType(new BigDecimal(value.toString())));
        } else {
            updateState(name + "#" + number, QuantityType.valueOf(value.doubleValue(), unit));
        }
    }

    private void update(Channels channel, boolean value) {
        updateState(channel.name().toLowerCase(), OnOffType.from(value));
    }

    private void update(Channels channel, int number, boolean value) {
        updateState(channel.name().toLowerCase() + "#" + number, OnOffType.from(value));
    }

    private CompletableFuture<@Nullable Void> sendPacket(byte[] data) {
        return writeCharacteristic(SERVICE_UUID, PROTOCOL_CHAR_UUID, data, true);
    }
}
