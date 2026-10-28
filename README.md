# Enchantment Expansion（附魔拓展）

一个为 Minecraft 1.21.1（NeoForge）添加更多原版风格附魔的模组。

## 已实现的附魔

| 附魔 | 英文名 | 适用物品 | 效果 |
| --- | --- | --- | --- |
| 夜视 | Night Vision | 头盔 | 佩戴时获得无限持续时间的夜视（无粒子、无图标） |
| 熔炼 | Smelting | 挖掘工具 | 方块掉落物自动烧炼（如铁矿石直接掉铁锭），与精准采集互斥 |
| 熔岩视野 | Lava Vision | 头盔 | 在熔岩下也能看清周围，兼容 Iris / OptiFine 光影 |
| 生命偷取 | Life Steal | 武器 | 攻击时按实际造成的伤害恢复自身生命（比例可配置，最高 3 级） |
| 矿物高亮 | Ore Vision | 镐 | 手持时以真实纹理 + 彩色线框透墙高亮周围矿物，探测半径可配置 |

## 获取方式

所有附魔均为普通附魔（非宝藏），可通过附魔台、村民交易等常规途径获得，也会出现在创造模式的附魔书列表中。模组另提供专属创造模式物品栏「附魔拓展」，直接陈列各附魔不同等级的附魔书。

## 配置

模组使用 NeoForge 的配置系统，运行时生成 `config/enchantment_expansion-common.toml`。可在游戏内「模组列表 → 附魔拓展 → 配置」中图形化调整，或直接编辑配置文件。

| 配置项 | 说明 | 默认值 |
| --- | --- | --- |
| `life_steal.heal_percent_per_level` | 生命偷取每级恢复「实际造成伤害」的比例（0.10 表示 10%） | `0.10`（范围 0.0 ~ 1.0） |
| `ore_vision.radius` | 矿物高亮探测半径（以玩家为中心的正方体边长，单位格） | `8`（范围 1 ~ 64） |

## 下载与安装

将构建出的 JAR 放入 Minecraft 的 `mods` 文件夹即可。依赖 NeoForge 1.21.1，需要 Java 21。

## 构建

```bash
gradlew.bat build
```

成品 JAR 输出到 `build/libs/`。

## 开发环境

- Minecraft 1.21.1
- NeoForge
- Java 21

## 许可

MIT License
