package games.brennan.ediblebackpacks.config;

/** Where the open/close button sits on a screen carrying the backpack panels. */
public enum ButtonAnchor {
    /**
     * Just above the left panel, outside the GUI box — the default, and the same place on
     * every screen. Inside the GUI there is no spot that is free everywhere: the inventory's
     * landmarks (offhand slot, recipe button) are container slots in a chest, and the chest
     * title bar is where sorting mods put their own buttons.
     */
    ABOVE_PANEL,
    /**
     * Wherever {@code buttonX}/{@code buttonY} say, relative to the GUI's top-left corner.
     * The presets this enum used to carry live on as coordinates to paste in — see
     * {@code client/ButtonPlacement}'s {@code OFFHAND_*} and {@code RECIPE_BOOK_*}.
     */
    CUSTOM
}
