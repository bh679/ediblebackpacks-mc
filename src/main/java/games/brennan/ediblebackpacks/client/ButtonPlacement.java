package games.brennan.ediblebackpacks.client;

import games.brennan.ediblebackpacks.config.ButtonAnchor;

/**
 * Pure placement math for the open/close button — coordinates relative to the survival
 * screen's {@code leftPos}/{@code topPos}, like every vanilla slot. No Minecraft imports,
 * so it is unit-tested alongside the layout math.
 *
 * <p>The GUI is 176×166. Landmarks the anchors are pinned to: the player-portrait viewport
 * spans x 26..75, the offhand slot sits at x 77..93 / y 62..78, the crafting grid starts at
 * x 98, and vanilla's own recipe-book button occupies x 104..124 at y 61.</p>
 *
 * <p>None of those landmarks exist in a chest or shulker box, where the same coordinates land
 * on container slots — so those screens get {@link #CONTAINER_X}/{@link #CONTAINER_Y} instead.
 * An explicitly placed button ({@code CUSTOM}) is still honoured verbatim: the player put it
 * there on purpose.</p>
 */
public final class ButtonPlacement {

    /**
     * Chrome on every side of the glyph, which together with it sets the face at 10×10 —
     * {@link #PADDING} and {@link #GLYPH} are the numbers that matter, the size follows from
     * them. That is close to half of vanilla's 20×18 recipe-book button, whose chrome this one
     * wears ({@code BackpackToggleButton}): it sits OVER the inventory rather than beside it, where
     * anything larger reads as a third piece of GUI furniture instead of a small affordance.
     * The glyph shrank with the face rather than filling it — an icon out to the edges read as
     * cramped, not small.
     */
    public static final int PADDING = 2;
    /** The item renderer's only size (16) scaled down — see {@code BackpackToggleButton}. */
    public static final int GLYPH = 6;

    public static final int WIDTH = GLYPH + PADDING * 2;
    public static final int HEIGHT = GLYPH + PADDING * 2;

    /**
     * Above the offhand slot, centred on it (77..93) and well clear of the portrait viewport
     * ending at 75. Ending at y 60 still leaves the slot's chrome (from 61) untouched — the
     * button lost its height off the top, so it sits where it always did.
     */
    public static final int OFFHAND_X = 80;
    public static final int OFFHAND_Y = 50;

    /** Two pixels right of vanilla's recipe-book button (x 104..124, y 61..79), centred on it. */
    public static final int RECIPE_BOOK_X = 126;
    public static final int RECIPE_BOOK_Y = 65;

    /**
     * Chest-shaped screens: the free right end of the title bar. Those GUIs are also 176 wide,
     * their title bar runs y 1..17 and the first slot row starts at y 18, so a 10px-tall button
     * centred at y 4 clears both; x 160 keeps the 6px the chrome needs on the right.
     */
    public static final int CONTAINER_X = 160;
    public static final int CONTAINER_Y = 4;

    /** A button position relative to the screen's top-left corner. */
    public record Pos(int x, int y) {}

    private ButtonPlacement() {}

    /** The survival inventory screen's placement. */
    public static Pos resolve(ButtonAnchor anchor, int customX, int customY) {
        return resolve(anchor, customX, customY, false);
    }

    /**
     * @param containerScreen true on a chest-shaped screen, where the inventory-only landmarks
     *                        the two preset anchors name are not there to sit beside
     */
    public static Pos resolve(ButtonAnchor anchor, int customX, int customY, boolean containerScreen) {
        return switch (anchor) {
            case RECIPE_BOOK -> containerScreen ? containerPos() : new Pos(RECIPE_BOOK_X, RECIPE_BOOK_Y);
            case CUSTOM -> new Pos(customX, customY);
            case OFFHAND -> containerScreen ? containerPos() : new Pos(OFFHAND_X, OFFHAND_Y);
        };
    }

    private static Pos containerPos() {
        return new Pos(CONTAINER_X, CONTAINER_Y);
    }
}
