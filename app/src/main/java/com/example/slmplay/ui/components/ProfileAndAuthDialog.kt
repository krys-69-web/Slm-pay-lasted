package com.example.slmplay.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.slmplay.data.model.UserAccount
import com.example.slmplay.data.model.UserProfile
import com.example.ui.theme.*

@Composable
fun ProfileAndAuthDialog(
    isOpen: Boolean,
    userProfile: UserProfile,
    savedAccounts: List<UserAccount> = emptyList(),
    tracksCount: Int = 0,
    playlistsCount: Int = 0,
    favoritesCount: Int = 0,
    onDismiss: () -> Unit,
    onLogin: (username: String, emailOrId: String) -> Unit,
    onRegister: (username: String, emailOrId: String) -> Unit,
    onSwitchAccount: (userId: String) -> Unit = {},
    onUpdateProfile: (username: String, avatarUri: String?) -> Unit,
    onExportCloudData: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    if (!isOpen) return

    var isRegisterMode by remember { mutableStateOf(false) }
    var inputUsername by remember(userProfile.username) { mutableStateOf(userProfile.username) }
    var inputEmail by remember(userProfile.emailOrId) { mutableStateOf(userProfile.emailOrId) }
    var inputPassword by remember { mutableStateOf("") }
    var currentAvatarUri by remember(userProfile.avatarUri) { mutableStateOf(userProfile.avatarUri) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isEditingName by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            currentAvatarUri = uri.toString()
            onUpdateProfile(inputUsername, uri.toString())
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(32.dp))
                .background(DarkCanvas)
                .border(1.dp, GlassBorder, RoundedCornerShape(32.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header with Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AppleCrimson.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (userProfile.isLoggedIn) Icons.Default.Person else Icons.Default.Lock,
                                contentDescription = null,
                                tint = AppleCrimson,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (userProfile.isLoggedIn) "Espace Utilisateur" else if (isRegisterMode) "Créer un profil" else "Connexion",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(DarkGlassCard)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (userProfile.isLoggedIn) {
                    // ================= LOGGED IN PROFILE VIEW =================
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            // Avatar View with edit badge
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Box(
                                    modifier = Modifier
                                        .size(92.dp)
                                        .shadow(16.dp, CircleShape, spotColor = AppleCrimson.copy(alpha = 0.5f))
                                        .clip(CircleShape)
                                        .border(2.5.dp, Brush.linearGradient(listOf(AppleCrimson, ApplePurple)), CircleShape)
                                        .background(Color.Black)
                                        .clickable { imagePickerLauncher.launch("image/*") }
                                ) {
                                    if (currentAvatarUri != null) {
                                        AsyncImage(
                                            model = currentAvatarUri,
                                            contentDescription = "Avatar de profil",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(id = R.drawable.slm_logo),
                                            contentDescription = "Avatar SLM Play",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                // Edit camera icon badge
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(AppleCrimson)
                                        .clickable { imagePickerLauncher.launch("image/*") }
                                        .padding(5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = "Changer l'image de profil", tint = Color.White, modifier = Modifier.size(15.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Username
                            if (isEditingName) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(0.9f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = inputUsername,
                                        onValueChange = { inputUsername = it },
                                        label = { Text("Nom d'utilisateur", color = TextSecondary, fontSize = 12.sp) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = DarkGlassCard,
                                            unfocusedContainerColor = DarkGlassCard,
                                            focusedBorderColor = AppleCrimson,
                                            unfocusedBorderColor = GlassBorder,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            if (inputUsername.isNotBlank()) {
                                                onUpdateProfile(inputUsername.trim(), currentAvatarUri)
                                                isEditingName = false
                                            }
                                        },
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(AppleCrimson)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Valider", tint = Color.White)
                                    }
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = userProfile.username.ifBlank { "Utilisateur" },
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = {
                                            inputUsername = userProfile.username
                                            isEditingName = true
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Modifier le nom", tint = AppleCrimson, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            if (userProfile.emailOrId.isNotBlank()) {
                                Text(
                                    text = userProfile.emailOrId,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Real User Statistics Card
                        item {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                backgroundColor = DarkGlassElevated
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Titres", fontSize = 11.sp, color = TextSecondary)
                                        Text("$tracksCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppleCrimson)
                                    }
                                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(GlassBorder))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Favoris", fontSize = 11.sp, color = TextSecondary)
                                        Text("$favoritesCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ApplePurple)
                                    }
                                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(GlassBorder))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Playlists", fontSize = 11.sp, color = TextSecondary)
                                        Text("$playlistsCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    }
                                }
                            }
                        }

                        // Multi-Account Switcher Section (if accounts exist)
                        if (savedAccounts.size > 1) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "COMPTES ENREGISTRÉS SUR CET APPAREIL",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextTertiary,
                                        letterSpacing = 0.5.sp,
                                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                                    )

                                    savedAccounts.forEach { acc ->
                                        val isCurrent = acc.id == userProfile.userId
                                        GlassCard(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp)
                                                .clickable {
                                                    if (!isCurrent) onSwitchAccount(acc.id)
                                                },
                                            shape = RoundedCornerShape(12.dp),
                                            backgroundColor = if (isCurrent) DarkGlassCard else Color.Black.copy(alpha = 0.3f),
                                            borderColor = if (isCurrent) AppleCrimson.copy(alpha = 0.5f) else GlassBorder
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(30.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isCurrent) AppleCrimson else DarkGlassCard),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(acc.username, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                                    if (acc.emailOrId.isNotBlank()) {
                                                        Text(acc.emailOrId, fontSize = 10.sp, color = TextSecondary)
                                                    }
                                                }
                                                if (isCurrent) {
                                                    Text("Actif", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppleCrimson)
                                                } else {
                                                    Text("Basculer", fontSize = 11.sp, color = TextSecondary)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Cloud Export
                        item {
                            GlassButton(
                                onClick = onExportCloudData,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_export_cloud_data"),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = ApplePurple, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Sauvegarde & Export des données", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                        Text("Exporter vos playlists, favoris et métadonnées", fontSize = 11.sp, color = TextSecondary)
                                    }
                                    Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(14.dp))
                                }
                            }
                        }

                        // Logout Action
                        item {
                            GlassButton(
                                onClick = onLogout,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_logout_profile"),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Logout, contentDescription = null, tint = Color(0xFFFF9500), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Se déconnecter de cet espace", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                }
                            }
                        }

                        // Delete Account Action
                        item {
                            GlassButton(
                                onClick = { showDeleteConfirmDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_delete_account"),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = AppleCrimson, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Supprimer ce profil et ses données", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppleCrimson)
                                }
                            }
                        }
                    }
                } else {
                    // ================= AUTH / LOGIN / REGISTER VIEW =================
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            // Official SLM Logo (technical branding asset)
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .shadow(14.dp, RoundedCornerShape(20.dp), spotColor = AppleCrimson.copy(alpha = 0.5f))
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.Black)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.slm_logo),
                                    contentDescription = "Logo SLM Play",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = if (isRegisterMode) "Créer un nouvel espace" else "Espace utilisateur SLM",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            Text(
                                text = if (isRegisterMode) "Créez votre profil isolé et commencez avec un espace propre." else "Connectez-vous pour accéder à vos musiques et playlists.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Tab Toggle (Connexion / Inscription)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(DarkGlassCard)
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (!isRegisterMode) AppleCrimson else Color.Transparent)
                                        .clickable {
                                            isRegisterMode = false
                                            errorMessage = null
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Connexion", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (!isRegisterMode) Color.White else TextSecondary)
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isRegisterMode) AppleCrimson else Color.Transparent)
                                        .clickable {
                                            isRegisterMode = true
                                            errorMessage = null
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Créer un profil", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isRegisterMode) Color.White else TextSecondary)
                                }
                            }
                        }

                        // List of existing accounts to quickly switch to
                        if (savedAccounts.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "PROFILS EXISTANTS SUR L'APPAREIL",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextTertiary,
                                        letterSpacing = 0.5.sp,
                                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                                    )

                                    savedAccounts.forEach { acc ->
                                        GlassCard(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp)
                                                .clickable { onSwitchAccount(acc.id) },
                                            shape = RoundedCornerShape(12.dp),
                                            backgroundColor = DarkGlassCard
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = AppleCrimson, modifier = Modifier.size(24.dp))
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(acc.username, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                                    if (acc.emailOrId.isNotBlank()) {
                                                        Text(acc.emailOrId, fontSize = 10.sp, color = TextSecondary)
                                                    }
                                                }
                                                Text("Accéder", fontSize = 11.sp, color = AppleCrimson, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            // Inputs
                            OutlinedTextField(
                                value = inputUsername,
                                onValueChange = {
                                    inputUsername = it
                                    errorMessage = null
                                },
                                label = { Text("Nom d'utilisateur / Pseudo", color = TextSecondary, fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary) },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = DarkGlassCard,
                                    unfocusedContainerColor = DarkGlassCard,
                                    focusedBorderColor = AppleCrimson,
                                    unfocusedBorderColor = GlassBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("auth_username_input")
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = inputEmail,
                                onValueChange = {
                                    inputEmail = it
                                    errorMessage = null
                                },
                                label = { Text("E-mail ou Identifiant (Optionnel)", color = TextSecondary, fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = TextSecondary) },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = DarkGlassCard,
                                    unfocusedContainerColor = DarkGlassCard,
                                    focusedBorderColor = AppleCrimson,
                                    unfocusedBorderColor = GlassBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("auth_email_input")
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = inputPassword,
                                onValueChange = { inputPassword = it },
                                label = { Text("Mot de passe", color = TextSecondary, fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary) },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = DarkGlassCard,
                                    unfocusedContainerColor = DarkGlassCard,
                                    focusedBorderColor = AppleCrimson,
                                    unfocusedBorderColor = GlassBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("auth_password_input")
                            )
                        }

                        if (errorMessage != null) {
                            item {
                                Text(
                                    text = errorMessage!!,
                                    color = AppleCrimson,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        item {
                            // Submit Button
                            GlassButton(
                                onClick = {
                                    val name = inputUsername.trim()
                                    val email = inputEmail.trim()
                                    if (name.isBlank()) {
                                        errorMessage = "Veuillez renseigner un nom d'utilisateur."
                                        return@GlassButton
                                    }
                                    if (isRegisterMode) {
                                        onRegister(name, email)
                                    } else {
                                        onLogin(name, email)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("auth_submit_btn"),
                                shape = RoundedCornerShape(16.dp),
                                isPrimary = true
                            ) {
                                Text(
                                    text = if (isRegisterMode) "Créer mon espace personnel" else "Se connecter",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Account Deletion Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text("Supprimer ce profil ?", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Text(
                    "Cette action supprimera définitivement vos listes, favoris et paramètres associés à ce compte utilisateur.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteAccount()
                    }
                ) {
                    Text("Supprimer définitivement", color = AppleCrimson, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Annuler", color = TextPrimary)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
