package com.cj.transporte.data

import android.content.Context
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import android.widget.Toast

object TicketPrinterHelper {

    fun imprimirTicket58mm(context: Context, ticket: TicketEntity) {
        // 🚩 Mudança: Agora busca conexões Bluetooth pareadas
        val connection = BluetoothPrintersConnections.selectFirstPaired()

        if (connection != null) {
            try {
                // 203 DPI, 48mm de largura de papel, 32 caracteres por linha
                val printer = EscPosPrinter(connection, 203, 48f, 32)

                val nFormatado = String.format("%06d", ticket.numeroControle)

                val layout = """
                    [R]<b>Nº $nFormatado</b>
                    [C]<b><font size='big'>TICKET DE CARGA</font></b>
                    [C]ID: ${ticket.dispositivoId.takeLast(6)}
                    [C]--------------------------------
                    [L]<b>PLACA:</b> [R]${ticket.placa}
                    [L]<b>MATERIAL:</b> [R]${ticket.material}
                    [L]<b>QTD:</b> [R]${ticket.quantidade} m3
                    [L]<b>ORIGEM:</b> [R]${ticket.obraOrigem}
                    [L]<b>SAIDA:</b> [R]${ticket.dataSaida} ${ticket.horaSaida}
                    [C]--------------------------------
                    [C]<qrcode size='40'>${ticket.gerarStringQrCode()}</qrcode>
                    [C]--------------------------------
                    [C]${ticket.serial.take(8).uppercase()}
                    [L]
                    [L]
                """.trimIndent()

                printer.printFormattedText(layout)

            } catch (e: Exception) {
                Toast.makeText(context, "Erro na impressora: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            // Se cair aqui, é porque o Bluetooth está desligado ou a impressora não foi pareada nas configs do Android
            throw Exception("Impressora Bluetooth não pareada ou desligada!")
        }
    }
}