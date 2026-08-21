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
            g.fill(cx - BORDER, cy - BORDER, cx + w + BORDER, cy + h + BORDER, DARK);
            g.fill(cx - BORDER + 1, cy - BORDER + 1, cx + w + BORDER - 1, cy + h + BORDER - 1, LIGHT);
            g.fill(cx - BORDER + 2, cy - BORDER + 2, cx + w + BORDER - 2, cy + h + BORDER - 2, FACE);
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
}
