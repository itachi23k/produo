package com.cj.transporte

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cj.transporte.data.TicketEntity
import com.cj.transporte.data.TicketPrinterHelper
import com.cj.transporte.ui.theme.ProducaoTheme
import com.cj.transporte.viewmodel.TicketViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

class DetalheTicketActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ticketId = intent.getIntExtra("TICKET_ID", -1)

        setContent {
            ProducaoTheme {
                val viewModel: TicketViewModel = viewModel()
                var ticket by remember { mutableStateOf<TicketEntity?>(null) }
                val context = this

                LaunchedEffect(ticketId) {
                    if (ticketId != -1) {
                        // Agora o ViewModel terá essa função e o erro sumirá
                        ticket = viewModel.buscarTicket(ticketId)
                    }
                }

                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("DETALHE DO TICKET", fontWeight = FontWeight.Bold) }
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            containerColor = MaterialTheme.colorScheme.primary,
                            onClick = {
                                try {
                                    ticket?.let { TicketPrinterHelper.imprimirTicket58mm(context, it) }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        ) { Icon(Icons.Default.Print, "Imprimir", tint = Color.White) }
                    }
                ) { padding ->
                    ticket?.let { t ->
                        Column(
                            modifier = Modifier
                                .padding(padding)
                                .padding(16.dp)
                                .fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        val nFormatado = String.format("%06d", t.numeroControle)
                                        Text("Nº CONTROLE", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        Text(nFormatado, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                                    }

                                    HorizontalDivider(color = Color.DarkGray)

                                    InfoRow("Placa", t.placa)
                                    InfoRow("Material", t.material)
                                    InfoRow("Quantidade", "${t.quantidade} m³")
                                    InfoRow("Origem", t.obraOrigem)
                                    InfoRow("Data/Hora", "${t.dataSaida} - ${t.horaSaida}")
                                    InfoRow("Apontador", t.apontadorOrigem)
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            // --- QR CODE NA TELA ---
                            Text(
                                "QR CODE PARA RECEBIMENTO",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Geramos o bitmap do QR Code
                            val qrBitmap = remember(t.serial) {
                                gerarQrCodeBitmap(t.gerarStringQrCode())
                            }

                            qrBitmap?.let { bmp ->
                                Box(
                                    modifier = Modifier
                                        .size(220.dp)
                                        .background(Color.White) // Fundo branco para garantir leitura
                                        .padding(10.dp)
                                ) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "QR Code do Ticket",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Serial: ${t.serial.take(8).uppercase()}",
                                fontSize = 10.sp,
                                color = Color.DarkGray
                            )
                        }
                    } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    @Composable
    fun InfoRow(label: String, value: String) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.Gray, fontSize = 14.sp)
            Text(value, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        }
    }

    // Função que gera o QR Code como um Bitmap
    private fun gerarQrCodeBitmap(conteudo: String): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(conteudo, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}