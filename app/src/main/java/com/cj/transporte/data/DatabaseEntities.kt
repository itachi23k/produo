package com.cj.transporte.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "tickets")
data class TicketEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val serial: String = UUID.randomUUID().toString(),
    val statusTicket: String = "ABERTO", // ABERTO (só saiu) ou FECHADO (chegou)
    val statusSinc: String = "LOCAL",

    // --- DADOS DA ORIGEM (SAÍDA) ---
    val obraOrigem: String,
    val placa: String,
    val dataSaida: String,
    val horaSaida: String,
    val apontadorOrigem: String,
    val estacaInicial: String,
    val contratoInicial: String,
    val kmInicial: Double, // Será calculado no ViewModel
    val motorista: String,
    val material: String,
    val quantidade: Double,
    val gpsOrigem: String, // Ex: "Lat: -3.1, Lng: -60.0"

    // --- DADOS DO DESTINO (CHEGADA - Opcionais na saída) ---
    val obraDestino: String? = null,
    val dataChegada: String? = null,
    val horaChegada: String? = null,
    val apontadorDestino: String? = null,
    val estacaDestino: String? = null,
    val contratoDestino: String? = null,
    val kmDestino: Double? = null,
    val gpsDestino: String? = null
) {
    // 🚩 ESSA É A STRING QUE VAI DENTRO DO QR CODE
    // Compactada usando o delimitador "|" para economizar espaço no papel
    fun gerarStringQrCode(): String {
        return "$serial|$placa|$obraOrigem|$material|$quantidade|$dataSaida|$horaSaida|$apontadorOrigem"
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

@Database(entities = [TicketEntity::class], version = 1)
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