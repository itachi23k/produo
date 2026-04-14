package com.cj.transporte

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cj.transporte.ui.theme.ProducaoTheme
import com.cj.transporte.viewmodel.TicketViewModel
import com.cj.transporte.data.TicketEntity
import com.cj.transporte.data.TicketPrinterHelper
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class HubActivity : ComponentActivity() {

    // Scanner de QR Code
    private lateinit var barcodeLauncher: ActivityResultLauncher<ScanOptions>

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configuração do Scanner
        barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
            if (result.contents != null) {
                // Passamos a string bruta (serial|placa|motorista...) para o formulário decompor
                val intent = Intent(this, FormularioTicketActivity::class.java).apply {
                    putExtra("MODO", "CHEGADA")
                    putExtra("PAYLOAD_QR", result.contents)
                }
                startActivity(intent)
            }
        }

        setContent {
            ProducaoTheme {
                val viewModel: TicketViewModel = viewModel()
                val tickets by viewModel.todosTickets.collectAsState(initial = emptyList())
                val context = LocalContext.current

                LaunchedEffect(Unit) { viewModel.verificarLicenca() }
                LaunchedEffect(tickets) { viewModel.iniciarSincronizacao(tickets) }

                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("CONTROLE DE TRANSPORTE", color = Color.White, fontWeight = FontWeight.Bold) },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF121212))
                        )
                    },
                    containerColor = Color(0xFF121212)
                ) { padding ->
                    Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {

                        CardCentral(
                            titulo = "GERAR TICKET",
                            subtitulo = "Registrar Saída de Carga",
                            icone = Icons.Default.LocalShipping,
                            corIcone = Color(0xFF4CAF50),
                            onClick = {
                                startActivity(Intent(context, FormularioTicketActivity::class.java).putExtra("MODO", "SAIDA"))
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        CardCentral(
                            titulo = "FECHAR TICKET",
                            subtitulo = "Registrar Chegada (Ler QR)",
                            icone = Icons.Default.QrCodeScanner,
                            corIcone = Color(0xFFFF9800),
                            onClick = {
                                val options = ScanOptions().apply {
                                    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                    setPrompt("Aponte para o QR Code do Ticket")
                                    setBeepEnabled(true)
                                    setOrientationLocked(false)
                                }
                                barcodeLauncher.launch(options)
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                        Text("HISTÓRICO DE TICKETS", color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(tickets, key = { it.serial }) { ticket ->
                                CardHistorico(ticket)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CardCentral(titulo: String, subtitulo: String, icone: androidx.compose.ui.graphics.vector.ImageVector, corIcone: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(120.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Row(Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icone, contentDescription = null, tint = corIcone, modifier = Modifier.size(56.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(titulo, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(subtitulo, color = Color.Gray, fontSize = 14.sp)
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CardHistorico(ticket: TicketEntity) {
    val context = LocalContext.current
    val corStatus = if (ticket.statusTicket == "ABERTO") Color(0xFFFF9800) else Color(0xFF4CAF50)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    val intent = Intent(context, DetalheTicketActivity::class.java)
                    intent.putExtra("TICKET_ID", ticket.id)
                    context.startActivity(intent)
                },
                onLongClick = {
                    try {
                        TicketPrinterHelper.imprimirTicket58mm(context, ticket)
                        Toast.makeText(context, "Imprimindo...", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            ),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.fillMaxHeight().width(6.dp).background(corStatus))
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(ticket.placa, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(ticket.statusTicket, color = corStatus, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text("${ticket.material} - ${ticket.quantidade} m³", color = Color.LightGray, fontSize = 14.sp)
                Text("De: ${ticket.obraOrigem} | Para: ${ticket.obraDestino ?: "Em Trânsito"}", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}