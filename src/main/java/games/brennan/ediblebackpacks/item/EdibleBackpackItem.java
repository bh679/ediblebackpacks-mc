package games.brennan.ediblebackpacks.item;

import games.brennan.ediblebackpacks.config.EBConfig;
import games.brennan.ediblebackpacks.network.EBNetwork;
import games.brennan.ediblebackpacks.registry.ModAttachments;
import games.brennan.ediblebackpacks.storage.BackpackData;
import games.brennan.ediblebackpacks.storage.BackpackPolicy;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * An edible backpack. Eating one permanently unlocks {@link #slotsGranted}
 * backpack slots (server-authoritative, capped by config {@code maxSlots}).
 * Zero nutrition, always edible — the point is the storage, not the meal.
 *
 * <p>Non-stackable: a backpack is a bulky object, and stacking would make the
 * compressed variant pointless.</p>
 *
 * <p>A grant tops up to the cap rather than being refused outright — see
 * {@link BackpackPolicy#effectiveGrant} for why partial beats all-or-nothing
 * here. Eating is only refused once there is no room at all.</p>
 */
public final class EdibleBackpackItem extends Item {

    public static final FoodProperties FOOD = new FoodProperties.Builder()
        .nutrition(0)
        .saturationModifier(0f)
        .alwaysEdible()
        .build();

    private final int slotsGranted;

    public EdibleBackpackItem(Properties properties, int slotsGranted) {
        super(properties.food(FOOD).stacksTo(1));
        this.slotsGranted = slotsGranted;
    }

    public int slotsGranted() {
        return slotsGranted;
    }

    /** Slots this backpack would actually grant the player right now. */
    private int grantFor(Player player) {
        return BackpackPolicy.effectiveGrant(
            player.getData(ModAttachments.BACKPACK).unlocked(), slotsGranted, EBConfig.maxSlots());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Refuse to start eating only at the cap (both sides — maxSlots is a
        // synced SERVER config, so the client check matches).
        if (grantFor(player) <= 0) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("ediblebackpacks.msg.full"), true);
            }
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        return super.use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            int granted = grantFor(player);
            if (granted > 0) {
                BackpackData data = player.getData(ModAttachments.BACKPACK);
                data.setUnlocked(data.unlocked() + granted);
                EBNetwork.syncSlotCount(player);
                player.displayClientMessage(
                    Component.translatable("ediblebackpacks.msg.slots", data.unlocked()), true);
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }
}
