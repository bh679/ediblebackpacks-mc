package games.brennan.ediblebackpacks.client;

import games.brennan.ediblebackpacks.config.ButtonAnchor;
import games.brennan.ediblebackpacks.menu.BackpackLayout;

/**
 * Pure placement math for the open/close button — coordinates relative to the screen's
 * {@code leftPos}/{@code topPos}, like every vanilla slot. No Minecraft imports, so it is
 * unit-tested alongside the layout math.
 *
 * <p>The button sits above the left backpack panel, outside the 176-wide GUI box. Nowhere
 * inside that box is free on every screen the panels appear on: the survival inventory's
 * landmarks (the offhand slot, vanilla's recipe-book button) are ordinary container slots in a
 * chest, and the chest title bar — where this used to go — is the strip inventory-sorting mods
 * put their own button row on. Beside the panel there is nothing to collide with.</p>
 *
 * <p>The exception is a screen with no room above it, and {@code CUSTOM}, which is honoured
 * verbatim: the player put it there on purpose.</p>
 */
public final class ButtonPlacement {

    /**
     * Chrome on every side of the glyph, which together with it sets the face at 10×10 —
     * {@link #PADDING} and {@link #GLYPH} are the numbers that matter, the size follows from
     * them. That is close to half of vanilla's 20×18 recipe-book button, whose chrome this one
     * wears ({@code BackpackToggleButton}): it sits beside the panels rather than being a
     * window of its own, where anything larger reads as a third piece of GUI furniture instead
     * of a small affordance. The glyph shrank with the face rather than filling it — an icon
     * out to the edges read as cramped, not small.
     */
    public static final int PADDING = 2;
    /** The item renderer's only size (16) scaled down — see {@code BackpackToggleButton}. */
    public static final int GLYPH = 6;

    public static final int WIDTH = GLYPH + PADDING * 2;
    public static final int HEIGHT = GLYPH + PADDING * 2;

    /** Air between the button's bottom edge and the panel chrome under it. */
    private static final int PANEL_GAP = 1;

    /**
     * Right edge flush with the left panel's inner chrome edge, so the button lines up with
     * the panel below it rather than floating between it and the GUI.
     */
    public static final int ABOVE_PANEL_X =
        BackpackLayout.LEFT_INNER_X + BackpackLayout.SLOT + BackpackLayout.BORDER - WIDTH;
    /** Clear of the panel's top chrome, which starts {@link BackpackLayout#BORDER} above the slots. */
    public static final int ABOVE_PANEL_Y =
        BackpackLayout.Y0 - BackpackLayout.BORDER - HEIGHT - PANEL_GAP;

    /**
     * Survival-inventory fallback for a screen with no room above it: over the offhand slot
     * (x 77..93 / y 62..78), clear of the portrait viewport ending at x 75, and stopping above
     * the slot's chrome at y 61. Was a preset anchor until the button moved beside the panel;
     * kept as coordinates to paste into {@code CUSTOM}.
     */
    public static final int OFFHAND_X = 80;
    public static final int OFFHAND_Y = 50;

    /**
     * Two pixels right of vanilla's recipe-book button (x 104..124, y 61..79), centred on it.
     * Kept for the same reason as {@link #OFFHAND_X}.
     */
    public static final int RECIPE_BOOK_X = 126;
    public static final int RECIPE_BOOK_Y = 65;

    /**
     * Chest-shaped fallback for a screen with no room above it: the right end of the title bar.
     * Those GUIs are 176 wide, their title bar runs y 1..17 and the first slot row starts at
     * y 18, so a 10px-tall button centred at y 4 clears both; x 160 keeps the 6px the chrome
     * needs on the right. Contended space — a sorting mod's buttons live here too — which is
     * why it is only what a cramped screen falls back to.
     */
    public static final int CONTAINER_X = 160;
    public static final int CONTAINER_Y = 4;

    /** A button position relative to the screen's top-left corner. */
    public record Pos(int x, int y) {}

    private ButtonPlacement() {}

    /** Placement on a screen with room above the GUI, which is nearly all of them. */
    public static Pos resolve(ButtonAnchor anchor, int customX, int customY) {
        return resolve(anchor, customX, customY, false, true);
    }

    /**
     * @param containerScreen true on a chest-shaped screen, which has no offhand slot to fall
     *                        back onto
     * @param roomAbove       false when the GUI is tall enough, or the window short enough,
     *                        that {@link #ABOVE_PANEL_Y} would run off the top of the screen —
     *                        the caller knows {@code topPos}, this class does not
     */
    public static Pos resolve(ButtonAnchor anchor, int customX, int customY,
                              boolean containerScreen, boolean roomAbove) {
        if (anchor == ButtonAnchor.CUSTOM) return new Pos(customX, customY);
        if (roomAbove) return new Pos(ABOVE_PANEL_X, ABOVE_PANEL_Y);
        return containerScreen
            ? new Pos(CONTAINER_X, CONTAINER_Y)
            : new Pos(OFFHAND_X, OFFHAND_Y);
    }

    /** True when a GUI whose top edge is {@code guiTop} px down the screen has room above it. */
    public static boolean roomAbove(int guiTop) {
        return guiTop + ABOVE_PANEL_Y >= 0;
    }
}
