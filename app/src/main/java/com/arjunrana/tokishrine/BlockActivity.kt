package com.arjunrana.tokishrine

import androidx.activity.ComponentActivity
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunrana.tokishrine.data.repo.EventRepository
import com.arjunrana.tokishrine.ui.theme.NocturneTheme

/*
 * Phase 4 block-screen placeholder (phase-04 scope): plain text naming
 * the trigger, launched by the detection service. The real block screen —
 * humour assets, Anton, walk-away accounting, the Phase 5 challenge — is
 * deliberately not here. Back simply finishes; no overlay permission is
 * involved (accessibility services carry the background-launch exemption,
 * PRD §13/§14).
 */
class BlockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val triggerType = intent?.getStringExtra(EXTRA_TRIGGER_TYPE)
        val target = intent?.getStringExtra(EXTRA_TARGET)
        if (!triggerType.isNullOrBlank() && !target.isNullOrBlank()) {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            )
            val blockName = intent?.getStringExtra(EXTRA_BLOCK_NAME).orEmpty()
            setContent {
                NocturneTheme {
                    val colors = NocturneTheme.colors
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(colors.bg)
                            .statusBarsPadding()
                            .padding(20.dp),
                    ) {
                        Column(Modifier.align(Alignment.CenterStart)) {
                            if (blockName.isNotEmpty()) {
                                Text(
                                    blockName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.neutral.step500,
                                )
                            }
                            Text(
                                target,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.text,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            Text(
                                if (triggerType == EventRepository.TARGET_TYPE_APP) {
                                    "App trigger"
                                } else {
                                    "Site trigger"
                                },
                                fontSize = 13.5.sp,
                                color = colors.neutral.step500,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
            }
        } else {
            // Nothing nameable (defensive: only the service launches this)
            // — no placeholder to show, so don't linger.
            finish()
        }
    }

    companion object {
        const val EXTRA_TRIGGER_TYPE = "trigger_type"
        const val EXTRA_TARGET = "target"
        const val EXTRA_BLOCK_NAME = "block_name"
    }
}
