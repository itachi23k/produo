package com.cj.transporte

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cj.transporte.data.TicketEntity
import com.cj.transporte.ui.theme.ProducaoTheme
import com.cj.transporte.viewmodel.TicketViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FormularioTicketActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val modo = intent.getStringExtra("MODO") ?: "SAIDA"
        val payloadQr = intent.getStringExtra("PAYLOAD_QR")

        setContent {
            ProducaoTheme {
                val viewModel: TicketViewModel = viewModel()
                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope() // 🚩 Solução para o erro do escopo da Coroutine
                val isReadOnly = modo == "CHEGADA"

                // Listas do Banco de Dados
                val caminhoes by viewModel.listaCaminhoes.collectAsState(initial = emptyList())
                val materiaisAux by viewModel.listaMateriais.collectAsState(initial = emptyList())
                val opcoesObras = listOf("BR-319", "Lote A", "Lote B", "Manutenção Urbana")

                // Estados dos Campos
                var obra by remember { mutableStateOf(opcoesObras[0]) } // 🚩 Solução para o erro 'obra'
                var placa by remember { mutableStateOf("") }
                var motorista by remember { mutableStateOf("") }
                var material by remember { mutableStateOf("") }
                var qtd by remember { mutableStateOf("") }
                var estaca by remember { mutableStateOf("") }

                var serialRecuperado by remember { mutableStateOf("") }
                var obraOrigemLida by remember { mutableStateOf("") }

                var expObra by remember { mutableStateOf(false) }
                var expPlaca by remember { mutableStateOf(false) }
                var expMat by remember { mutableStateOf(false) }

                // Sincroniza listas se tiver internet
                LaunchedEffect(Unit) { viewModel.sincronizarListasAuxiliares() }

                // 🚩 LÓGICA DE DECOMPOSIÇÃO DO QR CODE
                LaunchedEffect(payloadQr) {
                    if (modo == "CHEGADA" && payloadQr != null) {
                        val partes = payloadQr.split("|")
                        if (partes.size >= 5) {
                            serialRecuperado = partes[0]
                            placa = partes[1]
                            motorista = partes[2]
                            material = partes[3]
                            qtd = partes[4]
                            obraOrigemLida = partes.getOrNull(5) ?: "Lido via QR"
                        }
                    }
                }

                Scaffold(
                    topBar = { CenterAlignedTopAppBar(title = { Text(if(modo == "SAIDA") "NOVO TICKET" else "FECHAR TICKET", fontWeight = FontWeight.Bold) }) }
                ) { padding ->
                    Column(Modifier.padding(padding).padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                        // --- CAMPO OBRA ---
                        ExposedDropdownMenuBox(expanded = expObra, onExpandedChange = { expObra = it }) {
                            OutlinedTextField(
                                value = if(isReadOnly) "Para: $obra" else obra,
                                onValueChange = {}, readOnly = true,
                                label = { Text(if(isReadOnly) "Obra de Destino" else "Obra de Origem") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expObra) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = expObra, onDismissRequest = { expObra = false }) {
                                opcoesObras.forEach { item ->
                                    DropdownMenuItem(text = { Text(item) }, onClick = { obra = item; expObra = false })
                                }
                            }
                        }

                        // --- CAMPO PLACA ---
                        ExposedDropdownMenuBox(expanded = expPlaca, onExpandedChange = { if(!isReadOnly) expPlaca = it }) {
                            OutlinedTextField(
                                value = placa, onValueChange = { if(!isReadOnly) placa = it },
                                label = { Text("Placa") }, readOnly = isReadOnly,
                                trailingIcon = { if(!isReadOnly) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expPlaca) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = expPlaca, onDismissRequest = { expPlaca = false }) {
                                caminhoes.forEach { c ->
                                    DropdownMenuItem(text = { Text(c.placa) }, onClick = {
                                        placa = c.placa; motorista = c.motorista; expPlaca = false
                                    })
                                }
                            }
                        }

                        OutlinedTextField(value = motorista, onValueChange = { if(!isReadOnly) motorista = it }, label = { Text("Motorista") }, readOnly = isReadOnly, modifier = Modifier.fillMaxWidth())

                        // --- CAMPO MATERIAL ---
                        ExposedDropdownMenuBox(expanded = expMat, onExpandedChange = { if(!isReadOnly) expMat = it }) {
                            OutlinedTextField(
                                value = material, onValueChange = { if(!isReadOnly) material = it },
                                label = { Text("Material") }, readOnly = isReadOnly,
                                trailingIcon = { if(!isReadOnly) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expMat) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = expMat, onDismissRequest = { expMat = false }) {
                                materiaisAux.forEach { m ->
                                    DropdownMenuItem(text = { Text(m.nome) }, onClick = { material = m.nome; expMat = false })
                                }
                            }
                        }

                        OutlinedTextField(value = qtd, onValueChange = { if(!isReadOnly) qtd = it }, label = { Text("Quantidade (m³)") }, readOnly = isReadOnly, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

                        OutlinedTextField(value = estaca, onValueChange = { estaca = it }, label = { Text(if(isReadOnly) "Estaca de Chegada" else "Estaca de Saída") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

                        Spacer(Modifier.weight(1f))

                        Button(onClick = {
                            if (placa.isEmpty() || material.isEmpty() || estaca.isEmpty()) {
                                Toast.makeText(context, "Preencha os campos essenciais!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            if (modo == "SAIDA") {
                                val ticket = TicketEntity(
                                    numeroControle = viewModel.proximoNumeroControle(),
                                    dispositivoId = viewModel.androidId,
                                    obraOrigem = obra, placa = placa.uppercase(), motorista = motorista,
                                    material = material, quantidade = qtd.toDoubleOrNull() ?: 0.0,
                                    estacaInicial = estaca, contratoInicial = obra, kmInicial = viewModel.calcularKm(estaca),
                                    dataSaida = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
                                    horaSaida = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                                    apontadorOrigem = viewModel.nomeApontadorLogado, gpsOrigem = "Registrado"
                                )
                                viewModel.salvarTicketSaida(ticket) { finish() }

                            } else {
                                // 🚩 CORREÇÃO DOS ERROS: Usamos coroutineScope.launch nativo do Compose
                                coroutineScope.launch {
                                    val ticketLocal = viewModel.buscarTicketPorSerial(serialRecuperado)

                                    val dataAt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                                    val horaAt = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

                                    if (ticketLocal != null) {
                                        viewModel.fecharTicketDestino(ticketLocal, ticketLocal.copy(
                                            obraDestino = obra, estacaDestino = estaca, kmDestino = viewModel.calcularKm(estaca),
                                            dataChegada = dataAt, horaChegada = horaAt
                                        )) { finish() }
                                    } else {
                                        val ticketIncompleto = TicketEntity(
                                            serial = serialRecuperado, numeroControle = 0, dispositivoId = viewModel.androidId,
                                            statusTicket = "FECHADO", statusSinc = "LOCAL",
                                            placa = placa, motorista = motorista, material = material, quantidade = qtd.toDoubleOrNull() ?: 0.0,
                                            obraOrigem = obraOrigemLida, dataSaida = "QR", horaSaida = "QR", apontadorOrigem = "QR",
                                            estacaInicial = "0", contratoInicial = "QR", kmInicial = 0.0, gpsOrigem = "QR",
                                            obraDestino = obra, estacaDestino = estaca, kmDestino = viewModel.calcularKm(estaca),
                                            dataChegada = dataAt, horaChegada = horaAt, apontadorDestino = viewModel.nomeApontadorLogado
                                        )
                                        viewModel.fecharTicketOffline(ticketIncompleto) { finish() }
                                    }
                                }
                            }
                        }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                            Text(if (modo == "SAIDA") "GERAR TICKET" else "FINALIZAR TRANSPORTE")
                        }
                    }
                }
            }
        }
    }
}