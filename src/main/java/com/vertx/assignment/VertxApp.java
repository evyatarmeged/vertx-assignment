package com.vertx.assignment;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class VertxApp extends AbstractVerticle {

    // Constants
    private static final String VALUE = "value";
    private static final String LEXICAL = "lexical";
    private static final String ERROR = "error";
    private static final String TEXT = "text";
    private static final String CONTENT_TYPE = "content-type";
    private static final String JSON_UTF = "application/json; charset=utf-8";
    private static final String POST_ERROR = "Only POST requests are allowed.";
    private static final String TYPE_ERROR = "Parameter `text` must be of type String.";
    private static final String DECODE_ERROR = "Request body must be valid JSON.";
    private static final String INVALID_REQUEST = "Invalid request. Parameter `text` should" +
        " not contain special chars, digits or spaces and should not be null.";
    private static final Pattern FORBIDDEN_CHARS = Pattern.compile("[^a-zA-z]");


    // Call respondToBadRequest with message `POST_ERROR` upon receiving GET requests
    private void analyzeGetHandler(RoutingContext context) {
        respondToBadRequest(context, POST_ERROR);
    }

    /** POST requests handler.
     * Creates a new HashMap to be used as responseObject.
     * Extracts request body as JSON, gets parameter `text` from it and validates it.
     * Computes param's char value, get closest word by char value using helperUtils
     * and add its to responseObject.
     * Gets closest word by Levenshtein distance using helperUtils findClosestWord method, and also appends it
     * to responseObject.
     * Cache param in helperUtils.
     * Returns JSON response.
     * @param context: RoutingContext object to create response for.
     * */
    private void analyzePostHandler(RoutingContext context) {
        Map<String, Object> responseObject = new HashMap<>();
        // Get request body as JSON. DecodeException handled
        try {
            JsonObject reqBody = context.getBodyAsJson();
            try {
                // Extract String value from parameter `text`. ClassCastException handled
                String param = reqBody.getString(TEXT);
                // Validate param
                if (!validateParam(param)) {
                    respondToBadRequest(context, INVALID_REQUEST);
                // Perform main handler logic
                } else {
                    // Get character values
                    int charValues = HelperUtils.computeCharactersValue(param);

                    // Get closest characters value from helperUtils.wordValueMap
                    int closestValueIndex = HelperUtils.getIndexOfMatchingValuedWordSet(charValues);

                    // Get random word from word Set and put it in responseObject
                    Object closestValueWord = HelperUtils.randomWordFromSet(closestValueIndex);
                    responseObject.put(VALUE, closestValueWord);

                    // Get closest word by Levenshtein distance and put it in responseObject
                    responseObject.put(LEXICAL, HelperUtils.findClosestWord(param));

                    // Cache word in helperUtils
                    HelperUtils.cacheWord(param, charValues);

                    // Wrap response
                    context
                        .response()
                        .putHeader(CONTENT_TYPE, JSON_UTF)
                        .end(Json.encodePrettily(responseObject));
                }
            } catch (ClassCastException exception) {
                respondToBadRequest(context, TYPE_ERROR);
            }
        } catch (DecodeException exception) {
            respondToBadRequest(context, DECODE_ERROR);
        }
    }

    /**
     * Check that parameter does not contain digits, spaces or special chars.
     * @param param: Parameter from POST request.
     * @return true if parameter is valid, false otherwise.
     * */
    private boolean validateParam(String param) {
        boolean validated = true;
        try {
            Matcher matcher = FORBIDDEN_CHARS.matcher(param);
            if (matcher.find()) {
                validated = false;
            }
        // Catch NPE in case of empty parameter
        } catch (NullPointerException exception) {
            validated = false;
        }
        return validated;
    }

    /**
     * Respond to a bad request with a status code of 400 and a custom error message.
     * @param context: RoutingContext object to create response for.
     * @param message: Custom error message to be sent in the response.
     * */
    private void respondToBadRequest(RoutingContext context, String message) {
        Map<String, String> responseObject = new HashMap<>();
        responseObject.put(ERROR, message);
        context
            .response()
            .putHeader(CONTENT_TYPE, JSON_UTF)
            .setStatusCode(400)
            .end(Json.encodePrettily(responseObject));
    }

    /**
     * AbstractVerticle interface function.
     * Create a Router object with a BodyHandler and define handlers for route `/analyze`
     * for both GET & POST requests. Create the server and listen on defined port.
     * */
    @Override
    public void start() throws Exception {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.post("/analyze").handler(this::analyzePostHandler);
        router.get("/analyze").handler(this::analyzeGetHandler);

        // Check for SystemProperty "PORT" existence and validity or use a default 8080 port.
        String envPort = System.getProperty("PORT");
        int port = "".equals(envPort) || !envPort.matches("[0-9]+") ? 8080 : Integer.parseInt(envPort);
        vertx
            .createHttpServer()
            .requestHandler(router::accept)
            .listen(port);
        System.out.println("Listening on port " + port);
    }
}
