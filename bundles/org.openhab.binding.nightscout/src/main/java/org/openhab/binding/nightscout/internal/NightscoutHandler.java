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
package org.openhab.binding.nightscout.internal;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.binding.nightscout.internal.dto.Response;
import org.openhab.binding.nightscout.internal.dto.Result;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

import com.google.gson.Gson;

/**
 * The {@link NightscoutHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Fabian Wolter - Initial contribution
 */
@NonNullByDefault
public class NightscoutHandler extends BaseThingHandler {
    private static final String API_PATH = "/api/v3/";
    private static final Map<String, String> DIRECTIONS = new HashMap<>();
    private static final Duration SENSOR_TIMEOUT = Duration.ofMinutes(15);
    private Gson gson = new Gson();
    private HttpClient httpClient;
    private TimeZoneProvider timeZoneProvider;
    private @Nullable ScheduledFuture<?> future;
    private @Nullable NightscoutConfiguration config;

    static {
        DIRECTIONS.put("NONE", "⇼");
        DIRECTIONS.put("TripleUp", "⤊");
        DIRECTIONS.put("DoubleUp", "⇈");
        DIRECTIONS.put("SingleUp", "↑");
        DIRECTIONS.put("FortyFiveUp", "↗");
        DIRECTIONS.put("Flat", "→");
        DIRECTIONS.put("FortyFiveDown", "↘");
        DIRECTIONS.put("SingleDown", "↓");
        DIRECTIONS.put("DoubleDown", "⇊");
        DIRECTIONS.put("TripleDown", "⤋");
        DIRECTIONS.put("NOT COMPUTABLE", "-");
        DIRECTIONS.put("RATE OUT OF RANGE", "⇕");
    }

    public NightscoutHandler(Thing thing, HttpClient httpClient, TimeZoneProvider timeZoneProvider) {
        super(thing);
        this.httpClient = httpClient;
        this.timeZoneProvider = timeZoneProvider;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            poll();
        }
    }

    @Override
    public void initialize() {
        var localConfig = config = getConfigAs(NightscoutConfiguration.class);

        updateStatus(ThingStatus.UNKNOWN);

        future = scheduler.scheduleWithFixedDelay(this::poll, 0, localConfig.refreshInterval, TimeUnit.SECONDS);
    }

    private void poll() {
        try {
            Response entries = getResponse("entries.json?limit=1&sort$desc=date", Response.class);

            State glucose = UnDefType.UNDEF;
            State trendArrow = UnDefType.UNDEF;

            if (!entries.getResult().isEmpty()) {
                Result entry = entries.getResult().get(0);

                if (entry.getType().equals("sgv")) {
                    Instant lastSensorValueReceived = Instant.ofEpochMilli(entry.getDate());

                    if (Duration.between(lastSensorValueReceived, Instant.now()).compareTo(SENSOR_TIMEOUT) < 0) {
                        glucose = new DecimalType(entry.getSgv());

                        trendArrow = UnDefType.NULL;
                        if (DIRECTIONS.containsKey(entry.getDirection())) {
                            trendArrow = StringType.valueOf(DIRECTIONS.get(entry.getDirection()));
                        }
                    }

                    updateState("last-sensor-value", new DateTimeType(
                            ZonedDateTime.ofInstant(lastSensorValueReceived, timeZoneProvider.getTimeZone())));
                }
            }

            updateGlucoseAndTrendArrow(glucose, trendArrow);

            updateStatus(ThingStatus.ONLINE);
        } catch (NightscoutException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        } catch (InterruptedException e) {
            // nothing
        }
    }

    private void updateGlucoseAndTrendArrow(State glucose, State trendArrow) {
        updateState("glucose", glucose);
        updateState("trend-arrow", trendArrow);
    }

    private <T> T getResponse(String query, Class<T> clazz) throws InterruptedException, NightscoutException {
        var localConfig = config;

        if (localConfig == null) {
            throw new NightscoutException("Config is null");
        }

        try {
            ContentResponse response = httpClient
                    .newRequest(localConfig.url + API_PATH + query + "&token=" + localConfig.accessToken).send();

            if (response.getStatus() == HttpStatus.OK_200) {
                var dto = gson.fromJson(response.getContentAsString(), clazz);

                if (dto == null) {
                    throw new NightscoutException("Response is empty");
                } else {
                    return dto;
                }
            } else {
                throw new NightscoutException("Unexpected status code: " + response.getStatus());
            }
        } catch (ExecutionException e) {
            String message = e.getMessage();
            if (message != null && message.contains("Authentication challenge without WWW-Authenticate header")) {
                throw new NightscoutException("Access token incorrect");
            } else {
                throw new NightscoutException("Failed to connect to Nightscout: " + e.getMessage());
            }
        } catch (TimeoutException e) {
            throw new NightscoutException("Timeout when connecting to Nightscout");
        }
    }

    @Override
    public void dispose() {
        var localFuture = future;
        if (localFuture != null) {
            localFuture.cancel(true);
        }

        updateGlucoseAndTrendArrow(UnDefType.NULL, UnDefType.NULL);
    }
}
