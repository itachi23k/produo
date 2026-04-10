package com.cj.transporte.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProducaoDao {

    @Transaction
    @Query("SELECT * FROM relatorios WHERE id = :relatorioId")
    suspend fun getRelatorioPorId(relatorioId: Int): RelatorioComSegmentosRoom?

    @Query("DELETE FROM segmentos WHERE relatorioId = :relatorioId")
    suspend fun deletarSegmentosDoRelatorio(relatorioId: Int)

    @Transaction
    @Query("SELECT * FROM relatorios ORDER BY id DESC")
    fun getRelatoriosComSegmentosRoom(): Flow<List<RelatorioComSegmentosRoom>>

    @Insert
    suspend fun insertRelatorio(relatorio: RelatorioEntity): Long

    @Insert
    suspend fun insertSegmentos(segmentos: List<SegmentoEntity>)

    @Delete
    suspend fun deletarRelatorio(relatorio: RelatorioEntity) // ADICIONE ESTA LINHA

    @Update
    suspend fun updateRelatorio(relatorio: RelatorioEntity)
}

// MUDAMOS PARA VERSÃO 3: Para garantir que o Room recrie as tabelas com as novas colunas
@Database(entities = [RelatorioEntity::class, SegmentoEntity::class], version = 6   )
abstract class AppDatabase : RoomDatabase() {
    abstract fun producaoDao(): ProducaoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "producao_db"
                )
                    // Como mudamos a estrutura da RelatorioEntity (tempoManha, etc),
                    // o fallback limpa o erro de Cursor e MissingType recriando o banco.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}