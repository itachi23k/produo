package com.cj.transporte.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "tickets")
data class TicketEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val numeroControle: Int,
    val dispositivoId: String,
    val serial: String = UUID.randomUUID().toString(),
    val statusTicket: String = "ABERTO",
    val statusSinc: String = "LOCAL",

    // --- ORIGEM ---
    val obraOrigem: String,
    val placa: String,
    val motorista: String,
    val material: String,
    val quantidade: Double,
    val dataSaida: String,
    val horaSaida: String,
    val estacaInicial: String,
    val contratoInicial: String,
    val kmInicial: Double,
    val apontadorOrigem: String,
    val gpsOrigem: String,

    // --- DESTINO ---
    val obraDestino: String? = null,
    val dataChegada: String? = null,
    val horaChegada: String? = null,
    val apontadorDestino: String? = null,
    val estacaDestino: String? = null,
    val kmDestino: Double? = null,
    val gpsDestino: String? = null
) {
    // No DatabaseEntities.kt dentro da TicketEntity
    fun gerarStringQrCode(): String {
        // Agora o QR Code leva tudo: Serial, Placa, Motorista, Material e Qtd
        return "$serial|$placa|$motorista|$material|$quantidade|$obraOrigem"
    }
}



@Entity(tableName = "caminhoes")
data class CaminhaoEntity(@PrimaryKey val placa: String, val motorista: String)

@Entity(tableName = "materiais")
data class MaterialEntity(@PrimaryKey val nome: String)

@Dao
interface TicketDao {
    @Query("SELECT * FROM tickets ORDER BY id DESC")
    fun getAllTickets(): Flow<List<TicketEntity>>

    // 🚩 AQUI ESTAVA O ERRO: A função que o ViewModel precisava
    @Query("SELECT * FROM tickets WHERE id = :ticketId LIMIT 1")
    suspend fun getTicketPorId(ticketId: Int): TicketEntity?

    @Query("SELECT * FROM tickets WHERE serial = :serial LIMIT 1")
    suspend fun getTicketPorSerial(serial: String): TicketEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: TicketEntity): Long

    @Update
    suspend fun updateTicket(ticket: TicketEntity)

    // --- Queries de Apoio ---
    @Query("SELECT * FROM caminhoes ORDER BY placa ASC")
    fun getAllCaminhoes(): Flow<List<CaminhaoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCaminhoes(lista: List<CaminhaoEntity>)

    @Query("SELECT * FROM materiais ORDER BY nome ASC")
    fun getAllMateriais(): Flow<List<MaterialEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMateriais(lista: List<MaterialEntity>)
}

@Database(entities = [TicketEntity::class, CaminhaoEntity::class, MaterialEntity::class], version = 3)
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