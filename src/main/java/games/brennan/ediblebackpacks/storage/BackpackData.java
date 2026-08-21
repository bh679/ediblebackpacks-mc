package games.brennan.ediblebackpacks.storage;

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
    private final ItemStackHandler items = new ItemStackHandler(BackpackLayout.MAX_SLOTS);

    public int unlocked() {
        return unlocked;
    }

    public void setUnlocked(int value) {
        this.unlocked = BackpackPolicy.clampUnlocked(value, BackpackLayout.MAX_SLOTS);
    }

    public ItemStackHandler items() {
        return items;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("unlocked", unlocked);
        tag.put("items", items.serializeNBT(provider));
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        setUnlocked(tag.getInt("unlocked"));
        if (tag.contains("items")) {
            items.deserializeNBT(provider, tag.getCompound("items"));
        }
        // Guard a shrunken save from a hypothetical older/newer format.
        if (items.getSlots() != BackpackLayout.MAX_SLOTS) {
            items.setSize(BackpackLayout.MAX_SLOTS);
        }
    }
}
