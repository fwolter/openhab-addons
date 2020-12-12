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
package org.openhab.binding.myq.internal.handler;

import static org.openhab.binding.myq.internal.MyQBindingConstants.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentProvider;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.binding.myq.internal.MyQConfiguration;
import org.openhab.binding.myq.internal.MyQDiscoveryService;
import org.openhab.binding.myq.internal.api.Account;
import org.openhab.binding.myq.internal.api.Action;
import org.openhab.binding.myq.internal.api.Devices;
import org.openhab.binding.myq.internal.api.LoginRequest;
import org.openhab.binding.myq.internal.api.LoginResponse;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The {@link MyQAccountHandler} is responsible for communicating with the MyQ API based on an account.
 *
 * @author Dan Cunningham - Initial contribution
 */
@NonNullByDefault
public class MyQAccountHandler extends BaseBridgeHandler {
    private static final String BASE_URL = "https://api.myqdevice.com/api";

    private final Logger logger = LoggerFactory.getLogger(MyQAccountHandler.class);
    private final Gson gsonUpperCase = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
            .create();
    private final Gson gsonLowerCase = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    private @Nullable Future<?> normalPollFuture;
    private @Nullable Future<?> rapidPollFuture;
    private @Nullable String securityToken;
    private @Nullable Account account;
    private MyQDiscoveryService discoveryService;
    private Integer normalRefreshInterval = 60;
    private Integer rapisRefreshInterval = 5;
    private HttpClient httpClient;

