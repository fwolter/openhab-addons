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

/**
 * List of states of an inverter/charger.
 *
 * @author Fabian Wolter - Initial contribution
 */
public enum DeviceState {
    DOWN,
    STARTUP,
    OFF,
    DEVICE_IN_SLAVE_MODE,
    INVERT_FULL,
    INVERT_HALF,
    INVERT_AES,
    POWER_ASSIST,
    BYPASS,
    CHARGE_INITIALIZING,
    CHARGE_BULK,
    CHARGE_ABSORPTION,
    CHARGE_FLOAT,
    CHARGE_STORAGE,
    CHARGE_REPEATED_ABSORPTION,
    CHARGE_FORCED_ABSORPTION,
    CHARGE_EQUALIZE,
    CHARGE_BULK_STOPPED
}
