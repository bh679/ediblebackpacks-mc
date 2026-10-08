package games.brennan.ediblebackpacks;

import games.brennan.ediblebackpacks.item.EdibleBackpackItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.Foods;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackpackFoodTest {

    @Test
    void plainBackpackFeedsLikeAnApple() {
        FoodProperties food = EdibleBackpackItem.PLAIN_FOOD;
        assertEquals(Foods.APPLE.nutrition(), food.nutrition());
        assertEquals(Foods.APPLE.saturation(), food.saturation());
        assertTrue(food.effects().isEmpty());
        assertTrue(food.canAlwaysEat());
    }

    @Test
    void goldenBackpackFeedsLikeAGoldenApple() {
        FoodProperties food = EdibleBackpackItem.GOLDEN_FOOD;
        assertEquals(Foods.GOLDEN_APPLE.nutrition(), food.nutrition());
        assertEquals(Foods.GOLDEN_APPLE.saturation(), food.saturation());
        assertTrue(food.canAlwaysEat());
        List<MobEffectInstance> effects = food.effects().stream()
            .map(FoodProperties.PossibleEffect::effect).toList();
        assertEquals(2, effects.size());
        assertTrue(effects.stream().anyMatch(e -> e.is(MobEffects.REGENERATION) && e.getAmplifier() == 1));
        assertTrue(effects.stream().anyMatch(e -> e.is(MobEffects.ABSORPTION) && e.getAmplifier() == 0));
    }
}
