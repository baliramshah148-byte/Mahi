package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChatMessage
import com.example.model.MessageSender
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonRose
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatBubble(
    message: ChatMessage,
    onSpeakClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isMahi = message.sender == MessageSender.MAHI
    val context = LocalContext.current
    val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    val formattedTime = timeFormatter.format(Date(message.timestamp))

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = if (isMahi) Arrangement.Start else Arrangement.End
    ) {
        if (isMahi) {
            // Mahi Mini Avatar Badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(NeonRose, ElectricViolet)
                        )
                    )
            ) {
                Text(
                    text = message.mood.emoji,
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isMahi) Alignment.Start else Alignment.End,
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            // Header: Name & Mood tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isMahi) Arrangement.Start else Arrangement.End,
                modifier = Modifier.padding(bottom = 3.dp, start = 4.dp, end = 4.dp)
            ) {
                Text(
                    text = if (isMahi) "Mahi" else "You",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isMahi) NeonRose else MaterialTheme.colorScheme.secondary
                )

                if (isMahi) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = message.mood.glowColor.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, message.mood.glowColor.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = message.mood.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = message.mood.glowColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (message.isVoice) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Spoken by voice",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // Message Bubble Body
            val bubbleShape = if (isMahi) {
                RoundedCornerShape(
                    topStart = 4.dp,
                    topEnd = 18.dp,
                    bottomEnd = 18.dp,
                    bottomStart = 18.dp
                )
            } else {
                RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 4.dp,
                    bottomEnd = 18.dp,
                    bottomStart = 18.dp
                )
            }

            val backgroundModifier = if (isMahi) {
                Modifier.background(MaterialTheme.colorScheme.surfaceVariant, bubbleShape)
            } else {
                Modifier.background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    ),
                    bubbleShape
                )
            }

            Box(
                modifier = Modifier
                    .clip(bubbleShape)
                    .then(backgroundModifier)
                    .border(
                        width = 1.dp,
                        color = if (isMahi) {
                            message.mood.glowColor.copy(alpha = 0.35f)
                        } else {
                            Color.White.copy(alpha = 0.2f)
                        },
                        shape = bubbleShape
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag(if (isMahi) "mahi_message_bubble" else "user_message_bubble")
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 21.sp,
                        letterSpacing = 0.2.sp
                    ),
                    color = if (isMahi) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        Color.White
                    }
                )
            }

            // Actions row (TTS Speak & Copy)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isMahi) Arrangement.Start else Arrangement.End,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                if (isMahi) {
                    IconButton(
                        onClick = { onSpeakClick(message.text) },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("hear_again_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Hear Mahi speak again",
                            tint = NeonRose,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Mahi AI Message", message.text)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy message",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
