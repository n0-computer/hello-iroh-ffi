package computer.iroh.dot

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import computer.iroh.Endpoint
import computer.iroh.EndpointOptions
import computer.iroh.IrohAndroid
import computer.iroh.presetN0
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented usage example for [IrohAndroid.installAndroidContext].
 *
 * On Android, iroh's DNS resolver reaches the system DNS list via JNI
 * through the `ndk_context` crate. [Endpoint.bind] panics with
 * "android context was not initialized" unless the app has populated
 * `ndk_context` first by calling [IrohAndroid.installAndroidContext]
 * with an Android [android.content.Context]. The right place to do
 * that is in [android.app.Application.onCreate] (or any startup hook
 * that runs before the first [Endpoint] is constructed).
 *
 * This test wires the call as a real Android app would and then
 * confirms [Endpoint.bind] succeeds, which in turn proves that the
 * JNI context was installed and DNS resolution is reachable.
 */
@RunWith(AndroidJUnit4::class)
class IrohAndroidContextTest {

    @Test
    fun installContextLetsEndpointBind() = runBlocking {
        // Step 1: at app startup, hand iroh the process's Application
        // Context. Idempotent — safe to call more than once.
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        IrohAndroid.installAndroidContext(context)

        // Step 2: now Endpoint.bind() can read system DNS and start up.
        val ep = Endpoint.bind(EndpointOptions(preset = presetN0()))
        try {
            assertNotNull("endpoint id should be non-null after bind", ep.id())
            assertTrue("endpoint should have at least one bound socket", ep.boundSockets().isNotEmpty())
        } finally {
            ep.shutdown()
        }
    }
}
