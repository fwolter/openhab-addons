package org.openhab.binding.hydrawise.internal.api.graphql;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.hydrawise.internal.api.graphql.schema.AuthToken;

@NonNullByDefault
public interface HydrawiseAuthTokenProvider {

    public @Nullable AuthToken getAuthToken();

    public void authTokenUpdated(AuthToken token);
}
