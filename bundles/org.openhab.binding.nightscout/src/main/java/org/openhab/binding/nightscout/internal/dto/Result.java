/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.binding.nightscout.internal.dto;

/**
 *
 * @author Fabian Wolter - Initial contribution
 */
public class Result {
    private long date;
    private int sgv;
    private String direction;
    private String type;

    public long getDate() {
        return date;
    }

    public int getSgv() {
        return sgv;
    }

    public String getDirection() {
        return direction;
    }

    public String getType() {
        return type;
    }
}
