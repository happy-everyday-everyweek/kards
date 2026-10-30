package com.kards.core

/**
 * Wiki 规则条目的稳定标识。每条对应 Wiki 上一处可引用的机制描述。
 *
 * 来源页（官方 kards.miraheze.org）：基本机制、卡牌、卡组、国家、词条、游戏模式、军衔、军需箱、
 * 勋章、预备卡池、竞技场卡池、构筑类型、基本构筑思路、新手手册、Kards_Wiki。
 * 来源页（第三方 wiki.biligame.com/kards）：首页、基本知识、卡片查询。
 */
enum class RuleId(val domain: Domain, val cn: String, val source: String) {

    // —— 指挥点 ——
    KREDIT_RESET_TO_SLOTS(Domain.CORE, "每回合开始指挥点被强制设为与指挥点槽数相等", "基本机制"),
    KREDIT_SPENT_ON_ALL_ACTIONS(Domain.CORE, "部署/指令/激活反制/移动/攻击都消耗指挥点", "基本机制"),
    KREDIT_SPEND_VISIBLE_EXCEPT_COUNTER(Domain.CORE, "除反制激活外其余扣除实时可见", "基本机制"),
    KREDIT_CAP_24(Domain.CORE, "指挥点上限 24，溢出舍弃", "基本机制"),
    KREDIT_FROM_ORDERS(Domain.CORE, "部分指令可提供额外指挥点", "基本机制"),
    KREDIT_FROM_UNITS(Domain.CORE, "部分单位效果可提供额外指挥点", "基本机制"),

    // —— 指挥点槽 ——
    SLOT_START_0(Domain.CORE, "开局双方指挥点槽均为 0", "基本机制"),
    SLOT_PLUS_1_PER_TURN_TO_12(Domain.CORE, "每回合开始获得 1 个指挥点槽，至 12", "基本机制"),
    SLOT_CAP_24(Domain.CORE, "指挥点槽上限 24", "基本机制"),
    SLOT_CARDS_BREAK_12(Domain.CORE, "指令与部署效果可突破 12 槽限制", "基本机制"),
    SLOT_REDUCE_SELF(Domain.CORE, "部分卡牌会减少己方指挥点槽", "基本机制"),
    SLOT_REDUCE_ENEMY(Domain.CORE, "部分卡牌会减少敌方指挥点槽", "基本机制"),

    // —— 抽牌与手牌 ——
    DRAW_ONE_PER_TURN(Domain.CORE, "每回合开始抽一张牌", "基本机制"),
    OPENING_FIRST_4_NO_DRAW(Domain.CORE, "先手开局 4 张，第一回合开始不抽牌", "基本机制"),
    OPENING_SECOND_5_PLUS_1(Domain.CORE, "后手开局 5 张，第一回合开始再抽 1 张", "基本机制"),
    HAND_CAP_9_BURN(Domain.CORE, "手牌上限 9，第 10 张被爆掉", "基本机制"),
    BURN_NOT_DISCARD(Domain.CORE, "爆牌不被视作弃牌", "基本机制"),
    DRAW_NOT_ON_OPENING(Domain.CORE, "开局发牌不触发抽取效果", "基本机制"),

