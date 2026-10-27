package com.vayle.enchantment_expansion.event;

import com.vayle.enchantment_expansion.EnchantmentExpansion;
import com.vayle.enchantment_expansion.enchantment.ModEnchantmentHelper;
import com.vayle.enchantment_expansion.enchantment.ModEnchantments;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * 夜视（Night Vision）附魔处理器。
 * <p>
 * 佩戴夜视头盔时提供无限持续时间的夜视；脱下头盔后移除模组添加的夜视。
 */
@EventBusSubscriber(modid = EnchantmentExpansion.MOD_ID)
public final class NightVisionEventHandler {
    /** 无限持续时间的标志值。 */
    private static final int INFINITE_DURATION = -1;

    private NightVisionEventHandler() {
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity) || entity.level().isClientSide()) {
            return;
        }

        ItemStack helmet = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty()) {
            removeModNightVision(entity);
            return;
        }

        int level = ModEnchantmentHelper.getLevel(helmet, entity.level().registryAccess(), ModEnchantments.NIGHT_VISION);
        if (level > 0) {
            if (!entity.hasEffect(MobEffects.NIGHT_VISION)) {
                entity.addEffect(new MobEffectInstance(
                        MobEffects.NIGHT_VISION,
                        INFINITE_DURATION,
                        0,
                        true,   // ambient：环境效果，不产生粒子
                        false,  // visible：不显示在界面右上角
                        false   // showIcon：不显示状态图标
                ));
            }
        } else {
            removeModNightVision(entity);
        }
    }

    /** 只移除本模组添加的隐藏无限夜视，不影响普通夜视药水。 */
    private static void removeModNightVision(LivingEntity entity) {
        MobEffectInstance effect = entity.getEffect(MobEffects.NIGHT_VISION);
        if (effect != null
                && effect.getDuration() < 0
                && effect.isAmbient()
                && !effect.isVisible()
                && !effect.showIcon()) {
            entity.removeEffect(MobEffects.NIGHT_VISION);
        }
    }
}
