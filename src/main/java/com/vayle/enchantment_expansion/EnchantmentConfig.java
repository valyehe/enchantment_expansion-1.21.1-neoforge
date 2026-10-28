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

    // ===== 矿物高亮（Ore Vision） =====

    /** 矿物探测半径（格），以玩家为中心的正方体边长。 */
    public final ModConfigSpec.IntValue oreVisionRadius;

    private EnchantmentConfig(ModConfigSpec.Builder builder) {
        // ===== 生命偷取 =====
        builder.translation("config.enchantment_expansion.life_steal").push("life_steal");
        lifeStealHealPercentPerLevel = builder
                .comment("每级生命偷取恢复“实际造成伤害”的比例，0.10 表示 10%，0.30 表示 30%。")
                .translation("config.enchantment_expansion.life_steal.heal_percent_per_level")
                .defineInRange("heal_percent_per_level", 0.10D, 0.0D, 1.0D);
        builder.pop();

        // ===== 矿物高亮 =====
        builder.translation("config.enchantment_expansion.ore_vision").push("ore_vision");
        oreVisionRadius = builder
                .comment("矿物探测半径（格），以玩家为中心的正方体边长。")
                .translation("config.enchantment_expansion.ore_vision.radius")
                .defineInRange("radius", 8, 1, 64);
        builder.pop();
    }
}
