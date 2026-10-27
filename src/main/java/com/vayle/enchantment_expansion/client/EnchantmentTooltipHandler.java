package com.vayle.enchantment_expansion.client;

import com.vayle.enchantment_expansion.EnchantmentExpansion;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * 附魔描述处理器。
 * <p>
 * 在物品提示（Tooltip）中为本模组附魔追加一行灰色说明文字，
 * 翻译键形如 {@code enchantment.enchantment_expansion.<附魔名>.desc}。
 * 同时兼容普通附魔物品（{@code ENCHANTMENTS}）与附魔书（{@code STORED_ENCHANTMENTS}）。
 */
@EventBusSubscriber(modid = EnchantmentExpansion.MOD_ID, value = Dist.CLIENT)
public final class EnchantmentTooltipHandler {
    /** 说明文字的颜色（浅灰，弱化显示以区别于附魔名称）。 */
    private static final int DESC_COLOR = 0x9E9E9E;

    private EnchantmentTooltipHandler() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        // 普通物品的附魔与附魔书存储的附魔分开存放，需分别检查。
        appendDescriptions(event, event.getItemStack().get(DataComponents.ENCHANTMENTS));
        appendDescriptions(event, event.getItemStack().get(DataComponents.STORED_ENCHANTMENTS));
    }

    /** 遍历一组附魔，为本模组的附魔追加描述行。 */
    private static void appendDescriptions(ItemTooltipEvent event, ItemEnchantments enchantments) {
        if (enchantments == null || enchantments.isEmpty()) {
            return;
        }

        for (var entry : enchantments.entrySet()) {
            Holder<Enchantment> holder = entry.getKey();
            // 只处理本模组命名空间的附魔。
            if (!holder.is(key -> key.location().getNamespace().equals(EnchantmentExpansion.MOD_ID))) {
                continue;
            }

            // 根据附魔 ResourceKey 拼出描述的语言键，例如 enchantment.enchantment_expansion.night_vision.desc。
            ResourceKey<Enchantment> key = holder.unwrapKey().orElse(null);
            if (key == null) {
                continue;
            }
            String descKey = "enchantment." + key.location().getNamespace() + "." + key.location().getPath() + ".desc";
            event.getToolTip().add(Component.translatable(descKey).withStyle(style -> style.withColor(DESC_COLOR)));
        }
    }
}
