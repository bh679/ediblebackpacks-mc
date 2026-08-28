package games.brennan.ediblebackpacks.config;

/** Where the open/close button sits on the survival inventory screen. */
public enum ButtonAnchor {
    /** Directly above the offhand (shield) slot — the default. */
    OFFHAND,
    /** Immediately right of the vanilla recipe-book button. */
    RECIPE_BOOK,
    /** Wherever {@code buttonX}/{@code buttonY} say, relative to the GUI's top-left corner. */
    CUSTOM
}
