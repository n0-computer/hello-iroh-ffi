package computer.iroh.pong

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import computer.iroh.Endpoint
import computer.iroh.EndpointOptions
import computer.iroh.presetN0
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    sealed interface State {
        data object Binding : State
        data class Ready(val endpointId: String) : State
        data class Error(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Binding)
    val state: StateFlow<State> = _state.asStateFlow()

    private var endpoint: Endpoint? = null

    init {
        viewModelScope.launch {
            try {
                val ep = Endpoint.bind(EndpointOptions(preset = presetN0()))
                endpoint = ep
                _state.value = State.Ready(ep.id().toString())
            } catch (t: Throwable) {
                _state.value = State.Error(t.message ?: t.toString())
            }
        }
    }
}
