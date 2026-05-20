package computer.iroh.pong

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import computer.iroh.pong.net.IrohPeer

class MainViewModel : ViewModel() {

    val peer = IrohPeer(viewModelScope)

    init {
        peer.start()
    }
}
