package com.termux.app.ownercall;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.terminal.HostTmuxSessionName;
import com.termux.shared.logger.Logger;

import java.net.MalformedURLException;
import java.net.URL;

public final class OwnerCallFileUrl {

    private static final String LOG_TAG = "OwnerCallFileUrl";
    private static final String ACCESS_TOKEN_PARAMETER = "k";
    private static final String QUERY_PARAMETER_SEPARATOR = "&";
    private static final String QUERY_VALUE_SEPARATOR = "=";
    private static final char PATH_SEPARATOR = '/';

    private OwnerCallFileUrl() {
    }

    @Nullable
    public static String resolve(@Nullable String sessionDefinitionUrl,
                                 @Nullable String projectCode,
                                 @Nullable String sessionName) {
        if (sessionDefinitionUrl == null || sessionDefinitionUrl.trim().isEmpty()
            || sessionName == null || sessionName.isEmpty()) {
            return null;
        }
        URL sessionDefinition;
        try {
            sessionDefinition = new URL(sessionDefinitionUrl.trim());
        } catch (MalformedURLException unusableSessionDefinitionUrl) {
            Logger.logWarn(LOG_TAG, "the stored session definition URL is not a URL, so the owner "
                + "call file of " + sessionName + " cannot be addressed");
            return null;
        }
        String accessToken = accessTokenOf(sessionDefinition.getQuery());
        return sessionDefinition.getProtocol() + "://" + sessionDefinition.getAuthority()
            + directoryOf(sessionDefinition.getPath())
            + OwnerCallFilePath.of(projectCode, HostTmuxSessionName.normalize(sessionName))
            + (accessToken == null ? ""
            : "?" + ACCESS_TOKEN_PARAMETER + QUERY_VALUE_SEPARATOR + accessToken);
    }

    @NonNull
    private static String directoryOf(@Nullable String sessionDefinitionPath) {
        if (sessionDefinitionPath == null) {
            return String.valueOf(PATH_SEPARATOR);
        }
        int lastSeparator = sessionDefinitionPath.lastIndexOf(PATH_SEPARATOR);
        if (lastSeparator < 0) {
            return String.valueOf(PATH_SEPARATOR);
        }
        return sessionDefinitionPath.substring(0, lastSeparator + 1);
    }

    @Nullable
    private static String accessTokenOf(@Nullable String query) {
        if (query == null || query.isEmpty()) {
            return null;
        }
        for (String parameter : query.split(QUERY_PARAMETER_SEPARATOR)) {
            String prefix = ACCESS_TOKEN_PARAMETER + QUERY_VALUE_SEPARATOR;
            if (parameter.startsWith(prefix)) {
                return parameter.substring(prefix.length());
            }
        }
        return null;
    }

    @NonNull
    public static String describe(@Nullable String ownerCallFileUrl) {
        if (ownerCallFileUrl == null) {
            return "no owner call file";
        }
        int queryStart = ownerCallFileUrl.indexOf('?');
        return queryStart < 0 ? ownerCallFileUrl : ownerCallFileUrl.substring(0, queryStart);
    }
}
