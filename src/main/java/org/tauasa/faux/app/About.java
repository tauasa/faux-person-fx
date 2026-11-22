package org.tauasa.faux.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * What the About dialog says. The versions come from the build, through
 * {@code application.properties}, which Maven fills in.
 */
@Component
public class About {

    public static final String NAME = "faux-person-fx";
    public static final String PROJECT_URL = "https://github.com/tauasa/faux-person-fx";
    public static final String WEBSITE_URL = "https://tauasa.org";
    public static final String COPYRIGHT = "Copyright (c) 2026 Tauasa Timoteo";

    private final String version;
    private final String fauxPersonVersion;

    public About(@Value("${faux.fx.version:dev}") String version,
            @Value("${faux.fx.faux-person-version:unknown}") String fauxPersonVersion) {
        this.version = built(version, "dev");
        this.fauxPersonVersion = built(fauxPersonVersion, "unknown");
    }

    /** A run that skipped Maven's filtering, such as from an IDE, still holds {@code @...@}. */
    private static String built(String value, String fallback) {
        return value == null || value.isBlank() || value.startsWith("@") ? fallback : value;
    }

    /** This app's version. */
    public String version() {
        return version;
    }

    /** The faux-person version it was built with. */
    public String fauxPersonVersion() {
        return fauxPersonVersion;
    }
}
