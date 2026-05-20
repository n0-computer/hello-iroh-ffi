package computer.iroh.pong

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import computer.iroh.pong.ui.EndpointScreen
import computer.iroh.pong.ui.theme.IrohPongTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IrohPongTheme {
                EndpointScreen(viewModel = viewModel<MainViewModel>())
            }
        }
    }
}
