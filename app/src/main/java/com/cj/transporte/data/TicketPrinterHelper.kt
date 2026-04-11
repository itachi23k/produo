package com.cj.transporte.data

import android.content.Context
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.usb.UsbPrintersConnections

object TicketPrinterHelper {

    fun imprimirTicket58mm(context: Context, ticket: TicketEntity) {
        val connection = UsbPrintersConnections.selectFirstConnected(context)
        if (connection != null) {
            val printer = EscPosPrinter(connection, 203, 48f, 32)

            // Formatação do número: Nº 000001
            val nFormatado = String.format("%06d", ticket.numeroControle)
            val idCurto = ticket.dispositivoId.takeLast(6)

            val layout = """
                [R]<b>Nº $nFormatado</b>
                [C]<b><font size='big'>TICKET DE CARGA</font></b>
                [C]ID: $idCurto
                [C]--------------------------------
                [L]<b>PLACA:</b> [R]${ticket.placa}
                [L]<b>MATERIAL:</b> [R]${ticket.material}
                [L]<b>QTD:</b> [R]${ticket.quantidade} m3
                [L]<b>ORIGEM:</b> [R]${ticket.obraOrigem}
                [L]<b>SAÍDA:</b> [R]${ticket.dataSaida} ${ticket.horaSaida}
                [C]--------------------------------
                [C]<qrcode size='40'>${ticket.gerarStringQrCode()}</qrcode>
                [C]--------------------------------
                [C]CONFIRA OS DADOS NO DESTINO
                [L]
                [L]
            """.trimIndent()

            printer.printFormattedText(layout)
        } else {
            throw Exception("Impressora USB não detectada.")
        }
    }
}