package games.brennan.ediblebackpacks.client;

import games.brennan.ediblebackpacks.EdibleBackpacks;
import games.brennan.ediblebackpacks.config.EBClientConfig;
import games.brennan.ediblebackpacks.menu.BackpackLayout;
import games.brennan.ediblebackpacks.registry.ModAttachments;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the two backpack panel backgrounds behind the appended slots on the
 * survival {@link InventoryScreen}. Slot items/highlights are rendered by the
 * vanilla screen loop (the slots live in the menu); only the chrome is drawn
 * here.
 *
 * <p>Also the place the panels get positioned for the frame: {@link BackpackPanelLayout}
 * decides whether the recipe book is forcing them right of the GUI, and the chrome below
 * is drawn in whatever mode it picked, from the same {@link BackpackLayout} math the slots
 * use — so the two cannot drift apart.</p>
 */
@EventBusSubscriber(modid = EdibleBackpacks.MOD_ID, value = Dist.CLIENT)
public final class BackpackScreenPanels {

    // Vanilla-inventory palette, sampled from gui/container/inventory.png.
    private static final int FACE = 0xFFC6C6C6;
    /** 1px outer border, all the way around. */
    private static final int OUTLINE = 0xFF000000;
    /** Bevel highlight — the window is lit from the top-left, so top + left. */
    private static final int LIGHT = 0xFFFFFFFF;
    /** Bevel shadow — the sides facing away from the light: bottom + right. */
    private static final int SHADE = 0xFF555555;
    private static final int SLOT_BG = 0xFF8B8B8B;
    /**
     * Staircase depth of the outer border's corner cut, in pixels — matches the
     * vanilla inventory texture, whose outermost corner is a 3-pixel triangle
     * (2px cut on the outer row, 1px on the next).
     */
    private static final int CORNER_CUT = 2;

    /**
     * The toggle button of the screen currently open, so the render pass can keep it in
     * step with the GUI (the recipe book slides everything right) and hide it for a player
     * who has no backpack yet. Replaced on every screen init; cleared with the screen.
     */
    private static BackpackToggleButton button;

    private BackpackScreenPanels() {}

    @SubscribeEvent
    public static void onRenderBackground(ContainerScreenEvent.Render.Background event) {
        if (!(event.getContainerScreen() instanceof InventoryScreen screen)) return;

        Player player = screen.getMinecraft().player;
        if (player == null) return;
        int unlocked = player.getData(ModAttachments.BACKPACK).unlocked();
        placeButton(screen, unlocked);

        // Place the slots before anything reads their position — this runs ahead of the
        // frame's input handling, so hit-testing matches what is drawn below.
        BackpackPanelLayout.Mode mode = BackpackPanelLayout.update(screen, unlocked);
        // CLOSED and HIDDEN both mean "draw nothing" — see BackpackPanelLayout#hidden.
        if (unlocked <= 0 || BackpackPanelLayout.hidden()) return;

        GuiGraphics g = event.getGuiGraphics();
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        boolean bookMode = mode == BackpackPanelLayout.Mode.BOOK;

        drawPanel(g, left, top, unlocked, false, bookMode);
        drawPanel(g, left, top, unlocked, true, bookMode);
    }

