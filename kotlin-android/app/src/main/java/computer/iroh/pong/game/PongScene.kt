package computer.iroh.pong.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

private val FieldShape = RoundedCornerShape(18.dp)

@Composable
fun PongScene(
    game: PongGame,
    myColor: Color,
    opponentColor: Color,
    onDrag: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val paddleHeightPx = with(density) { 12.dp.toPx() }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clip(FieldShape)
            .background(Color(0x14000000), FieldShape)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val nx = (change.position.x / size.width.toFloat()) * 2f - 1f
                    onDrag(nx.coerceIn(-1f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onPress = { offset ->
                    val nx = (offset.x / size.width.toFloat()) * 2f - 1f
                    onDrag(nx.coerceIn(-1f, 1f))
                })
            },
    ) {
        drawCenterLine()
        drawPaddle(
            x = game.opponentPaddlePredictedX,
            y = PongGame.OPPONENT_PADDLE_Y,
            color = opponentColor,
            paddleHeightPx = paddleHeightPx,
        )
        drawPaddle(
            x = game.myPaddleX,
            y = PongGame.MY_PADDLE_Y,
            color = myColor,
            paddleHeightPx = paddleHeightPx,
        )
        drawBall(game.ballPos)
        drawScores(
            mine = game.myScore,
            theirs = game.theirScore,
            myColor = myColor,
            opponentColor = opponentColor,
            measurer = measurer,
        )
    }
}

private fun DrawScope.drawCenterLine() {
    val midY = size.height / 2f
    val margin = 16f
    var x = margin
    while (x < size.width - margin) {
        val end = min(x + 16f, size.width - margin)
        drawLine(
            color = Color(0x59000000),
            start = Offset(x, midY),
            end = Offset(end, midY),
            strokeWidth = 2f,
            cap = StrokeCap.Round,
        )
        x += 28f
    }
}

private fun DrawScope.drawPaddle(x: Float, y: Float, color: Color, paddleHeightPx: Float) {
    val widthPx = PongGame.PADDLE_HALF_WIDTH * 2f * size.width
    val center = fieldToCanvas(Vec2(x, y))
    drawRoundRect(
        color = color,
        topLeft = Offset(center.x - widthPx / 2f, center.y - paddleHeightPx / 2f),
        size = Size(widthPx, paddleHeightPx),
        cornerRadius = CornerRadius(paddleHeightPx / 2f, paddleHeightPx / 2f),
    )
}

private fun DrawScope.drawBall(pos: Vec2) {
    val minDim = min(size.width, size.height)
    val radiusPx = PongGame.BALL_RADIUS * minDim
    val center = fieldToCanvas(pos)
    drawCircle(
        color = Color(0xDD111111),
        radius = radiusPx,
        center = center,
    )
}

private fun DrawScope.drawScores(
    mine: UShort,
    theirs: UShort,
    myColor: Color,
    opponentColor: Color,
    measurer: TextMeasurer,
) {
    val style = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
    )
    val theirText = measurer.measure(AnnotatedString(theirs.toString()), style.copy(color = opponentColor))
    val myText = measurer.measure(AnnotatedString(mine.toString()), style.copy(color = myColor))

    drawText(theirText, topLeft = Offset(18f, 10f))
    drawText(
        myText,
        topLeft = Offset(size.width - 18f - myText.size.width, size.height - 10f - myText.size.height),
    )
}

private fun DrawScope.fieldToCanvas(p: Vec2): Offset {
    val x = (p.x + 1f) * 0.5f * size.width
    val y = (p.y + 1f) * 0.5f * size.height
    return Offset(x, y)
}

object PongColors {
    fun forEndpointId(hex: String): Color {
        var hash = 0
        for (c in hex) hash = hash * 31 + c.code
        val hue = ((hash % 360) + 360) % 360
        val hsv = floatArrayOf(hue.toFloat(), 0.7f, 0.95f)
        return Color(android.graphics.Color.HSVToColor(hsv))
    }
}
