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

import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.victronenergymk3.internal.Mk3Exception;

/**
 * Handles header (length) and checksum of the Mk3 protocol at ISO Layer 2.
 *
 * @author Fabian Wolter - Initial contribution
 */
@NonNullByDefault
public class Mk3ProtocolL2 {
    public static Optional<byte[]> readFrame(byte[] frameL2) throws Mk3Exception {
        int length = Byte.toUnsignedInt(frameL2[0]) + 2;

        if (length == 0) {
            throw new Mk3Exception("Length is zero");
        }

        if (frameL2.length < length) {
            // frame is not yet complete
            return Optional.empty();
        }

        if (frameL2[1] != (byte) 0xFF) {
            throw new Mk3Exception("Unexpected fill byte");
        }

        byte expectedByteSum = sumBytes(frameL2, length);
        if (expectedByteSum != 0) {
            throw new Mk3Exception(String.format("Checksum error: Expected: %02X Received: %02X",
                    (byte) (256 - expectedByteSum), frameL2[length - 1]));
        }

        byte[] payloadL3 = new byte[length - 3];
        System.arraycopy(frameL2, 2, payloadL3, 0, length - 3);

        return Optional.of(payloadL3);
    }

    public static byte[] createFrame(int... payloadL3) {
        byte[] result = new byte[payloadL3.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) payloadL3[i];
        }
        return createFrame(result);
    }

    public static byte[] createFrame(byte... payloadL3) {
        byte length = (byte) (payloadL3.length + 1);

        byte[] frameL2 = new byte[payloadL3.length + 3];
        frameL2[0] = length;
        frameL2[1] = (byte) 0xFF;
        System.arraycopy(payloadL3, 0, frameL2, 2, payloadL3.length);
        frameL2[frameL2.length - 1] = (byte) (256 - (sumBytes(frameL2, frameL2.length - 1)));

        return frameL2;
    }

    public static byte[] createFrameVE(byte... payloadL3) {
        byte length = (byte) (payloadL3.length);

        byte[] frameL2 = new byte[payloadL3.length + 2];
        frameL2[0] = length;
        System.arraycopy(payloadL3, 0, frameL2, 1, payloadL3.length);
        int sumBytes = sumBytes(frameL2, frameL2.length - 1);
        if (sumBytes == 0) {
            frameL2[frameL2.length - 1] = 0;
        } else {
            frameL2[frameL2.length - 1] = (byte) (256 - sumBytes);
        }

        return frameL2;
    }

    private static byte sumBytes(byte[] data, int length) {
        int sum = 0;

        for (int i = 0; i < length; i++) {
            sum += Byte.toUnsignedInt(data[i]);
        }

        return (byte) sum;
    }
}
