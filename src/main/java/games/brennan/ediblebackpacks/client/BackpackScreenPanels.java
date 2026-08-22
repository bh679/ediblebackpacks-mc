package games.brennan.ediblebackpacks.client;

import games.brennan.ediblebackpacks.EdibleBackpacks;
import games.brennan.ediblebackpacks.menu.BackpackLayout;
import games.brennan.ediblebackpacks.registry.ModAttachments;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;

/**
 * Draws the two backpack panel backgrounds behind the appended slots on the
 * survival {@link InventoryScreen}. Slot items/highlights are rendered by the
 * vanilla screen loop (the slots live in the menu); only the chrome is drawn
 * here. Hidden entirely while the recipe book is open — the slots themselves
 * also deactivate (see {@code BackpackSlot#isActive()}).
 */
@EventBusSubscriber(modid = EdibleBackpacks.MOD_ID, value = Dist.CLIENT)
public final class BackpackScreenPanels {

    // Vanilla-inventory palette: panel face, dark + light bevels, slot inset.
    private static final int FACE = 0xFFC6C6C6;
    private static final int DARK = 0xFF555555;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int SLOT_BG = 0xFF8B8B8B;
    private static final int BORDER = 4;
    /**
     * Staircase depth of the outer border's corner cut, in pixels — matches the
     * vanilla inventory texture, whose outermost corner is a 3-pixel triangle
     * (2px cut on the outer row, 1px on the next).
     */
    private static final int CORNER_CUT = 2;

    private BackpackScreenPanels() {}

    @SubscribeEvent
    public static void onRenderBackground(ContainerScreenEvent.Render.Background event) {
        if (!(event.getContainerScreen() instanceof InventoryScreen screen)) return;
        if (ClientPanelState.recipeBookOpen()) return;

        Player player = screen.getMinecraft().player;
        if (player == null) return;
        int unlocked = player.getData(ModAttachments.BACKPACK).unlocked();
        if (unlocked <= 0) return;

        GuiGraphics g = event.getGuiGraphics();
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();

        drawPanel(g, left, top, unlocked, false);
        drawPanel(g, left, top, unlocked, true);
    }

    private static void drawPanel(GuiGraphics g, int guiLeft, int guiTop, int unlocked, boolean right) {
        int slots = BackpackLayout.unlockedOnPanel(unlocked, right);
        if (slots <= 0) return;

        // Panel chrome per COLUMN (fill is down-then-across, so a partially
        // unlocked panel is a run of full columns + one short column — each
        // column gets its own bevelled face so the chrome hugs the slots).
        int cols = BackpackLayout.columnsFor(slots);
        int base = right ? BackpackLayout.PANEL_SLOTS : 0;
        for (int c = 0; c < cols; c++) {
            int colSlots = BackpackLayout.slotsInColumn(slots, c);
            int cx = guiLeft + BackpackLayout.slotX(base + c * BackpackLayout.ROWS);
            int cy = guiTop + BackpackLayout.Y0;
            int w = BackpackLayout.SLOT;
            int h = colSlots * BackpackLayout.SLOT;

            // Only the panel's true outside corners are rounded. Columns overlap
            // and are drawn inner-to-outer, so column c+1 repaints c's outward
            // border — rounding it there would punch holes into the next column.
            // Exception: a short final column leaves the previous column's
            // outward BOTTOM corner exposed, so that one keeps its round.
            boolean inner = c == 0;
            boolean outerTop = c == cols - 1;
            boolean outerBottom = outerTop || BackpackLayout.slotsInColumn(slots, c + 1) < colSlots;
            boolean tl = right ? inner : outerTop;
            boolean bl = right ? inner : outerBottom;
            boolean tr = right ? outerTop : inner;
            boolean br = right ? outerBottom : inner;

            int x0 = cx - BORDER, y0 = cy - BORDER, x1 = cx + w + BORDER, y1 = cy + h + BORDER;
            fillRounded(g, x0, y0, x1, y1, DARK, CORNER_CUT, tl, tr, bl, br);
            fillRounded(g, x0 + 1, y0 + 1, x1 - 1, y1 - 1, LIGHT, CORNER_CUT - 1, tl, tr, bl, br);
            g.fill(x0 + 2, y0 + 2, x1 - 2, y1 - 2, FACE);
        }

        // Slot insets — only for unlocked slots on this panel.
        for (int i = 0; i < slots; i++) {
            int sx = guiLeft + BackpackLayout.slotX(base + i);
            int sy = guiTop + BackpackLayout.slotY(base + i);
            g.fill(sx - 1, sy - 1, sx + 17, sy + 17, DARK);
            g.fill(sx, sy, sx + 18, sy + 18, LIGHT);
            g.fill(sx, sy, sx + 16, sy + 16, SLOT_BG);
        }
    }

    /**
     * Fills a rect whose flagged corners are stepped back like the vanilla GUI's
     * rounded corners: the outermost row is inset by {@code cut} px, the next by
     * {@code cut - 1}, and so on. The skipped pixels are simply never drawn, so
     * the screen behind shows through — which is what makes the corner read as
     * round. {@code cut <= 0} draws a plain rect.
     */
    private static void fillRounded(GuiGraphics g, int x0, int y0, int x1, int y1, int color,
                                    int cut, boolean tl, boolean tr, boolean bl, boolean br) {
        for (int d = 0; d < cut; d++) {
            int inset = cut - d;
            g.fill(x0 + (tl ? inset : 0), y0 + d, x1 - (tr ? inset : 0), y0 + d + 1, color);
            g.fill(x0 + (bl ? inset : 0), y1 - d - 1, x1 - (br ? inset : 0), y1 - d, color);
        }
        g.fill(x0, y0 + Math.max(cut, 0), x1, y1 - Math.max(cut, 0), color);
    }
}
