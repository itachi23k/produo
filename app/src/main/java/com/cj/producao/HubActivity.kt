package com.cj.producao

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cj.producao.data.*
import com.cj.producao.ui.theme.ProducaoTheme
import com.cj.producao.ui.theme.VerdeProducao
import com.cj.producao.viewmodel.ProducaoViewModel
import androidx.compose.animation.animateColorAsState

class HubActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProducaoTheme {
                val viewModel: ProducaoViewModel = viewModel()
                val autorizado = viewModel.acessoAutorizado

                // Coleta de dados otimizada
                val relatorios by viewModel.todosRelatorios.collectAsState(initial = emptyList())
                var relatorioParaDeletar by remember { mutableStateOf<RelatorioComSegmentosRoom?>(null) }

                LaunchedEffect(Unit) { viewModel.verificarLicenca() }

                if (!autorizado) {
                    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Red)
                            Spacer(Modifier.height(16.dp))
                            Text("ACESSO RESTRITO", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text(viewModel.mensagemErro, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    LaunchedEffect(relatorios) { viewModel.iniciarSincronizacao(relatorios) }

                    // Dialog de Confirmação
                    relatorioParaDeletar?.let { item ->
                        AlertDialog(
                            onDismissRequest = { relatorioParaDeletar = null },
                            title = { Text("Excluir Relatório?") },
                            text = { Text("Deseja apagar este relatório do dispositivo e da nuvem?") },
                            confirmButton = {
                                TextButton(onClick = {
                                    viewModel.deletarRelatorioCompleto(item)
                                    relatorioParaDeletar = null
                                }) { Text("EXCLUIR", color = Color.Red) }
                            },
                            dismissButton = {
                                TextButton(onClick = { relatorioParaDeletar = null }) { Text("CANCELAR") }
                            }
                        )
                    }

                    Scaffold(
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = { Text("PRODUÇÃO", fontWeight = FontWeight.Bold, color = Color.White) },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = VerdeProducao)
                            )
                        },
                        floatingActionButton = {
                            FloatingActionButton(
                                onClick = { startActivity(Intent(this, NovoRelatorioActivity::class.java)) },
                                containerColor = VerdeProducao
                            ) { Icon(Icons.Default.Add, "Novo", tint = Color.White) }
                        }
                    ) { padding ->
                        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                            if (relatorios.isEmpty()) {
                                Text("Nenhum relatório salvo.", Modifier.align(Alignment.Center), color = Color.Gray)
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // O uso da KEY é fundamental para a fluidez do Swipe
                                    items(relatorios, key = { it.relatorio.serial }) { item ->

                                        val dismissState = rememberSwipeToDismissBoxState(
                                            confirmValueChange = { value ->
                                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                                    relatorioParaDeletar = item
                                                    false
                                                } else false
                                            }
                                        )

                                        SwipeToDismissBox(
                                            state = dismissState,
                                            enableDismissFromStartToEnd = false,
                                            backgroundContent = {
                                                val color by animateColorAsState(
                                                    when (dismissState.targetValue) {
                                                        SwipeToDismissBoxValue.EndToStart -> Color.Red
                                                        else -> Color.Transparent
                                                    }, label = "color"
                                                )
                                                Box(
                                                    Modifier.fillMaxSize().background(color).padding(horizontal = 20.dp),
                                                    contentAlignment = Alignment.CenterEnd
                                                ) {
                                                    Icon(Icons.Default.Delete, null, tint = Color.White)
                                                }
                                            }
                                        ) {
                                            CardRelatorio(item)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardRelatorio(item: RelatorioComSegmentosRoom) {
    val context = LocalContext.current
    val r = item.relatorio

    // OTIMIZAÇÃO: Trava o cálculo do texto e cor para não rodar durante o scroll/swipe
    val resumoTrecho = remember(item.relatorio.serial) {
        if (item.segmentos.isNotEmpty()) {
            "Estacas: ${item.segmentos.first().estacaInicial} a ${item.segmentos.last().estacaFinal}"
        } else "Sem segmentos lançados"
    }

    val corStatus = remember(r.statusSinc) {
        if (r.statusSinc == "LOCAL") Color.Gray else VerdeProducao
    }

    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(
            onClick = {
                context.startActivity(Intent(context, NovoRelatorioActivity::class.java).putExtra("EDIT_ID", r.id))
            },
            onLongClick = {
                Toast.makeText(context, "Gerando PDF...", Toast.LENGTH_SHORT).show()

                // O processamento pesado de String só acontece no clique, não no desenho
                val listaMateriaisParaPdf = r.materiaisJson?.split("|")?.mapNotNull { str ->
                    val partes = str.split(":")
                    if (partes.size == 2) {
                        MaterialItem(partes[0], partes[1].toDoubleOrNull())
                    } else null
                } ?: emptyList()

                val dadosCompletos = item.copy(materiais = listaMateriaisParaPdf)
                GeradorPdfEngenharia(context).gerarECompartilhar(dadosCompletos)
            }
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.fillMaxHeight().width(6.dp).background(corStatus))
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${r.contrato} - ${r.servico}", fontWeight = FontWeight.Bold, maxLines = 1)
                    Icon(Icons.Default.PictureAsPdf, null, Modifier.size(16.dp), tint = Color.LightGray)
                }

                Text(
                    text = "$resumoTrecho - ${r.dataObra}",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )

                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("Status: ${r.statusSinc}", fontSize = 11.sp, color = corStatus)
                    if (r.tempoManha != "BOM") {
                        Text(
                            text = "Tempo: ${r.tempoManha}",
                            fontSize = 11.sp,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}