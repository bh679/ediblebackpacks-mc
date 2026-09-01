package games.brennan.ediblebackpacks;

import games.brennan.ediblebackpacks.client.ButtonPlacement;
import games.brennan.ediblebackpacks.config.ButtonAnchor;
import games.brennan.ediblebackpacks.menu.BackpackLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure placement math for the open/close button — no Minecraft on the classpath. */
class ButtonPlacementTest {

    /** The left panel's inner (GUI-facing) chrome edge — what the button lines its right edge up with. */
    private static final int PANEL_INNER_EDGE =
        BackpackLayout.LEFT_INNER_X + BackpackLayout.SLOT + BackpackLayout.BORDER;
    /** Top of the panel chrome, which sits BORDER above the first slot row. */
    private static final int PANEL_TOP = BackpackLayout.Y0 - BackpackLayout.BORDER;

    @Test
    void theDefaultAnchorSitsAbovethePanelAndOutsideTheGui() {
        ButtonPlacement.Pos pos = ButtonPlacement.resolve(ButtonAnchor.ABOVE_PANEL, 0, 0);
        assertEquals(PANEL_INNER_EDGE, pos.x() + ButtonPlacement.WIDTH,
                     "right edge flush with the panel's inner chrome edge");
        assertTrue(pos.x() + ButtonPlacement.WIDTH <= 0, "must stay left of the GUI box");
        assertTrue(pos.y() + ButtonPlacement.HEIGHT <= PANEL_TOP, "must clear the panel chrome");
    }

    @Test
    void theDefaultAnchorIsTheSameSpotOnEveryScreen() {
        // The whole point of moving it off the title bar: one spot, inventory or chest.
        assertEquals(ButtonPlacement.resolve(ButtonAnchor.ABOVE_PANEL, 0, 0, false, true),
                     ButtonPlacement.resolve(ButtonAnchor.ABOVE_PANEL, 0, 0, true, true));
    }

    @Test
    void theFaceIsTheGlyphPlusPaddingOnEverySide() {
        // The button draws the glyph inset by PADDING, so the face has to be derived from it —
        // a size that drifts from this leaves the icon off-centre.
        assertEquals(ButtonPlacement.GLYPH + ButtonPlacement.PADDING * 2, ButtonPlacement.WIDTH);
        assertEquals(ButtonPlacement.GLYPH + ButtonPlacement.PADDING * 2, ButtonPlacement.HEIGHT);
    }

    @Test
    void customAnchorUsesTheGivenCoordinates() {
        assertEquals(new ButtonPlacement.Pos(-30, 7), ButtonPlacement.resolve(ButtonAnchor.CUSTOM, -30, 7));
    }

    @Test
    void customIsHonouredEvenWithNoRoomAbove() {
        // The player put it there on purpose; a cramped screen is no reason to move it.
        assertEquals(new ButtonPlacement.Pos(-30, 7),
                     ButtonPlacement.resolve(ButtonAnchor.CUSTOM, -30, 7, true, false));
    }

    @Test
    void customIgnoredByTheDefaultAnchor() {
        assertEquals(ButtonPlacement.resolve(ButtonAnchor.ABOVE_PANEL, 0, 0),
                     ButtonPlacement.resolve(ButtonAnchor.ABOVE_PANEL, 999, 999));
    }

    @Test
    void withNoRoomAboveTheButtonComesBackInsideTheGui() {
        // Nothing is drawn off the top of the screen: a tall GUI in a short window falls back
        // to a spot inside the box, contended or not.
        ButtonPlacement.Pos chest = ButtonPlacement.resolve(ButtonAnchor.ABOVE_PANEL, 0, 0, true, false);
        assertTrue(chest.y() >= 0 && chest.y() + ButtonPlacement.HEIGHT <= 18,
                   "chest fallback must clear the first slot row at y 18");
        assertTrue(chest.x() >= 0 && chest.x() + ButtonPlacement.WIDTH <= 176, "inside the GUI");

        ButtonPlacement.Pos inv = ButtonPlacement.resolve(ButtonAnchor.ABOVE_PANEL, 0, 0, false, false);
        // Portrait viewport ends at x 75; offhand slot spans x 77..93, its chrome from y 61.
        assertTrue(inv.x() > 75, "must not overlap the player portrait");
        assertTrue(inv.x() >= 77 && inv.x() + ButtonPlacement.WIDTH <= 94, "over the offhand slot");
        assertEquals(60, inv.y() + ButtonPlacement.HEIGHT, "stops above the slot chrome");
    }

    @Test
    void roomAboveTracksWhereTheButtonWouldLand() {
        assertFalse(ButtonPlacement.roomAbove(0), "a GUI at the very top has nothing above it");
        assertTrue(ButtonPlacement.roomAbove(-ButtonPlacement.ABOVE_PANEL_Y),
                   "exactly enough room is enough");
        assertFalse(ButtonPlacement.roomAbove(-ButtonPlacement.ABOVE_PANEL_Y - 1), "one pixel short");
    }

    @Test
    void theLegacyCoordinatesStayValidCustomPositions() {
        // They are documented in the config comment as CUSTOM values, so they have to keep
        // describing the landmarks they name.
        assertTrue(ButtonPlacement.OFFHAND_X >= 77
                   && ButtonPlacement.OFFHAND_X + ButtonPlacement.WIDTH <= 94, "over the offhand slot");
        assertTrue(ButtonPlacement.RECIPE_BOOK_X >= 124, "clear of vanilla's recipe button");
        assertTrue(ButtonPlacement.RECIPE_BOOK_X + ButtonPlacement.WIDTH <= 176, "inside the GUI");
        assertTrue(ButtonPlacement.RECIPE_BOOK_Y >= 61
                   && ButtonPlacement.RECIPE_BOOK_Y + ButtonPlacement.HEIGHT <= 79, "centred on it");
    }
}
