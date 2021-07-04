package org.openhab.binding.bluetooth.daly.internal;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;

@NonNullByDefault
public enum Channels {
    PRESSURE(Units.VOLT),
    ACQUISITION(Units.VOLT),
    CURRENT(Units.AMPERE),
    STATE_OF_CHARGE(Units.PERCENT),

    MAX_VOLTAGE(Units.VOLT),
    MAX_VOLTAGE_CELL_NO(null),
    MIN_VOLTAGE(Units.VOLT),
    MIN_VOLTAGE_CELL_NO(null),

    MAX_TEMPERATURE(SIUnits.CELSIUS),
    MAX_TEMPERATURE_CELL_NO(null),
    MIN_TEMPERATURE(SIUnits.CELSIUS),
    MIN_TEMPERATURE_CELL_NO(null),

    CHARGE_DISCHARGE_STATUS(null),
    CHARGING_MOS_TUBE_STATUS(null),
    DISCHARGING_MOS_TUBE_STATUS(null),
    BMS_LIFE(null),
    RESIDUAL_CAPACITY(Units.AMPERE_HOUR),

    STRING_SIZE(null),
    TEMPERATURE(SIUnits.CELSIUS),
    CHARGER_STATUS(null),
    LOAD_STATUS(null),
    DI_STATE(null),
    DO_STATE(null),
    CHARGE_DISCHARGE_CYCLES(null),

    CELL_VOLTAGE(Units.VOLT),

    CELL_TEMPERATURE(Units.VOLT),

    CELL_EQUILIBRIUM_STATE(null),

    FAILURE_STATUS(null);

    private Channels(@Nullable Unit<?> unit) {
        this.unit = unit;
    }

    private @Nullable Unit<?> unit;

    public @Nullable Unit<?> getUnit() {
        return unit;
    }
}
