# KARDS 离线复刻

二战题材卡牌游戏 KARDS 的离线复刻项目。按用户要求，**移除一切服务器相关功能**：没有联机、账号、匹配。

## 卡牌数据全部来自真实数据源

卡池不是编的。数据来自 `kards.pandabot.com.cn` 的公开接口，该接口直接对接游戏内卡牌数据，提供：

```
卡名 / 国家 / 卡类 / 部署费用 / 攻击 / 防御 / 稀有度 / 卡池归属 / 效果文本
```

共 **1561 张** 真实卡牌，覆盖十国。每一个数值都能追溯到接口，**没有任何推导值**。

早期版本曾按「兵种系数 × 费用」推导攻防，那属于编造，已全部删除。现在数据源缺什么就留空：接口不提供「行动花费」，`actionCost` 一律为 `0`（未知），不猜。

数据链路：

```
tools/fetch_pandabot.py
  → tools/sources/pandabot_cards.json
  → tools/gen_pool_from_pandabot.py
  → core/src/main/kotlin/com/kards/core/WikiCardPool.kt
```

## 结构

```
core/   规则引擎（纯 Kotlin/JVM，零第三方依赖）
        Model.kt        领域模型：十国、四类卡、五兵种、四档稀有度、词条
        RuleCatalog.kt  规则目录
        Engine.kt       回合、指挥点、部署、移动、攻击、词条结算、胜负
        Deck.kt         卡组校验、游戏模式、Bot
        MetaSystems.kt  军衔、军需箱、勋章、卡池、构筑、经济
        EffectTable.kt  指令与反制效果
        CardPool.kt     卡池入口（5 张总部 + 生成的 1561 张）

app/    Android APK（MD3 + Compose，人机对战 Demo）
```

## CI

全部测试与 APK 构建都在 GitHub Actions 云端运行，本地不跑：

- `test` job：`gradle :core:test`，跑全部单元与集成测试
- `build-apk` job：测试通过后构建 Debug APK，产物上传为 artifact

## Demo 玩法（人机对战）

1. 开局各 40 张卡组，总部 20 防
2. 每回合指挥点回满，槽位上限逐步提升
3. 手牌选中后部署到支援阵线或前线
4. 选中自己的单位可移动或攻击
5. 点击敌方单位或总部发起攻击
6. 点「结束回合」后 Bot 行动
7. 总部防御归零判负

## 已知限制

- 接口不提供行动花费，`actionCost` 为 0
- 反制卡在 Demo 中暂不支持手动激活
- 卡牌效果文本已抓取，但尚未全部落成可执行逻辑

## 构建

```bash
gradle :core:test          # 测试
gradle :app:assembleDebug  # 打包
```