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
package org.openhab.binding.victronenergymk3.internal.protocol;

/**
 * TODO
 *
 * @author Fabian Wolter - Initial contribution
 */
public abstract class Request {
    abstract public byte[] getFrame();

    public static class Address extends Request {
        private int address;

        public Address(int address) {
            this.address = address;
        }

        @Override
        public byte[] getFrame() {
            return Mk3ProtocolL3.createAddressRequest(0);
        }

        public int getAddress() {
            return address;
        }

        @Override
        public String toString() {
            return "Find device";
        }
    }

    public static class SetWinmonMode extends Request {
        @Override
        public byte[] getFrame() {
            return Mk3ProtocolL3.createSCommandRequest();
        }

        @Override
        public String toString() {
            return "Set Communication Mode";
        }
    }

    public static class VariableValue extends Request {
        private RamVariable variable;

        public VariableValue(RamVariable variable) {
            this.variable = variable;
        }

        @Override
        public byte[] getFrame() {
            return Mk3ProtocolL3.createReadRamVarRequest(variable);
        }

        public RamVariable getVariable() {
            return variable;
        }

        @Override
        public String toString() {
            return "Value: " + variable.toString();
        }
    }

    public static class VariableInfo extends Request {
        private RamVariable variable;

        public VariableInfo(RamVariable variable) {
            this.variable = variable;
        }

        @Override
        public byte[] getFrame() {
            return Mk3ProtocolL3.createRamVarInfoRequest(variable);
        }

        public RamVariable getVariable() {
            return variable;
        }

        @Override
        public String toString() {
            return "Info: " + variable.toString();
        }
    }

    public static class DeviceState extends Request {
        @Override
        public byte[] getFrame() {
            return Mk3ProtocolL3.createDeviceStateRequest();
        }

        @Override
        public String toString() {
            return "Request Device State";
        }
    }
}