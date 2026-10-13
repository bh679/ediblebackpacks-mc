package games.brennan.ediblebackpacks.storage;

import games.brennan.ediblebackpacks.config.AutoCollectMode;
import games.brennan.ediblebackpacks.menu.BackpackLayout;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Per-player backpack attachment payload: how many slots are unlocked (by
 * eating backpacks) plus the backing 108-slot inventory. The handler is always
 * full-size so menu slot indices stay stable; locked slots are simply
 * inactive/refused at the {@code BackpackSlot} level.
 */
public final class BackpackData implements INBTSerializable<CompoundTag> {

    private int unlocked = 0;
    private boolean panelsOpen = true;
    private AutoCollectMode autoCollect = AutoCollectMode.WHILE_OPEN;
    private final ItemStackHandler items = new ItemStackHandler(BackpackLayout.MAX_SLOTS);

    public int unlocked() {
        return unlocked;
    }

    public void setUnlocked(int value) {
        this.unlocked = BackpackPolicy.clampUnlocked(value, BackpackLayout.MAX_SLOTS);
    }

    /**
     * Whether the panels are open. Closing them shuts the backpack outright — the slots
     * refuse items on both sides ({@code menu/BackpackSlot}) rather than merely going
     * invisible, because quick-move consults {@code mayPlace} and would otherwise post
     * stacks into slots nobody can see.
     */
    public boolean panelsOpen() {
        return panelsOpen;
    }

    public void setPanelsOpen(boolean value) {
        this.panelsOpen = value;
    }

    /**
     * The player's auto-collect choice, sent from their client config
     * ({@code network/AutoCollectPayload}). Kept here, saved, and carried through death so
     * pickups follow it even before the next login re-sends it.
     */
    public AutoCollectMode autoCollect() {
        return autoCollect;
    }

    public void setAutoCollect(AutoCollectMode value) {
        this.autoCollect = value == null ? AutoCollectMode.WHILE_OPEN : value;
    }

    public ItemStackHandler items() {
        return items;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("unlocked", unlocked);
        tag.putBoolean("panelsOpen", panelsOpen);
        tag.putString("autoCollect", autoCollect.name());
        tag.put("items", items.serializeNBT(provider));
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        setUnlocked(tag.getInt("unlocked"));
        // Saves written before the toggle existed have no tag — those players had the
        // panels showing, so absent means open.
        panelsOpen = !tag.contains("panelsOpen") || tag.getBoolean("panelsOpen");
        // Older saves have no tag: they collected while open, so that is what they keep.
        autoCollect = AutoCollectMode.byName(tag.getString("autoCollect"));
        if (tag.contains("items")) {
            items.deserializeNBT(provider, tag.getCompound("items"));
        }
        // Guard a shrunken save from a hypothetical older/newer format.
        if (items.getSlots() != BackpackLayout.MAX_SLOTS) {
            items.setSize(BackpackLayout.MAX_SLOTS);
        }
    }
}
