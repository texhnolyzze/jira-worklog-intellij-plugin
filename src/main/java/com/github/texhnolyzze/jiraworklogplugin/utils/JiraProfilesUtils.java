package com.github.texhnolyzze.jiraworklogplugin.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.ide.util.PropertiesComponent;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps an application-wide (across projects) list of Jira URLs the user has successfully connected to.
 * Credentials for each URL are stored in the OS keychain via {@link com.intellij.ide.passwordSafe.PasswordSafe},
 * keyed by URL, so a known URL effectively is a known URL/credentials pair.
 * The list is ordered by most recent successful connection first.
 */
public final class JiraProfilesUtils {

    public static final String KNOWN_URLS_PROPERTY = "jiraWorklogPlugin.knownJiraUrls";

    private JiraProfilesUtils() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    public static List<String> getKnownUrls() {
        final String json = PropertiesComponent.getInstance().getValue(KNOWN_URLS_PROPERTY);
        if (StringUtils.isBlank(json)) {
            return new ArrayList<>();
        }
        try {
            return Utils.OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
        } catch (final JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    public static void addKnownUrl(final @NotNull String url) {
        final List<String> urls = new ArrayList<>(getKnownUrls());
        urls.remove(url);
        urls.add(0, url);
        try {
            PropertiesComponent.getInstance().setValue(
                KNOWN_URLS_PROPERTY,
                Utils.OBJECT_MAPPER.writeValueAsString(urls)
            );
        } catch (final JsonProcessingException e) {
            // nothing to do - in-memory list of this session is unaffected
        }
    }

}
