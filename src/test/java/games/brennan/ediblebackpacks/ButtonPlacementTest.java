package games.brennan.ediblebackpacks;

import games.brennan.ediblebackpacks.client.ButtonPlacement;
import games.brennan.ediblebackpacks.config.ButtonAnchor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure placement math for the open/close button — no Minecraft on the classpath. */
class ButtonPlacementTest {

    @Test
    void offhandAnchorSitsAboveTheShieldSlotAndClearsThePortrait() {
        ButtonPlacement.Pos pos = ButtonPlacement.resolve(ButtonAnchor.OFFHAND, 0, 0);
        // Portrait viewport ends at 75; offhand slot spans x 77..93, chrome from y 61.
        assertTrue(pos.x() > 75, "must not overlap the player portrait");
        assertTrue(pos.x() <= 77, "must still sit over the offhand slot");
        assertEquals(60, pos.y() + ButtonPlacement.HEIGHT, "bottom edge stops above the slot chrome");
    }

    @Test
    void recipeBookAnchorClearsTheVanillaButton() {
        ButtonPlacement.Pos pos = ButtonPlacement.resolve(ButtonAnchor.RECIPE_BOOK, 0, 0);
        // Vanilla's own button is x 104..124 at y 61.
        assertTrue(pos.x() >= 124, "must not overlap vanilla's recipe button");
        assertEquals(61, pos.y(), "level with it");
        assertTrue(pos.x() + ButtonPlacement.WIDTH <= 176, "stays inside the GUI");
    }

    @Test
    void customAnchorUsesTheGivenCoordinates() {
        assertEquals(new ButtonPlacement.Pos(-30, 7), ButtonPlacement.resolve(ButtonAnchor.CUSTOM, -30, 7));
    }

    @Test
    void customIgnoredByTheFixedAnchors() {
        assertEquals(ButtonPlacement.resolve(ButtonAnchor.OFFHAND, 0, 0),
                     ButtonPlacement.resolve(ButtonAnchor.OFFHAND, 999, 999));
    }
}
