package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.example.ui.MainAppContainer
import com.example.ui.theme.HighDensityBg
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Ensure system windows (status bar, navigation bar) are preserved and not drawn over
    WindowCompat.setDecorFitsSystemWindows(window, true)
    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
          color = HighDensityBg
        ) {
          MainAppContainer()
        }
      }
    }
  }

  override fun onTrimMemory(level: Int) {
    super.onTrimMemory(level)
    AuditApplication.clearAppMemoryCaches(this)
  }
}

