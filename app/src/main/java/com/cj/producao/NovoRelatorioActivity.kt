package com.cj.producao

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cj.producao.data.*
import com.cj.producao.ui.theme.ProducaoTheme
import com.cj.producao.ui.theme.VerdeProducao
import com.cj.producao.viewmodel.ProducaoViewModel
import java.text.SimpleDateFormat
import java.util.*

// --- LISTAS DE APOIO ---
val listaContratos = listOf("SR-592/2020", "SR-593/2020", "TT-563/2024")
val listaServicos = listOf(
                    "APLICAÇÃO DE BGS",
                    "APLICAÇÃO DE CBUQ",
                    "CAMADA DE MACADAME SECO",
                    "CAMADA DE REFORÇO",
                    "ENROCAMENTO",
                    "EROSÃO",
                    "EXECUÇÃO DE BUEIRO",
                    "EXECUÇÃO DE PONTILHÃO",
                    "FAIXA DE DOMÍNIO",
                    "HIDROSSEMEADURA",
                    "IMPLANTAÇÃO DE PLACAS",
                    "IMPRIMAÇÃO",
                    "LANÇAMENTO DE BGS",
                    "LASTRO DE BRITA",
                    "LIMPEZA DE SARJETA",
                    "LIMPEZA DE VALA DE DRENAGEM",
                    "PINTURA DE LIGAÇÃO",
                    "PLANTIO DE GRAMA",
                    "PONTE DE MADEIRA",
                    "RECOMPOSIÇÃO MECANIZADA DE ATERRO",
                    "REFORMA DE PONTE",
                    "REGULARIZAÇÃO DE SUBLEITO",
                    "REMENDO PROFUNDO",
                    "REMOÇÃO E TRANSPORTE",
                    "REVESTIMENTO PRIMÁRIO 01",
                    "REVESTIMENTO PRIMÁRIO 02",
                    "ROÇADA",
                    "TAPA BURACO",
                    "VALA DE DRENAGEM"
)
val opcoesLado = listOf("D", "E", "D/E", "EIXO")
val listaMateriaisRef = listOf("AREIA", "BRITA 01", "CBUQ", "BGS", "RACHÃO", "SOLO", "CIMENTO")
val opcoesTempo = listOf("BOM", "INSTÁVEL", "CHUVA")
val opcoesPraticabilidade = listOf("PRATICÁVEL", "IMPRATICÁVEL")

// --- CLASSES DE ESTADO ---
class TurnoPluviometriaState {
    var tempo by mutableStateOf("BOM")
    var praticabilidade by mutableStateOf("PRATICÁVEL")
}

class MaterialUIState(val nome: String, initialQtd: String = "") {
    var quantidade by mutableStateOf(initialQtd)
}

class SegmentoUIState {
    var lado by mutableStateOf("EIXO")
    var estacaInicial by mutableStateOf("")
    var fracaoInicial by mutableStateOf("")
    var estacaFinal by mutableStateOf("")
    var fracaoFinal by mutableStateOf("")
    var largura by mutableStateOf("")
    var comprimento by mutableStateOf("")
    var espessura by mutableStateOf("")

    fun atualizarComprimento() {
        val inicio = (estacaInicial.toIntOrNull() ?: 0) * 20 + (fracaoInicial.toDoubleOrNull() ?: 0.0)
        val fim = (estacaFinal.toIntOrNull() ?: 0) * 20 + (fracaoFinal.toDoubleOrNull() ?: 0.0)
        val resultado = fim - inicio
        comprimento = if (resultado >= 0) String.format(Locale.US, "%.2f", resultado) else ""
    }

    fun atualizarEstacaFinal() {
        val inicioTotal = (estacaInicial.toIntOrNull() ?: 0) * 20 + (fracaoInicial.toDoubleOrNull() ?: 0.0)
        val comp = comprimento.toDoubleOrNull() ?: 0.0
        val fimTotal = inicioTotal + comp
        estacaFinal = (fimTotal / 20).toInt().toString()
        fracaoFinal = String.format(Locale.US, "%.2f", fimTotal % 20)
    }
}

class NovoRelatorioActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        verificarPermissoesArquivos()
        val editId = intent.getIntExtra("EDIT_ID", 0)

        setContent {
            ProducaoTheme {
                val viewModel: ProducaoViewModel = viewModel()

                // Estados reativos para permitir a carga de dados
                var contrato by remember { mutableStateOf(listaContratos[0]) }
                var servico by remember { mutableStateOf(listaServicos[0]) }
                val segmentos = remember { mutableStateListOf<SegmentoUIState>() }
                val materiais = remember {
                    mutableStateListOf<MaterialUIState>().apply {
                        listaMateriaisRef.forEach { add(MaterialUIState(it)) }
                    }
                }
                val pManha = remember { TurnoPluviometriaState() }
                val pTarde = remember { TurnoPluviometriaState() }
                var serialRelatorio by remember { mutableStateOf(UUID.randomUUID().toString()) }

                // LOGICA DE CARGA: Se for edição, busca os dados salvos no banco
                if (editId != 0) {
                    LaunchedEffect(editId) {
                        // 1. Buscamos o dado já processado (sem travar a UI)
                        val dadosProntos = viewModel.carregarDadosParaEdicao(editId)

                        dadosProntos?.let { item ->
                            // 2. Atribuição direta de valores simples
                            contrato = item.relatorio.contrato
                            servico = item.relatorio.servico
                            pManha.tempo = item.relatorio.tempoManha
                            pManha.praticabilidade = item.relatorio.trechoManha
                            pTarde.tempo = item.relatorio.tempoTarde
                            pTarde.praticabilidade = item.relatorio.trechoTarde
                            serialRelatorio = item.relatorio.serial

                            // 3. Materiais: Apenas associamos os valores (muito mais rápido)
                            val mapaMat = item.materiais.associate { it.nome to (it.quantidade?.toString() ?: "") }
                            materiais.forEach { it.quantidade = mapaMat[it.nome] ?: "" }

                            // 4. Segmentos: Adicionamos todos de uma vez (addAll é mais performático que loop)
                            val novosSegmentos = item.segmentos.map { s ->
                                SegmentoUIState().apply {
                                    lado = s.lado
                                    estacaInicial = s.estacaInicial.toString()
                                    fracaoInicial = s.fracaoInicial.toString()
                                    estacaFinal = s.estacaFinal.toString()
                                    fracaoFinal = s.fracaoFinal.toString()
                                    largura = s.largura.toString()
                                    comprimento = s.comprimento.toString()
                                    espessura = s.espessura.toString()
                                }
                            }
                            segmentos.clear()
                            segmentos.addAll(novosSegmentos)
                        }
                    }
                }

                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("Relatório de Produção", color = Color.White, fontWeight = FontWeight.Bold) },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = VerdeProducao)
                        )
                    }
                ) { padding ->
                    FormularioConteudoContent(
                        contratoInicial = contrato,
                        servicoInicial = servico,
                        segmentosIniciais = segmentos,
                        materiaisIniciais = materiais,
                        pluvManha = pManha,
                        pluvTarde = pTarde,
                        editId = editId,
                        serialAtual = serialRelatorio,
                        onSalvar = { relatorio, listaParaBanco ->
                            viewModel.salvarOuAtualizar(relatorio, listaParaBanco)
                            finish()
                        },
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }

    private fun verificarPermissoesArquivos() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioConteudoContent(
    contratoInicial: String,
    servicoInicial: String,
    segmentosIniciais: MutableList<SegmentoUIState>,
    materiaisIniciais: MutableList<MaterialUIState>,
    pluvManha: TurnoPluviometriaState,
    pluvTarde: TurnoPluviometriaState,
    editId: Int,
    serialAtual: String,
    onSalvar: (RelatorioEntity, List<SegmentoEntity>) -> Unit,
    modifier: Modifier = Modifier
) {
    var contrato by remember(contratoInicial) { mutableStateOf(contratoInicial) }
    var servico by remember(servicoInicial) { mutableStateOf(servicoInicial) }
    var dataRelatorio by remember { mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())) }
    var expContrato by remember { mutableStateOf(false) }
    var expServico by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        item {
            OutlinedTextField(value = dataRelatorio, onValueChange = { dataRelatorio = it }, label = { Text("Data") }, modifier = Modifier.fillMaxWidth())
        }

        item {
            ExposedDropdownMenuBox(expanded = expContrato, onExpandedChange = { expContrato = it }) {
                OutlinedTextField(
                    value = contrato, onValueChange = {}, readOnly = true, label = { Text("Contrato") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expContrato) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expContrato, onDismissRequest = { expContrato = false }) {
                    listaContratos.forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { contrato = item; expContrato = false }) }
                }
            }
        }

        item {
            ExposedDropdownMenuBox(expanded = expServico, onExpandedChange = { expServico = it }) {
                OutlinedTextField(
                    value = servico, onValueChange = {}, readOnly = true, label = { Text("Serviço") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expServico) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expServico, onDismissRequest = { expServico = false }) {
                    listaServicos.forEach { item ->
                        DropdownMenuItem(text = { Text(item) }, onClick = {
                            servico = item; expServico = false
                            val sug = when(item) {
                                "REVESTIMENTO PRIMÁRIO 01", "REVESTIMENTO PRIMÁRIO 02" -> "8.00"
                                "REGULARIZAÇÃO DE SUBLEITO" -> "9.00"
                                else -> ""
                            }
                            if (sug.isNotEmpty()) segmentosIniciais.forEach { it.largura = sug }
                        })
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    // A chamada já passa o contrato com os traços no lugar das barras
                    val importados = FileScanner.scanMedicoes(contrato.replace("/", "-"), servico)

                    if (importados.isEmpty()) {
                        Toast.makeText(context, "Nenhuma pasta encontrada", Toast.LENGTH_SHORT).show()
                    } else {
                        // Limpa a lista atual para evitar sobreposição se clicar 2x
                        segmentosIniciais.clear()

                        // Adiciona os cards exatos que vieram da leitura das pastas
                        importados.forEach { imp ->
                            segmentosIniciais.add(
                                SegmentoUIState().apply {
                                    estacaInicial = imp.estaca.toString()
                                    lado = imp.lado // Puxando o lado "D", "E", "D/E" derivado do nome da pasta
                                }
                            )
                        }
                        Toast.makeText(context, "${importados.size} registros importados!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("IMPORTAR FOTOS DO REGISTRO")
            }
        }

        itemsIndexed(
            items = segmentosIniciais,
            key = { index, _ -> index } // Ajuda o Compose a não se perder no desenho
        ) { index, segmento ->
            CardSegmento(index + 1, segmento) { segmentosIniciais.removeAt(index) }
        }

        item {
            Button(onClick = { segmentosIniciais.add(SegmentoUIState()) }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray), modifier = Modifier.fillMaxWidth()) {
                Text("+ Adicionar Segmento Manual")
            }
        }

        item {
            Text("2 - Registro Pluviométrico", fontWeight = FontWeight.Bold, color = VerdeProducao)
            SecaoPluviometria("Manhã", pluvManha)
            Spacer(Modifier.height(8.dp))
            SecaoPluviometria("Tarde", pluvTarde)
        }

        item {
            Text("3 - Materiais Aplicados (Qtd m³)", fontWeight = FontWeight.Bold, color = VerdeProducao)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9))
            ) {
                Column(Modifier.padding(8.dp)) {
                    materiaisIniciais.forEach { mat ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(mat.nome, Modifier.weight(1f), fontSize = 14.sp)
                            OutlinedTextField(
                                value = mat.quantidade,
                                onValueChange = { novoValor -> mat.quantidade = novoValor },
                                placeholder = { Text("0.00", fontSize = 12.sp) },
                                modifier = Modifier.width(110.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    if (segmentosIniciais.isEmpty()) {
                        Toast.makeText(context, "Adicione segmentos!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // 1. Converte materiais para String (JSON simples)
                    val materiaisFinal = materiaisIniciais
                        .filter { it.quantidade.isNotBlank() }
                        .joinToString("|") { "${it.nome}:${it.quantidade}" }

                    // 2. Cria a entidade Relatorio incluindo Serial, Materiais e Clima
                    val relatorio = RelatorioEntity(
                        id = editId,
                        contrato = contrato,
                        servico = servico,
                        dataObra = dataRelatorio,
                        statusSinc = "LOCAL",
                        tempoManha = pluvManha.tempo,
                        trechoManha = pluvManha.praticabilidade,
                        tempoTarde = pluvTarde.tempo,
                        trechoTarde = pluvTarde.praticabilidade,
                        materiaisJson = materiaisFinal,
                        serial = serialAtual
                    )

                    // 3. Mapeia segmentos
                    val lista = segmentosIniciais.map { ui ->
                        SegmentoEntity(
                            relatorioId = editId, lado = ui.lado,
                            estacaInicial = ui.estacaInicial.toIntOrNull() ?: 0,
                            fracaoInicial = ui.fracaoInicial.toDoubleOrNull() ?: 0.0,
                            estacaFinal = ui.estacaFinal.toIntOrNull() ?: 0,
                            fracaoFinal = ui.fracaoFinal.toDoubleOrNull() ?: 0.0,
                            largura = ui.largura.toDoubleOrNull() ?: 0.0,
                            comprimento = ui.comprimento.toDoubleOrNull() ?: 0.0,
                            espessura = ui.espessura.toDoubleOrNull() ?: 0.0
                        )
                    }
                    onSalvar(relatorio, lista)
                },
                modifier = Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = VerdeProducao)
            ) {
                Text(if (editId == 0) "SALVAR NO DISPOSITIVO" else "ATUALIZAR RELATÓRIO", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecaoPluviometria(titulo: String, state: TurnoPluviometriaState) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
        Column(Modifier.padding(8.dp)) {
            Text(titulo, fontWeight = FontWeight.Bold, color = VerdeProducao, fontSize = 12.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Tempo:", fontSize = 11.sp, modifier = Modifier.width(50.dp))
                opcoesTempo.forEach { t ->
                    FilterChip(selected = state.tempo == t, onClick = { state.tempo = t }, label = { Text(t, fontSize = 9.sp) })
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Trecho:", fontSize = 11.sp, modifier = Modifier.width(50.dp))
                opcoesPraticabilidade.forEach { p ->
                    FilterChip(selected = state.praticabilidade == p, onClick = { state.praticabilidade = p }, label = { Text(p, fontSize = 9.sp) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardSegmento(numero: Int, state: SegmentoUIState, onRemove: () -> Unit) {
    var expLado by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Segmento #$numero", fontWeight = FontWeight.Bold, color = VerdeProducao)
                IconButton(onClick = onRemove) { Icon(Icons.Default.Cloud, null, tint = Color.Red) }
            }
            ExposedDropdownMenuBox(expanded = expLado, onExpandedChange = { expLado = it }) {
                OutlinedTextField(
                    value = state.lado, onValueChange = {}, readOnly = true, label = { Text("Lado") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expLado) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expLado, onDismissRequest = { expLado = false }) {
                    opcoesLado.forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { state.lado = item; expLado = false }) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = state.estacaInicial, onValueChange = { state.estacaInicial = it; state.atualizarComprimento() }, label = { Text("Est. Inic.") }, placeholder = { Text("0") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = state.fracaoInicial, onValueChange = { state.fracaoInicial = it; state.atualizarComprimento() }, label = { Text("Fr. Inic.") }, placeholder = { Text("0.0") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = state.estacaFinal, onValueChange = { state.estacaFinal = it; state.atualizarComprimento() }, label = { Text("Est. Final") }, placeholder = { Text("0") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = state.fracaoFinal, onValueChange = { state.fracaoFinal = it; state.atualizarComprimento() }, label = { Text("Fr. Final") }, placeholder = { Text("0.0") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = state.comprimento, onValueChange = { state.comprimento = it; state.atualizarEstacaFinal() }, label = { Text("Comp (m)") }, placeholder = { Text("0.00") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = state.largura, onValueChange = { state.largura = it }, label = { Text("Larg (m)") }, placeholder = { Text("0.00") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = state.espessura, onValueChange = { state.espessura = it }, label = { Text("Esp (m)") }, placeholder = { Text("0.00") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        }
    }
}