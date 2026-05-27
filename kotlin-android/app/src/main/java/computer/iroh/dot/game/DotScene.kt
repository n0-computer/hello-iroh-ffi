package computer.iroh.dot.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.min

private val FieldShape = RoundedCornerShape(18.dp)

@Composable
fun DotScene(
    game: DotGame,
    myColor: Color,
    opponentColor: Color,
    onDrag: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clip(FieldShape)
            .background(Color(0x14000000), FieldShape)
            .pointerInput(Unit) {
                detectDragGestures { change, _ -> reportDrag(change.position, size, onDrag) }
            }
            .pointerInput(Unit) {
                detectTapGestures(onPress = { offset -> reportDrag(offset, size, onDrag) })
            },
    ) {
        drawDot(game.theirPos, opponentColor)
        drawDot(game.myPos, myColor)
    }
}

private fun reportDrag(
    position: Offset,
    size: androidx.compose.ui.unit.IntSize,
    onDrag: (Float, Float) -> Unit,
) {
    val nx = (position.x / size.width.toFloat()) * 2f - 1f
    val ny = (position.y / size.height.toFloat()) * 2f - 1f
    onDrag(nx.coerceIn(-1f, 1f), ny.coerceIn(-1f, 1f))
}

private fun DrawScope.drawDot(pos: Vec2, color: Color) {
    val radiusPx = DotGame.DOT_RADIUS * min(size.width, size.height)
    drawCircle(color = color, radius = radiusPx, center = fieldToCanvas(pos))
}

private fun DrawScope.fieldToCanvas(p: Vec2): Offset {
    val x = (p.x + 1f) * 0.5f * size.width
    val y = (p.y + 1f) * 0.5f * size.height
    return Offset(x, y)
}

object DotColors {
    fun forEndpointId(hex: String): Color {
        var hash = 0
        for (c in hex) hash = hash * 31 + c.code
        val hue = ((hash % 360) + 360) % 360
        val hsv = floatArrayOf(hue.toFloat(), 0.7f, 0.95f)
        return Color(android.graphics.Color.HSVToColor(hsv))
    }
}
