/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.binding.iaqualink.internal.api;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.binding.iaqualink.internal.api.model.AccountInfo;
import org.openhab.binding.iaqualink.internal.api.model.Auxiliary;
import org.openhab.binding.iaqualink.internal.api.model.Device;
import org.openhab.binding.iaqualink.internal.api.model.Home;
import org.openhab.binding.iaqualink.internal.api.model.OneTouch;
import org.openhab.binding.iaqualink.internal.api.model.SignIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**
 * IAqualink HTTP Client
 *
 * The {@link IAqualinkClient} provides basic HTTP commands to control and monitor a iAquaLink
 * based system.
 *
 * GSON is used to provide custom deserialization on the JSON results. These results
 * unfortunately are not returned as normalized JSON objects and require complex deserialization
 * handlers.
 *
 * @author Dan Cunningham - Initial contribution
 *
 */
@NonNullByDefault
public class IAqualinkClient {
    private final Logger logger = LoggerFactory.getLogger(IAqualinkClient.class);

    private static final String HEADER_AGENT = "iAqualink/98 CFNetwork/978.0.7 Darwin/18.6.0";
    private static final String HEADER_ACCEPT = "*/*";
    private static final String HEADER_ACCEPT_LANGUAGE = "en-us";
    private static final String HEADER_ACCEPT_ENCODING = "br, gzip, deflate";

    private static final String SUPPORT_URL = "https://support.iaqualink.com";
    private static final String SIGNIN_PATH = "%s/users/sign_in.json";
    private static final String DEVICES_PATH = "%s/devices.json?api_key=%s&authentication_token=%s&user_id=%s";
    private static final String IAQUALINK_URL = "https://iaqualink-api.realtime.io/v1/mobile/session.json?actionID=command&command=%s&serial=%s&sessionID=%s";

    private Gson gson = new GsonBuilder().registerTypeAdapter(Home.class, new HomeDeserializer())
            .registerTypeAdapter(OneTouch[].class, new OneTouchDeserializer())
            .registerTypeAdapter(Auxiliary[].class, new AuxDeserializer()).create();
    private Gson gsonInternal = new GsonBuilder().create();
    private JsonParser parser = new JsonParser();
    private HttpClient httpClient;

    @SuppressWarnings("serial")
    public class NotAuthorizedException extends Exception {
        public NotAuthorizedException(String message) {
            super(message);
        }
    }

    /**
     *
     * @param httpClient
     */
    public IAqualinkClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Initial login to service
     *
     * @param username
     * @param password
     * @param apiKey
     * @return
     * @throws IOException
     * @throws NotAuthorizedException
     */
    public AccountInfo login(@Nullable String username, @Nullable String password, @Nullable String apiKey)
            throws InterruptedException, IOException, NotAuthorizedException {
        String signIn = gson.toJson(new SignIn(apiKey, username, password)).toString();
        try {
            ContentResponse response = httpClient.newRequest(String.format(SIGNIN_PATH, SUPPORT_URL))
                    .method(HttpMethod.POST).content(new StringContentProvider(signIn), "application/json").send();
            if (response.getStatus() == HttpStatus.UNAUTHORIZED_401) {
                throw new NotAuthorizedException(response.getReason());
            }
            if (response.getStatus() != HttpStatus.OK_200) {
                throw new IOException(response.getReason());
            }
            return gson.fromJson(response.getContentAsString(), AccountInfo.class);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            throw new IOException(e);
        }
    }

    /**
     * List all devices (pools) registered to an account
     *
     * @param apiKey
     * @param token
     * @param id
     * @return {@link Device[]}
     * @throws IOException
     * @throws NotAuthorizedException
     */
    public Device[] getDevices(@Nullable String apiKey, @Nullable String token, int id)
            throws InterruptedException, IOException, NotAuthorizedException {
        return getAqualinkObject(String.format(DEVICES_PATH, SUPPORT_URL, apiKey, token, id), Device[].class);
    }