    // —— 卡牌类型 ——
    CARD_FOUR_TYPES(Domain.CARD, "卡牌分总部/单位/指令/反制四类", "卡牌"),
    HQ_DEFENSE_20(Domain.CARD, "总部初始 20 防御", "卡牌"),
    HQ_WIN_DOWN_TO_ZERO(Domain.CARD, "总部防御降至 0 及以下即败", "卡牌"),
    HQ_IMMOBILE_SUPPORT_ONLY(Domain.CARD, "总部不能攻击移动，只能处于支援阵线", "卡牌"),
    HQ_ONE_PER_DECK(Domain.CARD, "每卡组有且仅有一张总部", "卡牌"),
    HQ_CANNOT_BE_SUPPRESSED(Domain.CARD, "总部无法被抑制", "卡牌"),
    HQ_CAN_CARRY_EFFECT(Domain.CARD, "总部可在游戏内附加特殊效果且持续整局", "卡牌"),
    HQ_HEAL_CARDS(Domain.CARD, "存在提升总部防御力的卡牌", "卡牌"),
    UNIT_FIVE_TYPES(Domain.CARD, "单位分步兵/坦克/战斗机/轰炸机/炮兵", "卡牌"),
    ORDER_ONE_SHOT(Domain.CARD, "指令一次性触发后移除", "卡牌"),
    ORDER_NAVAL_SUBTYPE(Domain.CARD, "海战版本后部分指令具有海军子类型", "卡牌"),
    COUNTER_ACTIVATED_IN_HAND_HIDDEN(Domain.CARD, "反制在手中激活且敌方不可见", "卡牌"),
    COUNTER_TRIGGER_ON_OPPONENT_TURN(Domain.CARD, "反制在敌方回合满足条件触发", "卡牌"),
    COUNTER_DEACTIVATE_NEXT_TURN(Domain.CARD, "反制未触发则下回合开始取消激活", "卡牌"),
    COUNTER_PRIORITY_OVER_ORDER(Domain.CARD, "反制优先级一般大于指令", "卡牌"),
    COUNTER_COST_HIDDEN(Domain.CARD, "反制激活费用不在敌方视角显示", "卡牌"),
    COUNTER_REMOVED_AFTER_TRIGGER(Domain.CARD, "反制触发后被移除", "卡牌"),
    COUNTER_RARE_IN_USSR(Domain.CARD, "部分国家反制卡很少甚至没有", "卡牌"),
    ORDER_FROM_RANDOM_THREE(Domain.CARD, "彩票类指令从随机三张中选一", "游戏术语"),

    // —— 卡组 ——
    DECK_40_TOTAL(Domain.DECK, "完整卡组为 1 张主国总部 + 39 张其他卡", "卡组"),
    DECK_ALLY_MAX_12(Domain.DECK, "盟国卡牌最多 12 张", "卡组"),
    DECK_ALLY_NO_ELITE(Domain.DECK, "盟国不能携带精英卡", "卡组"),
    DECK_COPY_LIMIT_BY_RARITY(Domain.DECK, "普通 4 / 限定 3 / 特殊 2 / 精英 1", "卡组"),
    DECK_REQUIRES_MAIN_NATION(Domain.DECK, "构筑须选定一个主国", "国家"),
    DECK_MAIN_NATION_UNLIMITED(Domain.DECK, "主国卡牌数量不受限制", "国家"),
    DECK_MAX_30(Domain.DECK, "最多可拥有 30 套卡组", "卡组"),
    DECK_STAR_MARK(Domain.DECK, "可将卡组设为星标并置顶", "卡组"),
    DECK_CODE_SHARE(Domain.DECK, "卡组代码可分享并自动建卡组", "卡组"),
    DECK_CODE_PARSE(Domain.DECK, "卡组代码可在解析器中解析", "卡组"),
    DECK_AUTO_FILL(Domain.DECK, "不足 40 张时可选自动补足", "卡组"),
    DECK_NAME_RULE(Domain.DECK, "默认卡组名为主国名加数字，不能重名", "卡组"),

    // —— 士气伤害 ——
    MORALE_DAMAGE_ON_EMPTY_DRAW(Domain.DECK, "牌库空时抽牌改为总部受士气伤害", "卡组"),
    MORALE_DAMAGE_ESCALATES(Domain.DECK, "士气伤害从 1 起按等差递增", "卡组"),
    MORALE_NO_RESET(Domain.DECK, "士气伤害计数不重置", "卡组"),
    MORALE_TRUE_DAMAGE(Domain.DECK, "士气伤害为真实伤害，不能减少也无视免疫", "卡组"),
    BOND_MORALE(Domain.KEYWORD, "协力条件不满足时总部受士气伤害", "词条"),
    MORALE_GRAY_EFFECT(Domain.DECK, "士气伤害时总部出现灰色特效并提示数值", "卡组"),

