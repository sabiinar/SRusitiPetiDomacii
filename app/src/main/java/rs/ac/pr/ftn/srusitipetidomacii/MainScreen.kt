package rs.ac.pr.ftn.srusitipetidomacii

import android.view.MotionEvent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment


@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun MainScreen() {
    val wordsRepository = remember { WordsRepository() }
    var words by remember { mutableStateOf(wordsRepository.words) }
    var textFieldValue by remember { mutableStateOf(TextFieldValue()) }
    var ascendingOrder by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val lazyListState = rememberLazyListState()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var showDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var longPressJob by remember { mutableStateOf<Job?>(null) }

    fun sentenceCase(word: String): String {
        return word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aplikacija: Lista Reči") },
                actions = {
                    IconButton(onClick = {
                        ascendingOrder = !ascendingOrder
                        wordsRepository.sortWords(ascendingOrder)
                        words = wordsRepository.words
                    }) {
                        Icon(
                            imageVector = if (ascendingOrder) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                            contentDescription = if (ascendingOrder) "Sortiraj A → Z" else "Sortiraj Z → A"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        scope.launch { lazyListState.scrollToItem(0) }
                    },
                    modifier = Modifier.pointerInteropFilter {
                        when (it.action) {
                            MotionEvent.ACTION_DOWN -> {
                                longPressJob = scope.launch {
                                    delay(500L)
                                    showDialog = true
                                }
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                longPressJob?.cancel()
                            }
                        }
                        false
                    }
                ) {
                    Text("Idi na početak")
                }
                Button(onClick = {
                    scope.launch {
                        val lastIndex = if (words.isNotEmpty()) words.size - 1 else 0
                        lazyListState.scrollToItem(lastIndex)
                    }
                }) {
                    Text("Idi na kraj")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            TextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    if (newValue.text.all { it.isLetter() }) {
                        textFieldValue = newValue
                    }
                    errorMessage = null
                },
                label = { Text("Unesi reč") },
                isError = errorMessage != null,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Text(
                    text = "${textFieldValue.text.length} / 20",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val word = textFieldValue.text.trim()
                    when {
                        word.isEmpty() || word.length > 20 -> {
                            errorMessage = "Unesite reč do 20 slova"
                        }
                        words.any { it.equals(word, true) } -> {
                            errorMessage = "Reč već postoji"
                        }
                        else -> {
                            wordsRepository.addWord(word)
                            wordsRepository.sortWords(ascendingOrder) // sort immediately after adding
                            words = wordsRepository.words
                            keyboardController?.hide()
                            Toast.makeText(context, "Reč uspešno dodata", Toast.LENGTH_SHORT).show()
                            textFieldValue = TextFieldValue("")
                        }
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Dodaj",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text("Dodaj reč")
                }
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(words) { word ->
                    ListItem(
                        headlineContent = {
                            Text(sentenceCase(word))
                        },
                        trailingContent = {
                            IconButton(onClick = {
                                wordsRepository.removeWord(word)
                                words = wordsRepository.words
                                Toast.makeText(context, "Obrisano: $word", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Obriši")
                            }
                        }
                    )
                    Divider()
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Brisanje svih reči") },
            text = { Text("Da li ste sigurni da želite da obrišete sve reči?") },
            confirmButton = {
                TextButton(onClick = {
                    wordsRepository.clearAll()
                    words = wordsRepository.words
                    showDialog = false
                    Toast.makeText(context, "Sve reči obrisane", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Da")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Ne")
                }
            }
        )
    }
}
