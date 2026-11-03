package com.notiapp.alert

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notiapp.R

/** Volledig scherm "inkomend gesprek", ook op het vergrendelscherm. */
class RingingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            val active by AlertService.active.collectAsState()
            // Bevestigd of geannuleerd (hier of ergens anders): scherm sluiten.
            LaunchedEffect(active) { if (active == null) finish() }
            val alert = active ?: return@setContent

            val pulse by rememberInfiniteTransition(label = "puls").animateFloat(
                initialValue = 1f, targetValue = 1.15f,
                animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "schaal",
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1F1A17))
                    .safeDrawingPadding()
                    .padding(32.dp),
            ) {
                Column(
                    Modifier.fillMaxWidth().align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        Modifier.size(120.dp).scale(pulse).background(Color(0xFFE8622A), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.NotificationsActive, null, tint = Color.White, modifier = Modifier.size(64.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(alert.message.senderName, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text(
                        alert.message.text, color = Color.White, fontSize = 26.sp,
                        textAlign = TextAlign.Center, lineHeight = 32.sp,
                    )
                }
                Button(
                    onClick = { AlertService.acknowledge(this@RingingActivity, alert.message.alertId) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E9E4F)),
                    modifier = Modifier.fillMaxWidth().height(72.dp).align(Alignment.BottomCenter),
                ) {
                    Text(stringResource(R.string.alert_ok), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
