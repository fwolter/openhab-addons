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
package org.openhab.binding.victronenergymk3.internal.protocol;

import java.util.Optional;
import java.util.function.Function;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.victronenergymk3.internal.Mk3Exception;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.types.State;

/**
 * Handles binary communication with Mk3 USB adapter at ISO Layer 3.
 *
 * @author Fabian Wolter - Initial contribution
 */
@NonNullByDefault
public class Mk3ProtocolL3 {
    public static final int RECEIVE_TIMEOUT_MS = 500;
    public static final int DEVICE_STATE_RESPONSE = 0x94;
    public static final int RAM_VARIABLE_VALUE_RESPONSE_VALID = 0x85;
    public static final int RAM_VARIABLE_VALUE_RESPONSE_INVALID = 0x90;
    public static final int RAM_VARIABLE_INFO_RESPONSE = 0x8E;
    public static final int RAM_VARIABLE_WRITE_SUCCESSFUL_RESPONSE = 0x87;
    public static final int ADDRESS_CMD = 'A';
    public static final int STATE_CMD = 'S';
    public static final int WINMON_W_CMD = 'Y';
    public static final int WINMON_X_CMD = 'X';
    private static final int SWITCH_ON = 3;
    private static final int S_CMD_WINMON_1_FLAG = 1;
    private static final int S_CMD_VARIANT_2_FLAG = 1 << 7;

    public static Optional<byte[]> readFrame(byte[] frameL2) throws Mk3Exception {
        return Mk3ProtocolL2.readFrame(frameL2);
    }

    public static byte[] createAddressRequest(int address) {
        return Mk3ProtocolL2.createFrame(ADDRESS_CMD, 1, address);
    }

    public static byte[] createSCommandRequest() {
        return Mk3ProtocolL2.createFrame(STATE_CMD, SWITCH_ON, 0, 0, 1, S_CMD_VARIANT_2_FLAG, 0, S_CMD_WINMON_1_FLAG,
                0);
    }

    public static byte[] createDeviceStateRequest() {
        return Mk3ProtocolL2.createFrame(WINMON_W_CMD, 0x0E, 0);
    }

    public static byte[] createRamVarInfoRequest(RamVariable ramVariable) {
        return Mk3ProtocolL2.createFrame(WINMON_W_CMD, 0x36, ramVariable.getRamId());
    }

    public static Function<Short, ? extends State> createRamVarScalingFunction(RamVariable ramVariable, short scaleRaw,
            short offset) throws Mk3Exception {
        double scale = Math.abs(scaleRaw);

        if (scale >= 0x4000) {
            scale = 1f / (0x8000 - scale);
        }

        final double finalScale = scale;
        final Unit<?> unit = ramVariable.getUnit();

        if (offset == (short) 0x8000) {
            // variable type is a bit
            return raw -> (raw & (1 << scaleRaw)) > 0 ? OnOffType.ON : OnOffType.OFF;
        } else if (unit != null) {
            // variable type is a value
            return raw -> {
                double x = raw;
                // checking for GRID_POWER_SETPOINT is a quirk as the sign in the scaling response is wrong
                if (scaleRaw > 0 && ramVariable != RamVariable.GRID_POWER_SETPOINT) {
                    // variable type is unsigned
                    x = Short.toUnsignedInt(raw);
                }
                return QuantityType.valueOf(ramVariable.getConverter().apply(finalScale * (x + offset)), unit);
            };
        } else {
            throw new Mk3Exception("RAM variable neither of type bit nor value: " + ramVariable);
        }
    }

    public static byte[] createReadRamVarRequest(RamVariable ramVariable) {
        byte ramId = ramVariable.getRamId();
        return Mk3ProtocolL2.createFrame(WINMON_W_CMD, 0x30, ramId);
    }

    public static byte[] createWriteRamVarRequest(RamVariable ramVariable) {
        return Mk3ProtocolL2.createFrame(WINMON_X_CMD, 0x32, ramVariable.getRamId());
    }

    public static byte[] createWriteDataRequest(short value) {
        return Mk3ProtocolL2.createFrame(WINMON_X_CMD, 0x34, value, value >> 8);
    }

    public static DeviceState convertDeviceState(byte state, byte subState) throws Mk3Exception {
        if (state == 9) {
            switch (subState) {
                case 0:
                    return DeviceState.CHARGE_INITIALIZING;
                case 1:
                    return DeviceState.CHARGE_BULK;
                case 2:
                    return DeviceState.CHARGE_ABSORPTION;
                case 3:
                    return DeviceState.CHARGE_FLOAT;
                case 4:
                    return DeviceState.CHARGE_STORAGE;
                case 5:
                    return DeviceState.CHARGE_REPEATED_ABSORPTION;
                case 6:
                    return DeviceState.CHARGE_FORCED_ABSORPTION;
                case 7:
                    return DeviceState.CHARGE_EQUALIZE;
                case 8:
                    return DeviceState.CHARGE_BULK_STOPPED;
                default:
                    throw new Mk3Exception("Unexpected charging state: " + subState);
            }
        } else {
            if (state < 0 || state >= DeviceState.values().length) {
                throw new Mk3Exception("Unexpected device state: " + state);
            }
            return DeviceState.values()[state];
        }
    }
}
