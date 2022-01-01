package org.openhab.binding.lcn.internal.subhandler;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.binding.lcn.internal.LcnModuleHandler;
import org.openhab.binding.lcn.internal.common.LcnChannelGroup;
import org.openhab.binding.lcn.internal.common.LcnException;
import org.openhab.binding.lcn.internal.common.PckGenerator;
import org.openhab.binding.lcn.internal.connection.ModInfo;
import org.openhab.core.library.types.PercentType;

public class LcnModuleRollershutterRelaySlatAngleSubHandler extends AbstractLcnModuleRollershutterRelaySubHandler {
    public LcnModuleRollershutterRelaySlatAngleSubHandler(LcnModuleHandler handler, ModInfo info) {
        super(handler, info);
    }

    @Override
    public void handleCommandPercent(@NonNull PercentType command, @NonNull LcnChannelGroup channelGroup, int number)
            throws LcnException {
        handler.sendPck(PckGenerator.controlShutterSlatAngle(number, command.intValue()));
    }
}