    // —— 国家 ——
    NATION_TEN(Domain.NATION, "总计十国", "国家"),
    NATION_FIVE_MAJOR(Domain.NATION, "德英美苏日既可主国也可盟国", "国家"),
    NATION_FIVE_ALLY_ONLY(Domain.NATION, "法意波芬与澳新军团只能做盟国", "国家"),
    NATION_ALLY_ONLY_NO_ELITE(Domain.NATION, "只能做盟国的国家没有精英卡", "国家"),
    NATION_EXILE_BOTH(Domain.NATION, "流亡卡牌可用于原籍或流亡国卡组", "词条"),
    NATION_EXILE_COUNTS_ALLY(Domain.NATION, "流亡卡在计算卡位时占用盟国卡位", "卡组"),
    NATION_EXILE_PRIORITY(Domain.NATION, "同时含原籍与流亡国时流亡卡优先占原籍卡位", "卡组"),
    NATION_EMOTES_MAIN_ONLY(Domain.NATION, "表情只能选主国或通用表情", "国家"),
    NATION_ANZAC_INTRODUCED(Domain.NATION, "澳新军团由 2026 年 6 月扩展包引入", "国家"),

    // —— 前线与阵线 ——
    FRONT_SINGLE_CONTROL(Domain.FRONT, "同一时间只能一方控制前线", "卡牌"),
    FRONT_LINE_CAP_5(Domain.FRONT, "每条阵线最多 5 张（支援阵线含总部）", "基本机制"),
    FRONT_INFANTRY_TANK_NEED_FRONT(Domain.FRONT, "步兵坦克须在前线才能打到敌方支援阵线与总部", "卡牌"),
    ACTION_COST_ON_MOVE_AND_ATTACK(Domain.FRONT, "移动与攻击各需支付行动花费", "基本机制"),
    FRONT_CONTROL_ADVANTAGE(Domain.FRONT, "控制前线可获得战略优势", "卡牌"),
    FRONT_RANGE_CONCEPT(Domain.FRONT, "前线的加入引入了射程与移动的概念", "首页"),

    // —— 兵种 ——
    INFANTRY_ADJACENT_ONLY(Domain.UNIT, "步兵只能攻击相邻阵线的敌人", "卡牌"),
    INFANTRY_STATS_COMPENSATE(Domain.UNIT, "步兵因行动受限往往攻防更高或行动花费更低", "卡牌"),
    INFANTRY_SUBTYPE_COMMANDO(Domain.UNIT, "英美的部分步兵具有突击队子类型", "卡牌"),
    TANK_MOVE_AND_ATTACK(Domain.UNIT, "坦克可同回合移动并攻击，顺序不限", "卡牌"),
    TANK_SUBTYPE(Domain.UNIT, "部分国家的坦克具有子类型", "卡牌"),
    TANK_LIGHT_CHEAP_HEAVY_STRONG(Domain.UNIT, "轻型坦克廉价但薄弱，重型昂贵但强大", "首页"),
    FIGHTER_ANY_LINE(Domain.UNIT, "战斗机可攻击任意阵线目标", "卡牌"),
    FIGHTER_INTERCEPT_BOMBER(Domain.UNIT, "战斗机拦截敌方轰炸机对同阵线的攻击", "卡牌"),
    FIGHTER_NO_SMOKE(Domain.UNIT, "战斗机不能具有烟幕", "词条"),
    BOMBER_NO_RETAL_ATTACKING(Domain.UNIT, "轰炸机攻击非战斗机目标不受反击", "卡牌"),
    BOMBER_NO_RETAL_WHEN_ATTACKED(Domain.UNIT, "轰炸机被攻击时不造成反击伤害", "卡牌"),
    BOMBER_IGNORE_GUARD(Domain.UNIT, "轰炸机可无视守护攻击", "卡牌"),
    ARTILLERY_NO_RETAL(Domain.UNIT, "炮兵攻击时不受反击", "卡牌"),
    ARTILLERY_IGNORE_GUARD(Domain.UNIT, "炮兵可无视守护攻击", "卡牌"),
    ARTILLERY_LOW_STATS(Domain.UNIT, "炮兵同费下攻防通常最低", "卡牌"),
    ARTILLERY_NO_BLITZ(Domain.UNIT, "绝大多数炮兵不具备闪击", "首页"),

