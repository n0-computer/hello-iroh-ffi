package computer.iroh.pong

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import computer.iroh.IrohAndroid
import computer.iroh.pong.game.MotionSource
import computer.iroh.pong.identity.IdentityStore
import computer.iroh.pong.net.IrohPeer

class MainViewModel(app: Application) : AndroidViewModel(app) {

    init {
        // iroh's DNS resolver needs ndk_context populated with the
        // process's JavaVM + Application context before any Endpoint is
        // constructed; idempotent on repeat calls.
        IrohAndroid.installAndroidContext(app)
    }

    val motion = MotionSource(app)
    private val identity = IdentityStore.loadOrCreate(app)
    val peer = IrohPeer(viewModelScope, motion, identity)

    init {
        motion.start()
        peer.start()
    }

    override fun onCleared() {
        super.onCleared()
        motion.stop()
    }
}
