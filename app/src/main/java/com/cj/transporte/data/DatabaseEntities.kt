package com.cj.transporte.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "tickets")
data class TicketEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val numeroControle: Int, // Sequência numérica para auditoria
    val dispositivoId: String, // ID do celular que gerou
    val serial: String = UUID.randomUUID().toString(),
    val statusTicket: String = "ABERTO",
    val statusSinc: String = "LOCAL",

    // --- DADOS DA ORIGEM ---
    val obraOrigem: String,
    val placa: String,
    val dataSaida: String,
    val horaSaida: String,
    val apontadorOrigem: String,
    val estacaInicial: String,
    val contratoInicial: String,
    val kmInicial: Double,
    val motorista: String,
    val material: String,
    val quantidade: Double,
    val gpsOrigem: String,

    // --- DADOS DO DESTINO ---
    val obraDestino: String? = null,
    val dataChegada: String? = null,
    val horaChegada: String? = null,
    val apontadorDestino: String? = null,
    val estacaDestino: String? = null,
    val contratoDestino: String? = null,
    val kmDestino: Double? = null,
    val gpsDestino: String? = null
) {
    fun gerarStringQrCode(): String {
        // Incluímos o número de controle no QR Code para o destino validar
        return "$serial|$numeroControle|$placa|$obraOrigem|$material|$quantidade"
    }
}

@Dao
interface TicketDao {
    @Query("SELECT * FROM tickets ORDER BY id DESC")
    fun getAllTickets(): Flow<List<TicketEntity>>

    @Query("SELECT * FROM tickets WHERE id = :ticketId")
    suspend fun getTicketPorId(ticketId: Int): TicketEntity?

    @Query("SELECT * FROM tickets WHERE serial = :serial LIMIT 1")
    suspend fun getTicketPorSerial(serial: String): TicketEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: TicketEntity): Long

    @Delete
    suspend fun deleteTicket(ticket: TicketEntity)

    @Update
    suspend fun updateTicket(ticket: TicketEntity)
}

@Database(entities = [TicketEntity::class], version = 2) // Versão 2 devido aos novos campos
abstract class AppDatabase : RoomDatabase() {
    abstract fun ticketDao(): TicketDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "transporte_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}