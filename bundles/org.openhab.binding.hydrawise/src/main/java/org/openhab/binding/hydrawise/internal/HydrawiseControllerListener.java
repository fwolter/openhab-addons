package org.openhab.binding.hydrawise.internal;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.hydrawise.internal.api.graphql.schema.Controller;

@NonNullByDefault
public interface HydrawiseControllerListener {

    public void onData(List<Controller> controllers);
}