    /**
     * Retrieves the HomeScreen as a {@link JsonElement}
     *
     * @param serialNumber
     * @param sessionId
     * @return {@link JsonElement}
     * @throws IOException
     * @throws NotAuthorizedException
     */
    public JsonElement getHomeJson(@Nullable String serialNumber, @Nullable String sessionId)
            throws InterruptedException, IOException, NotAuthorizedException {
        return getAqualinkJsonObject(String.format(IAQUALINK_URL, "get_home", serialNumber, sessionId));
    }

    /**
     * Retrieves the HomeScreen
     *
     * @param serialNumber
     * @param sessionId
     * @return {@link Home}
     * @throws IOException
     * @throws NotAuthorizedException
     */
    public Home getHome(@Nullable String serialNumber, @Nullable String sessionId)
            throws InterruptedException, IOException, NotAuthorizedException {
        return homeScreenCommand(serialNumber, sessionId, "get_home");
    }

    /**
     * Retrieves {@link OneTouch[]} macros
     *
     * @param serialNumber
     * @param sessionId
     * @return {@link OneTouch[]}
     * @throws IOException
     * @throws NotAuthorizedException
     */
    public OneTouch[] getOneTouch(@Nullable String serialNumber, @Nullable String sessionId)
            throws InterruptedException, IOException, NotAuthorizedException {
        return oneTouchCommand(serialNumber, sessionId, "get_onetouch");
    }

    /**
     * Retrieves {@link Auxiliary[]} devices
     *
     * @param serialNumber
     * @param sessionId
     * @return {@link Auxiliary[]}
     * @throws IOException
     * @throws NotAuthorizedException
     */
    public Auxiliary[] getAux(@Nullable String serialNumber, @Nullable String sessionId)
            throws InterruptedException, IOException, NotAuthorizedException {
        return getAqualinkObject(String.format(IAQUALINK_URL, "get_devices", serialNumber, sessionId),
                Auxiliary[].class);
    }

    /**
     * Sends a HomeScreen command
     *
     * @param serialNumber
     * @param sessionId
     * @param command
     * @return {@link Home}
     * @throws IOException
     * @throws NotAuthorizedException
     */
    public Home homeScreenCommand(@Nullable String serialNumber, @Nullable String sessionId, String command)
            throws InterruptedException, IOException, NotAuthorizedException {
        return getAqualinkObject(String.format(IAQUALINK_URL, command, serialNumber, sessionId), Home.class);
    }

    /**
     * Sends an Auxiliary command
     *
     * @param serialNumber
     * @param sessionId
     * @param command
     * @return
     * @throws IOException
     * @throws NotAuthorizedException
     */
    public Auxiliary[] auxCommand(@Nullable String serialNumber, @Nullable String sessionId, String command)
            throws InterruptedException, IOException, NotAuthorizedException {
        return getAqualinkObject(String.format(IAQUALINK_URL, command, serialNumber, sessionId), Auxiliary[].class);
    }

    /**
     * Sends an Auxiliary command
     *
     * @param serialNumber
     * @param sessionId
     * @param command
     * @param lightValue
     * @return
     * @throws IOException
     * @throws NotAuthorizedException
     */
    public Auxiliary[] auxCommand(@Nullable String serialNumber, @Nullable String sessionId, String command,
            int lightValue) throws InterruptedException, IOException, NotAuthorizedException {
        return getAqualinkObject(
                String.format(IAQUALINK_URL + "&light=%s&subtype=5", command, serialNumber, sessionId, lightValue),
                Auxiliary[].class);
    }

    /**
     * Sets the Spa Temperature Setpoint
     *
     * @param serialNumber
     * @param sessionId
     * @param spaSetpoint
     * @return
     * @throws IOException
     * @throws NotAuthorizedException
     */
    public Home setSpaTemp(@Nullable String serialNumber, @Nullable String sessionId, int spaSetpoint)
            throws InterruptedException, IOException, NotAuthorizedException {
        return getAqualinkObject(
                String.format(IAQUALINK_URL + "&temp1=%s", "set_temps", serialNumber, sessionId, spaSetpoint),
                Home.class);
    }

