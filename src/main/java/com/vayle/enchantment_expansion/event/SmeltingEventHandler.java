package com.vayle.enchantment_expansion.event;

import com.vayle.enchantment_expansion.EnchantmentExpansion;
import com.vayle.enchantment_expansion.enchantment.ModEnchantmentHelper;
import com.vayle.enchantment_expansion.enchantment.ModEnchantments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * 熔炼（Smelting）附魔处理器。
 * <p>
 * 当挖掘工具带有熔炼附魔时，把方块掉落物替换为其熔炉烧炼产物（如铁矿石直接掉铁锭）。
 */
@EventBusSubscriber(modid = EnchantmentExpansion.MOD_ID)
public final class SmeltingEventHandler {
    private SmeltingEventHandler() {
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        ServerLevel level = event.getLevel();

        // 工具没有熔炼附魔时不处理。
        if (ModEnchantmentHelper.getLevel(event.getTool(), level.registryAccess(), ModEnchantments.SMELTING) <= 0) {
            return;
        }

        // 逐项尝试烧炼；无烧炼配方的掉落物保持不变。
        for (ItemEntity itemEntity : event.getDrops()) {
            ItemStack result = getSmeltedResult(level, itemEntity.getItem());
            if (!result.isEmpty()) {
                // 烧炼产物数量 = 单个产物数量 × 掉落物数量。
                result.setCount(result.getCount() * itemEntity.getItem().getCount());
                itemEntity.setItem(result);
            }
        }
    }

    private static ItemStack getSmeltedResult(ServerLevel level, ItemStack input) {
        return level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), level)
                .map(recipe -> recipe.value().getResultItem(level.registryAccess()).copy())
                .orElse(ItemStack.EMPTY);
    }
}
