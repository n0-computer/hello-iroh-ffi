package computer.iroh.dot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import computer.iroh.dot.ui.EndpointScreen
import computer.iroh.dot.ui.theme.IrohDotTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IrohDotTheme {
                EndpointScreen(viewModel = viewModel<MainViewModel>())
            }
        }
    }
}
