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
package org.openhab.binding.victronenergymk3.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Thrown when the serial port receives unexpected data.
 *
 * @author Fabian Wolter - Initial contribution
 */
@NonNullByDefault
public class Mk3Exception extends Exception {
    private static final long serialVersionUID = 1849766553518330401L;

    public Mk3Exception(String message) {
        super(message);
    }

    public Mk3Exception(Exception e) {
        super(e);
    }
}
