package com.example.slmplay.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.slmplay.utils.AssistantChatMessage
import com.example.slmplay.utils.OpenAiAssistantManager
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun OpenAiAssistantDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    availableTrackTitles: List<String> = emptyList()
) {
    if (!isOpen) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val assistantManager = remember { OpenAiAssistantManager(context) }

    var apiKeyInput by remember { mutableStateOf(assistantManager.getApiKey()) }
    var serverUrlInput by remember { mutableStateOf(assistantManager.getServerUrl()) }
    var showConfigPanel by remember { mutableStateOf(false) }

    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val messages = remember {
        mutableStateListOf(
            AssistantChatMessage(
                sender = "assistant",
                text = "Bonjour ! Je suis votre assistant musical intelligent OpenAI pour SLM Play. Demandez-moi une suggestion de playlist, un conseil d'égalisation ou une recommandation selon votre humeur !"
            )
        )
    }

    fun sendMessage(prompt: String) {
        if (prompt.isBlank() || isLoading) return
        val userMsg = AssistantChatMessage(sender = "user", text = prompt)
        messages.add(userMsg)
        inputText = ""
        isLoading = true

        coroutineScope.launch {
            val result = assistantManager.sendMessage(prompt, availableTrackTitles)
            isLoading = false
            val replyText = result.getOrElse { "Une erreur s'est produite lors de la connexion à l'assistant." }
            messages.add(AssistantChatMessage(sender = "assistant", text = replyText))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f),
                shape = RoundedCornerShape(26.dp),
                backgroundColor = DarkGlassElevated,
                borderColor = GlassBorder
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF10A37F), Color(0xFF007AFF))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Assistant Musical OpenAI",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (assistantManager.getApiKey().isNotBlank()) "Connecté via OpenAI API" else "Connecté au serveur SLM",
                                    fontSize = 11.sp,
                                    color = Color(0xFF10A37F)
                                )
                            }
                        }

                        Row {
                            IconButton(
                                onClick = { showConfigPanel = !showConfigPanel },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Configuration",
                                    tint = if (showConfigPanel) AppleCrimson else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Fermer",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // Configuration Panel (collapsible)
                    AnimatedVisibility(visible = showConfigPanel) {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            shape = RoundedCornerShape(16.dp),
                            backgroundColor = DarkGlassCard
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Configuration Serveur & Clé OpenAI",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = apiKeyInput,
                                    onValueChange = {
                                        apiKeyInput = it
                                        assistantManager.setApiKey(it)
                                    },
                                    placeholder = { Text("Clé sk-... (Optionnel si serveur actif)", fontSize = 12.sp, color = TextTertiary) },
                                    label = { Text("Clé API OpenAI", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = Color(0xFF10A37F),
                                        unfocusedBorderColor = GlassBorder
                                    )
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = serverUrlInput,
                                    onValueChange = {
                                        serverUrlInput = it
                                        assistantManager.setServerUrl(it)
                                    },
                                    placeholder = { Text("http://10.0.2.2:3001/api/assistant", fontSize = 12.sp, color = TextTertiary) },
                                    label = { Text("URL du Serveur Assistant", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = Color(0xFF10A37F),
                                        unfocusedBorderColor = GlassBorder
                                    )
                                )
                            }
                        }
                    }

                    // Quick Prompt Chips
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "⚡ Playlist Gym",
                            "🌙 Chill nocturne",
                            "🎛️ Égalisation Bass"
                        ).forEach { chip ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkGlassCard)
                                    .clickable { sendMessage(chip) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(chip, fontSize = 11.sp, color = TextPrimary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Chat messages list
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            val isUser = msg.sender == "user"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .widthIn(max = 280.dp)
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (isUser) 16.dp else 4.dp,
                                                bottomEnd = if (isUser) 4.dp else 16.dp
                                            )
                                        )
                                        .background(
                                            if (isUser) AppleCrimson else DarkGlassCard
                                        )
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = msg.text,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }

                        if (isLoading) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF10A37F)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("L'assistant réfléchit...", fontSize = 12.sp, color = TextSecondary)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Input bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Posez une question musicale...", fontSize = 13.sp, color = TextTertiary) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(20.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = Color(0xFF10A37F),
                                unfocusedBorderColor = GlassBorder,
                                focusedContainerColor = DarkGlassCard,
                                unfocusedContainerColor = DarkGlassCard
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { sendMessage(inputText) },
                            enabled = inputText.isNotBlank() && !isLoading,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (inputText.isNotBlank() && !isLoading) Color(0xFF10A37F) else DarkGlassCard
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Envoyer",
                                tint = if (inputText.isNotBlank() && !isLoading) Color.White else TextTertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
