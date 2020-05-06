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

package org.openhab.binding.hydrawise.internal.api.graphql.schema;

/**
 * @author Dan Cunningham - Initial contribution
 */
public class Zone {

    public Integer id;
    public String name;
    public ZoneStatus status;
    public Icon icon;
    public ZoneNumber number;
    public ScheduledRuns scheduledRuns;
    public PastRuns pastRuns;
    //
    // @Override
    // public String toString() {
    // return new StringBuilder().append("ID: ").append(id).append(" name: ").append(name).append(" status: ")
    // .append(status).append("ICcon: ").append(icon).append(" number: ").append(number)
    // .append(" scheduledRuns: ").append(scheduledRuns).append("pastRuns: ").append(pastRuns).toString();
    // }
}
