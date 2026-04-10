package com.cj.transporte.data

import androidx.room.Embedded
import androidx.room.Ignore
import androidx.room.Relation

// Esta classe é apenas para transporte de dados (PDF)
data class RelatorioComSegmentosRoom(
    @Embedded val relatorio: RelatorioEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "relatorioId"
    )





    val segmentos: List<SegmentoEntity>,
    @Ignore val materiais: List<MaterialItem> = emptyList()
) {
    // Construtor secundário para o Room não reclamar do campo @Ignore no construtor primário
    // dependendo da versão do Room/Kotlin.
    constructor(relatorio: RelatorioEntity, segmentos: List<SegmentoEntity>) : this(relatorio, segmentos, emptyList())
}






// O erro 'Unresolved reference MaterialItem' some aqui
data class MaterialItem(
    val nome: String,
    val quantidade: Double?
)