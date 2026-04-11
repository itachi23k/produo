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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FormularioTicketActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val modo = intent.getStringExtra("MODO") ?: "SAIDA"
        val serialScaneado = intent.getStringExtra("SERIAL_SCANEADO")

        setContent {
            ProducaoTheme {
                val viewModel: TicketViewModel = viewModel()
                val context = LocalContext.current

                val opcoesObras = listOf("BR-319", "Lote A", "Lote B", "Manutenção Urbana")
                val opcoesMateriais = listOf("AREIA", "BRITA 01", "CBUQ", "BGS", "RACHÃO", "SOLO")

                var ticketExistente by remember { mutableStateOf<TicketEntity?>(null) }
                var obra by remember { mutableStateOf(opcoesObras[0]) }
                var placa by remember { mutableStateOf("") }
                var motorista by remember { mutableStateOf("") }
                var material by remember { mutableStateOf(opcoesMateriais[0]) }
                var qtd by remember { mutableStateOf("") }
                var estaca by remember { mutableStateOf("") }

                var expObra by remember { mutableStateOf(false) }
                var expMaterial by remember { mutableStateOf(false) }

                val sdfData = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val sdfHora = SimpleDateFormat("HH:mm", Locale.getDefault())

                LaunchedEffect(serialScaneado) {
                    if (modo == "CHEGADA" && serialScaneado != null) {
                        ticketExistente = viewModel.buscarTicketPorSerial(serialScaneado)
                        ticketExistente?.let { t ->
                            placa = t.placa
                            motorista = t.motorista
                            material = t.material
                            qtd = t.quantidade.toString()
                            obra = t.obraOrigem
                        }
                    }
                }

                Scaffold(
                    topBar = { CenterAlignedTopAppBar(title = { Text(if(modo == "SAIDA") "NOVO TICKET" else "FECHAR TICKET", fontWeight = FontWeight.Bold) }) }
                ) { padding ->
                    Column(Modifier.padding(padding).padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                        val isReadOnly = modo == "CHEGADA"

                        ExposedDropdownMenuBox(expanded = expObra && !isReadOnly, onExpandedChange = { if(!isReadOnly) expObra = it }) {
                            OutlinedTextField(
                                value = if(modo == "SAIDA") obra else (ticketExistente?.obraOrigem ?: obra),
                                onValueChange = {}, readOnly = true,
                                label = { Text(if(modo == "SAIDA") "Contrato/Obra Origem" else "Origem (Lido)") },
                                trailingIcon = { if(!isReadOnly) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expObra) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = expObra, onDismissRequest = { expObra = false }) {
                                opcoesObras.forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { obra = item; expObra = false }) }
                            }
                        }

                        OutlinedTextField(value = placa, onValueChange = { if(!isReadOnly) placa = it }, label = { Text("Placa") }, readOnly = isReadOnly, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = motorista, onValueChange = { if(!isReadOnly) motorista = it }, label = { Text("Motorista") }, readOnly = isReadOnly, modifier = Modifier.fillMaxWidth())

                        OutlinedTextField(
                            value = if(modo == "CHEGADA") "PARA: $obra" else material,
                            onValueChange = {}, readOnly = true,
                            label = { Text(if(modo == "CHEGADA") "Obra Destino" else "Material") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(value = qtd, onValueChange = { if(!isReadOnly) qtd = it }, label = { Text("Quantidade (m³)") }, readOnly = isReadOnly, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = estaca, onValueChange = { estaca = it }, label = { Text("Estaca") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

                        Spacer(Modifier.weight(1f))

                        Button(
                            onClick = {
                                if (placa.isEmpty() || estaca.isEmpty()) {
                                    Toast.makeText(context, "Dados incompletos!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                val dAt = sdfData.format(Date()); val hAt = sdfHora.format(Date())
                                val kmAt = viewModel.calcularKm(estaca)

                                if (modo == "SAIDA") {
                                    val novo = TicketEntity(
                                        numeroControle = viewModel.proximoNumeroControle(),
                                        dispositivoId = viewModel.androidId,
                                        obraOrigem = obra, placa = placa.uppercase(), motorista = motorista,
                                        material = material, quantidade = qtd.toDoubleOrNull() ?: 0.0,
                                        estacaInicial = estaca, contratoInicial = obra, kmInicial = kmAt,
                                        dataSaida = dAt, horaSaida = hAt, apontadorOrigem = viewModel.nomeApontadorLogado,
                                        gpsOrigem = "Registrado"
                                    )
                                    viewModel.salvarTicketSaida(novo) { finish() }
                                } else {
                                    ticketExistente?.let {
                                        viewModel.fecharTicketDestino(it, it.copy(
                                            obraDestino = obra, estacaDestino = estaca, kmDestino = kmAt,
                                            dataChegada = dAt, horaChegada = hAt, apontadorDestino = viewModel.nomeApontadorLogado
                                        ))
                                        finish()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Text(if (modo == "SAIDA") "GERAR TICKET" else "FINALIZAR TRANSPORTE")
                        }
                    }
                }
            }
        }
    }
}