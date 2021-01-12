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
package org.openhab.binding.victronenergymk3.internal.protocol;

import java.util.function.Function;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.unit.Units;

/**
 * List of all RAM variables of an inverter/charger.
 *
 * @author Fabian Wolter - Initial contribution
 */
@NonNullByDefault
public enum RamVariable {
    MAINS_VOLTAGE(0, Units.VOLT, Function.identity()),
    MAINS_CURRENT(1, Units.AMPERE, Function.identity()),
    INVERTER_VOLTAGE(2, Units.VOLT, Function.identity()),
    INVERTER_CURRENT(3, Units.AMPERE, Function.identity()),
    BATTERY_VOLTAGE(4, Units.VOLT, Function.identity()),
    BATTERY_CURRENT(5, Units.AMPERE, Function.identity()),
    BATTERY_VOLTAGE_RMS(6, Units.VOLT, Function.identity()),
    INVERTER_FREQUENCY(7, Units.HERTZ, RamVariable::frequencyConversion),
    MAINS_FREQUENCY(8, Units.HERTZ, RamVariable::frequencyConversion),
    AC_LOAD_CURRENT(9, Units.AMPERE, Function.identity()),
    CHARGE_STATE(13, Units.PERCENT, x -> x * 100),
    INVERTER_POWER_FILTERED(14, Units.WATT, Function.identity()),
    INVERTER_POWER2_FILTERED(15, Units.WATT, Function.identity()),
    OUTPUT_POWER_FILTERED(16, Units.WATT, Function.identity()),
    INVERTER_POWER(17, Units.WATT, Function.identity()),
    INVERTER_POWER2(18, Units.WATT, Function.identity()),
    OUTPUT_POWER(19, Units.WATT, Function.identity()),
    // The following variable is the first variable of the first configured Assistant in the inverter.
    // It is expected to be the ESS Assistant. See 3.6.2 https://www.victronenergy.com/live/ess:ess_mode_2_and_3
    GRID_POWER_SETPOINT(129, Units.WATT, x -> x * -1),
    GRID_POWER_EN(132, Units.WATT, Function.identity());

    private byte ramId;
    private @Nullable Unit<?> unit;
    private Function<Double, Double> converter;

    private RamVariable(int ramId, @Nullable Unit<?> unit, Function<Double, Double> converter) {
        this.ramId = (byte) ramId;
        this.unit = unit;
        this.converter = converter;
    }

    public byte getRamId() {
        return ramId;
    }

    public @Nullable Unit<?> getUnit() {
        return unit;
    }

    public Function<Double, Double> getConverter() {
        return converter;
    }

    private static double frequencyConversion(double raw) {
        if (raw == 0) { // When mains is not available
            return 0;
        }
        return 1d / raw * 10;
    }
}
