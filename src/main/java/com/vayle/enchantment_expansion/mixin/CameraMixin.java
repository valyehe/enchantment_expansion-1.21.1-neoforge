package com.vayle.enchantment_expansion.mixin;

import com.vayle.enchantment_expansion.enchantment.ModEnchantmentHelper;
import com.vayle.enchantment_expansion.enchantment.ModEnchantments;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 熔岩视野（Lava Vision）附魔的光影兼容补丁。
 * <p>
 * 原版无光影时，{@link Camera#getFluidInCamera()} 返回 {@link FogType#LAVA} 会触发原版熔岩雾，
 * 可被 {@code ViewportEvent.RenderFog} 事件调整。但开启光影后，光影包（如 Iris）会绕过该事件，
 * 自行根据 {@code getFluidInCamera()} 的返回值（即 {@code isEyeInWater} 着色器 uniform）绘制熔岩雾，
 * 导致熔岩视野附魔在光影下失效。
 * <p>
 * 本 Mixin 在相机实体佩戴熔岩视野头盔时，把 {@code getFluidInCamera()} 的熔岩判定结果改为
 * {@link FogType#NONE}，使光影包误以为玩家不在熔岩里，从而跳过自绘的熔岩雾。
 * 该方法全游戏仅有少数纯渲染代码引用，不涉及任何游戏逻辑，因此修改是安全的。
 */
@Mixin(Camera.class)
public abstract class CameraMixin {

    /**
     * 在 {@code getFluidInCamera()} 返回前拦截：仅当原结果为 {@code LAVA} 且佩戴熔岩视野头盔时，
     * 改为返回 {@code NONE}，让光影跳过熔岩雾。
     */
    @Inject(method = "getFluidInCamera", at = @At("RETURN"), cancellable = true)
    private void enchantmentExpansion$clearLavaFog(CallbackInfoReturnable<FogType> cir) {
        // 仅处理熔岩场景，不影响水、雪等其他流体雾。
        if (cir.getReturnValue() != FogType.LAVA) {
            return;
        }

        Camera camera = (Camera) (Object) this;
        // 相机实体必须是生物（玩家），否则无法读取头盔附魔。
        if (!(camera.getEntity() instanceof LivingEntity entity)) {
            return;
        }

        // 读取头盔上的熔岩视野附魔等级；未附魔则保持原样。
        int level = ModEnchantmentHelper.getLevel(
                entity.getItemBySlot(EquipmentSlot.HEAD),
                entity.level().registryAccess(),
                ModEnchantments.LAVA_VISION
        );
        if (level <= 0) {
            return;
        }

        // 有熔岩视野时清除熔岩雾，让光影与原版渲染都跳过熔岩雾。
        cir.setReturnValue(FogType.NONE);
    }
}