    // —— 战斗词条 ——
    KW_AMBUSH(Domain.KEYWORD, "伏击：每回合首次被攻击时先造成反击", "词条"),
    KW_AMBUSH_REFRESH(Domain.KEYWORD, "伏击在所有者下个回合开始时重新获得", "词条"),
    KW_AMBUSH_COUNTERPLAY(Domain.KEYWORD, "可用小单位或轰炸机炮兵先破除伏击", "词条"),
    KW_BLITZ(Domain.KEYWORD, "闪击：加入战场当回合即可行动", "词条"),
    KW_BLITZ_TANK_MOVE_AND_ATTACK(Domain.KEYWORD, "闪击单位仅坦克类可同回合移动并攻击", "词条"),
    KW_FURY(Domain.KEYWORD, "奋战：一回合可攻击两次且各付行动花费", "词条"),
    KW_GUARD(Domain.KEYWORD, "守护：相邻非守护单位与总部被守护", "词条"),
    KW_GUARD_NO_SELF(Domain.KEYWORD, "守护单位无法获得被守护，不能互相守护", "词条"),
    KW_GUARD_NO_SMOKE(Domain.KEYWORD, "守护单位无法获得烟幕", "词条"),
    KW_HEAVY_ARMOR(Domain.KEYWORD, "重甲 X：对战伤害减 X，最多 3", "词条"),
    KW_HEAVY_ARMOR_EFFECT_ONLY(Domain.KEYWORD, "重甲只减免对战伤害，对指令与效果伤害无效", "词条"),
    KW_SHOCK(Domain.KEYWORD, "冲击：首次攻击不吃反击，攻击总部不移除", "词条"),
    KW_SHOCK_VS_AMBUSH(Domain.KEYWORD, "冲击单位攻击伏击单位同样不吃反击", "词条"),
    KW_SMOKE(Domain.KEYWORD, "烟幕：无法被敌方单位攻击，行动后失去", "词条"),
    KW_SMOKE_NO_FRONT(Domain.KEYWORD, "位于前线的单位不能具有烟幕", "词条"),
    KW_SMOKE_EFFECT_STILL_WORKS(Domain.KEYWORD, "烟幕单位仍会受到指令与效果伤害", "词条"),

    // —— 其余词条 ——
    KW_DEPLOY(Domain.KEYWORD, "部署效果在单位部署时触发", "词条"),
    KW_DESTRUCTION(Domain.KEYWORD, "亡计效果在单位被消灭时触发", "词条"),
    KW_REPAIR(Domain.KEYWORD, "修复：防御力增加至最大防御力", "词条"),
    KW_COUNTDOWN(Domain.KEYWORD, "倒计时结束后触发事件", "词条"),
    KW_IMMUNE(Domain.KEYWORD, "免疫：不受伤害但仍可被消灭", "词条"),
    KW_MOBILIZE(Domain.KEYWORD, "动员：回合开始 +1+1，受伤后失去", "词条"),
    KW_PIN(Domain.KEYWORD, "压制：不能移动或攻击，至所有者下回合结束", "词条"),
    KW_ALPINE(Domain.KEYWORD, "山地：每有另一个友方山地则 +1+1", "词条"),
    KW_DEVELOP(Domain.KEYWORD, "开发：随机 3 张中选 1 加入手牌", "词条"),
    KW_INTEL(Domain.KEYWORD, "情报 X：显示对手 X 张非明牌手牌", "词条"),
    KW_INTEL_MAX_3(Domain.KEYWORD, "情报后缀每张卡最多 3", "首页"),
    KW_SUPPRESS(Domain.KEYWORD, "抑制：移除关键词并重置面板", "词条"),
    KW_TAKE_CONTROL(Domain.KEYWORD, "控制：控制权变更但所有者不变", "词条"),
    KW_TAKE_CONTROL_POSITION(Domain.KEYWORD, "被控制单位移到新控制者支援阵线，唯一前线单位除外", "词条"),
    KW_RETREAT(Domain.KEYWORD, "撤退：回支援阵线，满则回手牌", "词条"),
    KW_RETREAT_EXILE_IF_NOWHERE(Domain.KEYWORD, "无法返回手牌时单位被直接移除", "词条"),
    KW_VETERAN(Domain.KEYWORD, "老兵：满足条件后升为老兵并重置面板", "词条"),
    KW_SALVAGE(Domain.KEYWORD, "收缴：获得被消灭单位的 1/1 复制", "词条"),
    KW_SALVAGE_NATION(Domain.KEYWORD, "被收缴单位与收缴它的卡牌同国家", "词条"),
    KW_SALVAGE_KEEP_EFFECT(Domain.KEYWORD, "被收缴单位保留原始单位的所有效果", "词条"),
    KW_PINCER(Domain.KEYWORD, "钳击：与另一友方单位共享加成", "词条"),
    KW_PINCER_BREAK(Domain.KEYWORD, "钳击一方离场则另一方失去加成", "词条"),
    KW_COVERT(Domain.KEYWORD, "隐蔽：未被揭示前不受指令与效果影响", "词条"),
    KW_COVERT_COST_1(Domain.KEYWORD, "隐蔽单位行动花费为 1", "词条"),
    KW_COVERT_REVEAL(Domain.KEYWORD, "隐蔽单位在攻击或被攻击后失去隐蔽", "词条"),
    KW_CONVERT(Domain.KEYWORD, "转换：换成新卡牌并重置伤害与花费", "词条"),
    KW_FORECAST(Domain.KEYWORD, "预报：选择天气派生牌下回合获得", "词条"),
    KW_FORECAST_TIERS(Domain.KEYWORD, "天气预报分 2K/4K/6K 三档", "词条"),
    KW_SACRIFICE(Domain.KEYWORD, "亡命之计类卡牌以弃置自身为代价", "卡组"),
    COMBAT_VS_ATTACK(Domain.KEYWORD, "战斗不等于对战与攻击，且无视类型规则", "词条"),

