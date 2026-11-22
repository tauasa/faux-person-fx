package org.tauasa.faux.app;

import java.util.Locale;
import java.util.Optional;

/** Help > Appearance: how the app looks. */
public enum Appearance {
    LIGHT("Light"),
    DARK("Dark"),
    /** Follows the operating system's light or dark setting. */
    SYSTEM("System");

    /** The stylesheet that turns JavaFX's light Modena theme dark. */
    static final String DARK_STYLESHEET = Appearance.class.getResource("dark.css").toExternalForm();

    private final String label;

    Appearance(String label) {
        this.label = label;
    }

    /** The menu item's text. */
    public String label() {
        return label;
    }

    /**
     * Whether this appearance shows dark.
     *
     * @param systemIsDark whether the operating system is set to dark, for {@link #SYSTEM}
     */
    public boolean isDark(boolean systemIsDark) {
        return this == DARK || (this == SYSTEM && systemIsDark);
    }

    /** Reads a stored name, ignoring case and spaces. */
    static Optional<Appearance> parse(String text) {
        if (text == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(text.strip().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
