package games.brennan.ediblebackpacks;

import games.brennan.ediblebackpacks.menu.BackpackLayout;
import games.brennan.ediblebackpacks.menu.EffectPlacement;
import games.brennan.ediblebackpacks.menu.EffectPlacement.Placement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectPlacementTest {

    /** A roomy window: 640×400 with the 176×166 GUI centred. */
    private static final int WIDE_SCREEN = 640;
    private static final int TALL_GUI_TOP = (400 - 166) / 2;   // 117
    /** A cramped window: the GUI nearly fills the height. */
    private static final int SHORT_GUI_TOP = 10;

    private static int vanillaLeft(int guiLeft) {
        return guiLeft + 176 + 2;
    }

    @Test
    void leavesVanillaAloneWithNoRightPanelSlots() {
        int guiLeft = 100;
        // 54 unlocked fills the LEFT panel exactly; the right one is still empty.
        assertFalse(EffectPlacement.rightPanelPresent(54));
        Placement p = EffectPlacement.resolve(guiLeft, TALL_GUI_TOP, WIDE_SCREEN,
                                              vanillaLeft(guiLeft), false, 54, 3);
        assertEquals(vanillaLeft(guiLeft), p.left());
        assertEquals(TALL_GUI_TOP, p.top());
    }

    @Test
    void leavesVanillaAloneWithNoEffects() {
        int guiLeft = 100;
        Placement p = EffectPlacement.resolve(guiLeft, TALL_GUI_TOP, WIDE_SCREEN,
                                              vanillaLeft(guiLeft), false, 60, 0);
        assertEquals(TALL_GUI_TOP, p.top());
    }

    @Test
    void liftsTheStackClearOfThePanelTop() {
        int guiLeft = 100;
        int count = 2;
        Placement p = EffectPlacement.resolve(guiLeft, TALL_GUI_TOP, WIDE_SCREEN,
                                              vanillaLeft(guiLeft), false, 60, count);
        // Stays in vanilla's column, but the whole stack now ends above the panel.
        assertEquals(vanillaLeft(guiLeft), p.left());
        assertTrue(p.top() + EffectPlacement.stackHeight(count)
                       <= EffectPlacement.panelTop(TALL_GUI_TOP),
                   "stack still reaches into the panel");
        assertTrue(p.top() >= EffectPlacement.MIN_TOP, "stack pushed off the top of the screen");
    }

    @Test
    void slidesPastThePanelWhenTheScreenIsTooShort() {
        int guiLeft = 100;
        assertFalse(EffectPlacement.fitsAbove(SHORT_GUI_TOP, 4));
        Placement p = EffectPlacement.resolve(guiLeft, SHORT_GUI_TOP, WIDE_SCREEN,
                                              vanillaLeft(guiLeft), false, 108, 4);
        assertEquals(SHORT_GUI_TOP, p.top(), "vertical position should be vanilla's again");
        assertTrue(p.left() >= EffectPlacement.panelRight(guiLeft, 108),
                   "stack still overlaps the panel");
    }

    @Test
    void goesCompactWhenTheStripLeftBesideThePanelIsNarrow() {
        int guiLeft = 100;
        int screenWidth = EffectPlacement.panelRight(guiLeft, 108) + EffectPlacement.GAP
                          + EffectPlacement.NARROW;
        Placement p = EffectPlacement.resolve(guiLeft, SHORT_GUI_TOP, screenWidth,
                                              vanillaLeft(guiLeft), false, 108, 4);
        assertTrue(p.compact());
        assertEquals(screenWidth - EffectPlacement.NARROW, p.left());
    }

    @Test
    void pushesToTheTopWhenThereIsRoomNeitherAboveNorBeside() {
        int guiLeft = 100;
        int screenWidth = EffectPlacement.panelRight(guiLeft, 108) + 1;
        Placement p = EffectPlacement.resolve(guiLeft, SHORT_GUI_TOP, screenWidth,
                                              vanillaLeft(guiLeft), false, 108, 6);
        assertEquals(vanillaLeft(guiLeft), p.left());
        assertEquals(EffectPlacement.MIN_TOP, p.top());
    }

    @Test
    void panelRightEdgeTracksTheUnlockedColumns() {
        int guiLeft = 0;
        // One right-panel slot ⇒ one column of chrome.
        assertEquals(BackpackLayout.RIGHT_INNER_X + BackpackLayout.SLOT + BackpackLayout.BORDER,
                     EffectPlacement.panelRight(guiLeft, BackpackLayout.PANEL_SLOTS + 1));
        // Fully unlocked ⇒ all six columns.
        assertEquals(BackpackLayout.RIGHT_INNER_X + 6 * BackpackLayout.SLOT + BackpackLayout.BORDER,
                     EffectPlacement.panelRight(guiLeft, BackpackLayout.MAX_SLOTS));
        // No right-panel slots ⇒ no panel to dodge.
        assertEquals(guiLeft, EffectPlacement.panelRight(guiLeft, BackpackLayout.PANEL_SLOTS));
    }

    @Test
    void stackHeightMatchesVanillaStride() {
        assertEquals(32, EffectPlacement.stackHeight(1));
        assertEquals(33 * 4 + 32, EffectPlacement.stackHeight(5));
        // Past 5 effects vanilla compresses the stride to keep the stack bounded.
        assertEquals(132 / 5, EffectPlacement.stride(6));
        assertTrue(EffectPlacement.stackHeight(6) < EffectPlacement.stackHeight(5) + 33);
    }
}
