package org.resqmesh.app
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.resqmesh.app.data.PacketStore
import org.resqmesh.core.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class PacketStoreTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private val a="00000000-0000-0000-0000-000000000001"
    private val b="00000000-0000-0000-0000-000000000002"
    private val c="00000000-0000-0000-0000-000000000003"
    private fun withStore(block: (PacketStore) -> Unit) {
        val store=PacketStore(context,"test.db")
        try { block(store) } finally { store.close() }
    }
    @Before fun reset() { context.deleteDatabase("test.db") }
    @Test fun duplicatesAndAcknowledgementsSurviveReopen() {
        val p=Packet(originDeviceId=a,timestamp=1234,priority=Priority.CRITICAL,message="Help",peopleCount=3,injuredCount=1)
        withStore { s ->
            assertTrue(s.insert(p)); assertFalse(s.insert(p)); s.acknowledge(p.messageId,b)
        }
        withStore { s ->
            assertEquals(p,s.all().single()); assertFalse(s.insert(p))
            assertTrue(s.delivered(p.messageId,b)); assertFalse(s.delivered(p.messageId,c))
            s.acknowledge(p.messageId,b); assertEquals(1,s.deliveryCount())
        }
    }
    @Test fun relayPacketKeepsPathAcrossRestart() {
        val p=Packet(originDeviceId=a,timestamp=1234,priority=Priority.HIGH,message="Help",peopleCount=1,injuredCount=0)
        val received=Routing.receive(p,a,b)!!
        withStore { assertTrue(it.insert(received)) }
        withStore { s ->
            val onward=Routing.receive(s.all().single(),b,c)!!
            assertEquals(listOf(a,b,c),onward.path); assertEquals(2,onward.hopCount)
            s.acknowledge(p.messageId,c); assertEquals(1,s.relayCount(b))
        }
    }
}
