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
 */
public final class ButtonPlacement {

    public static final int WIDTH = 20;
    public static final int HEIGHT = 18;

    /**
     * Above the offhand slot. A 20px button centred on the slot would start at 75, one pixel
     * into the portrait viewport, so it starts at 76; ending at y 60 leaves the slot's chrome
     * (from 61) untouched.
     */
    public static final int OFFHAND_X = 76;
    public static final int OFFHAND_Y = 42;

    /** Two pixels right of the recipe-book button, level with it. */
    public static final int RECIPE_BOOK_X = 126;
    public static final int RECIPE_BOOK_Y = 61;

    /** A button position relative to the screen's top-left corner. */
    public record Pos(int x, int y) {}

    private ButtonPlacement() {}

    public static Pos resolve(ButtonAnchor anchor, int customX, int customY) {
        return switch (anchor) {
            case RECIPE_BOOK -> new Pos(RECIPE_BOOK_X, RECIPE_BOOK_Y);
            case CUSTOM -> new Pos(customX, customY);
            case OFFHAND -> new Pos(OFFHAND_X, OFFHAND_Y);
        };
    }
}
