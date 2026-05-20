package computer.iroh.pong

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import computer.iroh.pong.game.MotionSource
import computer.iroh.pong.net.IrohPeer

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val motion = MotionSource(app)
    val peer = IrohPeer(viewModelScope, motion)

    init {
        motion.start()
        peer.start()
    }

    override fun onCleared() {
        super.onCleared()
        motion.stop()
    }
}
