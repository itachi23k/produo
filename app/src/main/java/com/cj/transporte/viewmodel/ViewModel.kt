package com.cj.transporte.viewmodel

import android.app.Application
import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.cj.transporte.data.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow

class TicketViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).ticketDao()
    private val prefs = application.getSharedPreferences("config_pref", Context.MODE_PRIVATE)
    private val prefsControle = application.getSharedPreferences("controle_seq", Context.MODE_PRIVATE)

    var acessoAutorizado by mutableStateOf(prefs.getBoolean("is_active", false))
    var mensagemErro by mutableStateOf("")
    val nomeApontadorLogado: String = prefs.getString("nome_usuario", "Desconhecido") ?: "Desconhecido"
    val androidId: String = Settings.Secure.getString(application.contentResolver, Settings.Secure.ANDROID_ID)?.uppercase() ?: "UNKNOWN"

    val todosTickets: Flow<List<TicketEntity>> = dao.getAllTickets()
    val listaCaminhoes: Flow<List<CaminhaoEntity>> = dao.getAllCaminhoes()
    val listaMateriais: Flow<List<MaterialEntity>> = dao.getAllMateriais()

    suspend fun buscarTicket(id: Int): TicketEntity? = withContext(Dispatchers.IO) { dao.getTicketPorId(id) }

    suspend fun buscarTicketPorSerial(serial: String): TicketEntity? = withContext(Dispatchers.IO) { dao.getTicketPorSerial(serial) }

    fun proximoNumeroControle(): Int = prefsControle.getInt("ultimo_numero", 0) + 1
    fun calcularKm(estaca: String): Double = (estaca.toDoubleOrNull() ?: 0.0) * 0.02

    fun salvarTicketSaida(ticket: TicketEntity, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertTicket(ticket)
            prefsControle.edit().putInt("ultimo_numero", ticket.numeroControle).apply()
            enviarParaFirebase(ticket)
            withContext(Dispatchers.Main) { onSuccess() }
        }
    }

    // 🚩 FECHAMENTO VIA PAR (Quando já existe no banco local)
    fun fecharTicketDestino(ticketExistente: TicketEntity, dadosChegada: TicketEntity, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val ticketFechado = ticketExistente.copy(
                statusTicket = "FECHADO",
                statusSinc = "LOCAL",
                obraDestino = dadosChegada.obraDestino,
                dataChegada = dadosChegada.dataChegada,
                horaChegada = dadosChegada.horaChegada,
                apontadorDestino = nomeApontadorLogado,
                estacaDestino = dadosChegada.estacaDestino,
                kmDestino = dadosChegada.kmDestino
            )
            dao.updateTicket(ticketFechado)
            enviarParaFirebase(ticketFechado)
            withContext(Dispatchers.Main) { onSuccess() }
        }
    }

    // 🚩 FECHAMENTO OFFLINE (Cria a "metade" de destino quando o ticket de origem não está no celular)
    fun fecharTicketOffline(ticketIncompleto: TicketEntity, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertTicket(ticketIncompleto)
            enviarParaFirebase(ticketIncompleto)
            withContext(Dispatchers.Main) { onSuccess() }
        }
    }

    fun verificarLicenca() {
        FirebaseFirestore.getInstance().collection("fiscais_autorizados")
            .document(androidId).get().addOnSuccessListener { doc ->
                if (doc.exists() && doc.getBoolean("ativo") == true) {
                    acessoAutorizado = true
                    prefs.edit().putBoolean("is_active", true).apply()
                } else {
                    acessoAutorizado = false
                    mensagemErro = "Licença expirada."
                }
            }
    }

    fun iniciarSincronizacao(lista: List<TicketEntity>) {
        if (acessoAutorizado) {
            lista.filter { it.statusSinc == "LOCAL" }.forEach { enviarParaFirebase(it) }
        }
    }

    fun sincronizarListasAuxiliares() {
        val db = FirebaseFirestore.getInstance()
        db.collection("veiculos").get().addOnSuccessListener { docs ->
            val lista = docs.map { CaminhaoEntity(it.id, it.getString("motorista") ?: "") }
            viewModelScope.launch(Dispatchers.IO) { dao.insertCaminhoes(lista) }
        }
        db.collection("materiais").get().addOnSuccessListener { docs ->
            val lista = docs.map { MaterialEntity(it.id) }
            viewModelScope.launch(Dispatchers.IO) { dao.insertMateriais(lista) }
        }
    }

    private fun enviarParaFirebase(ticket: TicketEntity) {
        FirebaseFirestore.getInstance().collection("tickets_transporte")
            .document(ticket.serial).set(ticket)
            .addOnSuccessListener {
                viewModelScope.launch(Dispatchers.IO) {
                    dao.updateTicket(ticket.copy(statusSinc = "ENVIADO"))
                }
            }
    }
}