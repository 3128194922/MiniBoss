# MiniBoss

Rainbow Power 整合包的迷你 Boss 系统，使用 Forge 1.20.1 + Java 实现，替代原先的 KubeJS `mini_boss_system`。

## 功能

- 负幸运攻击按概率把目标转化为迷你 Boss。
- 血月自然生成通过 KubeJS 薄桥接调用 `MiniBossApi.convert(entity)`。
- 17 种词条、1~5 级，保留原 KubeJS 的 NBT 键，旧存档与 Jade 显示兼容。
- 只有真正转化后的 `Mob` 才挂载 AI Goal，不再给所有实体类型挂 8 个 Goal。
- AI 词条：自愈、援军、领袖、挖掘、隐匿、演化。
- 事件词条：寄生、适应、自爆、缴械、重装、驱散、暗影、刺客、刚性、狂怒、吸血。
- 迷你 Boss 额外掉落与 Dyeing 外观（Dyeing 可选）。
- 提供 `/miniboss` 管理命令，便于测试和排障。

## 环境

- Minecraft 1.20.1
- Forge 47.4.6+
- Java 17

## 词条

| Key | 中文 | 等级 | 效果 |
|---|---|---:|---|
| `Dig` | 挖掘 | 1~2 | 目标与自身之间有方块时，沿视线挖掘挡路方块 |
| `Regenerate` | 自愈 | 1~3 | 每 20 tick 回复 `等级` 点生命 |
| `Reinforce` | 援军 | 1~3 | 首次锁定目标时召唤 `等级` 只同类援军 |
| `Leader` | 领袖 | 1 | 每 40 tick 给 16 格内同类施加抗性/速度 |
| `Stealth` | 隐匿 | 1 | 无目标时持续隐身 |
| `Evolve` | 演化 | 1 | 按实体类型分化：女巫药水、蜘蛛网、爬行者跳跃、骷髅速度、史莱姆体型 |
| `Parasite` | 寄生 | 1~3 | 受击时召唤 `等级` 只蠹虫，40 tick 冷却 |
| `Adapt` | 适应 | 1~3 | 记录伤害类型，重复类型减伤 25%/50%/75% |
| `Explode` | 自爆 | 1~5 | 死亡时爆炸，强度 `等级 × 5` |
| `Disarm` | 缴械 | 1~5 | 攻击时按 `等级 × 5%` 概率卸下目标装备 |
| `HeavyArmor` | 重装 | 1~5 | 转化时护甲基础值 `+等级 × 5` |
| `Dispel` | 驱散 | 1~5 | 攻击时按 `等级 × 5%` 概率清除目标正面效果 |
| `Shadow` | 暗影 | 1 | 伤害增加 `15 - 所处亮度` |
| `Assassin` | 刺客 | 1~5 | 隐身时伤害 `+等级 × 2` |
| `Rigidity` | 刚性 | 1 | 低于 15 点伤害 ×0.2，高于 15 点 ×1.8 |
| `Fury` | 狂怒 | 1 | 血量越低伤害越高，最高 +100% |
| `Drain` | 吸血 | 1 | 造成伤害的 50% 回复自身 |

## 配置

首次启动生成 `config/miniboss-common.toml`。

```toml
entityBlacklist = ["minecraft:villager", "blockedmod:*"]
powerBlacklist = ["minecraft:creeper=Explode,Evolve", "*=Parasite"]
enableDyeingArmor = true
enableExtraLoot = true
healthMultiplier = 3.0
maxDraws = 4
logConversions = false
```

- `entityBlacklist`：命中实体完全不参与迷你 Boss 转化，支持精确 id 与 `namespace:*`。
- `powerBlacklist`：格式 `实体模式=词条1,词条2`；`*` 表示全局禁用。
- `maxDraws`：转化时最多抽几次，重复词条不会重复添加。
- 已有存档中的 `isMiniBoss` / `POWER` / `POWER_LEVELS` 不会被重新随机。

## 命令

需要权限等级 2：

```text
/miniboss convert <target>
/miniboss info <target>
/miniboss add <target> <power> [level]
/miniboss remove <target> <power>
/miniboss clear <target>
```

示例：

```mcfunction
/summon minecraft:zombie ~ ~ ~
/miniboss convert @e[type=minecraft:zombie,limit=1,sort=nearest]
/miniboss add @e[type=minecraft:zombie,limit=1,sort=nearest] Explode 5
/miniboss info @e[type=minecraft:zombie,limit=1,sort=nearest]
```

## KubeJS 迁移

原 KubeJS 实现已整体停用，源码保留为：

```text
server_scripts/mini_boss_system/*.js.disabled
startup_scripts/mini_boss_system/damage.js.disabled
```

保留的 KubeJS 文件只有：

- `server_scripts/mini_boss_system/bridge.js`：把 `global.convertToMiniBoss(entity)` 转发到 `MiniBossApi`。
- `startup_scripts/mini_boss_system/Jade.js`：读取兼容 NBT 并在 Jade 显示词条。

血月脚本已改为调用 `global.convertToMiniBoss(entity)`；负幸运转化、伤害词条、自爆、掉落和 AI 全部由本 mod 处理。

## 兼容性

- 保留 NBT 键：`isMiniBoss`、`POWER`、`POWER_LEVELS`、`_mb_p_*`、`_mb_lvl_*`、`_mb_adapt_types`。
- 旧存档中的迷你 Boss 会在实体进入世界时自动补挂对应 AI Goal。
- `fieldguide:bosses` 标签内的实体不会被转化。
- `dyeing` 可选；未安装时跳过外观命令。
- 不硬依赖 Jade、KubeJS、Curios 或 Dyeing。
- 不修改客户端渲染；Jade 文本仍由 KubeJS 提供。

## 构建

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
.\gradlew.bat build
```

产物：`build/libs/miniboss-1.0.0.jar`

## 测试

```powershell
.\gradlew.bat runGameTestServer
```

当前 GameTest：

| 测试 | 验证内容 | 状态 |
|---|---|---|
| `convertsZombie` | 僵尸可转化、写入标记、抽到词条、生命生效 | 通过 |
| `refusesDoubleConversion` | 同一实体不会重复转化 | 通过 |
| `writesLegacyKubeJsKeys` | `isMiniBoss` / `POWER` / `POWER_LEVELS` 兼容旧 KubeJS | 通过 |

验证边界：

- `gradlew build` 通过。
- `runGameTestServer` 3 个测试通过。
- 完整 Rainbow Power 整合包冒烟被既有 `caverns_and_chasms` 的 `Deeper.createAttributes()` `NoSuchMethodError` 阻断，该错误与本 mod 无关；MiniBoss 在崩溃前已成功加载。
- 尚未进行多人、长时战斗、所有 17 词条逐项实机验收。

## 目录

```text
src/main/java/com/rainbow/miniboss/
  api/MiniBossApi.java          KubeJS/其他 mod 可调用的稳定入口
  data/MiniBossData.java        NBT 兼容读写
  logic/MiniBossConversion.java 转化、黑名单、词条抽取
  logic/MiniBossGoals.java      AI Goal
  logic/MiniBossEvents.java     伤害/死亡/掉落/加入世界事件
  logic/MiniBossLoot.java       额外掉落
  logic/MiniBossCommands.java   /miniboss 命令
  power/MiniBossPower.java      词条枚举
```

## 许可证

MIT，见 [LICENSE](LICENSE)。

## 仓库

https://github.com/3128194922/MiniBoss
