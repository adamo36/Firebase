package com.example.android3

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.delay

// MODEL DANYCH
data class NoteMessage(
    val id: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

// VIEWMODEL (Logika Firebase)
class FirebaseViewModel : ViewModel() {
    // Połączenie z Firebase
    private val database = FirebaseDatabase.getInstance("https://android3-55619-default-rtdb.europe-west1.firebasedatabase.app/")
    private val myRef = database.getReference("notes")

    // Odczytywanie danych (State)
    private val _messages = mutableStateOf<List<NoteMessage>>(emptyList())
    val messages: State<List<NoteMessage>> = _messages

    var textInput by mutableStateOf("")

    init {
        // Nasłuchiwanie zmian w czasie rzeczywistym
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newList = mutableListOf<NoteMessage>()
                for (child in snapshot.children) {
                    val message = child.getValue(NoteMessage::class.java)
                    if (message != null) {
                        newList.add(message)
                    }
                }
                _messages.value = newList
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("Firebase", "Błąd odczytu", error.toException())
            }
        })
    }

    // Wysłanie danych
    fun sendMessage() {
        if (textInput.isNotBlank()) {
            val key = myRef.push().key ?: return
            val newMessage = NoteMessage(id = key, text = textInput)

            myRef.child(key).setValue(newMessage)
                .addOnSuccessListener {
                    textInput = ""
                }
        }
    }
}

// --- GŁÓWNA AKTYWNOŚĆ ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppNavigation()
            }
        }
    }
}

// --- NAWIGACJA ---
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(navController)
        }
        composable("home") {
            val viewModel: FirebaseViewModel = viewModel()
            MainScreen(viewModel)
        }
    }
}

// SPLASH SCREEN
@Composable
fun SplashScreen(navController: NavController) {
    LaunchedEffect(key1 = true) {
        delay(2000) // Czeka 2 sekundy
        navController.navigate("home") {
            popUpTo("splash") { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Logo",
                tint = Color.White,
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Projekt 3\nCzat grupowy",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

//APLIKACJA WŁAŚCIWA
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: FirebaseViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Android3 Czat grupowy") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Lista wiadomości
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                reverseLayout = true
            ) {
                items(viewModel.messages.value.reversed()) { msg ->
                    MessageCard(msg)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Panel wysyłania
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = viewModel.textInput,
                    onValueChange = { viewModel.textInput = it },
                    label = { Text("Wpisz wiadomość...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.sendMessage() },
                    enabled = viewModel.textInput.isNotBlank()
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = "Wyślij")
                }
            }
        }
    }
}

@Composable
fun MessageCard(msg: NoteMessage) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = msg.text, fontSize = 16.sp)
        }
    }
}