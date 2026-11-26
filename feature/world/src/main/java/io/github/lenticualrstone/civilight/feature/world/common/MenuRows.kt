package io.github.lenticualrstone.civilight.feature.world.common

import androidx.compose.runtime.Composable
import io.github.lenticualrstone.civilight.designsystem.component.MenuDeleteRow

/**
 * (2026-11-26) The `⋯` menu's last row `지우기`: the shared designsystem [MenuDeleteRow] (own divider, trash icon
 * and label in `warn`); [subtitle] (`읽기 전용`) under it.
 */
@Composable
fun DeleteMenuRow(title: String, subtitle: String?, onClick: (() -> Unit)?) = MenuDeleteRow(title, onClick, subtitle = subtitle)
