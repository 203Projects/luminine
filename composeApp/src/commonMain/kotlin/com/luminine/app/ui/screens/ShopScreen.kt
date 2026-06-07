package com.luminine.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luminine.app.ui.LuminineIcon
import com.luminine.app.ui.components.IconTile

private data class ShopProduct(val name: String, val price: String, val tag: String)

private val sampleProducts = listOf(
    ShopProduct("비타민C 1000 세럼", "₩38,000", "스킨케어"),
    ShopProduct("콜라겐 펩타이드", "₩45,000", "이너케어"),
    ShopProduct("오메가3 트리글리세라이드", "₩32,000", "영양제"),
    ShopProduct("저분자 단백질", "₩52,000", "식단"),
    ShopProduct("나이트 리커버리 크림", "₩41,000", "스킨케어"),
    ShopProduct("마그네슘 글리시네이트", "₩28,000", "수면"),
)

@Composable
fun ShopScreen(modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(sampleProducts) { product ->
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        IconTile(
                            LuminineIcon.Shop,
                            product.name,
                            size = 56.dp,
                            background = MaterialTheme.colorScheme.secondaryContainer,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    Text(product.tag, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(product.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Text(product.price, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
