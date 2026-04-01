package com.cj.producao.data // <--- ISSO É O MAIS IMPORTANTE

import android.os.Environment
import java.io.File

data class ImportData(val estaca: Int, val lado: String = "EIXO")

object FileScanner {



    fun scanMedicoes(contrato: String, servico: String): List<ImportData> {
        val resultados = mutableListOf<ImportData>()

        // Vai até a pasta pública onde o App Registro salva as fotos
        val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val pastaServico = File(root, "Medicoes_BR319/$contrato/$servico")

        if (!pastaServico.exists() || !pastaServico.isDirectory) {
            return emptyList() // Retorna vazio se não houver fotos
        }

        pastaServico.listFiles()?.forEach { pastaEstaca ->
            if (pastaEstaca.isDirectory && pastaEstaca.name.startsWith("Estaca_")) {

                // Extrai o conteúdo após o prefixo. Ex: "4032_D-E" ou apenas "4032"
                val nomeSemPrefixo = pastaEstaca.name.removePrefix("Estaca_")

                // Divide o nome utilizando o underline como separador
                val partes = nomeSemPrefixo.split("_")

                // A primeira parte sempre deverá ser o número da estaca
                val numeroEstaca = partes.getOrNull(0)?.toIntOrNull()

                // A segunda parte é o lado (se existir). O replace devolve a barra "/" no lugar do traço "-"
                val ladoEncontrado = partes.getOrNull(1)?.replace("-", "/") ?: ""

                // Verifica se tem pelo menos 1 arquivo .jpg lá dentro
                val temFoto = pastaEstaca.listFiles()?.any { it.extension.lowercase() == "jpg" } == true

                if (numeroEstaca != null && temFoto) {
                    // Alimenta a lista passando o estaca e o lado reconstruído
                    resultados.add(ImportData(estaca = numeroEstaca, lado = ladoEncontrado))
                }
            }
        }

        // Ordena da menor estaca para a maior
        return resultados.sortedBy { it.estaca }
    }
}