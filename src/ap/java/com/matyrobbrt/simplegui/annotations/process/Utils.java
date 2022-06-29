package com.matyrobbrt.simplegui.annotations.process;

import java.util.Locale;

public class Utils {
    public static String removeMarkerAndDecapitalize(String str, String marker) {
        final var newStr = removeMarker(str, marker);
        return newStr.substring(0, 1).toLowerCase(Locale.ROOT) + newStr.substring(1);
    }
    public static String removeMarker(String str, String marker) {
        return str.substring(marker.length());
    }
    public static String stripMarkers(String str, boolean decap, String... markers) {
        for (final var marker : markers)
            if (str.startsWith(marker))
                return decap ? removeMarkerAndDecapitalize(str, marker) : removeMarker(str, marker);
        return str;
    }
}
