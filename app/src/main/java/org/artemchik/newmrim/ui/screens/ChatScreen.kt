package org.artemchik.newmrim.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import org.artemchik.newmrim.R
import org.artemchik.newmrim.protocol.MrimConstants
import org.artemchik.newmrim.protocol.data.AnketaInfo
import org.artemchik.newmrim.protocol.data.MessageInfo
import org.artemchik.newmrim.ui.theme.*
import org.artemchik.newmrim.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(onBack: () -> Unit, viewModel: ChatViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty())
            listState.animateScrollToItem(uiState.messages.size - 1)
    }

    if (uiState.anketa != null) {
        AnketaFullScreen(
            anketa = uiState.anketa!!,
            email = uiState.contactEmail,
            onDismiss = viewModel::closeAnketa
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (uiState.contactAvatarUrl.isNotEmpty()) {
                            AsyncImage(
                                model = uiState.contactAvatarUrl,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(
                                Modifier.size(40.dp), shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        uiState.contactName.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        Column {
                            Text(uiState.contactName, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            when {
                                uiState.isTyping -> Text(stringResource(R.string.typing),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary)
                                uiState.contactStatus == MrimConstants.STATUS_ONLINE ->
                                    Text(stringResource(R.string.status_online), style = MaterialTheme.typography.labelSmall, color = OnlineGreen)
                                uiState.contactStatus == MrimConstants.STATUS_AWAY ->
                                    Text(stringResource(R.string.status_away), style = MaterialTheme.typography.labelSmall, color = AwayOrange)
                                else -> Text(uiState.contactEmail,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    if (uiState.isAnketaLoading) {
                        CircularProgressIndicator(Modifier.size(24.dp).padding(4.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = viewModel::showAnketa) {
                            Icon(Icons.Default.AccountCircle, contentDescription = stringResource(R.string.anketa_title))
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                        .navigationBarsPadding().imePadding(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.inputText,
                        onValueChange = viewModel::onInputChanged,
                        placeholder = { Text(stringResource(R.string.message_placeholder)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 5
                    )
                    FilledIconButton(
                        onClick = viewModel::sendMessage,
                        enabled = uiState.inputText.isNotBlank() && !uiState.isSending,
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape
                    ) {
                        if (uiState.isSending) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.AutoMirrored.Filled.Send, null)
                    }
                }
            }
        }
    ) { padding ->
        if (uiState.messages.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Outlined.ChatBubbleOutline, null,
                        Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    Text(stringResource(R.string.empty_chat_message),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = 8.dp),
                state = listState,
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                var lastDate = ""
                items(uiState.messages, key = { "${it.msgId}_${it.timestamp}" }) { msg ->
                    // Заголовок даты
                    val msgDate = formatDate(msg.timestamp)
                    if (msgDate != lastDate) {
                        lastDate = msgDate
                        DateHeader(msgDate)
                    }
                    MessageBubble(msg)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnketaFullScreen(anketa: AnketaInfo, email: String, onDismiss: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.anketa_title)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val user = anketa.users.firstOrNull() ?: emptyMap()
                val nickname = user["Nickname"] ?: email
                val firstName = user["FirstName"] ?: ""
                val lastName = user["LastName"] ?: ""
                val fullName = "$firstName $lastName".trim().ifEmpty { nickname }
                
                Spacer(Modifier.height(24.dp))
                
                // Аватарка (крупная)
                val largeAvatarUrl = buildString {
                    val parts = email.split("@")
                    if (parts.size == 2) {
                        append("http://obraz.mrim.su/")
                        append(parts[1].substringBeforeLast("."))
                        append("/")
                        append(parts[0])
                        append("/_mrimavatar180")
                    }
                }
                
                AsyncImage(
                    model = largeAvatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(Modifier.height(16.dp))
                
                Text(nickname, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                Spacer(Modifier.height(24.dp))
                
                // Кнопки
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.profile_chat))
                    }
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(email))
                            Toast.makeText(context, "Email скопирован", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.profile_copy))
                    }
                }
                
                Spacer(Modifier.height(32.dp))
                
                // Секция: Информация
                ProfileSectionTitle(stringResource(R.string.profile_info_section))
                Card(
                    Modifier.padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column {
                        ProfileItem(Icons.Default.Person, fullName, stringResource(R.string.profile_full_name))
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 0.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        ProfileItem(Icons.Default.Email, email, stringResource(R.string.profile_email))
                    }
                }
                
                // Секция: Личное
                ProfileSectionTitle(stringResource(R.string.profile_personal_section))
                Card(
                    Modifier.padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column {
                        val birthday = user["Birthday"] ?: ""
                        if (birthday.isNotEmpty()) {
                            ProfileItem(Icons.Default.Cake, birthday, stringResource(R.string.profile_birthday))
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        
                        val sexRaw = user["Sex"] ?: ""
                        if (sexRaw.isNotEmpty()) {
                            val sexLabel = when(sexRaw) {
                                "1" -> stringResource(R.string.sex_male)
                                "2" -> stringResource(R.string.sex_female)
                                else -> sexRaw
                            }
                            ProfileItem(Icons.Default.Male, sexLabel, stringResource(R.string.profile_sex)) // Упростим иконку для примера
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        
                        val zodiac = user["Zodiac"] ?: ""
                        if (zodiac.isNotEmpty()) {
                            ProfileItem(Icons.Default.AutoAwesome, zodiac, stringResource(R.string.profile_zodiac))
                        }
                    }
                }
                
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ProfileSectionTitle(title: String) {
    Text(
        title,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun ProfileItem(icon: ImageVector, value: String, label: String) {
    ListItem(
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        headlineContent = { Text(value, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = { Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun DateHeader(date: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ) {
            Text(
                date,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MessageBubble(message: MessageInfo) {
    val isDark = isSystemInDarkTheme()
    val isOut = message.isOutgoing
    val bubbleColor = if (isOut) {
        if (isDark) BubbleOutgoingDark else BubbleOutgoing
    } else {
        if (isDark) BubbleIncomingDark else BubbleIncoming
    }
    val shape = RoundedCornerShape(
        topStart = 16.dp, topEnd = 16.dp,
        bottomStart = if (isOut) 16.dp else 4.dp,
        bottomEnd = if (isOut) 4.dp else 16.dp
    )

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOut) Alignment.End else Alignment.Start
    ) {
        if (message.isAuthRequest) {
            Text(stringResource(R.string.auth_request),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
        }

        Surface(
            Modifier.widthIn(max = 300.dp),
            shape = shape, color = bubbleColor,
            tonalElevation = if (isOut) 2.dp else 0.dp
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text(
                    message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    Modifier.align(Alignment.End).padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (message.isOffline) {
                        Icon(Icons.Filled.Schedule, null,
                            Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Text(
                        formatTime(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )

                    if (isOut) {
                        Icon(
                            if (message.isDelivered) Icons.Filled.DoneAll else Icons.Filled.Done,
                            null, Modifier.size(14.dp),
                            tint = if (message.isDelivered) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(ts: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))

@Composable
private fun formatDate(ts: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = ts }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    return when {
        cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
        cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> stringResource(R.string.date_today)

        cal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
        cal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> stringResource(R.string.date_yesterday)

        else -> SimpleDateFormat("d MMMM yyyy", Locale("ru")).format(Date(ts))
    }
}
