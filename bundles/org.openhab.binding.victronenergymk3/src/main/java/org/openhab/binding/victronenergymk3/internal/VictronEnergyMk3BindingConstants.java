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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link VictronEnergyMk3BindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Fabian Wolter - Initial contribution
 */
@NonNullByDefault
public class VictronEnergyMk3BindingConstants {
    private static final String BINDING_ID = "victronenergymk3";
    public static final ThingTypeUID MK3_DEVICE = new ThingTypeUID(BINDING_ID, "mk3");
    public static final ThingTypeUID GENERIC_INVERTER = new ThingTypeUID(BINDING_ID, "genericInverter");
    static final String CHANNEL_DEVICE_STATE = "common#device_state";
    static final String RAM_GROUP = "ram";
}
