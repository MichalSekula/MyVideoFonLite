package pl.sekula.myvideofonlite.data.demo

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.sekula.myvideofonlite.domain.model.DeviceStatus
import pl.sekula.myvideofonlite.domain.model.DomainError
import pl.sekula.myvideofonlite.domain.model.LiveViewState
import pl.sekula.myvideofonlite.domain.model.Outcome

class DemoBackendTest {
    @Test
    fun `demo device is online by default`() = runTest {
        val r = DemoBackend().fetchDevice(DemoBackend.DEVICE_ID)
        assertTrue(r is Outcome.Success && r.value.status == DeviceStatus.ONLINE)
    }

    @Test
    fun `simulated offline breaks live view with readable error`() = runTest {
        val backend = DemoBackend().apply { simulateOffline.value = true }
        val session = backend.createLiveSession(DemoBackend.DEVICE_ID)
        session.connect()
        assertEquals(LiveViewState.Error(DomainError.DEVICE_OFFLINE), session.state.value)
    }

    @Test
    fun `talking enables audio`() = runTest {
        val session = DemoBackend().createLiveSession(DemoBackend.DEVICE_ID)
        session.connect()
        session.setTalking(true)
        assertEquals(LiveViewState.Connected(audioEnabled = true, talking = true), session.state.value)
    }
}
