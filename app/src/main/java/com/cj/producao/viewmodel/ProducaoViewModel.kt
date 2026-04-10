package com.cj.producao.viewmodel

import android.app.Application
import android.provider.Settings
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cj.producao.data.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.*

class ProducaoViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).producaoDao()
    private val prefs = application.getSharedPreferences("config_pref", Context.MODE_PRIVATE)

    var acessoAutorizado by mutableStateOf(prefs.getBoolean("is_active", false))
    var mensagemErro by mutableStateOf("")

    // Coleta a lista de relatórios do banco local
    val todosRelatorios: Flow<List<RelatorioComSegmentosRoom>> = dao.getRelatoriosComSegmentosRoom()

    val androidId: String = Settings.Secure.getString(
        application.contentResolver,
        Settings.Secure.ANDROID_ID
    )?.uppercase() ?: "UNKNOWN"

    // --- FUNÇÃO QUE RESOLVE O ERRO DE COMPILAÇÃO DA ACTIVITY ---
    suspend fun buscarRelatorio(id: Int): RelatorioComSegmentosRoom? {
        return withContext(Dispatchers.IO) {
            dao.getRelatorioPorId(id)
        }
    }

    suspend fun carregarDadosParaEdicao(id: Int): RelatorioComSegmentosRoom? {
        return withContext(Dispatchers.IO) {
            // Busca o dado bruto do Banco
            val dados = dao.getRelatorioPorId(id)

            dados?.let { item ->
                // Processa a String de materiais e transforma em Lista de Objetos aqui no fundo!
                val listaMateriaisProcessada = item.relatorio.materiaisJson?.split("|")?.mapNotNull { str ->
                    val partes = str.split(":")
                    if (partes.size == 2) {
                        MaterialItem(partes[0], partes[1].toDoubleOrNull())
                    } else null
                } ?: emptyList()

                // Retornamos o objeto "Turbinado" com a lista de materiais já pronta
                item.copy(materiais = listaMateriaisProcessada)
            }
        }
    }




    fun verificarLicenca() {
        val db = FirebaseFirestore.getInstance()
        db.collection("fiscais_autorizados").document(androidId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val ativo = doc.getBoolean("ativo") ?: false
                    val validadeStr = doc.getString("validade") ?: "2000-01-01"
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val validadeDate = try { sdf.parse(validadeStr) } catch (e: Exception) { Date(0) }

                    if (ativo && validadeDate != null && validadeDate.after(Date())) {
                        acessoAutorizado = true
                        prefs.edit().putBoolean("is_active", true).apply()
                    } else {
                        acessoAutorizado = false
                        mensagemErro = "Licença expirada ou suspensa."
                        prefs.edit().putBoolean("is_active", false).apply()
                    }
                } else {
                    acessoAutorizado = false
                    mensagemErro = "ID não autorizado.\nID: $androidId"
                    prefs.edit().putBoolean("is_active", false).apply()
                }
            }
    }

    fun salvarOuAtualizar(relatorio: RelatorioEntity, segmentos: List<SegmentoEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            if (relatorio.id == 0) {
                // Novo Relatório
                val idGerado = dao.insertRelatorio(relatorio.copy(statusSinc = "LOCAL"))
                dao.insertSegmentos(segmentos.map { it.copy(relatorioId = idGerado.toInt()) })
            } else {
                // Atualização de Relatório Existente
                dao.updateRelatorio(relatorio.copy(statusSinc = "LOCAL"))
                dao.deletarSegmentosDoRelatorio(relatorio.id)
                dao.insertSegmentos(segmentos.map { it.copy(relatorioId = relatorio.id) })
            }
        }
    }

    fun deletarRelatorioCompleto(item: RelatorioComSegmentosRoom) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Deleta Local (Cascade apaga segmentos)
                dao.deletarRelatorio(item.relatorio)

                // 2. Deleta Nuvem usando o SERIAL como ID do documento
                FirebaseFirestore.getInstance()
                    .collection("producao_obras")
                    .document(item.relatorio.serial)
                    .delete()
            } catch (e: Exception) {
                Log.e("Erro", "Falha ao deletar: ${e.message}")
            }
        }
    }

    fun iniciarSincronizacao(lista: List<RelatorioComSegmentosRoom>) {
        if (acessoAutorizado) {
            lista.filter { it.relatorio.statusSinc == "LOCAL" }.forEach { enviarParaFirebase(it) }
        }
    }

    private fun enviarParaFirebase(item: RelatorioComSegmentosRoom) {
        val db = FirebaseFirestore.getInstance()
        val dados = hashMapOf(
            "serial" to item.relatorio.serial,
            "dispositivoId" to androidId,
            "contrato" to item.relatorio.contrato,
            "servico" to item.relatorio.servico,
            "dataObra" to item.relatorio.dataObra,
            "tempoManha" to item.relatorio.tempoManha,
            "trechoManha" to item.relatorio.trechoManha,
            "tempoTarde" to item.relatorio.tempoTarde,
            "trechoTarde" to item.relatorio.trechoTarde,
            "materiaisJson" to item.relatorio.materiaisJson,
            "segmentos" to item.segmentos.map {
                mapOf(
                    "estacaInicial" to it.estacaInicial,
                    "fracaoInicial" to it.fracaoInicial, // ADICIONADO
                    "estacaFinal" to it.estacaFinal,     // ADICIONADO
                    "fracaoFinal" to it.fracaoFinal,     // ADICIONADO
                    "lado" to it.lado,
                    "comprimento" to it.comprimento,
                    "largura" to it.largura,
                    "espessura" to it.espessura
                )
            }
        )

        // Usamos o Serial Único como nome do documento para evitar duplicidade
        db.collection("producao_obras").document(item.relatorio.serial)
            .set(dados)
            .addOnSuccessListener {
                viewModelScope.launch(Dispatchers.IO) {
                    dao.updateRelatorio(item.relatorio.copy(statusSinc = "ENVIADO"))
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Erro ao sincronizar: ${e.message}")
            }
    }
}