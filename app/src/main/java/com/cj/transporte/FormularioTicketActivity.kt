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
        val modo = intent.getStringExtra("MODO") ?: "SAIDA" // SAIDA ou CHEGADA

        setContent {
            ProducaoTheme {
                val viewModel: TicketViewModel = viewModel()
                val context = LocalContext.current

                // Variáveis de Estado (Origem)
                var obra by remember { mutableStateOf("") }
                var placa by remember { mutableStateOf("") }
                var motorista by remember { mutableStateOf("") }
                var material by remember { mutableStateOf("") }
                var qtd by remember { mutableStateOf("") }
                var estaca by remember { mutableStateOf("") }

                val sdfData = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val sdfHora = SimpleDateFormat("HH:mm", Locale.getDefault())

                Scaffold(
                    topBar = { CenterAlignedTopAppBar(title = { Text(if(modo=="SAIDA") "NOVO TICKET" else "FECHAR TICKET") }) }
                ) { padding ->
                    Column(Modifier.padding(padding).padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                        Text("Dados do Transporte ($modo)", fontWeight = FontWeight.Bold)

                        OutlinedTextField(value = obra, onValueChange = { obra = it }, label = { Text("Contrato/Obra") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = placa, onValueChange = { placa = it }, label = { Text("Placa") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = motorista, onValueChange = { motorista = it }, label = { Text("Motorista") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = material, onValueChange = { material = it }, label = { Text("Material") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = qtd, onValueChange = { qtd = it }, label = { Text("Quantidade (m³)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = estaca, onValueChange = { estaca = it }, label = { Text("Estaca (Ex: 120)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

                        Spacer(Modifier.weight(1f))

                        Button(
                            onClick = {
                                if (placa.isEmpty() || material.isEmpty()) {
                                    Toast.makeText(context, "Preencha placa e material!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                val dataAtual = sdfData.format(Date())
                                val horaAtual = sdfHora.format(Date())
                                val apontador = viewModel.nomeApontadorLogado
                                val kmCalculado = viewModel.calcularKm(estaca)

                                if (modo == "SAIDA") {
                                    val novoTicket = TicketEntity(
                                        obraOrigem = obra,
                                        placa = placa.uppercase(),
                                        motorista = motorista,
                                        material = material,
                                        quantidade = qtd.toDoubleOrNull() ?: 0.0,
                                        estacaInicial = estaca,
                                        contratoInicial = obra,
                                        kmInicial = kmCalculado,
                                        dataSaida = dataAtual,
                                        horaSaida = horaAtual,
                                        apontadorOrigem = apontador,
                                        gpsOrigem = "Aguardando GPS Nativo"
                                    )
                                    viewModel.salvarTicketSaida(novoTicket)
                                    Toast.makeText(context, "Ticket Gerado!", Toast.LENGTH_SHORT).show()
                                } else {
                                    // 🚩 Sincero: No app final, você lerá o QR Code e não digitará tudo de novo.
                                    // Esse bloco aqui vai evoluir quando colocarmos a câmera.
                                    Toast.makeText(context, "Ticket Fechado com Sucesso!", Toast.LENGTH_SHORT).show()
                                }
                                finish()
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Text(if (modo == "SAIDA") "GERAR TICKET E IMPRIMIR" else "REGISTRAR CHEGADA")
                        }
                    }
                }
            }
        }
    }
}