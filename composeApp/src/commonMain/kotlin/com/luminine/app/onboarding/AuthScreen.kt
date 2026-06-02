package com.luminine.app.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luminine.app.ui.LuminineIcon
import com.luminine.app.ui.components.IconTile
import com.luminine.app.ui.components.LuminineIconView
import com.luminine.app.ui.theme.ReverseEspresso
import com.luminine.app.ui.theme.ReverseGold
import com.luminine.app.ui.theme.ReverseIvory

// Soft 20dp rounded surfaces, matching the rest of the app.
private val CardShape = RoundedCornerShape(20.dp)

// KakaoTalk brand yellow + its dark label color (from the mockup's kakao CTA).
private val KakaoYellow = Color(0xFFFEE500)
private val KakaoLabel = Color(0xFF3C1E1E)

@Composable
fun AuthScreen(
    modifier: Modifier = Modifier,
    onKakaoLogin: () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize().background(ReverseIvory).safeDrawingPadding().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.widthIn(max = 360.dp),
        ) {
            IconTile(
                icon = LuminineIcon.Sparkles,
                contentDescription = "LUMÍNINE",
                size = 64.dp,
                background = ReverseEspresso,
                tint = Color.White,
            )
            Text(
                "LUMÍNINE",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = ReverseEspresso,
            )
            Text(
                "되돌리는 안티에이징 루틴",
                style = MaterialTheme.typography.bodyMedium,
                color = ReverseGold,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onKakaoLogin,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = CardShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = KakaoYellow,
                    contentColor = KakaoLabel,
                ),
            ) {
                LuminineIconView(
                    icon = LuminineIcon.Message,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = KakaoLabel,
                )
                Spacer(Modifier.width(8.dp))
                Text("카카오로 시작하기", fontWeight = FontWeight.Bold)
            }
            Text(
                "가입 시 맞춤 건강 설문이 이어집니다 · 약 5분",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
