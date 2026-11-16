package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.HexForgeTheme
import io.hexforge.core.common.AppLanguage

class MainActivity : ComponentActivity() {
    private val container by lazy { HexForgeContainer(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: HexForgeViewModel = viewModel(factory = HexForgeViewModel.Factory(container))
            val uiState by vm.uiState.collectAsStateWithLifecycle()
            val isPersian = uiState.preferences.appLanguage == AppLanguage.PERSIAN

            HexForgeTheme(
                isPersianPrimary = isPersian,
                forceRtl = isPersian,
            ) {
                HexForgeShell(
                    state = uiState,
                    viewModel = vm,
                )
            }
        }
    }
}
