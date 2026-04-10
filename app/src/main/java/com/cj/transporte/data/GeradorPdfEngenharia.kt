package com.cj.transporte.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import com.cj.transporte.R
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class GeradorPdfEngenharia(private val context: Context) {

    fun gerarECompartilhar(dados: RelatorioComSegmentosRoom) {
        WebView.enableSlowWholeDocumentDraw()

        val webView = WebView(context)

        // --- AJUSTE 1: FORÇAR O MODO DESKTOP/A4 ---
        webView.settings.javaScriptEnabled = true
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.setSupportZoom(false)
        // Isso evita que o Android tente "ajudar" mudando o tamanho das fontes
        webView.settings.textZoom = 100

        val base64Lcm = getBase64FromDrawable(R.drawable.logo_lcm)
        val base64Dnit = getBase64FromDrawable(R.drawable.logo_dnit)

        val html = StringBuilder()

        // MANTENDO SEU HTML EXATAMENTE IGUAL
        html.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=595">
                <style>
                    @page { size: A4; margin: 0; }
                    body { 
                        font-family: Arial, sans-serif; 
                        margin: 0; 
                        padding: 8mm; 
                        color: black; 
                        width: 595px; /* Força a largura do A4 em pixels de densidade */
                    }
                    table { width: 100%; border-collapse: collapse; table-layout: fixed; margin-bottom: 10px; }
                    td, th { border: 1px solid black; padding: 4px; font-size: 9px; text-align: center; }
                    .header-main { font-size: 16px; font-weight: bold; }
                    .label-bold { font-weight: bold; font-size: 9px; }
                    .section-header { text-align: left; font-weight: bold; font-size: 11px; padding: 5px; border: none; }
                    .no-border { border: none !important; }
                </style>
            </head>
            <body>
                <table>
                    <tr>
                        <td class="no-border" style="width: 20%; text-align: left;">
                            <img src="data:image/png;base64,$base64Lcm" width="70">
                        </td>
                        <td class="no-border" style="width: 60%;" class="header-main">
                            FICHA DE PRODUÇÃO DE CAMPO
                        </td>
                        <td class="no-border" style="width: 20%; text-align: right;">
                            <img src="data:image/png;base64,$base64Dnit" width="70">
                        </td>
                    </tr>
                </table>

                <table>
                    <tr class="label-bold">
                        <td style="width: 15%;">OBRA</td>
                        <td style="width: 15%;">Nº CONT.</td>
                        <td style="width: 45%;">SERVIÇO</td>
                        <td style="width: 25%;">DATA</td>
                    </tr>
                    <tr>
                        <td>284</td>
                        <td>${dados.relatorio.contrato}</td>
                        <td>${dados.relatorio.servico}</td>
                        <td>${dados.relatorio.dataObra}</td>
                    </tr>
                </table>

                <div class="section-header">1 - DESCRIÇÃO DO SERVIÇO</div>
                <table>
                    <tr class="label-bold">
                        <td rowspan="2" style="width: 6%;">LADO</td>
                        <td colspan="2" style="width: 20%;">ESTACA INICIAL</td>
                        <td colspan="2" style="width: 20%;">ESTACA FINAL</td>
                        <td rowspan="2" style="width: 11%;">COMP. (m)</td>
                        <td rowspan="2" style="width: 11%;">LARG. (m)</td>
                        <td rowspan="2" style="width: 11%;">ESP./ALT. (m)</td>
                        <td rowspan="2" style="width: 15%;">VOLUME (m³)</td>
                    </tr>
                    <tr class="label-bold">
                        <td style="width: 10%;">EST.</td><td style="width: 10%;">FRAÇÃO</td>
                        <td style="width: 10%;">EST.</td><td style="width: 10%;">FRAÇÃO</td>
                    </tr>
        """.trimIndent())

        dados.segmentos.forEach { seg ->
            val vol = (seg.comprimento ?: 0.0) * (seg.largura ?: 0.0) * (seg.espessura ?: 0.0)
            html.append("""
                <tr>
                    <td>${seg.lado}</td>
                    <td>${seg.estacaInicial}</td>
                    <td>${String.format(Locale.US, "%.2f", seg.fracaoInicial)}</td>
                    <td>${seg.estacaFinal}</td>
                    <td>${String.format(Locale.US, "%.2f", seg.fracaoFinal)}</td>
                    <td>${String.format(Locale.US, "%.2f", seg.comprimento)}</td>
                    <td>${String.format(Locale.US, "%.2f", seg.largura)}</td>
                    <td>${String.format(Locale.US, "%.2f", seg.espessura)}</td>
                    <td style="font-weight:bold;">${String.format(Locale.US, "%.2f", vol)}</td>
                </tr>
            """.trimIndent())
        }

        html.append("""
                </table>

                <div style="font-weight: bold; font-size: 10px; margin-top: 10px; margin-bottom: 2px;">2 - REGISTRO PLUVIOMÉTRICO</div>
                <table>
                    <tr style="background-color: #f0f0f0; font-weight: bold;">
                        <td style="width: 20%;">PERÍODO</td>
                        <td style="width: 40%;">CONDIÇÃO DO TEMPO</td>
                        <td style="width: 40%;">CONDIÇÃO DO TRECHO</td>
                    </tr>
                    <tr>
                        <td style="font-weight: bold;">MANHÃ</td>
                        <td>${dados.relatorio.tempoManha ?: "BOM"}</td> 
                        <td>${dados.relatorio.trechoManha ?: "PRATICÁVEL"}</td> 
                    </tr>
                    <tr>
                        <td style="font-weight: bold;">TARDE</td>
                        <td>${dados.relatorio.tempoTarde ?: "BOM"}</td>
                        <td>${dados.relatorio.trechoTarde ?: "PRATICÁVEL"}</td>
                    </tr>
                </table>

                <div style="font-weight: bold; font-size: 10px; margin-top: 10px; margin-bottom: 2px;">3 - MATERIAL APLICADO</div>
                <table style="width: 100%;">
                    <tr style="background-color: #f0f0f0; font-weight: bold;">
                        <td style="width: 70%;">TIPO DE MATERIAL</td>
                        <td style="width: 30%;">QUANTIDADE (m³)</td>
                    </tr>
                    ${gerarLinhasMaterial(dados.materiais)} 
                </table>
            </body>
            </html>
        """.trimIndent())

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                // --- O AJUSTE DE QUALIDADE ---
                // 1f = Baixa (25kb)
                // 3f = Alta (Qualidade de impressão 300 DPI) - Recomendado
                // 4f = Ultra (Arquivos maiores, máxima nitidez)
                val escalaQualidade = 3f

                val larguraA4Base = 595
                val alturaA4Base = 842

                // Calculamos o tamanho "Gigante" para o WebView pintar com detalhes
                val larguraRender = (larguraA4Base * escalaQualidade).toInt()
                val alturaRender = (alturaA4Base * escalaQualidade).toInt()

                // Forçamos o WebView a renderizar nessa resolução alta
                view?.measure(
                    View.MeasureSpec.makeMeasureSpec(larguraRender, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(alturaRender, View.MeasureSpec.EXACTLY)
                )
                view?.layout(0, 0, larguraRender, alturaRender)

                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        val pdfDocument = PdfDocument()
                        // O PDF continua sendo A4 (595x842), o papel não muda de tamanho!
                        val pageInfo = PdfDocument.PageInfo.Builder(larguraA4Base, alturaA4Base, 1).create()
                        val page = pdfDocument.startPage(pageInfo)

                        val canvas = page.canvas

                        // --- O SEGREDO DA NITIDEZ ---
                        // Como o WebView está 3x maior, precisamos encolher o desenho
                        // para caber no papel. Isso "funde" os pixels e cria a nitidez.
                        val fatorEscalaInversa = 1f / escalaQualidade
                        canvas.scale(fatorEscalaInversa, fatorEscalaInversa)

                        view?.draw(canvas)

                        pdfDocument.finishPage(page)

                        // 1. Tratamento do Serviço (remove espaços e coloca em maiúsculo)
                        val servicoLimpo = dados.relatorio.servico.replace(" ", "_").uppercase()

                        // 2. Tratamento do Contrato (troca / por - para não dar erro de pasta)
                        val contratoLimpo = dados.relatorio.contrato.replace("/", "-")

                         // 3. Data formatada (usando a data do relatório ou a data atual)
                        // Se dados.relatorio.dataObra já for "25/03/2026", vamos limpar as barras:
                        val dataLimpa = dados.relatorio.dataObra.replace("/", "-")

                        // 4. Montagem do nome final
                        val nomeArquivo = "Relatorio_${servicoLimpo}_${contratoLimpo}_${dataLimpa}.pdf"

                        // 5. Criação do arquivo no cache
                        val file = File(context.cacheDir, nomeArquivo)

                        val outputStream = FileOutputStream(file)
                        pdfDocument.writeTo(outputStream)

                        pdfDocument.close()
                        outputStream.close()

                        abrirCompartilhamento(file)

                    } catch (e: Exception) {
                        Log.e("PDF_ERROR", "Erro: ${e.message}")
                    }
                }, 800)
            }
        }

        webView.loadDataWithBaseURL(null, html.toString(), "text/html", "utf-8", null)
    }

    private fun abrirCompartilhamento(file: File) {
        try {
            val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Enviar via:"))
        } catch (e: Exception) { Log.e("SHARE", "Erro: ${e.message}") }
    }

    private fun gerarLinhasMaterial(materiais: List<MaterialItem>?): String {
        val sb = StringBuilder()
        for (i in 0 until 5) {
            val mat = materiais?.getOrNull(i)
            val nomeExibicao = mat?.nome ?: "&nbsp;"
            val qtdExibicao = if (mat?.quantidade != null && mat.quantidade > 0) String.format(Locale.US, "%.2f", mat.quantidade) else "&nbsp;"
            sb.append("<tr><td style=\"height: 20px; text-align: left; padding-left: 10px;\">$nomeExibicao</td><td>$qtdExibicao</td></tr>")
        }
        return sb.toString()
    }

    private fun getBase64FromDrawable(resourceId: Int): String {
        return try {
            val bitmap = BitmapFactory.decodeResource(context.resources, resourceId)
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) { "" }
    }
}