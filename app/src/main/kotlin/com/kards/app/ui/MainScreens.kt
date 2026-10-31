package com.kards.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kards.app.game.Campaign
import com.kards.app.game.CampaignCatalog
import com.kards.app.game.CampaignProgress
import com.kards.app.game.DeckEditorState
import com.kards.app.game.CampaignStrategy
import com.kards.app.game.Mission
import com.kards.app.game.MissionGoal
import com.kards.core.CardType
import com.kards.core.Nation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    completed: Int,
    hasDeck: Boolean,
    onCampaigns: () -> Unit,
    onDeck: () -> Unit,
    onQuickBattle: () -> Unit
) {
    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("KARDS · 离线战场") }) }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(24.dp)) {
                        Text("战争艺术", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("选择战役，构筑卡组，在离线对战中击败指挥官。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(20.dp))
                        Button(onClick = onCampaigns, Modifier.fillMaxWidth()) { Text("进入战区") }
                    }
                }
            }
            item {
                OutlinedCard(Modifier.fillMaxWidth().clickable(onClick = onDeck)) {
                    ListItem(headlineContent = { Text("我的卡组") },
                        supportingContent = { Text(if (hasDeck) "39 张卡牌 · 已可用于对战" else "从真实卡池选择并构筑 39 张卡牌") },
                        trailingContent = { Text("编辑") })
                }
            }
            item {
                OutlinedCard(Modifier.fillMaxWidth().clickable(onClick = onQuickBattle)) {
                    ListItem(headlineContent = { Text("快速人机对战") },
                        supportingContent = { Text("使用自定义卡组，未构筑时自动生成") },
                        trailingContent = { Text("开始") })
                }
            }
            item { Text("战役进度 $completed / ${CampaignCatalog.all.size}", style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignScreen(progress: CampaignProgress, onSelect: (Campaign) -> Unit, onBack: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("战区") }, navigationIcon = { TextButton(onClick = onBack) { Text("返回") } })
    }) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("五个单人 PVE 历史战役，选择你的阵营与对手。",
                Modifier.padding(bottom = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(CampaignCatalog.all) { campaign ->
                ElevatedCard(Modifier.fillMaxWidth().clickable { onSelect(campaign) }) {
                    Column(Modifier.padding(20.dp)) {
                        Text(campaign.subtitle, style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary)
                        Text(campaign.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${campaign.nation.cn} 对阵 ${campaign.enemy.cn} · ${campaign.objective}")
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AssistChip(onClick = { onSelect(campaign) },
                                label = { Text("历史战役 · 特殊任务待核实") })
                            Spacer(Modifier.weight(1f))
                            Text(if (progress.isCompleted(campaign.id)) "已通关" else "开始战役",
                                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignDetailScreen(
    campaign: Campaign,
    progress: CampaignProgress,
    onStart: (Mission, CampaignStrategy) -> Unit,
    onBack: () -> Unit
) {
    var selectedStrategyId by remember(campaign.id) {
        mutableStateOf(progress.strategy(campaign.id) ?: campaign.strategies.first().id)
    }
    val strategy = campaign.strategies.first { it.id == selectedStrategyId }
    var revision by remember { mutableIntStateOf(0) }
    revision
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(campaign.title) },
            navigationIcon = { TextButton(onClick = onBack) { Text("返回") } }
        )
    }) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("${campaign.nation.cn} 对阵 ${campaign.enemy.cn}",
                    style = MaterialTheme.typography.titleMedium)
                Text("选择一条策略路线，策略会改变整场战役的卡组加成。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    campaign.strategies.forEach { s ->
                        val chosen = s.id == selectedStrategyId
                        OutlinedCard(Modifier.fillMaxWidth().clickable {
                            selectedStrategyId = s.id
                            progress.selectStrategy(campaign.id, s.id)
                            revision++
                        }) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = chosen, onClick = {
                                    selectedStrategyId = s.id
                                    progress.selectStrategy(campaign.id, s.id)
                                    revision++
                                })
                                Column(Modifier.weight(1f)) {
                                    Text(s.name, fontWeight = FontWeight.SemiBold)
                                    Text(s.description, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
            item { Text("作战阶段", style = MaterialTheme.typography.titleMedium) }
            items(campaign.missions) { mission ->
                val unlocked = mission.index <= progress.unlocked(campaign.id)
                val stars = progress.stars(campaign.id, mission.index)
                OutlinedCard(Modifier.fillMaxWidth().clickable(enabled = unlocked) {
                    onStart(mission, strategy)
                }) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(mission.title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text(if (!unlocked) "未解锁" else if (stars > 0) "★".repeat(stars) else "可挑战")
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(mission.brief, style = MaterialTheme.typography.bodySmall)
                        Text("目标：${mission.goal.cn} · 奖励：${mission.reward}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckEditorScreen(editor: DeckEditorState, onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    refresh
    Scaffold(topBar = {
        TopAppBar(title = { Text("卡组编辑 · ${editor.cards.size}/39") },
            navigationIcon = { TextButton(onClick = { editor.save(); onBack() }) { Text("保存") } },
            actions = { TextButton(onClick = { editor.autoFill(); refresh++ }) { Text("自动补全") } })
    }) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp)) {
            LazyRowCompat {
                listOf(Nation.GERMANY, Nation.USSR, Nation.USA, Nation.UK, Nation.JAPAN).forEach { nation ->
                    FilterChip(selected = editor.nation == nation, onClick = { editor.changeNation(nation); refresh++ },
                        label = { Text(nation.cn) }, modifier = Modifier.padding(end = 6.dp))
                }
            }
            OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text("搜索当前国家卡牌") }, singleLine = true)
            Spacer(Modifier.height(10.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                items(editor.available(query), key = { it.id }) { card ->
                    val count = editor.count(card)
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${card.cost}K · ${card.name}", fontWeight = FontWeight.SemiBold)
                                Text(if (card.type == CardType.UNIT) "${card.unitType?.cn} · ${card.attack}/${card.defense} · ${card.rarity.cn}"
                                else "${card.type.cn} · ${card.rarity.cn}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text("$count/${card.rarity.maxCopies}", Modifier.padding(horizontal = 8.dp))
                            FilledTonalIconButton(onClick = { editor.remove(card); refresh++ }, enabled = count > 0) { Text("−") }
                            Spacer(Modifier.width(6.dp))
                            FilledTonalIconButton(onClick = { editor.add(card); refresh++ },
                                enabled = count < card.rarity.maxCopies && editor.cards.size < 39) { Text("+") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LazyRowCompat(content: @Composable () -> Unit) {
    androidx.compose.foundation.lazy.LazyRow { item { Row { content() } } }
}