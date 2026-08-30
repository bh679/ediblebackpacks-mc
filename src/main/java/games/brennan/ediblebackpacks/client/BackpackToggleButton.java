package games.brennan.ediblebackpacks.client;

import games.brennan.ediblebackpacks.registry.ModItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The open/close button on the inventory and chest screens — where it sits, and whether it is
 * there at all, is the player's choice ({@code config/EBClientConfig}; the hotkey works either
 * way).
 *
 * <p>The chrome is vanilla's recipe-book button, drawn rather than blitted. Its sprite
 * ({@code recipe_book/button}) has the book artwork baked into the texture, so borrowing the
 * sprite meant wearing that icon underneath this one's — two pictures in a ten-pixel box. The
 * frame around it is only a 1px bevel, and the palette below is sampled straight from it, so
 * drawing it gives the same button minus the picture. It also sizes to anything without
 * stretching a 20×18 texture, which is what the sprite would have been doing here.</p>
 *
 * <p>The glyph is an {@code edible_backpack} item — no new texture asset, which also keeps the
 * icon in step if the item art ever changes — and its colour carries the state. Open, or under
 * the cursor, it is the item at full colour; shut and unhovered it is washed out with
 * {@link #SCRIM}, so a closed backpack reads as closed at a glance and an idle button never
 * becomes the loudest thing on a screen made almost entirely of grey.</p>
 *
 * <p>Pressing it flips {@code BackpackData#panelsOpen} on the client and tells the server
 * (see {@link ClientPanelState#toggle()}). Closing is a real lock, not a hide: the slots go
 * inert on both sides, so a shift-click while closed just does what vanilla would.</p>
 *
 * <p>Position is rewritten every frame from {@code BackpackScreenPanels} ({@link
 * ButtonPlacement} does the anchor math) — opening the recipe book slides the whole GUI
 * right, and vanilla only moves its own widgets.</p>
 */
public final class BackpackToggleButton extends Button {

    // Sampled from gui/sprites/recipe_book/button.png and button_highlighted.png. The idle
    // face is the same grey the panels are drawn in; hover goes to vanilla's blue.
    private static final int FACE = 0xFFC6C6C6;
    private static final int SHADE = 0xFF555555;
    private static final int HOVER_FACE = 0xFF8892C9;
    private static final int HOVER_SHADE = 0xFF343E75;
    /** Both sprites are lit from the top-left with the same white, hovered or not. */
    private static final int LIGHT = 0xFFFFFFFF;

    /**
     * Translucent wash in the GUI's own face grey, drawn over the glyph to pull its colour
     * toward the chrome around it. Vanilla's own idiom for a muted item — the recipe book
     * washes ghost ingredients the same way — and unlike a shader tint it needs nothing of the
     * item's render type.
     */
    private static final int SCRIM = 0x99C6C6C6;

    /** The item renderer only ever draws 16×16, so the glyph is scaled to size rather than cropped. */
    private static final float GLYPH_SCALE = ButtonPlacement.GLYPH / 16.0f;

    private final ItemStack icon = new ItemStack(ModItems.EDIBLE_BACKPACK.get());

    public BackpackToggleButton() {
        super(0, 0, ButtonPlacement.WIDTH, ButtonPlacement.HEIGHT, Component.empty(), b -> {
            ClientPanelState.toggle();
            ((BackpackToggleButton) b).refreshTooltip();
        }, DEFAULT_NARRATION);
        refreshTooltip();
    }

    /** Says what the next press will do, so it tracks the state rather than describing it. */
    public void refreshTooltip() {
        setTooltip(Tooltip.create(Component.translatable(
            ClientPanelState.panelsOpen() ? "ediblebackpacks.button.close" : "ediblebackpacks.button.open")));
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean lit = isHoveredOrFocused();
        drawChrome(graphics, lit);

        int x = getX() + ButtonPlacement.PADDING;
        int y = getY() + ButtonPlacement.PADDING;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0f);
        graphics.pose().scale(GLYPH_SCALE, GLYPH_SCALE, 1.0f);
        graphics.renderFakeItem(icon, 0, 0);
        graphics.pose().popPose();

        // Full colour while the backpack is open, and while the cursor is on the button.
        if (ClientPanelState.panelsOpen() || lit) return;
        // Above the item, which draws at z 150 — a plain fill at the GUI's own depth would go
        // under it and wash nothing.
        graphics.pose().pushPose();
        graphics.pose().translate(0.0f, 0.0f, 200.0f);
        graphics.fill(x, y, x + ButtonPlacement.GLYPH, y + ButtonPlacement.GLYPH, SCRIM);
        graphics.pose().popPose();
    }

    /**
     * The recipe button's bevel: white along the top and left, shadow along the bottom and
     * right, and the four corner pixels left out — the same 1px cut the vanilla sprite has,
     * which is what keeps it from reading as a hard rectangle.
     */
    private void drawChrome(GuiGraphics g, boolean lit) {
        int face = lit ? HOVER_FACE : FACE;
        int shade = lit ? HOVER_SHADE : SHADE;
        int x0 = getX();
        int y0 = getY();
        int x1 = x0 + getWidth();
        int y1 = y0 + getHeight();

        g.fill(x0 + 1, y0, x1 - 1, y1, face);
        g.fill(x0, y0 + 1, x1, y1 - 1, face);

        g.fill(x0 + 1, y0, x1 - 1, y0 + 1, LIGHT);
        g.fill(x0, y0 + 1, x0 + 1, y1 - 1, LIGHT);
        g.fill(x0 + 1, y1 - 1, x1 - 1, y1, shade);
        g.fill(x1 - 1, y0 + 1, x1, y1 - 1, shade);
    }
}
