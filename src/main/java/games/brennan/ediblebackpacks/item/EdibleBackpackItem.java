package games.brennan.ediblebackpacks.item;

import games.brennan.ediblebackpacks.config.EBConfig;
import games.brennan.ediblebackpacks.network.EBNetwork;
import games.brennan.ediblebackpacks.registry.ModAttachments;
import games.brennan.ediblebackpacks.storage.BackpackData;
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
 * The edible backpack. Eating one permanently unlocks +1 backpack slot
 * (server-authoritative, capped by config {@code maxSlots}). Zero nutrition,
 * always edible — the point is the slot, not the meal.
 */
public final class EdibleBackpackItem extends Item {

    public static final FoodProperties FOOD = new FoodProperties.Builder()
        .nutrition(0)
        .saturationModifier(0f)
        .alwaysEdible()
        .build();

    public EdibleBackpackItem(Properties properties) {
        super(properties.food(FOOD));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Refuse to start eating at the cap (both sides — maxSlots is a synced
        // SERVER config, so the client check matches).
        if (player.getData(ModAttachments.BACKPACK).unlocked() >= EBConfig.maxSlots()) {
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
            BackpackData data = player.getData(ModAttachments.BACKPACK);
            if (data.unlocked() < EBConfig.maxSlots()) {
                data.setUnlocked(data.unlocked() + 1);
                EBNetwork.syncSlotCount(player);
                player.displayClientMessage(
                    Component.translatable("ediblebackpacks.msg.slots", data.unlocked()), true);
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }
}
