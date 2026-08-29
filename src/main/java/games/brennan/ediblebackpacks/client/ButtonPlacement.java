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
     * One pixel of button chrome around the 16x16 item glyph. Deliberately smaller than
     * vanilla's own 20x18 recipe-book button: this one sits over the inventory rather than
     * beside it, and at the vanilla size it read as a third piece of GUI furniture rather
     * than a small affordance.
     */
    public static final int WIDTH = 18;
    public static final int HEIGHT = 16;

    /**
     * Above the offhand slot. Centred on the slot (77..93) an 18px button starts at 76, which
     * also clears the portrait viewport ending at 75; ending at y 60 leaves the slot's chrome
     * (from 61) untouched.
     */
    public static final int OFFHAND_X = 76;
    public static final int OFFHAND_Y = 44;

    /** Two pixels right of the recipe-book button, level with it. */
    public static final int RECIPE_BOOK_X = 126;
    public static final int RECIPE_BOOK_Y = 61;

    /**
     * Chest-shaped screens: the free right end of the title bar. Those GUIs are also 176 wide
     * and their first slot row starts at y 18, so a 16px-tall button at y 1 ends flush against
     * it, and x 152 leaves the 6px the chrome needs on the right.
     */
    public static final int CONTAINER_X = 152;
    public static final int CONTAINER_Y = 1;

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
