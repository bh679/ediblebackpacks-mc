package games.brennan.ediblebackpacks.registry;

import games.brennan.ediblebackpacks.EdibleBackpacks;
import games.brennan.ediblebackpacks.item.EdibleBackpackItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Item registry: the plain edible backpack (+1 slot) and its 3×3 compressed
 * form, the golden edible backpack (+9 slots). The recipe is one-way.
 */
@EventBusSubscriber(modid = EdibleBackpacks.MOD_ID)
public final class ModItems {

    /** Slots granted by one golden edible backpack — also the 3×3 recipe's input count. */
    public static final int GOLDEN_SLOTS = 9;

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EdibleBackpacks.MOD_ID);

    public static final DeferredItem<Item> EDIBLE_BACKPACK = ITEMS.register(
        "edible_backpack",
        () -> new EdibleBackpackItem(new Item.Properties(), 1)
    );

    public static final DeferredItem<Item> GOLDEN_EDIBLE_BACKPACK = ITEMS.register(
        "golden_edible_backpack",
        () -> new EdibleBackpackItem(new Item.Properties(), GOLDEN_SLOTS)
    );

    private ModItems() {}

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    @SubscribeEvent
    public static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS
            || event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(EDIBLE_BACKPACK.get());
            event.accept(GOLDEN_EDIBLE_BACKPACK.get());
        }
    }
}