    // —— 游戏模式 ——
    MODE_RANKED(Domain.MODE, "排位：军衔匹配且唯一晋升军衔的模式", "游戏模式"),
    MODE_CASUAL(Domain.MODE, "休闲：可测卡组但不影响军衔", "游戏模式"),
    MODE_CLASSIC(Domain.MODE, "经典：仅当卡组含预备卡牌才能进入", "游戏模式"),
    MODE_TRAINING(Domain.MODE, "训练：与五位 AI 之一对战", "游戏模式"),
    MODE_ARENA(Domain.MODE, "竞技场：随机国家与卡牌，7 胜或 3 败结算", "游戏模式"),
    MODE_ARENA_ENTRY(Domain.MODE, "竞技场需 150 金币或 20 钻石或一张门票", "游戏模式"),
    MODE_ARENA_LAST_PICK(Domain.MODE, "竞技场最后一抓为精英卡位机制", "游戏模式"),
    MODE_ARENA_PROBABILITY(Domain.MODE, "稀有度越高抽到概率越低", "游戏模式"),
    MODE_ARENA_COPY_DECK(Domain.MODE, "可复制竞技场卡组但同主盟国不可用于其他模式", "游戏模式"),
    MODE_DUEL_CODE(Domain.MODE, "战斗代码：相同代码匹配，3-15 字符", "游戏模式"),
    MODE_DUEL_CODE_CHARSET(Domain.MODE, "战斗代码可用几乎所有字符但不支持 emoji", "游戏模式"),
    MODE_FRIEND_DUEL(Domain.MODE, "好友对战可使用各自卡牌并聊天", "游戏术语"),
    MODE_CAMPAIGN(Domain.MODE, "战役模式首次通关可得卡牌奖励", "基本知识"),
    MODE_DAILY_MISSION(Domain.MODE, "每日任务每天刷新，可获 50~60 金币", "新手手册"),
    MODE_FIRST_WIN_BONUS(Domain.MODE, "每日 PVP 首胜可获 20 金币", "新手手册"),

