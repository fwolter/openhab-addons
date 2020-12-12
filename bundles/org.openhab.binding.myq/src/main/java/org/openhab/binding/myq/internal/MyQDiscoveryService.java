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
package org.openhab.binding.myq.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.DiscoveryListener;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.config.discovery.ScanListener;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MyQDiscoveryService} is responsible for discovering MyQ things
 *
 * @author Dan Cunningham - Initial contribution
 */
@NonNullByDefault
public class MyQDiscoveryService implements DiscoveryService {
    private final Logger logger = LoggerFactory.getLogger(MyQDiscoveryService.class);
    private static final Set<ThingTypeUID> SUPPORTED_DISCOVERY_THING_TYPES_UIDS = Set
            .of(MyQBindingConstants.THING_TYPE_GARAGEDOOR);
    private final Map<ThingUID, DiscoveryResult> cachedResults = new HashMap<>();
    private final Set<DiscoveryListener> discoveryListeners = new CopyOnWriteArraySet<>();

    public void deviceDiscovered(DiscoveryResult result) {

        for (DiscoveryListener discoveryListener : discoveryListeners) {
            try {
                discoveryListener.thingDiscovered(this, result);
            } catch (Exception e) {
                logger.error("An error occurred while calling the discovery listener {}.",
                        discoveryListener.getClass().getName(), e);
            }
        }
        synchronized (cachedResults) {
            cachedResults.put(result.getThingUID(), result);
        }
    }

    @Override
    public Collection<ThingTypeUID> getSupportedThingTypes() {
        return SUPPORTED_DISCOVERY_THING_TYPES_UIDS;
    }

    @Override
    public int getScanTimeout() {
        return 0;
    }

    @Override
    public boolean isBackgroundDiscoveryEnabled() {
        return false;
    }

    @Override
    public void startScan(@Nullable ScanListener listener) {
        if (listener != null) {
            listener.onFinished();
        }
    }

    @Override
    public void abortScan() {
    }

    @Override
    public void addDiscoveryListener(@Nullable DiscoveryListener listener) {
        logger.trace("Adding discovery listener");
        if (listener == null) {
            return;
        }
        synchronized (cachedResults) {
            for (DiscoveryResult cachedResult : cachedResults.values()) {
                listener.thingDiscovered(this, cachedResult);
            }
        }
        discoveryListeners.add(listener);
    }

    @Override
    public void removeDiscoveryListener(@Nullable DiscoveryListener listener) {
        discoveryListeners.remove(listener);
    }
}
