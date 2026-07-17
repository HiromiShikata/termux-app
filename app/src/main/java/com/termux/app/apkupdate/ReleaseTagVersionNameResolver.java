package com.termux.app.apkupdate;

public final class ReleaseTagVersionNameResolver {

    public String resolveFromTag(String tagName) {
        String version = tagName;
        if (version.startsWith("v") || version.startsWith("V")) {
            version = version.substring(1);
        }
        int buildMetadataIndex = version.indexOf('+');
        if (buildMetadataIndex >= 0) {
            version = version.substring(0, buildMetadataIndex);
        }
        return version;
    }
}
