package com.kaizen.khushu.ui.screens.settings


import com.kaizen.khushu.ui.components.KhushuLogoBadge

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons


import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.kaizen.khushu.ui.theme.Antonio
import com.kaizen.khushu.ui.theme.BeVietnamPro
import com.kaizen.khushu.BuildConfig

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.collectAsState

import androidx.compose.ui.text.font.FontWeight


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    viewModel: SettingsViewModel,
    onNavigateSettings: () -> Unit,
    onNavigateCustomize: () -> Unit,
    onNavigateAbout: () -> Unit,
    onDismiss: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.extraLarge,
        dragHandle = null
    ) {
        val animatedFraction by animateFloatAsState(
            targetValue = 0.73f,
            animationSpec = tween(300),
            label = "sheetHeight"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(animatedFraction)
        ) {
            SettingsBrandingHeader(
                logoStyle = settings.logoStyle,
                onDismiss = onDismiss,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(PaddingValues(horizontal = 24.dp, vertical = 12.dp))
            ) {
                MainMenuView(
                    onSettings = onNavigateSettings,
                    onCustomize = onNavigateCustomize
                )

                Spacer(Modifier.weight(1f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SocialCircleButton(
                        imageVector = Icons.Default.Info,
                        onClick = onNavigateAbout
                    )
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.padding(bottom = 50.dp, top = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MainMenuView(
    onSettings: () -> Unit,
    onCustomize: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalAlignment = Alignment.End,
    ) {
        SettingsMenuItem(text = "Settings", onClick = onSettings)
        SettingsMenuItem(text = "Customize", onClick = onCustomize)
    }
}

@Composable
private fun SettingsMenuItem(
    text: String,
    onClick: () -> Unit
) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineLarge.copy(
            fontFamily = Antonio,
            fontSize = 32.sp,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
            .padding(end = 12.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.End
    )
}


@Composable
private fun SettingsBrandingHeader(
    logoStyle: String,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KhushuLogoBadge(
                logoStyle = logoStyle,
                size = 56.dp,
                iconSize = 48.dp
            )
            Spacer(modifier = Modifier.width(18.dp))
            Text(
                text = "Khushu",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = Antonio,
                    fontSize = 28.sp,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SocialCircleButton(
    imageVector: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            modifier = Modifier.size(26.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
