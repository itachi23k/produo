package com.cj.producao.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = VerdeProducao,
    background = VerdeFundo,
    surface = BrancoPuro,
    onPrimary = BrancoPuro,
    onBackground = android.graphics.Color.BLACK.let { androidx.compose.ui.graphics.Color(it) }
)

@Composable
fun ProducaoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}