    // —— 军衔 ——
    RANK_TOTAL_25(Domain.RANK, "共 25 个段位军衔", "军衔"),
    RANK_STARS_5(Domain.RANK, "除元帅外每段 5 颗星", "军衔"),
    RANK_WIN_1_STAR(Domain.RANK, "每次胜利获得 1 颗星", "军衔"),
    RANK_WIN_STREAK_2_STARS(Domain.RANK, "连胜获得 2 颗星且需至少连续 3 胜", "军衔"),
    RANK_LOSE_STREAK_DROP(Domain.RANK, "0 星时连续 3 败会掉段", "军衔"),
    RANK_BELOW_20_NO_DROP(Domain.RANK, "20 段以下失败不降星也不降段", "军衔"),
    RANK_FLOOR_15_10_5_1(Domain.RANK, "达到 15/10/5/1 段后赛季内不再降至更低", "军衔"),
    RANK_FIELD_MARSHAL_CLUB(Domain.RANK, "进入元帅段位加入军官俱乐部并改为积分排名", "军衔"),
    RANK_MIN_5_WINS(Domain.RANK, "至少 5 场排位胜利才能领取赛季结算奖励", "军衔"),
    RANK_SEASON_RESET_25(Domain.RANK, "每月赛季结束所有人失去 25 颗星", "军衔"),
    RANK_TIER_REWARDS(Domain.RANK, "不同段位对应不同赛季结算奖励", "军衔"),

    // —— 军需箱 ——
    CRATE_FIVE_TIERS(Domain.CRATE, "军需箱共五个阶段", "军需箱"),
    CRATE_WEEKLY_RESET(Domain.CRATE, "每周三下午 6 点按进度结算", "军需箱"),
    CRATE_MAX_TIER_CARRY(Domain.CRATE, "到达五级后多余进度保留到下周", "军需箱"),
    CRATE_EXP_200(Domain.CRATE, "军需箱总经验条长度 200", "军需箱"),
    CRATE_COUNTRY_LEVEL_8(Domain.CRATE, "每提升一级国家等级增加 8 点进度", "军需箱"),
    CRATE_PACK_3_6(Domain.CRATE, "普通卡包 3 点、军官卡包 6 点", "军需箱"),
    CRATE_DISMANTLE_EXP(Domain.CRATE, "销毁卡牌按稀有度给 0.8/2.4/8/24 点", "军需箱"),
    CRATE_BATTLE_PASS_UP(Domain.CRATE, "激活战场通票可在开启时再升一级", "军需箱"),

    // —— 勋章 ——
    MEDAL_ACHIEVEMENT(Domain.MEDAL, "勋章是成就系统，完成成就自动解锁", "勋章"),
    MEDAL_FIVE_SLOTS(Domain.MEDAL, "完整勋章最多五个展示栏位", "勋章"),
    MEDAL_ALL_MODES(Domain.MEDAL, "任意模式（含训练与好友对战）都可达成勋章条件", "勋章"),
    MEDAL_HIDE_PROFILE(Domain.MEDAL, "启用隐藏档案后勋章不对外展示", "勋章"),
    MEDAL_LADDER_TIERS(Domain.MEDAL, "黑铁/青铜/白银/黄金/元帅对应 20/15/10/5/1 段", "勋章"),
    MEDAL_KILL_COUNTERS(Domain.MEDAL, "按消灭单位类型计数解锁勋章", "勋章"),
    MEDAL_SET_COLLECTION(Domain.MEDAL, "收集整套扩展包卡牌可解锁对应勋章", "勋章"),

    // —— 卡池 ——
    POOL_RESERVE(Domain.POOL, "预备卡池卡牌不能在排位与休闲使用", "预备卡池"),
    POOL_RESERVE_CLASSIC(Domain.POOL, "预备卡牌仍可在经典模式使用", "预备卡池"),
    POOL_ROTATION_YEARLY(Domain.POOL, "新轮换系统一年一次并按整套轮换", "预备卡池"),
    POOL_SERVICE_MIN_18(Domain.POOL, "每张牌服役期至少 18 个月", "预备卡池"),
    POOL_STANDING_ALWAYS(Domain.POOL, "常备卡池卡牌永不进入预备", "预备卡池"),
    POOL_ROTATION_WARNING(Domain.POOL, "即将轮换的卡牌在合成界面显示提示文本", "预备卡池"),
    POOL_DRAFT_INDEPENDENT(Domain.POOL, "竞技场使用独立的竞技场卡池", "竞技场卡池"),
    POOL_DRAFT_ADJUSTED(Domain.POOL, "少数卡牌出现概率被官方单独调低", "游戏模式"),

