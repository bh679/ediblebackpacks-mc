package games.brennan.ediblebackpacks.client;

import games.brennan.ediblebackpacks.registry.ModItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The open/close button on the survival inventory screen — where it sits, and whether it is
 * there at all, is the player's choice ({@code config/EBClientConfig}; the hotkey works
 * either way). Slightly under vanilla's 20×18 recipe-button footprint at 18×16, vanilla button chrome, with an
 * {@code edible_backpack} item drawn on it as the glyph — no new texture asset, which also
 * keeps the icon in step if the item art ever changes.
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

    /**
     * Translucent wash in the GUI's own face grey (the {@code BackpackScreenPanels} palette),
     * drawn over the glyph to pull its colour toward the chrome around it. Vanilla's own idiom
     * for a muted item — the recipe book washes ghost ingredients the same way — and unlike a
     * shader tint it needs nothing of the item's render type.
     */
    private static final int SCRIM = 0x99C6C6C6;

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

    /**
     * The item renderer only ever draws 16×16, so the glyph is scaled rather than positioned:
     * half scale gives the 8×8 {@link ButtonPlacement#GLYPH}, inset by
     * {@link ButtonPlacement#PADDING} on every side. {@code renderItem} respects the current
     * pose, so the transform is all it takes.
     */
    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        int x = getX() + ButtonPlacement.PADDING;
        int y = getY() + ButtonPlacement.PADDING;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0f);
        graphics.pose().scale(0.5f, 0.5f, 1.0f);
        graphics.renderFakeItem(icon, 0, 0);
        graphics.pose().popPose();
        // Above the item, which draws at z 150 — a plain fill at the GUI's own depth would go
        // under it and wash nothing.
        graphics.pose().pushPose();
        graphics.pose().translate(0.0f, 0.0f, 200.0f);
        graphics.fill(x, y, x + ButtonPlacement.GLYPH, y + ButtonPlacement.GLYPH, SCRIM);
        graphics.pose().popPose();
    }
}
