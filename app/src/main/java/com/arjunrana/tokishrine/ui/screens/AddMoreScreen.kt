package com.arjunrana.tokishrine.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunrana.tokishrine.data.apps.AppEntry
import com.arjunrana.tokishrine.data.apps.InstalledAppsRepository
import com.arjunrana.tokishrine.data.repo.BlockRepository
import com.arjunrana.tokishrine.data.repo.ConflictingOwnershipException
import com.arjunrana.tokishrine.ui.components.ButtonVariant
import com.arjunrana.tokishrine.ui.components.NocturneAppbar
import com.arjunrana.tokishrine.ui.components.NocturneCtaButton
import com.arjunrana.tokishrine.ui.components.NocturneSegmented
import com.arjunrana.tokishrine.ui.components.SectionLabel
import com.arjunrana.tokishrine.ui.icons.Ph
import com.arjunrana.tokishrine.ui.icons.PhosphorIcon
import com.arjunrana.tokishrine.ui.theme.NocturneTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/*
 * Add more (P7-F5, 8 October §17 addendum): the add-only contents screen an
 * ON block's APPS & SITES row opens — the create-flow contents step without
 * step indicators or Next. Existing targets show included and locked; the
 * full selection goes to BlockRepository.addTargets in one transaction, so
 * additions protect on the next detection emission (a target added during
 * this block's active pause shares it and starts blocking when it ends —
 * Arjun's 8 October rule). No §10 event exists for this flow. Back and
 * Never mind return to the detail screen with nothing saved.
 */
