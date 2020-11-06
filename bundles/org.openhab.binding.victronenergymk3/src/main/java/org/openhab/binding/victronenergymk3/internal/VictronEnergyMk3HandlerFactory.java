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

import static org.openhab.binding.victronenergymk3.internal.VictronEnergyMk3BindingConstants.*;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.transport.serial.SerialPortManager;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link VictronEnergyMk3HandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Fabian Wolter - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.victronenergymk3", service = ThingHandlerFactory.class)
public class VictronEnergyMk3HandlerFactory extends BaseThingHandlerFactory {
    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections
            .unmodifiableSet(Stream.of(MK3_DEVICE, GENERIC_INVERTER).collect(Collectors.toSet()));

    // TODO
    @NonNullByDefault({})
    private @Reference SerialPortManager serialPortManager;

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (MK3_DEVICE.equals(thingTypeUID)) {
            return new Mk3BridgeHandler((Bridge) thing, serialPortManager);
        }

        if (GENERIC_INVERTER.equals(thingTypeUID)) {
            return new InverterThingHandler(thing);
        }

        return null;
    }
}
