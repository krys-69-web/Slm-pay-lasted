package com.example.slmplay.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.slmplay.utils.BiometricAuthPromptHelper
import com.example.slmplay.utils.PinVerificationResult
import com.example.slmplay.utils.SecurityVaultManager
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class PinLockMode {
    UNLOCK,           // Unlock private space or app
    SETUP_NEW,        // Enter new PIN
    SETUP_CONFIRM,    // Confirm new PIN
    SETUP_DECOY       // Configure fake/decoy PIN
}

@Composable
fun PinLockScreen(
    securityVaultManager: SecurityVaultManager,
    isForAppLaunch: Boolean = false,
    onAuthSuccess: (isDecoy: Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isConfigured = remember { securityVaultManager.isPinConfigured() }

    var currentMode by remember {
        mutableStateOf(PinLockMode.UNLOCK)
    }

    val pinTargetLength = remember {
        if (isConfigured) securityVaultManager.getPinLength() else 4
    }

    var enteredPin by remember { mutableStateOf("") }
    var temporaryPinForConfirmation by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isShaking by remember { mutableStateOf(false) }

    // Biometrics & Device Credential availability
    val canUseBiometrics = remember {
        securityVaultManager.isBiometricEnabled() && BiometricAuthPromptHelper.isBiometricOrDeviceCredentialAvailable(context)
    }

    // Trigger biometric automatically on UNLOCK mode if available
    LaunchedEffect(Unit) {
        if (currentMode == PinLockMode.UNLOCK && canUseBiometrics) {
            (context as? FragmentActivity)?.let { activity ->
                BiometricAuthPromptHelper.showBiometricPrompt(
                    activity = activity,
                    title = "Déverrouillage Sécurisé",
                    subtitle = "Reconnaissance faciale (Face Lock), Empreinte ou Code Android",
                    onSuccess = {
                        onAuthSuccess(false)
                    },
                    onError = { err ->
                        // Biometric failed or dismissed, fallback to PIN
                        errorMessage = err
                    }
                )
            }
        }
    }

    fun handlePinDigit(digit: String) {
        if (enteredPin.length < pinTargetLength) {
            errorMessage = null
            val newPin = enteredPin + digit
            enteredPin = newPin

            if (newPin.length == pinTargetLength) {
                coroutineScope.launch {
                    delay(120) // Slight visual delay to show full dots
                    when (currentMode) {
                        PinLockMode.UNLOCK -> {
                            val result = securityVaultManager.verifyPin(newPin)
                            when (result) {
                                PinVerificationResult.SUCCESS_REAL -> {
                                    onAuthSuccess(false)
                                }
                                PinVerificationResult.SUCCESS_DECOY -> {
                                    onAuthSuccess(true)
                                }
                                PinVerificationResult.INVALID -> {
                                    errorMessage = "Code PIN incorrect"
                                    isShaking = true
                                    delay(400)
                                    isShaking = false
                                    enteredPin = ""
                                }
                            }
                        }
                        PinLockMode.SETUP_NEW -> {
                            temporaryPinForConfirmation = newPin
                            enteredPin = ""
                            currentMode = PinLockMode.SETUP_CONFIRM
                        }
                        PinLockMode.SETUP_CONFIRM -> {
                            if (newPin == temporaryPinForConfirmation) {
                                securityVaultManager.setMasterPin(newPin)
                                onAuthSuccess(false)
                            } else {
                                errorMessage = "Les codes ne correspondent pas"
                                isShaking = true
                                delay(400)
                                isShaking = false
                                enteredPin = ""
                                currentMode = PinLockMode.SETUP_NEW
                            }
                        }
                        PinLockMode.SETUP_DECOY -> {
                            securityVaultManager.setDecoyPin(newPin)
                            onAuthSuccess(false)
                        }
                    }
                }
            }
        }
    }

    fun handleDeleteDigit() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            errorMessage = null
        }
    }

    val title = when (currentMode) {
        PinLockMode.UNLOCK -> if (isForAppLaunch) "SLM Play Verrouillé" else "Espace Privé Sécurisé"
        PinLockMode.SETUP_NEW -> "Créer votre Code PIN"
        PinLockMode.SETUP_CONFIRM -> "Confirmez votre Code PIN"
        PinLockMode.SETUP_DECOY -> "Configurer le Faux Code PIN"
    }

    val subtitle = when (currentMode) {
        PinLockMode.UNLOCK -> "Face Lock (Visage), Empreinte ou Code PIN (0000)"
        PinLockMode.SETUP_NEW -> "Choisissez un nouveau code à $pinTargetLength chiffres"
        PinLockMode.SETUP_CONFIRM -> "Répétez le code pour confirmation"
        PinLockMode.SETUP_DECOY -> "Code d'urgence pour ouvrir un espace factice"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0D0F14),
                        Color(0xFF141722),
                        Color(0xFF090A0E)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 24.dp)
        ) {
            // Header with dismiss / back
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isForAppLaunch) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(DarkGlassCard)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = Color.White
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(42.dp))
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AppleCrimson.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppleCrimson.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = AppleCrimson,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Chiffrement AES-256",
                            color = AppleCrimson,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Central PIN Prompt
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(AppleCrimson.copy(alpha = 0.35f), Color.Transparent)
                            )
                        )
                        .border(1.5.dp, AppleCrimson.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (currentMode == PinLockMode.UNLOCK) Icons.Default.Lock else Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until pinTargetLength) {
                        val isFilled = i < enteredPin.length
                        val dotScale by animateFloatAsState(
                            targetValue = if (isFilled) 1.25f else 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessHigh),
                            label = "dot_scale"
                        )

                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .scale(dotScale)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) AppleCrimson else Color.White.copy(alpha = 0.15f)
                                )
                                .border(
                                    1.dp,
                                    if (isFilled) AppleCrimson else Color.White.copy(alpha = 0.3f),
                                    CircleShape
                                )
                        )
                    }
                }

                // Error message
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFFF453A),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                // Default PIN indicator & Change PIN action
                if (currentMode == PinLockMode.UNLOCK) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.05f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = AppleCrimson,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Code par défaut : 0000 • Leurre : 9999",
                                color = TextSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            enteredPin = ""
                            errorMessage = null
                            currentMode = PinLockMode.SETUP_NEW
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = AppleCrimson,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Personnaliser le code PIN",
                            color = AppleCrimson,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = {
                            enteredPin = ""
                            errorMessage = null
                            currentMode = PinLockMode.UNLOCK
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Annuler (Déverrouiller avec 0000)",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Central Mobile Biometric / Face Lock Quick Button
            if (currentMode == PinLockMode.UNLOCK) {
                Surface(
                    onClick = {
                        (context as? FragmentActivity)?.let { activity ->
                            BiometricAuthPromptHelper.showBiometricPrompt(
                                activity = activity,
                                title = "Déverrouillage Sécurisé",
                                subtitle = "Reconnaissance faciale (Face Lock), Empreinte ou Code Mobile",
                                onSuccess = { onAuthSuccess(false) },
                                onError = { err -> errorMessage = err }
                            )
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = AppleCrimson.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppleCrimson.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(AppleCrimson.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = null,
                                tint = AppleCrimson,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Déverrouiller avec le mobile",
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Face Lock • Empreinte • Sécurité Android",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = AppleCrimson,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Keypad
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                )

                for (row in rows) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(22.dp),
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (digit in row) {
                            KeypadButton(
                                text = digit,
                                modifier = Modifier.weight(1f),
                                onClick = { handlePinDigit(digit) }
                            )
                        }
                    }
                }

                // Bottom row: Biometric / Empty, 0, Backspace
                Row(
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Biometric button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentMode == PinLockMode.UNLOCK) {
                            IconButton(
                                onClick = {
                                    (context as? FragmentActivity)?.let { activity ->
                                        BiometricAuthPromptHelper.showBiometricPrompt(
                                            activity = activity,
                                            onSuccess = { onAuthSuccess(false) },
                                            onError = { err -> errorMessage = err }
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(DarkGlassCard)
                                    .border(1.dp, GlassBorder, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Biométrie Mobile",
                                    tint = AppleCrimson,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    // Digit 0
                    KeypadButton(
                        text = "0",
                        modifier = Modifier.weight(1f),
                        onClick = { handlePinDigit("0") }
                    )

                    // Backspace button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = { handleDeleteDigit() },
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(DarkGlassCard)
                                .border(1.dp, GlassBorder, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Backspace,
                                contentDescription = "Effacer",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(CircleShape)
            .background(DarkGlassCard)
            .border(1.dp, GlassBorder, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = AppleCrimson.copy(alpha = 0.3f)),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
