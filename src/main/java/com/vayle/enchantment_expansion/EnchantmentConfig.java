package com.vayle.enchantment_expansion;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 模组配置。
 * <p>
 * 使用 NeoForge 的 {@link ModConfigSpec} 定义可调参数，运行时会生成
 * {@code config/enchantment_expansion-common.toml}，玩家可直接编辑或通过配置界面修改。
 * 这里集中管理各附魔的可调数值，供事件处理器在运行时读取。
 */
public final class EnchantmentConfig {
    /** 配置实例（通过 {@code configure} 与 {@code CONFIG_SPEC} 共用同一个构建器）。 */
    public static final EnchantmentConfig CONFIG;
    /** 配置规范，注册到模组容器时使用。 */
    public static final ModConfigSpec CONFIG_SPEC;

    static {
        var pair = new ModConfigSpec.Builder().configure(EnchantmentConfig::new);
        CONFIG = pair.getLeft();
        CONFIG_SPEC = pair.getRight();
    }

    // ===== 生命偷取（Life Steal） =====

    /** 每级附魔恢复“实际造成伤害”的比例（例如 0.10 表示 10%）。 */
    public final ModConfigSpec.DoubleValue lifeStealHealPercentPerLevel;

    private EnchantmentConfig(ModConfigSpec.Builder builder) {
        // 生命偷取分组：分组标题用 translation 在 push 前设置，push 时会写入分组级翻译键。
        builder.translation("config.enchantment_expansion.life_steal").push("life_steal");
        lifeStealHealPercentPerLevel = builder
                .comment("每级生命偷取恢复“实际造成伤害”的比例，0.10 表示 10%，0.30 表示 30%。")
                .translation("config.enchantment_expansion.life_steal.heal_percent_per_level")
                .defineInRange("heal_percent_per_level", 0.10D, 0.0D, 1.0D);
        builder.pop();
    }
}
