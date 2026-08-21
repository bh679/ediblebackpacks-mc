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

/** Item registry — one item: the edible backpack. */
@EventBusSubscriber(modid = EdibleBackpacks.MOD_ID)
public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EdibleBackpacks.MOD_ID);

    public static final DeferredItem<Item> EDIBLE_BACKPACK = ITEMS.register(
        "edible_backpack",
        () -> new EdibleBackpackItem(new Item.Properties())
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
        }
    }
}
