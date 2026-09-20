package org.resqmesh.core
import org.junit.Assert.*
import org.junit.Test
class PacketTest {
    private val a = "00000000-0000-0000-0000-000000000001"
    private val b = "00000000-0000-0000-0000-000000000002"
    private val c = "00000000-0000-0000-0000-000000000003"
    private fun packet() = Packet(originDeviceId=a, timestamp=1234, priority=Priority.CRITICAL,
        message="Trapped after building collapse — உதவி", peopleCount=3, injuredCount=1, latitude=10.8, longitude=78.7)
    @Test fun serializationRetainsEveryField() { val original=packet(); assertEquals(original, Codec.decode(Codec.encode(original))) }
    @Test fun relayKeepsOriginAndLocation() {
        val original=packet()
        val atB=Routing.receive(original,a,b)!!
        val atC=Routing.receive(atB,b,c)!!
        assertEquals(2,atC.hopCount); assertEquals(6,atC.ttl)
        assertEquals(listOf(a,b,c),atC.path); assertEquals(original.messageId,atC.messageId)
        assertEquals(original.latitude,atC.latitude); assertEquals(original.timestamp,atC.timestamp)
        assertNull(Routing.receive(atC,c,a))
    }
    @Test fun criticalFirst() {
        val p=packet()
        assertEquals(Priority.CRITICAL,listOf(p.copy(priority=Priority.NORMAL),p).sortedWith(Routing.priorityOrder).first().priority)
    }
    @Test fun ttlStopsAtEight() {
        var p=packet()
        for (i in 2..9) { p=Routing.receive(p,p.path.last(),"00000000-0000-0000-0000-"+i.toString().padStart(12,'0'))!! }
        assertEquals(0,p.ttl)
        assertFalse(Routing.canForward(p,c))
        assertNull(Routing.receive(p,p.path.last(),"00000000-0000-0000-0000-000000000010"))
    }
    @Test(expected=IllegalArgumentException::class) fun invalidCoordinatesRejected() { packet().copy(latitude=91.0).validate() }
    @Test(expected=IllegalArgumentException::class) fun oversizedRejected() { Codec.decode(ByteArray(9000)) }
    @Test fun wrongSenderRejected() { assertNull(Routing.receive(packet(),b,c)) }
    @Test fun neverForwardBackToOrigin() { assertFalse(Routing.canForward(Routing.receive(packet(),a,b)!!,a)) }
    @Test(expected=IllegalArgumentException::class) fun malformedPathRejected() { packet().copy(path=listOf(b)).validate() }
    @Test(expected=IllegalArgumentException::class) fun mismatchedTtlRejected() { packet().copy(ttl=7).validate() }
    @Test(expected=IllegalArgumentException::class) fun tooManyInjuredRejected() { packet().copy(injuredCount=4).validate() }
    @Test fun missingLocationAccepted() { assertNull(Codec.decode(Codec.encode(packet().copy(latitude=null,longitude=null))).latitude) }
    @Test fun samePriorityNewestFirst() {
        val old=packet(); val fresh=old.copy(messageId=java.util.UUID.randomUUID().toString(),timestamp=old.timestamp+1)
        assertEquals(fresh,listOf(old,fresh).sortedWith(Routing.priorityOrder).first())
    }
}