@Composable
fun AddMoreScreen(
    blockId: Long,
    blockRepo: BlockRepository,
    appsRepo: InstalledAppsRepository,
    onDone: () -> Unit,
) {
    val colors = NocturneTheme.colors
    val scope = rememberCoroutineScope()

    // Existing targets reload from the stored block (source of truth); the
    // user's additions survive recreation as names only, with labels
    // re-resolved through InstalledAppsRepository.
    var blockName by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }
    val existingApps = remember { mutableStateListOf<AppEntry>() }
    val existingSites = remember { mutableStateListOf<String>() }
    val appSaver = remember(appsRepo) {
        listSaver<SnapshotStateList<AppEntry>, String>(
            save = { apps -> apps.map { it.packageName } },
            restore = { names ->
                mutableStateListOf<AppEntry>().apply {
                    names.forEach { add(AppEntry(it, appsRepo.labelFor(it), null)) }
                }
            },
        )
    }
    val siteSaver = remember {
        listSaver<SnapshotStateList<String>, String>(
            save = { sites -> sites.toList() },
            restore = { restored -> mutableStateListOf<String>().apply { addAll(restored) } },
        )
    }
    val addedApps = rememberSaveable(saver = appSaver) { mutableStateListOf() }
    val addedSites = rememberSaveable(saver = siteSaver) { mutableStateListOf() }

    LaunchedEffect(blockId) {
        blockRepo.getBlockWithContents(blockId)?.let { stored ->
            blockName = stored.block.name
            existingApps.addAll(stored.apps.map { AppEntry(it.packageName, appsRepo.labelFor(it.packageName), null) })
            existingSites.addAll(stored.sites.map { it.domain })
        }
        // Confirm stays disabled until the block's own targets are loaded:
        // saving a selection that omits them would be a removal attempt.
        loaded = true
    }

    var appsTab by rememberSaveable { mutableStateOf(true) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var siteOwnerships by remember { mutableStateOf<Map<String, Long>?>(null) }
    LaunchedEffect(Unit) {
        siteOwnerships = blockRepo.siteOwnerships()
    }

    var working by remember { mutableStateOf(false) }
    var conflict by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    fun save() {
        working = true
        conflict = false
        failed = false
        scope.launch {
            try {
                blockRepo.addTargets(
                    blockId,
                    existingApps.map { it.packageName } + addedApps.map { it.packageName },
                    existingSites.toList() + addedSites.toList(),
                )
                onDone()
            } catch (owned: ConflictingOwnershipException) {
                conflict = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // TargetRemovalException is unreachable here — the selection
                // always includes every existing target — so this branch can
                // only be a genuine storage failure; nothing was written.
                failed = true
            } finally {
                working = false
            }
        }
    }

    BackHandler { if (showSearch) showSearch = false else onDone() }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
        ) {
            when {
                showSearch -> AppSearchPane(
                    selectedApps = addedApps,
                    ownerBlockId = blockId,
                    blockRepo = blockRepo,
                    appsRepo = appsRepo,
                    onClose = { showSearch = false },
                    lockedPackages = remember(existingApps) { existingApps.map { it.packageName }.toSet() },
                )

                else -> {
                    NocturneAppbar(title = blockName, onBack = onDone)
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        Text(
                            "What should this cover?",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 23.sp,
                            letterSpacing = (-0.2).sp,
                            color = colors.text,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Apps and sites already in this block stay in. You can only add.",
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = colors.neutral.step500,
                        )
                        Spacer(Modifier.height(16.dp))
                        NocturneSegmented(
                            options = listOf(Ph.SquaresFour to "Apps", Ph.Globe to "Websites"),
                            selectedIndex = if (appsTab) 0 else 1,
                            onSelect = { appsTab = it == 0 },
                        )
                        Spacer(Modifier.height(14.dp))

                        if (appsTab) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .background(colors.surface, RoundedCornerShape(10.dp))
                                    .border(1.dp, colors.divider, RoundedCornerShape(10.dp))
                                    .clickable { showSearch = true }
                                    .padding(horizontal = 11.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                PhosphorIcon(Ph.MagnifyingGlass, tint = colors.neutral.step500)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Search your installed apps…",
                                    fontSize = 13.5.sp,
                                    color = colors.neutral.step500,
                                )
                            }
                        } else {
                            SiteAddSection(
                                ownerships = siteOwnerships,
                                ownerBlockId = blockId,
                                onAdd = { domain ->
                                    if (existingSites.none { it == domain } && addedSites.none { it == domain }) {
                                        addedSites.add(domain)
                                    }
                                },
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                        SectionLabel("SELECTED APPS AND SITES")
                        Spacer(Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Locked first: the block's own targets are shown
                            // included and cannot be removed here (removing
                            // still requires turning the block off).
                            existingApps.forEach { entry ->
                                SelectedRow(entry.label, Ph.SquaresFour, onRemove = null)
                            }
                            existingSites.forEach { domain ->
                                SelectedRow(domain, Ph.Globe, onRemove = null)
                            }
                            addedApps.forEach { entry ->
                                SelectedRow(entry.label, Ph.SquaresFour, onRemove = { addedApps.remove(entry) })
                            }
                            addedSites.forEach { domain ->
                                SelectedRow(domain, Ph.Globe, onRemove = { addedSites.remove(domain) })
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Text(
                        "${existingApps.size + existingSites.size + addedApps.size + addedSites.size} selected",
                        fontSize = 12.5.sp,
                        color = colors.neutral.step500,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                    if (conflict) {
                        Text(
                            "Already added to a block",
                            fontSize = 11.5.sp,
                            lineHeight = 17.sp,
                            color = colors.neutral.step300,
                            modifier = Modifier.padding(bottom = 10.dp),
                        )
                    }
                    if (failed) {
                        Text(
                            "Couldn't save that. Try again.",
                            fontSize = 11.5.sp,
                            lineHeight = 17.sp,
                            color = colors.neutral.step300,
                            modifier = Modifier.padding(bottom = 10.dp),
                        )
                    }
                    // P7-F25: the shared CTA pair spec — 50dp/10dp/15sp, a
                    // 12dp gap, ≥16dp below the content above.
                    Spacer(Modifier.height(16.dp))
                    NocturneCtaButton(
                        "Confirm and Save Block",
                        enabled = !working && loaded,
                        onClick = ::save,
                    )
                    Spacer(Modifier.height(12.dp))
                    NocturneCtaButton(
                        "Never mind",
                        variant = ButtonVariant.SECONDARY,
                        enabled = !working,
                        onClick = onDone,
                    )
                }
            }
        }
    }
}
