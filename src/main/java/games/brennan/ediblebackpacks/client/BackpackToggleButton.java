package games.brennan.ediblebackpacks.client;

import games.brennan.ediblebackpacks.registry.ModItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The open/close button on the inventory and chest screens — where it sits, and whether it is
 * there at all, is the player's choice ({@code config/EBClientConfig}; the hotkey works either
 * way). It borrows vanilla's own recipe-book button sprites rather than the generic widget
 * chrome, so it reads as a sibling of the control it sits beside — including the highlighted
 * variant on hover, which {@link ImageButton} swaps in for free. Drawn at
 * {@link ButtonPlacement#WIDTH}×{@link ButtonPlacement#HEIGHT}, half the sprite's native
 * 20×18: this one sits over the inventory rather than beside it, where anything larger reads
 * as a third piece of GUI furniture instead of a small affordance.
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
public final class BackpackToggleButton extends ImageButton {

    /**
     * Translucent wash in the GUI's own face grey (the {@code BackpackScreenPanels} palette),
     * drawn over the glyph to pull its colour toward the chrome around it. Vanilla's own idiom
     * for a muted item — the recipe book washes ghost ingredients the same way — and unlike a
     * shader tint it needs nothing of the item's render type.
     */
    private static final int SCRIM = 0x99C6C6C6;

    /** The item renderer only ever draws 16×16, so the glyph is scaled to size rather than cropped. */
    private static final float GLYPH_SCALE = ButtonPlacement.GLYPH / 16.0f;

    private final ItemStack icon = new ItemStack(ModItems.EDIBLE_BACKPACK.get());

    public BackpackToggleButton() {
        super(0, 0, ButtonPlacement.WIDTH, ButtonPlacement.HEIGHT,
            RecipeBookComponent.RECIPE_BUTTON_SPRITES,
            b -> {
                ClientPanelState.toggle();
                ((BackpackToggleButton) b).refreshTooltip();
            }, Component.empty());
        refreshTooltip();
    }

    /** Says what the next press will do, so it tracks the state rather than describing it. */
    public void refreshTooltip() {
        setTooltip(Tooltip.create(Component.translatable(
            ClientPanelState.panelsOpen() ? "ediblebackpacks.button.close" : "ediblebackpacks.button.open")));
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);

        int x = getX() + ButtonPlacement.PADDING;
        int y = getY() + ButtonPlacement.PADDING;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0f);
        graphics.pose().scale(GLYPH_SCALE, GLYPH_SCALE, 1.0f);
        graphics.renderFakeItem(icon, 0, 0);
        graphics.pose().popPose();

        if (vivid()) return;
        // Above the item, which draws at z 150 — a plain fill at the GUI's own depth would go
        // under it and wash nothing.
        graphics.pose().pushPose();
        graphics.pose().translate(0.0f, 0.0f, 200.0f);
        graphics.fill(x, y, x + ButtonPlacement.GLYPH, y + ButtonPlacement.GLYPH, SCRIM);
        graphics.pose().popPose();
    }

    /** Full colour while the backpack is open, and while the cursor is on the button. */
    private boolean vivid() {
        return ClientPanelState.panelsOpen() || isHovered();
    }
}
