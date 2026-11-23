package pl.sekula.myvideofonlite.domain.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.sekula.myvideofonlite.domain.model.AuthState
import pl.sekula.myvideofonlite.domain.model.Capability
import pl.sekula.myvideofonlite.domain.model.DeviceStatus
import pl.sekula.myvideofonlite.domain.model.DoorbellEvent
import pl.sekula.myvideofonlite.domain.model.DomainError
import pl.sekula.myvideofonlite.domain.model.EventType
import pl.sekula.myvideofonlite.domain.model.Outcome
import pl.sekula.myvideofonlite.domain.repository.DoorbellRepository

class OpenGateTest {
    @Test
    fun `gate is not opened without user confirmation`() = runTest {
        val repo = FakeRepository()
        val result = OpenGate(repo)(userConfirmed = false)
        assertEquals(Outcome.Failure(DomainError.NOT_CONFIRMED), result)
        assertNull(repo.gateOpenedFor)
    }

    @Test
    fun `gate opens after confirmation when supported and online`() = runTest {
        val repo = FakeRepository()
        assertTrue(OpenGate(repo)(userConfirmed = true) is Outcome.Success)
        assertEquals("dev1", repo.gateOpenedFor)
    }

    @Test
    fun `gate unsupported when device has no unlock capability`() = runTest {
        val repo = FakeRepository(device(caps = setOf(Capability.LIVE_VIDEO)))
        assertEquals(Outcome.Failure(DomainError.UNSUPPORTED), OpenGate(repo)(true))
        assertNull(repo.gateOpenedFor)
    }

    @Test
    fun `gate refused when device status is unknown`() = runTest {
        val repo = FakeRepository(device(status = DeviceStatus.UNKNOWN))
        assertEquals(Outcome.Failure(DomainError.DEVICE_OFFLINE), OpenGate(repo)(true))
    }

    @Test
    fun `gate command is delegated to repository (mockk)`() = runTest {
        val repo = mockk<DoorbellRepository>()
        every { repo.device } returns flowOf(device())
        coEvery { repo.openGate("dev1") } returns Outcome.Success(Unit)
        OpenGate(repo)(true)
        coVerify(exactly = 1) { repo.openGate("dev1") }
    }
}

class ResolveHomeStateTest {
    @Test
    fun `no device means not configured`() {
        val s = ResolveHomeState(null, AuthState.SignedOut, emptyList(), null)
        assertEquals(HomeState.Kind.NOT_CONFIGURED, s.kind)
        assertFalse(s.canStartLiveView)
    }

    @Test
    fun `unknown status is never treated as online`() {
        val s = ResolveHomeState(device(DeviceStatus.UNKNOWN), AuthState.SignedIn("x"), emptyList(), null)
        assertEquals(HomeState.Kind.UNKNOWN, s.kind)
        assertFalse(s.canStartLiveView)
        assertFalse(s.canOpenGate)
    }

    @Test
    fun `expired auth wins over device status`() {
        val s = ResolveHomeState(device(), AuthState.Expired, emptyList(), null)
        assertEquals(HomeState.Kind.AUTH_ERROR, s.kind)
    }

    @Test
    fun `last ring is the newest ring of the selected device`() {
        val events = listOf(
            DoorbellEvent("dev1", EventType.RING, 100),
            DoorbellEvent("dev1", EventType.MOTION, 300),
            DoorbellEvent("dev1", EventType.RING, 200),
            DoorbellEvent("other", EventType.RING, 999),
        )
        val s = ResolveHomeState(device(), AuthState.SignedIn("x"), events, null)
        assertEquals(200L, s.lastRing?.timestampMillis)
        assertTrue(s.canStartLiveView)
        assertTrue(s.showGateButton)
    }

    @Test
    fun `gate button hidden when unsupported`() {
        val s = ResolveHomeState(device(caps = setOf(Capability.LIVE_VIDEO)), AuthState.SignedIn("x"), emptyList(), null)
        assertFalse(s.showGateButton)
    }
}

class StartLiveViewTest {
    @Test
    fun `offline device returns readable error`() = runTest {
        val repo = FakeRepository(device(DeviceStatus.OFFLINE))
        assertEquals(Outcome.Failure(DomainError.DEVICE_OFFLINE), StartLiveView(repo)())
    }

    @Test
    fun `connection failure is reported`() = runTest {
        val repo = FakeRepository().apply { sessionFails = true }
        val r = StartLiveView(repo)()
        assertTrue(r is Outcome.Failure && r.error == DomainError.CONNECTION)
    }

    @Test
    fun `successful session`() = runTest {
        assertTrue(StartLiveView(FakeRepository())() is Outcome.Success)
    }
}

class HandleDoorbellEventTest {
    @Test
    fun `ring is recorded and produces notification`() = runTest {
        val repo = FakeRepository()
        val n = HandleDoorbellEvent(repo)(DoorbellEvent("dev1", EventType.RING, 42))
        assertEquals(RingNotification("Wejście", 42), n)
        assertEquals(1, repo.events.value.size)
    }

    @Test
    fun `motion is recorded without notification`() = runTest {
        val repo = FakeRepository()
        assertNull(HandleDoorbellEvent(repo)(DoorbellEvent("dev1", EventType.MOTION, 42)))
        assertEquals(1, repo.events.value.size)
    }
}