    public MyQAccountHandler(Bridge bridge, HttpClient httpClient, MyQDiscoveryService discoveryService) {
        super(bridge);
        this.httpClient = httpClient;
        this.discoveryService = discoveryService;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public void initialize() {
        MyQConfiguration config = getConfigAs(MyQConfiguration.class);
        normalRefreshInterval = config.refreshInterval;
        updateStatus(ThingStatus.UNKNOWN);
        restartPolls(false);
    }

    @Override
    public void dispose() {
        stopPolls();
    }

    /**
     * Sends an action to the MyQ API
     *
     * @param serialNumber
     * @param action
     */
    public void sendAction(String serialNumber, String action) {
        Account localAccount = account;
        if (localAccount != null) {
            String content = sendRequest(requst(
                    String.format("%s/v5.1/Accounts/%s/Devices/%s/actions", BASE_URL, localAccount.userId,
                            serialNumber),
                    HttpMethod.PUT, securityToken, new StringContentProvider(gsonLowerCase.toJson(new Action(action))),
                    "application/json"));
            logger.trace("Action Response {}", content);
            restartPolls(true);
        }
    }

    private void stopPolls() {
        stopFuture(normalPollFuture);
        stopFuture(rapidPollFuture);
    }

    private void stopFuture(@Nullable Future<?> future) {
        if (future != null && !future.isCancelled()) {
            future.cancel(false);
        }
    }

    private void restartPolls(boolean rapid) {
        stopPolls();
        if (rapid) {
            normalPollFuture = scheduler.scheduleWithFixedDelay(this::poll, 35, normalRefreshInterval,
                    TimeUnit.SECONDS);
            rapidPollFuture = scheduler.scheduleWithFixedDelay(this::fetchData, 3, rapisRefreshInterval,
                    TimeUnit.SECONDS);
        } else {
            normalPollFuture = scheduler.scheduleWithFixedDelay(this::poll, 0, normalRefreshInterval, TimeUnit.SECONDS);
        }
    }

    private void poll() {
        stopFuture(rapidPollFuture);
        fetchData();
    }

    private void fetchData() {
        // if our token is null we need to login
        if (securityToken == null) {
            logger.debug("login");
            login();
            if (securityToken != null) {
                getAccount();
            }
        }
        // if we now have a token, get our data
        if (securityToken != null) {
            getDevices();
        }
    }

    private void login() {
        MyQConfiguration config = getConfigAs(MyQConfiguration.class);
        String content = sendRequest(requst(BASE_URL + "/v5/Login", HttpMethod.POST, null,
                new StringContentProvider(gsonUpperCase.toJson(new LoginRequest(config.username, config.password))),
                "application/json"));
        if (content != null) {
            account = gsonUpperCase.fromJson(content, Account.class);
            LoginResponse loginResponse = gsonUpperCase.fromJson(content, LoginResponse.class);
            if (loginResponse.securityToken != null) {
                securityToken = loginResponse.securityToken;
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Invalid Response Body");
            }
        } else {
            if (thing.getStatusInfo().getStatusDetail() == ThingStatusDetail.CONFIGURATION_ERROR) {
                // bad credentials, stop trying to login
                stopPolls();
            }
        }
    }

    private void getAccount() {
        String content = sendRequest(requst(BASE_URL + "/v5/My", HttpMethod.GET, securityToken, null, null));
        if (content != null) {
            account = gsonUpperCase.fromJson(content, Account.class);
        }
    }

    private void getDevices() {
        Account localAccount = account;
        if (localAccount == null) {
            return;
        }
        String content = sendRequest(requst(String.format("%s/v5.1/Accounts/%s/Devices", BASE_URL, localAccount.userId),
                HttpMethod.GET, securityToken, null, null));
        if (content != null) {
            Devices devices = gsonLowerCase.fromJson(content, Devices.class);
            if (devices != null) {
                devices.items.forEach(device -> {
                    logger.trace("Device {}", device.deviceFamily);
                    ThingTypeUID thingTypeUID = new ThingTypeUID(BINDING_ID, device.deviceFamily);
                    if (SUPPORTED_DISCOVERY_THING_TYPES_UIDS.contains(thingTypeUID)) {
                        String id = device.serialNumber.toLowerCase();
                        ThingUID thingUID = new ThingUID(thingTypeUID, getThing().getUID(), id);
                        Thing thing = getThing().getThing(thingUID);
                        if (thing == null) {
                            deviceDiscovered(thingUID, device.name);
                        } else {
                            ThingHandler handler = thing.getHandler();
                            if (handler != null) {
                                ((MyQDeviceHandler) handler).handleDeviceUpdate(device);
                            }
                        }
                    }
                });
            }
        }
    }

    private Request requst(String url, HttpMethod method, @Nullable String token, @Nullable ContentProvider content,
            @Nullable String contentType) {
        Request requst = httpClient.newRequest(url).method(method)
                .header("MyQApplicationId", "JVM/G9Nwih5BwKgNCjLxiFUQxQijAebyyg8QUHr7JOrP+tuPb8iHfRHKwTmDzHOu")
                .header("ApiVersion", "5.1").header("BrandId", "2").header("Culture", "en")
                .timeout(10, TimeUnit.SECONDS);
        if (token != null) {
            requst = requst.header("SecurityToken", token);
        }
        if (content != null & contentType != null) {
            requst = requst.content(content, contentType);
        }
        return requst;
    }

    private @Nullable String sendRequest(Request request) {
        try {
            ContentResponse contentResponse = request.send();
            int statusCode = contentResponse.getStatus();
            String content = contentResponse.getContentAsString();
            logger.trace("Account Response - status: {} content: {}", statusCode, content);
            switch (statusCode) {
                case HttpStatus.OK_200:
                    updateStatus(ThingStatus.ONLINE);
                    return content;
                case HttpStatus.UNAUTHORIZED_401:
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                            "Unauthorized - Check Credentials");
                    securityToken = null;
                    break;
                default:
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                            "Invalid Response Code " + statusCode);
            }
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            logger.debug("Could not login", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getLocalizedMessage());
        }
        return null;
    }

    private void deviceDiscovered(ThingUID thingUID, String name) {
        logger.trace("Adding device {} {} {}", thingUID, name, thingUID.getId());
        DiscoveryResult result = DiscoveryResultBuilder.create(thingUID).withLabel("MyQ " + name)
                .withProperty("serialNumber", thingUID.getId()).withRepresentationProperty("serialNumber")
                .withBridge(getThing().getUID()).build();
        discoveryService.deviceDiscovered(result);
    }
}
