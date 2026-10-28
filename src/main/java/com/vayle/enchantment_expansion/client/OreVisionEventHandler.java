package com.vayle.enchantment_expansion.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.vayle.enchantment_expansion.EnchantmentConfig;
import com.vayle.enchantment_expansion.EnchantmentExpansion;
import com.vayle.enchantment_expansion.enchantment.ModEnchantmentHelper;
import com.vayle.enchantment_expansion.enchantment.ModEnchantments;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * 矿物高亮（Ore Vision）附魔处理器（仅客户端）。
 * <p>
 * 手持附有矿物高亮附魔的镐子时，扫描以玩家为中心、边长为配置半径的立方体内的矿物方块
 * （通过 {@code #c:ores} 方块标签识别），用「真实纹理的半透明填充面 + 彩色线框」把它们
 * 高亮出来，方便在黑暗中或地下寻找矿脉。
 * <p>
 * 透墙（X-ray）实现说明：高亮必须能穿透前方遮挡的方块显示。这里不通过
 * {@code MultiBufferSource} 的延迟批次提交（那样深度测试状态会被 RenderType 在提交时覆盖），
 * 而是直接用 {@link Tesselator} + {@link BufferUploader} 立即绘制，并在绘制前关闭深度测试、
 * 关闭深度写入，从而让被任意厚度方块遮挡的矿物也能透出来。
 * <p>
 * 性能说明：全量扫描方块开销较大，因此只在固定的 tick 间隔刷新一次缓存列表，
 * 渲染阶段仅消费缓存，避免每帧重复扫描整片区域。
 */
@EventBusSubscriber(modid = EnchantmentExpansion.MOD_ID, value = Dist.CLIENT)
public final class OreVisionEventHandler {
    /** 识别矿物方块的通用标签（同时覆盖原版与模组添加的矿石）。 */
    private static final TagKey<Block> ORE_TAG = TagKey.create(
            Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores"));

    /** 线框粗细（像素）。 */
    private static final float LINE_WIDTH = 2.0F;

    /** 填充面的整体不透明度（0~1），作为颜色乘色的 alpha，越小越透明。 */
    private static final float FILL_ALPHA = 0.55F;

    /** 扫描间隔（tick）。每 10 tick（约 0.5 秒）刷新一次矿物缓存。 */
    private static final int SCAN_INTERVAL = 10;

    /** 当前缓存的矿物条目列表（位置 + 颜色 + 纹理，仅客户端渲染线程访问）。 */
    private static final List<OreEntry> CACHED_ORES = new ArrayList<>();

    /** 上次扫描的 tick 计数，用于限流。 */
    private static long lastScanTick = 0;

    private OreVisionEventHandler() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // 只在透明方块之后渲染，确保线框覆盖在方块上方。
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return;
        }

        // 读取主手镐上的矿物高亮等级；未附魔则清空缓存并跳过。
        int level = ModEnchantmentHelper.getLevel(
                player.getMainHandItem(),
                player.level().registryAccess(),
                ModEnchantments.ORE_VISION
        );
        if (level <= 0) {
            CACHED_ORES.clear();
            return;
        }

        // 限流扫描：仅在间隔到达时刷新缓存。
        long tick = player.level().getGameTime();
        if (tick - lastScanTick >= SCAN_INTERVAL || CACHED_ORES.isEmpty()) {
            lastScanTick = tick;
            scanOres(player);
        }

        if (CACHED_ORES.isEmpty()) {
            return;
        }

        Camera camera = event.getCamera();
        PoseStack poseStack = event.getPoseStack();
        if (poseStack == null) {
            return;
        }

        renderOreOutlines(poseStack, camera);
    }

    /**
     * 直接绘制所有缓存的矿物填充面与线框，绘制前关闭深度测试与深度写入，实现透墙（X-ray）效果。
     */
    private static void renderOreOutlines(PoseStack poseStack, Camera camera) {
        Vec3 cam = camera.getPosition();

        // 在 poseStack 上平移，减去相机位置，使顶点坐标回到相机空间。
        poseStack.pushPose();
        poseStack.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f matrix = poseStack.last().pose();

        // 打开混合，让颜色带半透明效果。
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        // 关键：关闭深度测试 + 关闭深度写入，使矿物透出遮挡方块显示。
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        Tesselator tesselator = Tesselator.getInstance();

        // 第一步：绘制矿物真实材质的半透明填充面（六个面），先画让色块在底层。
        // 使用 POSITION_TEX_COLOR 顶点格式，配合 position_tex_color 着色器，
        // 每顶点写纹理 UV 与颜色乘色，实现「真实纹理 + 半透明」。
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        BufferBuilder fillBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (OreEntry entry : CACHED_ORES) {
            drawTexturedFillBox(fillBuilder, matrix, entry);
        }
        BufferUploader.drawWithShader(fillBuilder.buildOrThrow());

        // 第二步：绘制发光线框（方块 12 条边），覆盖在填充面之上，提供清晰轮廓。
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.lineWidth(LINE_WIDTH);
        BufferBuilder lineBuilder = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        for (OreEntry entry : CACHED_ORES) {
            drawWireBox(lineBuilder, matrix, entry);
        }
        BufferUploader.drawWithShader(lineBuilder.buildOrThrow());

        // 恢复渲染状态，避免影响后续渲染。
        RenderSystem.lineWidth(1.0F);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();

        poseStack.popPose();
    }

    /**
     * 向缓冲区写入一个方块六个面的纹理填充面。
     * <p>
     * 使用矿物的真实纹理（粒子图标，矿石几乎都是 cube_all，六面同纹理），
     * 颜色乘色设为半透明白色，得到「真实材质 + 半透明」的透墙效果。
     * 纹理图集由 {@link TextureAtlasSprite#atlasLocation()} 提供，在绘制前绑定。
     */
    private static void drawTexturedFillBox(BufferBuilder builder, Matrix4f matrix, OreEntry entry) {
        double minX = entry.pos.getX();
        double minY = entry.pos.getY();
        double minZ = entry.pos.getZ();
        double maxX = minX + 1.0;
        double maxY = minY + 1.0;
        double maxZ = minZ + 1.0;

        TextureAtlasSprite sprite = entry.sprite;
        if (sprite == null) {
            return;
        }
        // 绑定该矿物纹理所在的图集。
        RenderSystem.setShaderTexture(0, sprite.atlasLocation());

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        float a = FILL_ALPHA;

        // 每个面 4 个顶点，纹理 UV 按面展开，颜色为半透明白色（乘色不改变纹理本色）。
        // 底面（UV: 0,0 -> 1,1）
        quadTex(builder, matrix, u0, v0, u1, v1, a,
                minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ);
        // 顶面
        quadTex(builder, matrix, u0, v0, u1, v1, a,
                minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ);
        // 西面
        quadTex(builder, matrix, u0, v0, u1, v1, a,
                minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ, minX, minY, maxZ);
        // 东面
        quadTex(builder, matrix, u0, v0, u1, v1, a,
                maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ);
        // 北面
        quadTex(builder, matrix, u0, v0, u1, v1, a,
                minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ);
        // 南面
        quadTex(builder, matrix, u0, v0, u1, v1, a,
                minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ);
    }

    /** 写入一个带纹理的四边形的四个顶点（颜色乘色半透明白）。 */
    private static void quadTex(BufferBuilder builder, Matrix4f matrix,
                                float u0, float v0, float u1, float v1, float a,
                                double x1, double y1, double z1,
                                double x2, double y2, double z2,
                                double x3, double y3, double z3,
                                double x4, double y4, double z4) {
        builder.addVertex(matrix, (float) x1, (float) y1, (float) z1).setUv(u0, v0).setColor(1.0F, 1.0F, 1.0F, a);
        builder.addVertex(matrix, (float) x2, (float) y2, (float) z2).setUv(u1, v0).setColor(1.0F, 1.0F, 1.0F, a);
        builder.addVertex(matrix, (float) x3, (float) y3, (float) z3).setUv(u1, v1).setColor(1.0F, 1.0F, 1.0F, a);
        builder.addVertex(matrix, (float) x4, (float) y4, (float) z4).setUv(u0, v1).setColor(1.0F, 1.0F, 1.0F, a);
    }

    /**
     * 向缓冲区写入一个方块的 12 条边（线框）。顶点直接写入，格式为 POSITION_COLOR。
     */
    private static void drawWireBox(BufferBuilder builder, Matrix4f matrix, OreEntry entry) {
        double minX = entry.pos.getX();
        double minY = entry.pos.getY();
        double minZ = entry.pos.getZ();
        double maxX = minX + 1.0;
        double maxY = minY + 1.0;
        double maxZ = minZ + 1.0;
        // 底面 4 条边。
        line(builder, matrix, entry, minX, minY, minZ, maxX, minY, minZ);
        line(builder, matrix, entry, maxX, minY, minZ, maxX, minY, maxZ);
        line(builder, matrix, entry, maxX, minY, maxZ, minX, minY, maxZ);
        line(builder, matrix, entry, minX, minY, maxZ, minX, minY, minZ);
        // 顶面 4 条边。
        line(builder, matrix, entry, minX, maxY, minZ, maxX, maxY, minZ);
        line(builder, matrix, entry, maxX, maxY, minZ, maxX, maxY, maxZ);
        line(builder, matrix, entry, maxX, maxY, maxZ, minX, maxY, maxZ);
        line(builder, matrix, entry, minX, maxY, maxZ, minX, maxY, minZ);
        // 四条竖棱。
        line(builder, matrix, entry, minX, minY, minZ, minX, maxY, minZ);
        line(builder, matrix, entry, maxX, minY, minZ, maxX, maxY, minZ);
        line(builder, matrix, entry, maxX, minY, maxZ, maxX, maxY, maxZ);
        line(builder, matrix, entry, minX, minY, maxZ, minX, maxY, maxZ);
    }

    /** 写入一条线段（两个顶点）。 */
    private static void line(BufferBuilder builder, Matrix4f matrix, OreEntry entry,
                             double x1, double y1, double z1,
                             double x2, double y2, double z2) {
        builder.addVertex(matrix, (float) x1, (float) y1, (float) z1)
                .setColor(entry.color.r(), entry.color.g(), entry.color.b(), entry.color.a());
        builder.addVertex(matrix, (float) x2, (float) y2, (float) z2)
                .setColor(entry.color.r(), entry.color.g(), entry.color.b(), entry.color.a());
    }

    /** 扫描玩家周围立方体内的矿物方块并写入缓存（含颜色与纹理）。 */
    private static void scanOres(Player player) {
        CACHED_ORES.clear();
        int radius = EnchantmentConfig.CONFIG.oreVisionRadius.get();
        BlockPos center = player.blockPosition();

        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            BlockState state = player.level().getBlockState(pos);
            if (state.is(ORE_TAG)) {
                CACHED_ORES.add(new OreEntry(pos.immutable(), colorOf(state), spriteOf(state)));
            }
        }
    }

    /**
     * 获取方块的真实纹理（粒子图标）。矿石几乎都是 {@code cube_all} 类型，
     * 六个面使用同一纹理，粒子图标即为其表面纹理。
     */
    private static TextureAtlasSprite spriteOf(BlockState state) {
        Minecraft mc = Minecraft.getInstance();
        BakedModel model = mc.getModelManager().getBlockModelShaper().getBlockModel(state);
        return model.getParticleIcon(ModelData.EMPTY);
    }

    /**
     * 根据矿物方块返回对应的高亮颜色。
     * <p>
     * 原版矿物按种类映射专属颜色，方便一眼区分；其他模组添加的矿物无法一一识别，
     * 统一使用兜底颜色 {@link #FALLBACK_COLOR}。
     */
    private static OreColor colorOf(BlockState state) {
        ResourceLocation id = state.getBlockHolder().unwrapKey()
                .map(key -> key.location())
                .orElse(null);
        if (id == null || !"minecraft".equals(id.getNamespace())) {
            return FALLBACK_COLOR;
        }
        return switch (id.getPath()) {
            case "coal_ore", "deepslate_coal_ore" -> COAL;
            case "iron_ore", "deepslate_iron_ore" -> IRON;
            case "copper_ore", "deepslate_copper_ore" -> COPPER;
            case "gold_ore", "deepslate_gold_ore", "nether_gold_ore" -> GOLD;
            case "redstone_ore", "deepslate_redstone_ore" -> REDSTONE;
            case "lapis_ore", "deepslate_lapis_ore" -> LAPIS;
            case "diamond_ore", "deepslate_diamond_ore" -> DIAMOND;
            case "emerald_ore", "deepslate_emerald_ore" -> EMERALD;
            case "nether_quartz_ore" -> QUARTZ;
            case "ancient_debris" -> ANCIENT_DEBRIS;
            default -> FALLBACK_COLOR;
        };
    }

    // ===== 矿物高亮颜色定义（RGB 分量，范围 0~1） =====

    /** 兜底颜色：其他模组添加的矿物统一使用该颜色（淡金色）。 */
    private static final OreColor FALLBACK_COLOR = new OreColor(1.0F, 0.85F, 0.2F, 0.9F);

    private static final OreColor COAL = new OreColor(0.35F, 0.35F, 0.38F, 0.9F);
    private static final OreColor IRON = new OreColor(0.85F, 0.85F, 0.88F, 0.9F);
    private static final OreColor COPPER = new OreColor(1.0F, 0.55F, 0.2F, 0.9F);
    private static final OreColor GOLD = new OreColor(1.0F, 0.85F, 0.1F, 0.9F);
    private static final OreColor REDSTONE = new OreColor(1.0F, 0.2F, 0.2F, 0.9F);
    private static final OreColor LAPIS = new OreColor(0.25F, 0.35F, 0.9F, 0.9F);
    private static final OreColor DIAMOND = new OreColor(0.3F, 0.9F, 0.95F, 0.9F);
    private static final OreColor EMERALD = new OreColor(0.2F, 0.85F, 0.4F, 0.9F);
    private static final OreColor QUARTZ = new OreColor(0.9F, 0.85F, 0.85F, 0.9F);
    private static final OreColor ANCIENT_DEBRIS = new OreColor(0.6F, 0.35F, 0.25F, 0.9F);

    /**
     * 一种矿物高亮颜色（RGBA，分量范围 0~1）。
     */
    private record OreColor(float r, float g, float b, float a) {
    }

    /**
     * 一条缓存的矿物记录：方块位置 + 高亮颜色 + 表面纹理。
     */
    private record OreEntry(BlockPos pos, OreColor color, TextureAtlasSprite sprite) {
    }
}
