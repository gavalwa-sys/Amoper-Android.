package africa.amoper.app.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import africa.amoper.app.data.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(user: User?, onLogout: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Profile") }) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Icon(Icons.Filled.AccountCircle, contentDescription = null, modifier = Modifier.size(88.dp))
            Spacer(Modifier.height(12.dp))
            Text(user?.name ?: "—", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(user?.email ?: "", style = MaterialTheme.typography.bodyMedium)
            user?.phone?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }

            if (!user?.roles.isNullOrEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row {
                    user!!.roles.forEach { role ->
                        AssistChip(onClick = {}, label = { Text(role) }, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Log out") }
        }
    }
}
