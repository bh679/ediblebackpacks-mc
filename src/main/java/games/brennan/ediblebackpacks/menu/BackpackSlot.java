package games.brennan.ediblebackpacks.menu;

import games.brennan.ediblebackpacks.client.ClientPanelState;
import games.brennan.ediblebackpacks.registry.ModAttachments;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * One backpack slot appended to the vanilla {@code InventoryMenu}. All 108
 * slots always exist (stable indices for vanilla container sync); locked
 * slots are inactive and refuse interaction.
 *
 * <p>{@link #isActive()} additionally hides the panels client-side while the
 * crafting recipe book is open (the book overlaps the left panel). The
 * server never consults {@code isActive} for click validation, so
 * {@link #mayPlace}/{@link #mayPickup} carry the authoritative lock check.</p>
 */
public final class BackpackSlot extends SlotItemHandler {

    private final Player owner;
    private final int backpackIndex;

    public BackpackSlot(IItemHandler handler, Player owner, int backpackIndex) {
        super(handler, backpackIndex,
            BackpackLayout.slotX(backpackIndex), BackpackLayout.slotY(backpackIndex));
        this.owner = owner;
        this.backpackIndex = backpackIndex;
    }

    private boolean unlockedNow() {
        return backpackIndex < owner.getData(ModAttachments.BACKPACK).unlocked();
    }

    @Override
    public boolean isActive() {
        if (!unlockedNow()) return false;
        // Client-only: hide while the recipe book is open. On the logical
        // server isClientSide is false, so the client class never loads there.
        if (owner.level().isClientSide && ClientPanelState.recipeBookOpen()) return false;
        return true;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return unlockedNow() && super.mayPlace(stack);
    }

    @Override
    public boolean mayPickup(Player player) {
        return unlockedNow() && super.mayPickup(player);
    }
}