    // —— 构筑思路 ——
    BUILD_THREE_CONDITIONS(Domain.BUILD, "卡组需能造成足够伤害、能防住伤害、持续有牌可打", "基本构筑思路"),
    BUILD_DRAW_MIN(Domain.BUILD, "过牌至少 3-4 张、总额外抽牌 6-8 张", "基本构筑思路"),
    BUILD_FAST_DRAW_MORE(Domain.BUILD, "快攻需更多廉价过牌，总额外抽牌 12 张以上", "基本构筑思路"),
    BUILD_SLOW_DRAW_LESS(Domain.BUILD, "慢速过牌过多反而有爆牌卡手风险", "基本构筑思路"),
    BUILD_SMALL_UNITS(Domain.BUILD, "卡组需要前期可部署的小型单位", "基本构筑思路"),
    BUILD_LARGE_UNITS(Domain.BUILD, "卡组需要中后期可部署的大型单位", "基本构筑思路"),
    BUILD_SINGLE_REMOVAL(Domain.BUILD, "单解针对单张单位，快攻宜用软解", "基本构筑思路"),
    BUILD_AOE_REMOVAL(Domain.BUILD, "群解用于清除对手优势场面", "基本构筑思路"),
    BUILD_FAST_NO_AOE(Domain.BUILD, "快攻一般不带群解，亡命之计是例外", "基本构筑思路"),
    BUILD_KEY_CARD(Domain.BUILD, "部分卡组围绕核心卡牌制胜", "卡组"),
    BUILD_OTK(Domain.BUILD, "OTK 卡组争取一回合内爆发斩杀", "卡组"),
    BUILD_FTK(Domain.BUILD, "FTK 指第一回合斩杀，实现概率极低", "游戏术语"),
    BUILD_RESERVE_ONLY_CLASSIC(Domain.BUILD, "含预备卡牌的卡组只能进入经典对战", "卡组"),

    // —— 资源与货币 ——
    CURRENCY_GOLD(Domain.ECONOMY, "金币可购买卡包、竞技场门票与战区", "基本知识"),
    CURRENCY_DIAMOND(Domain.ECONOMY, "钻石为高级货币，可购买卡包与门票", "基本知识"),
    CURRENCY_WILDCARD(Domain.ECONOMY, "万能牌只能制作同稀有度卡牌", "基本知识"),
    CURRENCY_WILDCARD_NATION(Domain.ECONOMY, "部分万能牌有国家限制", "基本知识"),
    CURRENCY_GENERIC_WILDCARD(Domain.ECONOMY, "通用万能牌可制作任意国家同稀有度卡", "基本知识"),
    CURRENCY_SHOP_FREE_CARD(Domain.ECONOMY, "每日可在商店免费获取一张卡牌", "基本知识"),
    CURRENCY_PACK_DUPLICATE(Domain.ECONOMY, "重复卡牌会计入军需箱进度", "基本知识"),
    CURRENCY_DAILY_TASK_LIMIT(Domain.ECONOMY, "每天只能更换一个任务，任务栏最多 3 个", "基本知识"),
    CURRENCY_NEWBIE_7DAY(Domain.ECONOMY, "新手七天签到，第七天赠送精英卡", "新手手册"),
    CURRENCY_EXP_BY_DURATION(Domain.ECONOMY, "对局获得的国家经验与游戏时长正相关", "游戏术语");

    enum class Domain(val cn: String) {
        CORE("基本机制"),
        CARD("卡牌"),
        DECK("卡组"),
        NATION("国家"),
        FRONT("前线"),
        UNIT("兵种"),
        KEYWORD("词条"),
        MODE("游戏模式"),
        RANK("军衔"),
        CRATE("军需箱"),
        MEDAL("勋章"),
        POOL("卡池"),
        BUILD("构筑"),
        ECONOMY("资源")
    }
}

/** Wiki 规则目录：给测试与文档提供来源可追溯的条目集合。 */
object RuleCatalog {
    val all: List<RuleId> get() = RuleId.entries.toList()

    fun byDomain(d: RuleId.Domain): List<RuleId> = all.filter { it.domain == d }

    fun bySource(source: String): List<RuleId> = all.filter { it.source == source }

    fun domains(): List<RuleId.Domain> = RuleId.Domain.entries.toList()

    fun size(): Int = all.size
}