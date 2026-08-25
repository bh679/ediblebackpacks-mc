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
 * <p>The backing handler is looked up from the player's attachment on every
 * access instead of being captured once. NeoForge <em>replaces</em> the
 * attachment object when it deserializes it (login) or copies it (respawn),
 * both of which happen after {@code InventoryMenu} is built in the
 * {@code Player} constructor — a captured handler would be orphaned from the
 * first relog on, silently voiding anything placed in the panels.</p>
 *
 * <p>{@link #isActive()} additionally hides the panels client-side while the
 * crafting recipe book is open (the book overlaps the left panel). The
 * server never consults {@code isActive} for click validation, so
 * {@link #mayPlace}/{@link #mayPickup} carry the authoritative lock check.</p>
 */
public final class BackpackSlot extends SlotItemHandler {

    private final Player owner;
    private final int backpackIndex;

    public BackpackSlot(Player owner, int backpackIndex) {
        // The super handler is only a seed; getItemHandler() below is what
        // every read/write actually goes through.
        super(owner.getData(ModAttachments.BACKPACK).items(), backpackIndex,
            BackpackLayout.slotX(backpackIndex), BackpackLayout.slotY(backpackIndex));
        this.owner = owner;
        this.backpackIndex = backpackIndex;
    }

    /** Always the player's current attachment, never the one captured at menu build time. */
    @Override
    public IItemHandler getItemHandler() {
        return owner.getData(ModAttachments.BACKPACK).items();
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

    // mayPlace/getMaxStackSize are re-implemented rather than delegated to
    // super: SlotItemHandler reads its captured field directly in those three,
    // where every other accessor goes through getItemHandler().

    @Override
    public boolean mayPlace(ItemStack stack) {
        return unlockedNow() && !stack.isEmpty() && getItemHandler().isItemValid(backpackIndex, stack);
    }

    @Override
    public boolean mayPickup(Player player) {
        return unlockedNow() && super.mayPickup(player);
    }

    @Override
    public int getMaxStackSize() {
        return getItemHandler().getSlotLimit(backpackIndex);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return Math.min(stack.getMaxStackSize(), getItemHandler().getSlotLimit(backpackIndex));
    }
}