    /**
     * Sets the Pool Temperature Setpoint
     *
     * @param serialNumber
     * @param sessionId
     * @param poolSetpoint
     * @return
     * @throws IOException
     * @throws NotAuthorizedException
     */
    public Home setPoolTemp(@Nullable String serialNumber, @Nullable String sessionId, int poolSetpoint)
            throws InterruptedException, IOException, NotAuthorizedException {
        return getAqualinkObject(
                String.format(IAQUALINK_URL + "&temp2=%s", "set_temps", serialNumber, sessionId, poolSetpoint),
                Home.class);
    }

    /**
     * Sends a OneTouch command
     *
     * @param serialNumber
     * @param sessionId
     * @param command
     * @return
     * @throws IOException
     * @throws NotAuthorizedException
     */
    public OneTouch[] oneTouchCommand(@Nullable String serialNumber, @Nullable String sessionId, String command)
            throws InterruptedException, IOException, NotAuthorizedException {
        return getAqualinkObject(String.format(IAQUALINK_URL, command, serialNumber, sessionId), OneTouch[].class);
    }

    /**
     *
     * @param <T>
     * @param url
     * @param typeOfT
     * @return
     * @throws IOException
     * @throws NotAuthorizedException
     */
    private <T> T getAqualinkObject(@Nullable String url, Type typeOfT)
            throws InterruptedException, IOException, NotAuthorizedException {
        try {
            logger.trace("Trying {}", url);
            ContentResponse response = httpClient.newRequest(url).method(HttpMethod.GET)//
                    .agent(HEADER_AGENT) //
                    .header(HttpHeader.ACCEPT_LANGUAGE, HEADER_ACCEPT_LANGUAGE) //
                    .header(HttpHeader.ACCEPT_ENCODING, HEADER_ACCEPT_ENCODING) //
                    .header(HttpHeader.ACCEPT, HEADER_ACCEPT) //
                    .send();

            logger.trace("Response {}", response.toString());
            if (response.getStatus() == HttpStatus.UNAUTHORIZED_401) {
                throw new NotAuthorizedException(response.getReason());
            }
            if (response.getStatus() != HttpStatus.OK_200) {
                throw new IOException(response.getReason());
            }
            return gson.fromJson(response.getContentAsString(), typeOfT);
        } catch (TimeoutException | ExecutionException | JsonParseException e) {
            throw new IOException(e);
        }
    }

    /**
     *
     * @param url
     * @return
     * @throws IOException
     * @throws NotAuthorizedException
     */
    private JsonElement getAqualinkJsonObject(String url)
            throws InterruptedException, IOException, NotAuthorizedException {
        try {
            logger.trace("Trying {}", url);
            ContentResponse response = httpClient.newRequest(url).method(HttpMethod.GET) //
                    .agent(HEADER_AGENT) //
                    .header(HttpHeader.ACCEPT_LANGUAGE, HEADER_ACCEPT_LANGUAGE) //
                    .header(HttpHeader.ACCEPT_ENCODING, HEADER_ACCEPT_ENCODING) //
                    .header(HttpHeader.ACCEPT, HEADER_ACCEPT) //
                    .send();
            logger.trace("Response {}", response.toString());
            if (response.getStatus() == HttpStatus.UNAUTHORIZED_401) {
                throw new NotAuthorizedException(response.getReason());
            }
            if (response.getStatus() != HttpStatus.OK_200) {
                throw new IOException(response.getReason());
            }
            return parser.parse(response.getContentAsString());
        } catch (TimeoutException | ExecutionException | JsonParseException e) {
            throw new IOException(e);
        }
    }

    /////////////// .........Here be dragons...../////////////////////////

