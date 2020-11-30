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
package org.openhab.binding.modbus.sbc.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.modbus.handler.BaseModbusThingHandler;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.io.transport.modbus.AsyncModbusFailure;
import org.openhab.io.transport.modbus.AsyncModbusReadResult;
import org.openhab.io.transport.modbus.ModbusBitUtilities;
import org.openhab.io.transport.modbus.ModbusReadFunctionCode;
import org.openhab.io.transport.modbus.ModbusReadRequestBlueprint;

/**
 * The {@link ALD1Handler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Fabian Wolter - Initial contribution
 */
@NonNullByDefault
public class ALD1Handler extends BaseModbusThingHandler {
    private static final int FIRST_READ_ADDRESS = 28;
    private static final int READ_LENGTH = 13;
    private static final int TRIES = 1;
    private ALD1Configuration config = new ALD1Configuration();
    private @Nullable ModbusReadRequestBlueprint blueprint;

    public ALD1Handler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        ModbusReadRequestBlueprint localBlueprint = blueprint;
        if (command instanceof RefreshType && localBlueprint != null) {
            submitOneTimePoll(localBlueprint, this::readSuccessful, this::readError);
        }
    }

    @Override
    public void initialize() {
        super.initialize();
        config = getConfigAs(ALD1Configuration.class);

        if (config.pollInterval <= 0) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Invalid poll interval: " + config.pollInterval);
            return;
        }

        ModbusReadRequestBlueprint localBlueprint = blueprint = new ModbusReadRequestBlueprint(getSlaveId(),
                ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS, FIRST_READ_ADDRESS - 1, READ_LENGTH, TRIES);

        updateStatus(ThingStatus.UNKNOWN);

        registerRegularPoll(localBlueprint, config.pollInterval, 0, this::readSuccessful, this::readError);
    }

    private void readSuccessful(AsyncModbusReadResult result) {
        result.getRegisters().ifPresent(registers -> {
            if (getThing().getStatus() != ThingStatus.ONLINE) {
                updateStatus(ThingStatus.ONLINE);
            }

            for (ALD1Registers channel : ALD1Registers.values()) {
                int index = channel.getRegisterAddress() - FIRST_READ_ADDRESS;

                ModbusBitUtilities.extractStateFromRegisters(registers, index, channel.getType())
                        .map(DecimalType::floatValue)
                        .map(v -> QuantityType.valueOf(v * channel.getMultiplier(), channel.getUnit()))
                        .ifPresent(v -> updateState(createChannelUid(channel), v));
            }
        });
    }

    private void readError(AsyncModbusFailure<ModbusReadRequestBlueprint> error) {
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                "Failed to retrieve data: " + error.getCause().getMessage());
    }

    private ChannelUID createChannelUid(ALD1Registers channel) {
        return new ChannelUID(thing.getUID(), channel.toString().toLowerCase());
    }
}
