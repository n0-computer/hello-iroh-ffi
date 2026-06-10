package computer.iroh.dot

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import computer.iroh.IrohAndroid
import computer.iroh.dot.game.MotionSource
import computer.iroh.dot.identity.IdentityStore
import computer.iroh.dot.net.IrohPeer

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
