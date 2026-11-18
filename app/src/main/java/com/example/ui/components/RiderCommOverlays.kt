package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CallUiSessionState
import com.example.model.DeliveryRider
import com.example.model.StoreOrder
import com.example.model.TripChatMessage
import com.example.util.RiderCommPolicy
import com.example.util.StoreUtils
import java.util.Locale

@Composable
fun IncomingRiderCallPopup(
    callState: CallUiSessionState,
    onAnswer: () -> Unit,
    onDecline: () -> Unit
) {
    if (!callState.isIncoming || !callState.isActiveOrRinging) return

    val pulse = rememberInfiniteTransition(label = "call_popup_pulse")
    val scale by pulse.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "popup_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(24.dp)
            .testTag("incoming_rider_call_popup"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(2.dp, Color(0xFF22C55E)),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                        contentDescription = null,
                        tint = Color(0xFF22C55E),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Incoming Voice Call",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF22C55E),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = callState.riderName.ifBlank { "Delivery Rider" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Order #${callState.orderId.takeLast(6)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onDecline,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("decline_incoming_call_button")
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Decline", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = onAnswer,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("answer_incoming_call_button")
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Answer", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveRiderCallBarAndModal(
    callState: CallUiSessionState,
    onMuteToggle: () -> Unit,
    onHoldToggle: () -> Unit,
    onSpeakerToggle: (Boolean) -> Unit,
    onEndCall: () -> Unit
) {
    if (!callState.isActiveOrRinging) return

    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_rider_call_bar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = Color(0xFF22C55E),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = callState.riderName.ifBlank { "Delivery Rider" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = when (callState.status) {
                                "RINGING" -> "Ringing..."
                                "CONNECTING" -> "Connecting WebRTC..."
                                "CONNECTED" -> "Connected • ${RiderCommPolicy.formatCallDuration(callState.durationSeconds)}"
                                else -> callState.status
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (callState.status == "CONNECTED") Color(0xFF4ADE80) else Color(0xFF94A3B8)
                        )
                    }
                }

                Button(
                    onClick = onEndCall,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("end_active_call_button")
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("End", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            // Controls row: Mute, Hold, Speaker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onMuteToggle,
                    border = BorderStroke(1.dp, if (callState.isMuted) Color(0xFFEF4444) else Color(0xFF475569)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("call_mute_toggle_button")
                ) {
                    Icon(
                        imageVector = if (callState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = null,
                        tint = if (callState.isMuted) Color(0xFFEF4444) else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (callState.isMuted) "Muted" else "Mute", color = Color.White, style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = onHoldToggle,
                    border = BorderStroke(1.dp, if (callState.isOnHold) Color(0xFFF59E0B) else Color(0xFF475569)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("call_hold_toggle_button")
                ) {
                    Icon(
                        imageVector = if (callState.isOnHold) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null,
                        tint = if (callState.isOnHold) Color(0xFFF59E0B) else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (callState.isOnHold) "On Hold" else "Hold", color = Color.White, style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = { onSpeakerToggle(!callState.isSpeakerOn) },
                    border = BorderStroke(1.dp, if (callState.isSpeakerOn) Color(0xFF22C55E) else Color(0xFF475569)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("call_speaker_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = if (callState.isSpeakerOn) Color(0xFF22C55E) else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (callState.isSpeakerOn) "Speaker" else "Earpiece", color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun RiderOrderChatSheet(
    order: StoreOrder,
    rider: DeliveryRider?,
    messages: List<TripChatMessage>,
    failedMessages: List<TripChatMessage>,
    onClose: () -> Unit,
    onSendMessage: (String) -> Unit,
    onRetryMessage: (TripChatMessage) -> Unit,
    onCallRider: () -> Unit
) {
    BackHandler { onClose() }

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val isTerminated = RiderCommPolicy.isTerminalOrderStage(order.status)

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxSize()
            .testTag("rider_order_chat_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
        ) {
            // Chat Header
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.testTag("chat_close_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val rName = rider?.name
                                val displayName = if (!rName.isNullOrBlank()) rName else "Assigned Rider"
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (rider?.isReliablyOnline() == true) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF22C55E))
                                    )
                                }
                            }
                            Text(
                                text = "Order #${order.orderId.takeLast(6)} • ${order.status.lowercase(Locale.US)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!isTerminated && rider != null && order.deliveryUid.isNotBlank()) {
                        Button(
                            onClick = onCallRider,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("chat_header_call_button")
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Call", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // Quick Chat Action Chips
            if (!isTerminated) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onSendMessage("Order Ready for Pickup") },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("quick_chat_ready_pickup")
                        ) {
                            Text("⚡ Order Ready for Pickup", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = { onSendMessage("Please contact restaurant staff.") },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("quick_chat_contact_staff")
                        ) {
                            Text("📞 Contact Restaurant", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Messages List Area
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No messages yet. Send a message to coordinate with the rider.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(messages, key = { it.messageId }) { msg ->
                        val isRestaurant = msg.senderRole.equals("RESTAURANT", ignoreCase = true)
                        val isFailed = failedMessages.any { it.messageId == msg.messageId }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isRestaurant) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                color = if (isRestaurant) Color(0xFF22C55E).copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(
                                    topStart = 12.dp,
                                    topEnd = 12.dp,
                                    bottomStart = if (isRestaurant) 12.dp else 2.dp,
                                    bottomEnd = if (isRestaurant) 2.dp else 12.dp
                                ),
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = msg.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(
                                        modifier = Modifier.align(Alignment.End),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = StoreUtils.formatTime(msg.timestamp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (isRestaurant) {
                                            if (isFailed) {
                                                IconButton(
                                                    onClick = { onRetryMessage(msg) },
                                                    modifier = Modifier.size(16.dp)
                                                ) {
                                                    Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = Color(0xFFEF4444))
                                                }
                                            } else {
                                                Icon(
                                                    imageVector = when (msg.status.uppercase(Locale.US)) {
                                                        "READ" -> Icons.Default.DoneAll
                                                        "DELIVERED" -> Icons.Default.DoneAll
                                                        else -> Icons.Default.Check
                                                    },
                                                    contentDescription = null,
                                                    tint = if (msg.status.uppercase(Locale.US) == "READ") Color(0xFF3B82F6) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Input Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isTerminated) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chat closed for completed/cancelled orders.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text("Message rider...") },
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 3,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_message_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF22C55E),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        IconButton(
                            onClick = {
                                val trimmed = textInput.trim()
                                if (trimmed.isNotEmpty()) {
                                    onSendMessage(trimmed)
                                    textInput = ""
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF22C55E))
                                .testTag("chat_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
