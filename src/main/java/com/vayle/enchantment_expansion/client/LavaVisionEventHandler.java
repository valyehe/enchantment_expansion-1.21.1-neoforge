package com.vayle.enchantment_expansion.client;

import com.vayle.enchantment_expansion.EnchantmentConfig;
import com.vayle.enchantment_expansion.EnchantmentExpansion;
import com.vayle.enchantment_expansion.enchantment.ModEnchantmentHelper;
import com.vayle.enchantment_expansion.enchantment.ModEnchantments;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * 熔岩视野（Lava Vision）附魔处理器。
 * <p>
 * 仅在客户端生效；佩戴附魔头盔时拉远熔岩雾，使玩家在熔岩下能看清周围。
 * 雾的远近可在配置文件 {@code config/enchantment_expansion-common.toml} 中调整。
 */
@EventBusSubscriber(modid = EnchantmentExpansion.MOD_ID, value = Dist.CLIENT)
public final class LavaVisionEventHandler {
    private LavaVisionEventHandler() {
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        // 只处理熔岩雾，且相机实体必须是生物。
        if (event.getType() != FogType.LAVA
                || !(event.getCamera().getEntity() instanceof LivingEntity entity)) {
            return;
        }

        // 检查头盔上的熔岩视野等级。
        int level = ModEnchantmentHelper.getLevel(
                entity.getItemBySlot(EquipmentSlot.HEAD),
                entity.level().registryAccess(),
                ModEnchantments.LAVA_VISION
        );
        if (level <= 0) {
            return;
        }

        // 从配置读取熔岩雾的起止距离；NeoForge 1.21.1 中需取消事件后修改才生效。
        float fogNear = EnchantmentConfig.CONFIG.lavaVisionFogNear.get().floatValue();
        float fogFar = EnchantmentConfig.CONFIG.lavaVisionFogFar.get().floatValue();
        event.setNearPlaneDistance(fogNear);
        event.setFarPlaneDistance(fogFar);
        event.setCanceled(true);
    }
}
