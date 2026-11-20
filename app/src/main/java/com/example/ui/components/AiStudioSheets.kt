package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ai.AiChatMessage
import com.example.ai.AiChatModel
import com.example.ai.AiImageSize
import com.example.ai.AiSystemRole
import com.example.ui.AiImageUiState
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioAzure
import com.example.ui.theme.StudioPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiImageGeneratorSheet(
    uiState: AiImageUiState,
    onGenerate: (String, AiImageSize) -> Unit,
    onAddToCanvas: (Bitmap, String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var prompt by remember { mutableStateOf("") }
    var selectedSize by remember { mutableStateOf(AiImageSize.SIZE_1K) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = StudioPurple)
                    Column {
                        Text("AI Image Studio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "Model: gemini-3-pro-image-preview",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            // Firebase Placeholder Notice
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = StudioAmber.copy(alpha = 0.14f)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = StudioAmber, modifier = Modifier.size(18.dp))
                    Text(
                        text = stringResource(R.string.ai_studio_notice),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Describe the image you want to create") },
                placeholder = { Text("e.g., Neon cyber city poster background with dramatic lighting") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_image_prompt_input")
            )

            Text("Output Image Size", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AiImageSize.entries.forEach { size ->
                    FilterChip(
                        selected = selectedSize == size,
                        onClick = { selectedSize = size },
                        label = { Text("${size.label} (${size.pixels}px)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_size_${size.label}")
                    )
                }
            }

            Button(
                onClick = { onGenerate(prompt, selectedSize) },
                enabled = uiState !is AiImageUiState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_generate_image_button")
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Generate ${selectedSize.label} Image")
            }

            when (uiState) {
                is AiImageUiState.Idle -> Unit
                is AiImageUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(8.dp))
                            Text("Synthesizing ${selectedSize.label} preview...")
                        }
                    }
                }
                is AiImageUiState.Success -> {
                    Image(
                        bitmap = uiState.result.bitmap.asImageBitmap(),
                        contentDescription = "Generated AI Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Text(
                        text = uiState.result.notice,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            onAddToCanvas(uiState.result.bitmap, prompt.ifBlank { "AI ${selectedSize.label}" })
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add to Canvas")
                    }
                }
                is AiImageUiState.Error -> {
                    Text(uiState.message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatAssistantSheet(
    messages: List<AiChatMessage>,
    isSending: Boolean,
    onSendMessage: (AiChatModel, AiSystemRole, String) -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedModel by remember { mutableStateOf(AiChatModel.HYDROGEN) }
    var selectedRole by remember { mutableStateOf(AiSystemRole.DESIGN_DIRECTOR) }
    var inputText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Gemini Creative Assistant", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(selectedModel.tagline, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row {
                    IconButton(onClick = onClearHistory) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear Chat")
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }
            }

            // Firebase notice
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = StudioAmber.copy(alpha = 0.14f)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = StudioAmber, modifier = Modifier.size(16.dp))
                    Text(stringResource(R.string.ai_studio_notice), style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Model Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AiChatModel.entries.forEach { model ->
                    FilterChip(
                        selected = selectedModel == model,
                        onClick = { selectedModel = model },
                        label = { Text(model.displayName) }
                    )
                }
            }

            // System Role Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AiSystemRole.entries.forEach { role ->
                    FilterChip(
                        selected = selectedRole == role,
                        onClick = { selectedRole = role },
                        label = { Text(role.title) }
                    )
                }
            }

            // Scrollable Conversation Thread
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Text(
                            text = "Ask ${selectedRole.title} for layout ideas, color palettes, video pacing tips, or typography pairing.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(messages, key = { it.id }) { msg ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (msg.isUser) StudioAzure else MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier.fillMaxWidth(0.88f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    if (!msg.isUser && msg.modelLabel != null) {
                                        Text(
                                            text = msg.modelLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = StudioAzure
                                        )
                                        Spacer(Modifier.height(2.dp))
                                    }
                                    Text(
                                        text = msg.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (msg.isUser) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask ${selectedRole.title}...") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_chat_input")
                )
                IconButton(
                    onClick = {
                        val txt = inputText
                        inputText = ""
                        onSendMessage(selectedModel, selectedRole, txt)
                    },
                    enabled = inputText.isNotBlank() && !isSending,
                    modifier = Modifier.testTag("ai_chat_send_button")
                ) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send message", tint = StudioAzure)
                    }
                }
            }
        }
    }
}
