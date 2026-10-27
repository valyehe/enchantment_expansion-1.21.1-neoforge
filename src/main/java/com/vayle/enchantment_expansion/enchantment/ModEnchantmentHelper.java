package com.vayle.enchantment_expansion.enchantment;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/** 读取数据包附魔等级的通用工具方法。 */
public final class ModEnchantmentHelper {
    private ModEnchantmentHelper() {
    }

    /**
     * 返回物品上指定附魔的等级；物品为空或没有该附魔时返回 0。
     *
     * @param stack       目标物品堆
     * @param registries  当前世界的动态注册表访问器
     * @param enchantment 附魔的 ResourceKey
     */
    public static int getLevel(ItemStack stack, RegistryAccess registries, ResourceKey<Enchantment> enchantment) {
        if (stack.isEmpty()) {
            return 0;
        }
        return registries.lookupOrThrow(Registries.ENCHANTMENT)
                .get(enchantment)
                .map(stack::getEnchantmentLevel)
                .orElse(0);
    }
}
