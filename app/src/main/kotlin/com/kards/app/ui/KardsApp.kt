package com.kards.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kards.app.game.GameController
import com.kards.core.CardType
import com.kards.core.Line
import com.kards.core.UnitInstance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KardsApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val progress = remember { com.kards.app.game.CampaignProgress(context) }
    val editor = remember { com.kards.app.game.DeckEditorState(context) }
    var screen by remember { mutableStateOf("home") }
    var campaign by remember { mutableStateOf<com.kards.app.game.Campaign?>(null) }
    var mission by remember { mutableStateOf<com.kards.app.game.Mission?>(null) }
    var strategy by remember { mutableStateOf<com.kards.app.game.CampaignStrategy?>(null) }
    var progressRevision by remember { mutableIntStateOf(0) }
    when (screen) {
        "home" -> HomeScreen(progress.completedCount(), editor.isReady,
            { screen = "campaigns" }, { screen = "deck" },
            { campaign = null; mission = null; strategy = null; screen = "battle" })
        "campaigns" -> CampaignScreen(progress, { campaign = it; screen = "campaignDetail" }, { screen = "home" })
        "campaignDetail" -> {
            progressRevision
            val current = campaign
            if (current != null) {
                CampaignDetailScreen(current, progress,
                    onStart = { m, s -> mission = m; strategy = s; screen = "battle" },
                    onBack = { screen = "campaigns" })
            }
        }
        "deck" -> DeckEditorScreen(editor) { screen = "home" }
        else -> {
            progressRevision
            val current = campaign
            val currentMission = mission
            BattleScreen(current, currentMission, strategy, editor.toDeck(), { stars ->
                if (current != null && currentMission != null) {
                    progress.complete(current.id, currentMission.index, stars)
                }
                progressRevision++
            }) {
                screen = if (current != null) "campaignDetail" else "home"
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BattleScreen(
    campaign: com.kards.app.game.Campaign?,
    mission: com.kards.app.game.Mission?,
    strategy: com.kards.app.game.CampaignStrategy?,
    deck: com.kards.core.Deck?,
    onMissionComplete: (Int) -> Unit,
    onExit: () -> Unit
) {
    val game = remember(campaign, mission, strategy, deck) {
        GameController(campaign, mission, strategy, deck, onMissionComplete)
    }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(game.revision, game.toast) {
        val t = game.toast
        if (t != null) {
            snackbar.showSnackbar(t)
            game.clearToast()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("KARDS", fontWeight = FontWeight.SemiBold)
                        Text(mission?.title ?: campaign?.title ?: "快速人机对战 · 离线规则引擎",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = { TextButton(onClick = onExit) { Text("退出") } },
                actions = {
                    IconButton(onClick = { game.newGame() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "重开")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                HqBar(
                    label = "BOT 总部",
                    defense = game.enemyHqDefense,
                    onClick = { if (game.selectedUnit != null) game.attackEnemyHq() }
                )
                LineRow(
                    title = "敌方前线",
                    units = game.enemyFront,
                    onUnitClick = { game.attackEnemy(it) }
                )
                LineRow(
                    title = "敌方支援阵线",
                    units = game.enemySupport,
                    onUnitClick = { game.attackEnemy(it) }
                )

                Spacer(Modifier.height(8.dp))
                Divider2()
                Spacer(Modifier.height(8.dp))

                LineRow(
                    title = "我方支援阵线",
                    units = game.mySupport,
                    highlight = true,
                    onUnitClick = { game.selectUnit(it) },
                    selected = game.selectedUnit
                )
                LineRow(
                    title = "我方前线",
                    units = game.myFront,
                    highlight = true,
                    onUnitClick = { game.selectUnit(it) },
                    selected = game.selectedUnit
                )

                HqBar(
                    label = "我的总部",
                    defense = game.myHqDefense,
                    onClick = { }
                )

                Spacer(Modifier.height(4.dp))
                ActionBar(game, mission)
                HandRow(game)
            }
        }
    }
}

@Composable
private fun Divider2() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@Composable
private fun HqBar(label: String, defense: Int, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.weight(1f))
            Text(
                "$defense",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(" 防", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun LineRow(
    title: String,
    units: List<UnitInstance>,
    highlight: Boolean = false,
    onUnitClick: (UnitInstance) -> Unit,
    selected: UnitInstance? = null
) {
    Column(Modifier.padding(horizontal = 12.dp, vertical = 2.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (units.isEmpty()) {
                Text(
                    "—",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            units.forEach { u ->
                UnitChip(
                    unit = u,
                    isSelected = selected === u,
                    isMine = highlight,
                    onClick = { onUnitClick(u) }
                )
            }
        }
    }
}

@Composable
private fun UnitChip(
    unit: UnitInstance,
    isSelected: Boolean,
    isMine: Boolean,
    onClick: () -> Unit
) {
    val container = when {
        isSelected -> MaterialTheme.colorScheme.tertiaryContainer
        isMine -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.errorContainer
    }
    Card(
        modifier = Modifier
            .width(84.dp)
            .height(58.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 1.dp
        )
    ) {
        Column(
            Modifier
                .padding(6.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                unit.card.name,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${unit.attack}/${unit.defense}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                if (unit.damage > 0) {
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "-${unit.damage}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionBar(game: GameController, mission: com.kards.app.game.Mission?) {
    Column(Modifier.padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "指挥点 ${game.myKredits}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "槽 ${game.mySlots}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
            if (!game.isMyTurn) {
                AssistChip(onClick = {}, label = { Text("Bot 行动中…") })
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { game.endTurn() },
                enabled = game.isMyTurn
            ) {
                Text("结束回合")
            }
        }

        Spacer(Modifier.height(6.dp))

        mission?.let { m ->
            val progressText = when (m.goal) {
                com.kards.app.game.MissionGoal.KILL_UNITS -> "已消灭 ${game.kills}/${m.goalAmount}"
                com.kards.app.game.MissionGoal.FRONT_UNITS -> "前线单位 ${game.myFront.size}/${m.goalAmount}"
                com.kards.app.game.MissionGoal.DESTROY_NAMED_HQ -> "摧毁${m.goalTarget}总部"
                com.kards.app.game.MissionGoal.DESTROY_HQ -> "摧毁敌方总部"
            }
            Text("任务：$progressText",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary)
            val stars = game.challengeStatus()
            if (stars.isNotEmpty()) {
                Text(
                    stars.joinToString("　") { (c, ok) -> (if (ok) "★" else "☆") + c.text },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val u = game.selectedUnit
        if (u != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (u.line == Line.SUPPORT) {
                    FilledTonalButton(onClick = { game.moveSelectedTo(Line.FRONT) }) {
                        Text("上前线")
                    }
                } else {
                    FilledTonalButton(onClick = { game.moveSelectedTo(Line.SUPPORT) }) {
                        Text("回支援")
                    }
                }
                Text(
                    "点击敌方单位或总部可攻击",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
            Spacer(Modifier.height(6.dp))
        }

        val hi = game.selectedHandIndex
        if (hi >= 0 && hi < game.hand.size) {
            val card = game.hand[hi].card
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (card.type == CardType.UNIT) {
                    FilledTonalButton(onClick = { game.deploySelected(Line.SUPPORT) }) {
                        Text("部署到支援")
                    }
                    FilledTonalButton(onClick = { game.deploySelected(Line.FRONT) }) {
                        Text("部署到前线")
                    }
                } else if (card.type == CardType.ORDER) {
                    FilledTonalButton(onClick = { game.playSelectedOrder() }) {
                        Text("使用指令")
                    }
                } else if (card.type == CardType.COUNTERMEASURE) {
                    FilledTonalButton(onClick = { game.playSelectedCounter() }) {
                        Text("激活反制")
                    }
                } else {
                    Text(
                        "该卡暂时无法使用",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun HandRow(game: GameController) {
    Column {
        Text(
            "手牌 ${game.hand.size}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp)
        )
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(game.hand) { index, hc ->
                val card = hc.card
                val selected = game.selectedHandIndex == index
                val affordable = hc.cost <= game.myKredits
                Card(
                    modifier = Modifier
                        .width(96.dp)
                        .height(92.dp)
                        .clickable { game.selectHand(index) }
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (affordable)
                            MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        Modifier
                            .padding(8.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(22.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(6.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${hc.cost}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            Text(
                                card.rarity.cn,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            card.name,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 13.sp
                        )
                        Text(
                            if (card.type == CardType.UNIT)
                                "${card.attack}/${card.defense}"
                            else card.type.cn,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}