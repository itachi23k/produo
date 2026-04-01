package com.cj.producao

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cj.producao.ui.theme.ProducaoTheme
import com.cj.producao.ui.theme.VerdeProducao
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.Source
import java.text.SimpleDateFormat
import java.util.Locale

class AtivacaoActivity : ComponentActivity() {

    private lateinit var deviceId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings

        deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID).uppercase()

        if (estaAtivadoValido()) {
            irParaHub()
            return
        }

        setContent {
            ProducaoTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    TelaAtivacao(
                        id = deviceId,
                        onAtivarClick = { verificarLicencaNuvem(deviceId) },
                        onLongClickId = { copiarIdParaAreaDeTransferencia() }
                    )
                }
            }
        }
    }

    private fun verificarLicencaNuvem(id: String) {
        val db = FirebaseFirestore.getInstance()
        // Buscando na coleção que você criou
        db.collection("fiscais_autorizados").document(id).get(Source.SERVER)
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    // Pegando os campos conforme você deve ter criado no Firestore
                    val ativo = doc.getBoolean("ativo") ?: false
                    val validadeStr = doc.getString("validade") ?: "2000-01-01"
                    val nomeFiscal = doc.getString("nome") ?: "Fiscal"

                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    val dataExpira = sdf.parse(validadeStr)?.time ?: 0L

                    if (ativo && dataExpira > System.currentTimeMillis()) {
                        salvarAtivacao(nomeFiscal, dataExpira)
                        irParaHub()
                    } else {
                        Toast.makeText(this, "Licença inativa ou expirada!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "ID não encontrado em fiscais_autorizados!", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { Toast.makeText(this, "Erro de conexão!", Toast.LENGTH_SHORT).show() }
    }

    private fun salvarAtivacao(nome: String, dataExpiracao: Long) {
        getSharedPreferences("config_pref", MODE_PRIVATE).edit().apply {
            putBoolean("is_active", true)
            putLong("data_expiracao", dataExpiracao)
            putString("nome_usuario", nome)
            apply()
        }
    }

    private fun estaAtivadoValido(): Boolean {
        val prefs = getSharedPreferences("config_pref", MODE_PRIVATE)
        return prefs.getBoolean("is_active", false) && prefs.getLong("data_expiracao", 0L) > System.currentTimeMillis()
    }

    private fun copiarIdParaAreaDeTransferencia() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("DeviceID", deviceId))
        Toast.makeText(this, "ID Copiado!", Toast.LENGTH_SHORT).show()
    }

    private fun irParaHub() {
        startActivity(Intent(this, HubActivity::class.java))
        finish()
    }
}

@Composable
fun TelaAtivacao(id: String, onAtivarClick: () -> Unit, onLongClickId: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("PRODUÇÃO", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = VerdeProducao)
        Spacer(Modifier.height(48.dp))
        Button(onClick = onAtivarClick, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = VerdeProducao)) {
            Text("VERIFICAR LICENÇA")
        }
        Spacer(Modifier.height(64.dp))
        Text(text = "ID: $id", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.pointerInput(Unit) { detectTapGestures(onLongPress = { onLongClickId() }) })
    }
}
