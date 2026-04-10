package com.cj.transporte.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "relatorios")
data class RelatorioEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val contrato: String,
    val servico: String,
    val dataObra: String,
    val statusSinc: String = "LOCAL",
    val tempoManha: String = "BOM",
    val trechoManha: String = "PRATICÁVEL",
    val tempoTarde: String = "BOM",
    val trechoTarde: String = "PRATICÁVEL",
    val materiaisJson: String? = null,
    val serial: String = UUID.randomUUID().toString() // Serial único para evitar duplicidade na nuvem
)