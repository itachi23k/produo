package com.cj.transporte.viewmodel

import android.app.Application
import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cj.transporte.data.AppDatabase
import com.cj.transporte.data.TicketEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TicketViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).ticketDao()
    private val prefs = application.getSharedPreferences("config_pref", Context.MODE_PRIVATE)
    private val prefsControle = application.getSharedPreferences("controle_seq", Context.MODE_PRIVATE)

    var acessoAutorizado by mutableStateOf(prefs.getBoolean("is_active", false))
    var mensagemErro by mutableStateOf("")
    val nomeApontadorLogado: String = prefs.getString("nome_usuario", "Desconhecido") ?: "Desconhecido"

    val todosTickets: Flow<List<TicketEntity>> = dao.getAllTickets()
    val androidId: String = Settings.Secure.getString(application.contentResolver, Settings.Secure.ANDROID_ID)?.uppercase() ?: "UNKNOWN"

    fun proximoNumeroControle(): Int {
        return prefsControle.getInt("ultimo_numero", 0) + 1
    }

    fun calcularKm(estacaStr: String): Double {
        val estaca = estacaStr.toDoubleOrNull() ?: 0.0
        return (estaca * 20.0) / 1000.0
    }

    suspend fun buscarTicketPorSerial(serial: String): TicketEntity? {
        return withContext(Dispatchers.IO) { dao.getTicketPorSerial(serial) }
    }

    fun salvarTicketSaida(ticket: TicketEntity, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertTicket(ticket)
            // Atualiza a sequência apenas após salvar com sucesso
            prefsControle.edit().putInt("ultimo_numero", ticket.numeroControle).apply()

            enviarParaFirebase(ticket)
            withContext(Dispatchers.Main) { onSuccess() }
        }
    }

    fun fecharTicketDestino(ticketExistente: TicketEntity, dadosChegada: TicketEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val ticketFechado = ticketExistente.copy(
                statusTicket = "FECHADO",
                statusSinc = "LOCAL",
                obraDestino = dadosChegada.obraDestino,
                dataChegada = dadosChegada.dataChegada,
                horaChegada = dadosChegada.horaChegada,
                apontadorDestino = dadosChegada.apontadorDestino,
                estacaDestino = dadosChegada.estacaDestino,
                kmDestino = dadosChegada.kmDestino,
                gpsDestino = dadosChegada.gpsDestino
            )
            dao.updateTicket(ticketFechado)
            enviarParaFirebase(ticketFechado)
        }
    }

    fun verificarLicenca() {
        val db = FirebaseFirestore.getInstance()
        db.collection("fiscais_autorizados").document(androidId).get().addOnSuccessListener { doc ->
            if (doc.exists() && (doc.getBoolean("ativo") == true)) {
                acessoAutorizado = true
                prefs.edit().putBoolean("is_active", true).apply()
            } else {
                acessoAutorizado = false
                mensagemErro = "Licença expirada."
            }
        }
    }

    // Adicione isso dentro da classe TicketViewModel
    suspend fun buscarTicket(id: Int): TicketEntity? {
        return withContext(Dispatchers.IO) {
            dao.getTicketPorId(id)
        }
    }

    fun iniciarSincronizacao(lista: List<TicketEntity>) {
        if (acessoAutorizado) {
            lista.filter { it.statusSinc == "LOCAL" }.forEach { enviarParaFirebase(it) }
        }
    }

    private fun enviarParaFirebase(ticket: TicketEntity) {
        val db = FirebaseFirestore.getInstance()
        db.collection("tickets_transporte").document(ticket.serial)
            .set(ticket)
            .addOnSuccessListener {
                viewModelScope.launch(Dispatchers.IO) { dao.updateTicket(ticket.copy(statusSinc = "ENVIADO")) }
            }
    }
}