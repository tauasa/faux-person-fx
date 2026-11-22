package org.tauasa.faux.app;

/** What a tab generates. */
public enum Kind {
    NAMES("Names", "name", "names"),
    COMPANIES("Companies", "company", "companies"),
    PEOPLE("People", "person", "people");

    private final String title;
    private final String singular;
    private final String plural;

    Kind(String title, String singular, String plural) {
        this.title = title;
        this.singular = singular;
        this.plural = plural;
    }

    /** The tab's title. */
    public String title() {
        return title;
    }

    /** "1 name", "2 names". */
    public String count(int n) {
        return n + " " + (n == 1 ? singular : plural);
    }
}
