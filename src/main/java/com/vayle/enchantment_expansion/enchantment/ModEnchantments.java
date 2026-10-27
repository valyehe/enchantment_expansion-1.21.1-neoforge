package com.vayle.enchantment_expansion.enchantment;

import com.vayle.enchantment_expansion.EnchantmentExpansion;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * 本模组所有附魔的 {@link ResourceKey} 集中定义处。
 * <p>
 * 附魔本身由数据包 JSON 定义（见 {@code data/enchantment_expansion/enchantment/*.json}），
 * 这里只保存稳定的键，运行时再通过世界动态注册表查询。
 */
public final class ModEnchantments {
    public static final ResourceKey<Enchantment> NIGHT_VISION = create("night_vision");
    public static final ResourceKey<Enchantment> SMELTING = create("smelting");
    public static final ResourceKey<Enchantment> LAVA_VISION = create("lava_vision");
    public static final ResourceKey<Enchantment> LIFE_STEAL = create("life_steal");

    private ModEnchantments() {
    }

    private static ResourceKey<Enchantment> create(String name) {
        return ResourceKey.create(
                Registries.ENCHANTMENT,
                ResourceLocation.fromNamespaceAndPath(EnchantmentExpansion.MOD_ID, name)
        );
    }
}
