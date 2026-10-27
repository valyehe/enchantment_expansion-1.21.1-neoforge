package com.vayle.enchantment_expansion.event;

import com.vayle.enchantment_expansion.EnchantmentConfig;
import com.vayle.enchantment_expansion.EnchantmentExpansion;
import com.vayle.enchantment_expansion.enchantment.ModEnchantmentHelper;
import com.vayle.enchantment_expansion.enchantment.ModEnchantments;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * 生命偷取（Life Steal）附魔处理器。
 * <p>
 * 攻击任意生物时，按最终实际造成的伤害恢复攻击者生命值。
 * 恢复比例可在配置文件 {@code config/enchantment_expansion-common.toml} 中调整。
 */
@EventBusSubscriber(modid = EnchantmentExpansion.MOD_ID)
public final class LifeStealEventHandler {
    private LifeStealEventHandler() {
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();

        // 仅在服务端处理，避免与客户端不同步。
        if (target.level().isClientSide()) {
            return;
        }

        // 攻击者必须是生物，且不能攻击自己（防止自伤触发吸血）。
        if (!(event.getSource().getDirectEntity() instanceof LivingEntity attacker) || attacker == target) {
            return;
        }

        // 读取攻击者主手武器上的生命偷取等级，未附魔则直接跳过。
        int level = ModEnchantmentHelper.getLevel(
                attacker.getMainHandItem(),
                attacker.level().registryAccess(),
                ModEnchantments.LIFE_STEAL
        );
        if (level <= 0) {
            return;
        }

        // 从配置读取每级恢复比例，以最终实际造成的伤害为基准计算治疗量。
        float healPercent = EnchantmentConfig.CONFIG.lifeStealHealPercentPerLevel.get().floatValue();
        float healing = event.getNewDamage() * healPercent * level;
        if (healing > 0) {
            attacker.heal(healing);
        }
    }
}