    /**
     * Adds the toggle button and keeps the slot positions right for the first frame, before
     * any render runs.
     */
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        button = null;
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;
        Player player = screen.getMinecraft().player;
        if (player == null) return;
        // Spectators get no crafting GUI at all — vanilla skips its own recipe button there.
        // Turning the button off leaves the hotkey as the only way in, which is the point.
        if (!player.isSpectator() && EBClientConfig.buttonEnabled()) {
            button = new BackpackToggleButton();
            event.addListener(button);
        }
        int unlocked = player.getData(ModAttachments.BACKPACK).unlocked();
        placeButton(screen, unlocked);
        BackpackPanelLayout.update(screen, unlocked);
    }

    /**
     * Follows the GUI: toggling the recipe book moves {@code leftPos}, and vanilla only
     * repositions its own widgets. The anchor is re-read every frame so a config edit lands
     * without reopening the screen. Hidden entirely until the player has eaten a backpack —
     * there is nothing to open yet.
     */
    private static void placeButton(InventoryScreen screen, int unlocked) {
        if (button == null) return;
        ButtonPlacement.Pos pos = ButtonPlacement.resolve(
            EBClientConfig.buttonAnchor(), EBClientConfig.buttonX(), EBClientConfig.buttonY());
        button.setPosition(screen.getGuiLeft() + pos.x(), screen.getGuiTop() + pos.y());
        button.visible = unlocked > 0;
        button.active = unlocked > 0;
        button.refreshTooltip();
    }

    private static void drawPanel(GuiGraphics g, int guiLeft, int guiTop, int unlocked,
                                  boolean right, boolean bookMode) {
        int slots = BackpackLayout.unlockedOnPanel(unlocked, right);
        if (slots <= 0) return;

        int base = right ? BackpackLayout.PANEL_SLOTS : 0;
        List<Block> blocks = silhouette(guiLeft, guiTop, base, slots, right, bookMode);

        // One pass per chrome layer, not per block: a block's FACE has to paint
        // over its neighbour's inner border, which has not been drawn yet if the
        // blocks are each taken outline-to-face in turn. Layer order is what
        // makes the two blocks read as a single window instead of two boxes.
        for (Block b : blocks) fillRounded(g, b.x0(), b.y0(), b.x1(), b.y1(), OUTLINE, CORNER_CUT, b);
        for (Block b : blocks) drawBevel(g, b.x0() + 1, b.y0() + 1, b.x1() - 1, b.y1() - 1, CORNER_CUT - 1, b);
        for (Block b : blocks) g.fill(b.x0() + 2, b.y0() + 2, b.x1() - 2, b.y1() - 2, FACE);

        // Slot insets — recessed, so their bevel is the panel's inverted:
        // shadow on the top-left, highlight on the bottom-right.
        for (int i = 0; i < slots; i++) {
            int sx = guiLeft + BackpackLayout.slotX(base + i, bookMode);
            int sy = guiTop + BackpackLayout.slotY(base + i);
            g.fill(sx - 1, sy - 1, sx + 17, sy + 17, SHADE);
            g.fill(sx, sy, sx + 18, sy + 18, LIGHT);
            g.fill(sx, sy, sx + 16, sy + 16, SLOT_BG);
        }
    }

    /** One chrome rectangle of a panel, carrying which of its corners are rounded. */
    private record Block(int x0, int y0, int x1, int y1, boolean tl, boolean tr, boolean bl, boolean br) {}

    /**
     * The panel's outline as at most two rectangles. Fill is down-then-across, so
     * every column but the last is full height: a block of full columns plus, when
     * the count is not a multiple of {@link BackpackLayout#ROWS}, the short final
     * column outside it. Only corners on the silhouette's outside are rounded —
     * where the two blocks meet, the edges are interior and stay square.
     */
    private static List<Block> silhouette(int guiLeft, int guiTop, int base, int slots, boolean right,
                                          boolean bookMode) {
        int fullCols = slots / BackpackLayout.ROWS;
        int rem = slots % BackpackLayout.ROWS;
        List<Block> blocks = new ArrayList<>(2);
        if (fullCols > 0) {
            // The short column, if any, covers this block's outward TOP corner but
            // never its bottom — that one stays exposed at the step.
            blocks.add(block(guiLeft, guiTop, base, 0, fullCols, BackpackLayout.ROWS, right, bookMode,
                             true, true, rem == 0, true));
        }
        if (rem > 0) {
            blocks.add(block(guiLeft, guiTop, base, fullCols, 1, rem, right, bookMode,
                             fullCols == 0, fullCols == 0, true, true));
        }
        return blocks;
    }

    /** Chrome rect around {@code cols} columns of {@code rows} slots, starting at fill-column {@code col0}. */
    private static Block block(int guiLeft, int guiTop, int base, int col0, int cols, int rows,
                               boolean right, boolean bookMode,
                               boolean innerTop, boolean innerBottom, boolean outerTop, boolean outerBottom) {
        int xa = guiLeft + BackpackLayout.slotX(base + col0 * BackpackLayout.ROWS, bookMode);
        int xb = guiLeft + BackpackLayout.slotX(base + (col0 + cols - 1) * BackpackLayout.ROWS, bookMode);
        int x0 = Math.min(xa, xb) - BackpackLayout.BORDER;
        int x1 = Math.max(xa, xb) + BackpackLayout.SLOT + BackpackLayout.BORDER;
        int y0 = guiTop + BackpackLayout.Y0 - BackpackLayout.BORDER;
        int y1 = guiTop + BackpackLayout.Y0 + rows * BackpackLayout.SLOT + BackpackLayout.BORDER;
        // The left panel grows leftward, so its inner edge is on the right — except in book
        // mode, where both panels sit right of the GUI and grow the same way.
        return right || bookMode
            ? new Block(x0, y0, x1, y1, innerTop, outerTop, innerBottom, outerBottom)
            : new Block(x0, y0, x1, y1, outerTop, innerTop, outerBottom, innerBottom);
    }

    /**
     * Fills a rect whose flagged corners are stepped back like the vanilla GUI's
     * rounded corners: the outermost row is inset by {@code cut} px, the next by
     * {@code cut - 1}, and so on. The skipped pixels are simply never drawn, so
     * the screen behind shows through — which is what makes the corner read as
     * round. {@code cut <= 0} draws a plain rect.
     */
    private static void fillRounded(GuiGraphics g, int x0, int y0, int x1, int y1, int color,
                                    int cut, Block b) {
        for (int d = 0; d < cut; d++) {
            int inset = cut - d;
            g.fill(x0 + (b.tl() ? inset : 0), y0 + d, x1 - (b.tr() ? inset : 0), y0 + d + 1, color);
            g.fill(x0 + (b.bl() ? inset : 0), y1 - d - 1, x1 - (b.br() ? inset : 0), y1 - d, color);
        }
        int m = Math.max(cut, 0);
        g.fill(x0, y0 + m, x1, y1 - m, color);
    }

    /**
     * Fills the bevel layer: {@link #LIGHT} everywhere, then {@link #SHADE} along
     * the bottom and right edges, so the window reads as lit from the top-left
     * like every vanilla one. The shaded runs each start a pixel in, which puts
     * the diagonal light/shadow seam at the top-right and bottom-left corners —
     * the same seam the inventory texture has.
     *
     * <p>The bevel is one pixel thick and {@code cut} is at most 1, so only the
     * top and bottom rows are ever stepped back; the right edge runs between
     * them at full width and needs no staircase of its own.</p>
     */
    private static void drawBevel(GuiGraphics g, int x0, int y0, int x1, int y1, int cut, Block b) {
        fillRounded(g, x0, y0, x1, y1, LIGHT, cut, b);
        // Right edge, from below the top row down to above the bottom row —
        // the top pixel stays lit, which is what angles the seam.
        if (y1 - 1 > y0 + 1) g.fill(x1 - 1, y0 + 1, x1, y1 - 1, SHADE);
        // Bottom edge, skipping its leftmost pixel for the same reason.
        int from = x0 + 1 + (b.bl() ? cut : 0);
        int to = x1 - (b.br() ? cut : 0);
        if (to > from) g.fill(from, y1 - 1, to, y1, SHADE);
    }
}
