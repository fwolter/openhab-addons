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
package org.openhab.binding.myq.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.myq.internal.MyQBindingConstants;
import org.openhab.binding.myq.internal.api.Device;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.BridgeHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;

/**
 * The {@link MyQLightHandler} is responsible for handling commands for a light thing, which are
 * sent to one of the channels.
 *
 * @author Dan Cunningham - Initial contribution
 */
@NonNullByDefault
public class MyQLightHandler extends BaseThingHandler implements MyQDeviceHandler {
    private @Nullable Device status;

    public MyQLightHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

        if (command instanceof RefreshType) {
            updateState();
            return;
        }

        if (command instanceof OnOffType) {
            Bridge bridge = getBridge();
            final Device localStatus = status;
            if (bridge != null && localStatus != null) {
                BridgeHandler handler = bridge.getHandler();
                if (handler != null) {
                    ((MyQAccountHandler) handler).sendAction(localStatus.serialNumber,
                            command == OnOffType.ON ? "lighton" : "lightoff");
                }

            }
        }
    }

    protected void updateState() {
        final Device localStatus = status;
        if (localStatus != null) {
            String lightState = localStatus.state.lightState;
            updateState("status", new StringType(lightState));
            updateState("switch", lightState.equals("on") ? OnOffType.ON : OnOffType.OFF);
        }
    }

    @Override
    public void handleDeviceUpdate(Device device) {
        if (!MyQBindingConstants.THING_TYPE_LIGHT.getId().equals(device.deviceFamily)) {
            return;
        }
        status = device;
        updateStatus(ThingStatus.ONLINE);
        updateState();
    }
}
