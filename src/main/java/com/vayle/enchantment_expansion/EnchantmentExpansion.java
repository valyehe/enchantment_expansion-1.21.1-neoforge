package com.vayle.enchantment_expansion;

import com.vayle.enchantment_expansion.enchantment.ModEnchantments;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 模组主类，负责模组级注册与初始化。
 * <p>
 * 当前职责：
 * <ul>
 *     <li>注册模组专属创造模式物品栏，用于陈列本模组的附魔书；</li>
 *     <li>注册 COMMON 类型配置文件；</li>
 *     <li>在客户端注册游戏内配置界面。</li>
 * </ul>
 * 各附魔的具体效果逻辑位于 {@code event} 与 {@code client} 包的事件处理器中。
 */
@Mod(EnchantmentExpansion.MOD_ID)
public final class EnchantmentExpansion {
    /** 模组 ID，与 {@code neoforge.mods.toml} 中的 {@code modId} 一致。 */
    public static final String MOD_ID = "enchantment_expansion";

    private static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    /** 模组专属创造模式物品栏，用于陈列本模组的附魔书。 */
    private static final DeferredHolder<CreativeModeTab, CreativeModeTab> ENCHANTMENT_EXPANSION_TAB =
            CREATIVE_MODE_TABS.register("enchantment_expansion", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.enchantment_expansion"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> new ItemStack(Items.ENCHANTED_BOOK))
                    .displayItems((parameters, output) -> {
                        addEnchantedBooks(parameters.holders(), output, ModEnchantments.NIGHT_VISION);
                        addEnchantedBooks(parameters.holders(), output, ModEnchantments.SMELTING);
                        addEnchantedBooks(parameters.holders(), output, ModEnchantments.LAVA_VISION);
                        addEnchantedBooks(parameters.holders(), output, ModEnchantments.LIFE_STEAL);
                    })
                    .build());

    /**
     * 模组构造器，由 NeoForge 在加载时调用。
     *
     * @param modEventBus  模组事件总线，用于注册 DeferredRegister。
     * @param modContainer 当前模组的容器，用于注册配置与扩展点。
     */
    public EnchantmentExpansion(IEventBus modEventBus, ModContainer modContainer) {
        CREATIVE_MODE_TABS.register(modEventBus);
        // 注册 COMMON 配置，运行时会生成 config/enchantment_expansion-common.toml。
        modContainer.registerConfig(ModConfig.Type.COMMON, EnchantmentConfig.CONFIG_SPEC);
        // 仅在客户端注册游戏内配置界面（在“模组列表 → 配置”中打开），避免专用服务端加载客户端类。
        if (FMLLoader.getDist().isClient()) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                    (container, modListScreen) -> new ConfigurationScreen(container, modListScreen));
        }
    }

    /** 为指定附魔的每一个可获取等级生成一本附魔书。 */
    private static void addEnchantedBooks(
            HolderLookup.Provider registries,
            CreativeModeTab.Output output,
            ResourceKey<Enchantment> enchantmentKey
    ) {
        var enchantment = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantmentKey);
        for (int level = 1; level <= enchantment.value().getMaxLevel(); level++) {
            output.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, level)));
        }
    }
}
