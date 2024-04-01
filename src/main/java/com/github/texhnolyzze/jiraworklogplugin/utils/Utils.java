package com.github.texhnolyzze.jiraworklogplugin.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public final class Utils {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().
        setSerializationInclusion(JsonInclude.Include.NON_NULL).
        registerModule(new JavaTimeModule());

    private Utils() {
        throw new UnsupportedOperationException();
    }

    public static boolean isValidUrl(final String url) {
        try {
            new URL(url).toURI();
        } catch (URISyntaxException | MalformedURLException ignored) {
            return false;
        }
        return true;
    }

}
