    package com.cj.transporte.data

    import androidx.room.Entity
    import androidx.room.ForeignKey
    import androidx.room.PrimaryKey

    @Entity(
        tableName = "segmentos",
        foreignKeys = [
            ForeignKey(
                entity = RelatorioEntity::class,
                parentColumns = ["id"],
                childColumns = ["relatorioId"],
                onDelete = ForeignKey.CASCADE
            )
        ]
    )
    data class SegmentoEntity(
        @PrimaryKey(autoGenerate = true) val id: Int = 0,
        val relatorioId: Int,
        val lado: String,
        val estacaInicial: Int,
        val fracaoInicial: Double,
        val estacaFinal: Int,
        val fracaoFinal: Double,
        val largura: Double,
        val comprimento: Double,
        val espessura: Double
    )