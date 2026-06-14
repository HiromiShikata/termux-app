package com.termux.app.apkupdate;

public final class AppVersionComparator {

    public boolean isNewer(String candidateVersionName, String currentVersionName) {
        return compare(candidateVersionName, currentVersionName) > 0;
    }

    public int compare(String leftVersionName, String rightVersionName) {
        int[] left = numericComponents(leftVersionName);
        int[] right = numericComponents(rightVersionName);
        int length = Math.max(left.length, right.length);
        for (int index = 0; index < length; index++) {
            int leftComponent = index < left.length ? left[index] : 0;
            int rightComponent = index < right.length ? right[index] : 0;
            if (leftComponent != rightComponent) {
                return Integer.compare(leftComponent, rightComponent);
            }
        }
        return 0;
    }

    private int[] numericComponents(String versionName) {
        String core = versionName.trim();
        int prereleaseIndex = core.indexOf('-');
        if (prereleaseIndex >= 0) {
            core = core.substring(0, prereleaseIndex);
        }
        int buildMetadataIndex = core.indexOf('+');
        if (buildMetadataIndex >= 0) {
            core = core.substring(0, buildMetadataIndex);
        }
        if (core.isEmpty()) {
            return new int[0];
        }
        String[] parts = core.split("\\.");
        int[] components = new int[parts.length];
        for (int index = 0; index < parts.length; index++) {
            components[index] = parseComponent(parts[index]);
        }
        return components;
    }

    private int parseComponent(String part) {
        StringBuilder digits = new StringBuilder();
        for (int index = 0; index < part.length(); index++) {
            char character = part.charAt(index);
            if (character >= '0' && character <= '9') {
                digits.append(character);
            } else {
                break;
            }
        }
        if (digits.length() == 0) {
            return 0;
        }
        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException numberFormatException) {
            return 0;
        }
    }
}
