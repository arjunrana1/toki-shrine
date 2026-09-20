package com.arjunrana.tokishrine.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable

/** Debug-only host whose content is reinstalled from onCreate on recreation. */
class CreateFlowTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { content?.invoke() }
    }

    companion object {
        var content: (@Composable () -> Unit)? = null
    }
}
