package com.cj.producao.ui.theme

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import java.util.Locale

class SegmentoUIState {
    var lado by mutableStateOf("D")
    var estacaInicial by mutableStateOf("")
    var fracaoInicial by mutableStateOf("0.0")
    var estacaFinal by mutableStateOf("")
    var fracaoFinal by mutableStateOf("0.0")
    var largura by mutableStateOf("0.0")
    var comprimento by mutableStateOf("0.0")
    var altura by mutableStateOf("0.0")

    fun calcularComprimento() {
        val inicio = (estacaInicial.toIntOrNull() ?: 0) * 20 + (fracaoInicial.toDoubleOrNull() ?: 0.0)
        val fim = (estacaFinal.toIntOrNull() ?: 0) * 20 + (fracaoFinal.toDoubleOrNull() ?: 0.0)
        val resultado = fim - inicio
        comprimento = if (resultado > 0) String.format(Locale.US, "%.2f", resultado) else "0.0"
    }

    fun calcularEstacaFinal() {
        val inicioTotal = (estacaInicial.toIntOrNull() ?: 0) * 20 + (fracaoInicial.toDoubleOrNull() ?: 0.0)
        val comp = comprimento.toDoubleOrNull() ?: 0.0
        val fimTotal = inicioTotal + comp

        estacaFinal = (fimTotal / 20).toInt().toString()
        fracaoFinal = String.format(Locale.US, "%.2f", fimTotal % 20)
    }
}