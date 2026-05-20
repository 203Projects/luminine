package com.reverse.healthtracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reverse.healthtracker.ui.ReverseIcon

@Composable
fun ReverseIconView(
    icon: ReverseIcon,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val color = if (tint == Color.Unspecified) Color(0xFF4B3628) else tint
        val stroke = Stroke(width = 2.1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val thin = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height
        fun p(x: Float, y: Float) = Offset(w * x, h * y)

        when (icon) {
            ReverseIcon.Home -> {
                drawLine(color, p(.18f, .48f), p(.5f, .2f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, p(.5f, .2f), p(.82f, .48f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawRoundRect(color, p(.28f, .44f), Size(w * .44f, h * .38f), CornerRadius(4.dp.toPx()), style = stroke)
            }
            ReverseIcon.Chart -> {
                listOf(.72f, .5f, .28f).forEachIndexed { index, top ->
                    val x = .24f + index * .22f
                    drawRoundRect(color, p(x, top), Size(w * .12f, h * (.82f - top)), CornerRadius(3.dp.toPx()))
                }
                drawLine(color, p(.16f, .84f), p(.86f, .84f), strokeWidth = thin.width, cap = StrokeCap.Round)
            }
            ReverseIcon.Book -> {
                drawRoundRect(color, p(.2f, .18f), Size(w * .28f, h * .64f), CornerRadius(4.dp.toPx()), style = stroke)
                drawRoundRect(color, p(.52f, .18f), Size(w * .28f, h * .64f), CornerRadius(4.dp.toPx()), style = stroke)
                drawLine(color, p(.5f, .2f), p(.5f, .82f), strokeWidth = thin.width, cap = StrokeCap.Round)
            }
            ReverseIcon.Care -> {
                val path = Path().apply {
                    moveTo(w * .5f, h * .78f)
                    cubicTo(w * .1f, h * .48f, w * .22f, h * .18f, w * .43f, h * .33f)
                    cubicTo(w * .5f, h * .18f, w * .8f, h * .2f, w * .77f, h * .48f)
                    cubicTo(w * .75f, h * .6f, w * .62f, h * .68f, w * .5f, h * .78f)
                }
                drawPath(path, color, style = stroke)
            }
            ReverseIcon.Menu -> {
                drawLine(color, p(.2f, .3f), p(.8f, .3f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, p(.2f, .5f), p(.8f, .5f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, p(.2f, .7f), p(.8f, .7f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            ReverseIcon.Sparkles -> {
                drawLine(color, p(.5f, .16f), p(.5f, .58f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, p(.29f, .37f), p(.71f, .37f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawCircle(color, radius = 2.4.dp.toPx(), center = p(.76f, .74f))
                drawCircle(color, radius = 1.8.dp.toPx(), center = p(.24f, .72f))
            }
            ReverseIcon.Check -> {
                drawLine(color, p(.22f, .54f), p(.42f, .72f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, p(.42f, .72f), p(.8f, .28f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            ReverseIcon.Plus -> {
                drawLine(color, p(.5f, .2f), p(.5f, .8f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, p(.2f, .5f), p(.8f, .5f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            ReverseIcon.Trash -> {
                drawLine(color, p(.3f, .28f), p(.7f, .28f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawRoundRect(color, p(.34f, .34f), Size(w * .32f, h * .46f), CornerRadius(3.dp.toPx()), style = stroke)
                drawLine(color, p(.43f, .44f), p(.43f, .68f), strokeWidth = thin.width, cap = StrokeCap.Round)
                drawLine(color, p(.57f, .44f), p(.57f, .68f), strokeWidth = thin.width, cap = StrokeCap.Round)
            }
            ReverseIcon.Pill, ReverseIcon.Supplement -> {
                drawRoundRect(color, p(.23f, .38f), Size(w * .54f, h * .24f), CornerRadius(12.dp.toPx()), style = stroke)
                drawLine(color, p(.5f, .39f), p(.5f, .61f), strokeWidth = thin.width, cap = StrokeCap.Round)
                if (icon == ReverseIcon.Supplement) drawCircle(color, radius = 2.dp.toPx(), center = p(.72f, .28f))
            }
            ReverseIcon.Dumbbell -> {
                drawLine(color, p(.28f, .5f), p(.72f, .5f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, p(.2f, .34f), p(.2f, .66f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, p(.3f, .38f), p(.3f, .62f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, p(.7f, .38f), p(.7f, .62f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, p(.8f, .34f), p(.8f, .66f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            ReverseIcon.Plate -> {
                drawCircle(color, radius = w * .24f, center = p(.45f, .52f), style = stroke)
                drawCircle(color, radius = w * .14f, center = p(.45f, .52f), style = thin)
                drawLine(color, p(.76f, .28f), p(.76f, .78f), strokeWidth = thin.width, cap = StrokeCap.Round)
            }
            ReverseIcon.Drop, ReverseIcon.Skin -> {
                val path = Path().apply {
                    moveTo(w * .5f, h * .18f)
                    cubicTo(w * .25f, h * .48f, w * .28f, h * .78f, w * .5f, h * .82f)
                    cubicTo(w * .72f, h * .78f, w * .75f, h * .48f, w * .5f, h * .18f)
                }
                drawPath(path, color, style = stroke)
                if (icon == ReverseIcon.Skin) drawLine(color, p(.4f, .62f), p(.6f, .62f), strokeWidth = thin.width, cap = StrokeCap.Round)
            }
            ReverseIcon.Moon, ReverseIcon.Sleep -> {
                drawArc(color, startAngle = 80f, sweepAngle = 240f, useCenter = false, topLeft = p(.22f, .18f), size = Size(w * .55f, h * .62f), style = stroke)
                drawArc(color, startAngle = 90f, sweepAngle = 210f, useCenter = false, topLeft = p(.38f, .18f), size = Size(w * .46f, h * .58f), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            }
            ReverseIcon.Mind -> {
                drawCircle(color, radius = w * .19f, center = p(.5f, .32f), style = stroke)
                drawArc(color, 205f, 130f, false, p(.24f, .48f), Size(w * .52f, h * .36f), style = stroke)
                drawLine(color, p(.5f, .52f), p(.5f, .82f), strokeWidth = thin.width, cap = StrokeCap.Round)
            }
            ReverseIcon.Body -> {
                drawCircle(color, radius = w * .11f, center = p(.5f, .22f), style = stroke)
                drawRoundRect(color, p(.32f, .38f), Size(w * .36f, h * .42f), CornerRadius(18.dp.toPx()), style = stroke)
            }
            ReverseIcon.Camera -> {
                drawRoundRect(color, p(.2f, .32f), Size(w * .6f, h * .42f), CornerRadius(5.dp.toPx()), style = stroke)
                drawCircle(color, radius = w * .12f, center = p(.5f, .53f), style = stroke)
                drawLine(color, p(.34f, .32f), p(.4f, .24f), strokeWidth = thin.width, cap = StrokeCap.Round)
            }
            ReverseIcon.Report -> {
                drawRoundRect(color, p(.26f, .18f), Size(w * .48f, h * .64f), CornerRadius(4.dp.toPx()), style = stroke)
                drawLine(color, p(.36f, .38f), p(.64f, .38f), strokeWidth = thin.width, cap = StrokeCap.Round)
                drawLine(color, p(.36f, .52f), p(.62f, .52f), strokeWidth = thin.width, cap = StrokeCap.Round)
                drawLine(color, p(.36f, .66f), p(.54f, .66f), strokeWidth = thin.width, cap = StrokeCap.Round)
            }
            ReverseIcon.Shop -> {
                drawRoundRect(color, p(.24f, .4f), Size(w * .52f, h * .4f), CornerRadius(3.dp.toPx()), style = stroke)
                drawArc(color, 180f, 180f, false, p(.34f, .2f), Size(w * .32f, h * .36f), style = stroke)
            }
            ReverseIcon.Youtube, ReverseIcon.Play -> {
                drawRoundRect(color, p(.2f, .32f), Size(w * .6f, h * .38f), CornerRadius(8.dp.toPx()), style = stroke)
                val path = Path().apply {
                    moveTo(w * .46f, h * .42f)
                    lineTo(w * .46f, h * .6f)
                    lineTo(w * .62f, h * .51f)
                    close()
                }
                drawPath(path, color)
            }
            ReverseIcon.Cafe, ReverseIcon.Message -> {
                drawRoundRect(color, p(.2f, .26f), Size(w * .6f, h * .44f), CornerRadius(6.dp.toPx()), style = stroke)
                drawLine(color, p(.36f, .7f), p(.28f, .82f), strokeWidth = thin.width, cap = StrokeCap.Round)
                if (icon == ReverseIcon.Message) {
                    drawLine(color, p(.32f, .42f), p(.66f, .42f), strokeWidth = thin.width, cap = StrokeCap.Round)
                    drawLine(color, p(.32f, .54f), p(.58f, .54f), strokeWidth = thin.width, cap = StrokeCap.Round)
                }
            }
            ReverseIcon.Alert -> {
                val path = Path().apply {
                    moveTo(w * .5f, h * .18f)
                    lineTo(w * .82f, h * .78f)
                    lineTo(w * .18f, h * .78f)
                    close()
                }
                drawPath(path, color, style = stroke)
                drawLine(color, p(.5f, .38f), p(.5f, .58f), strokeWidth = thin.width, cap = StrokeCap.Round)
                drawCircle(color, radius = 1.8.dp.toPx(), center = p(.5f, .68f))
            }
            ReverseIcon.Admin, ReverseIcon.User -> {
                drawCircle(color, radius = w * .12f, center = p(.5f, .28f), style = stroke)
                drawArc(color, 200f, 140f, false, p(.26f, .42f), Size(w * .48f, h * .38f), style = stroke)
                if (icon == ReverseIcon.Admin) drawCircle(color, radius = w * .08f, center = p(.72f, .34f), style = thin)
            }
            ReverseIcon.Energy -> {
                val path = Path().apply {
                    moveTo(w * .58f, h * .14f)
                    lineTo(w * .34f, h * .52f)
                    lineTo(w * .54f, h * .52f)
                    lineTo(w * .42f, h * .86f)
                    lineTo(w * .72f, h * .44f)
                    lineTo(w * .52f, h * .44f)
                    close()
                }
                drawPath(path, color, style = stroke)
            }
            ReverseIcon.Link -> {
                drawArc(color, 120f, 260f, false, p(.2f, .28f), Size(w * .36f, h * .36f), style = stroke)
                drawArc(color, -60f, 260f, false, p(.44f, .36f), Size(w * .36f, h * .36f), style = stroke)
                drawLine(color, p(.42f, .54f), p(.58f, .46f), strokeWidth = thin.width, cap = StrokeCap.Round)
            }
            ReverseIcon.Trophy -> {
                drawRoundRect(color, p(.34f, .22f), Size(w * .32f, h * .32f), CornerRadius(4.dp.toPx()), style = stroke)
                drawArc(color, 90f, 180f, false, p(.2f, .26f), Size(w * .2f, h * .22f), style = thin)
                drawArc(color, -90f, 180f, false, p(.6f, .26f), Size(w * .2f, h * .22f), style = thin)
                drawLine(color, p(.5f, .54f), p(.5f, .72f), strokeWidth = thin.width, cap = StrokeCap.Round)
                drawLine(color, p(.36f, .78f), p(.64f, .78f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
        }
    }
}

@Composable
fun IconTile(
    icon: ReverseIcon,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    background: Color = Color(0xFFF1E7DA),
    tint: Color = Color(0xFF6B4F35),
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        ReverseIconView(
            icon = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.54f),
        )
    }
}