    class HomeDeserializer implements JsonDeserializer<Home> {
        @Override
        public Home deserialize(@Nullable JsonElement json, @Nullable Type typeOfT,
                @Nullable JsonDeserializationContext context) throws JsonParseException {
            if (json == null) {
                throw new JsonParseException("No JSON");
            }
            JsonObject jsonObject = json.getAsJsonObject();
            JsonArray homeScreen = jsonObject.getAsJsonArray("home_screen");
            JsonObject home = new JsonObject();
            if (homeScreen != null) {
                homeScreen.forEach(element -> {
                    element.getAsJsonObject().entrySet().forEach(entry -> {
                        home.add(entry.getKey(), entry.getValue());
                    });
                });
                return gsonInternal.fromJson(home, Home.class);
            }
            throw new JsonParseException("Invalid structure for Home class");
        }
    }

    class OneTouchDeserializer implements JsonDeserializer<OneTouch[]> {
        @Override
        public OneTouch[] deserialize(@Nullable JsonElement json, @Nullable Type typeOfT,
                @Nullable JsonDeserializationContext context) throws JsonParseException {
            if (json == null) {
                throw new JsonParseException("No JSON");
            }
            JsonObject jsonObject = json.getAsJsonObject();
            JsonArray oneTouchScreen = jsonObject.getAsJsonArray("onetouch_screen");
            List<OneTouch> list = new ArrayList<OneTouch>();
            if (oneTouchScreen != null) {
                oneTouchScreen.forEach(oneTouchScreenElement -> {
                    oneTouchScreenElement.getAsJsonObject().entrySet().forEach(oneTouchScreenEntry -> {
                        if (oneTouchScreenEntry.getKey().startsWith("onetouch_")) {
                            JsonArray oneTouchArray = oneTouchScreenEntry.getValue().getAsJsonArray();
                            if (oneTouchArray != null) {
                                JsonObject oneTouchJson = new JsonObject();
                                oneTouchJson.add("name", new JsonPrimitive(oneTouchScreenEntry.getKey()));
                                oneTouchArray.forEach(arrayElement -> {
                                    arrayElement.getAsJsonObject().entrySet().forEach(oneTouchEntry -> {
                                        oneTouchJson.add(oneTouchEntry.getKey(), oneTouchEntry.getValue());
                                    });
                                });
                                list.add(gsonInternal.fromJson(oneTouchJson, OneTouch.class));
                            }
                        }
                    });
                });
            }
            return list.toArray(new OneTouch[list.size()]);
        }
    }

    class AuxDeserializer implements JsonDeserializer<Auxiliary[]> {
        @Override
        public Auxiliary[] deserialize(@Nullable JsonElement json, @Nullable Type typeOfT,
                @Nullable JsonDeserializationContext context) throws JsonParseException {
            if (json == null) {
                throw new JsonParseException("No JSON");
            }
            JsonObject jsonObject = json.getAsJsonObject();
            JsonArray auxScreen = jsonObject.getAsJsonArray("devices_screen");
            List<Auxiliary> list = new ArrayList<Auxiliary>();
            if (auxScreen != null) {
                auxScreen.forEach(auxElement -> {
                    auxElement.getAsJsonObject().entrySet().forEach(auxScreenEntry -> {
                        if (auxScreenEntry.getKey().startsWith("aux_")) {
                            JsonArray auxArray = auxScreenEntry.getValue().getAsJsonArray();
                            if (auxArray != null) {
                                JsonObject auxJson = new JsonObject();
                                auxJson.add("name", new JsonPrimitive(auxScreenEntry.getKey()));
                                auxArray.forEach(arrayElement -> {
                                    arrayElement.getAsJsonObject().entrySet().forEach(auxEntry -> {
                                        auxJson.add(auxEntry.getKey(), auxEntry.getValue());
                                    });
                                });
                                list.add(gsonInternal.fromJson(auxJson, Auxiliary.class));
                            }
                        }
                    });
                });
            }
            return list.toArray(new Auxiliary[list.size()]);
        }
    }
}
