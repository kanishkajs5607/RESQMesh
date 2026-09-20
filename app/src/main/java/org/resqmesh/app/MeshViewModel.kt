package org.resqmesh.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.resqmesh.app.data.PacketStore
import org.resqmesh.app.network.NearbyTransport
import org.resqmesh.app.network.Peer
import org.resqmesh.core.*
import java.util.UUID

data class MeshState(
    val deviceId: String="", val mode: String="CITIZEN", val active: Boolean=false,
    val network: String="Mesh paused • messages saved", val peers: List<Peer> = emptyList(),
    val packets: List<Packet> = emptyList(), val relayed: Int=0, val confirmations: Int=0,
    val events: List<String> = emptyList(), val notice: String?=null
)
class MeshViewModel(app: Application): AndroidViewModel(app), NearbyTransport.Listener {
    private val prefs=app.getSharedPreferences("identity",0)
    val deviceId: String=prefs.getString("deviceId",null) ?: UUID.randomUUID().toString().also {
        check(prefs.edit().putString("deviceId",it).commit())
    }
    private val store=PacketStore(app)
    private val mutable=MutableStateFlow(MeshState(deviceId=deviceId,mode=prefs.getString("mode","CITIZEN")!!))
    val state=mutable.asStateFlow()
    private val transport=NearbyTransport(app,deviceId,this)
    private val connected=mutableMapOf<String,String>()
    private val inFlight=mutableMapOf<Pair<String,String>,Long>()
    private val attempts=mutableMapOf<Pair<String,String>,Int>()
    // All controller state is main-thread confined. SQLite work uses IO.
    init {
        viewModelScope.launch { refresh() }
        viewModelScope.launch {
            while(isActive) { delay(5000); connected.keys.toList().forEach { flush(it) } }
        }
    }
    private fun event(text: String) {
        mutable.value=mutable.value.copy(events=(listOf(text)+mutable.value.events).take(12))
    }
    private suspend fun refresh() {
        val snapshot=withContext(Dispatchers.IO) { Triple(store.all(),store.relayCount(deviceId),store.deliveryCount()) }
        mutable.value=mutable.value.copy(packets=snapshot.first,relayed=snapshot.second,confirmations=snapshot.third)
    }
    fun mode(value: String) { prefs.edit().putString("mode",value).apply(); mutable.value=mutable.value.copy(mode=value) }
    fun start()=transport.start()
    fun stop()=transport.stop()
    fun connect(endpoint: String)=transport.connect(endpoint)
    fun confirm(endpoint: String,accept: Boolean)=transport.confirm(endpoint,accept)
    fun disconnect(endpoint: String)=transport.disconnect(endpoint)
    fun notice(message: String?) { mutable.value=mutable.value.copy(notice=message) }
    fun create(packet: Packet, done: (Boolean) -> Unit) {
        viewModelScope.launch {
            runCatching {
                require(packet.originDeviceId==deviceId)
                withContext(Dispatchers.IO) { store.insert(packet.validate()) }
                refresh(); event("SOS saved on this phone • ${packet.messageId.take(8)}")
                connected.keys.toList().forEach { flush(it) }
                done(true)
            }.onFailure { notice("Could not save SOS: ${it.message}"); done(false) }
        }
    }
    override fun onPeers(peers: List<Peer>) { mutable.value=mutable.value.copy(peers=peers) }
    override fun onState(state: String,active: Boolean) { mutable.value=mutable.value.copy(network=state,active=active) }
    override fun onError(message: String) { event(message); notice(message) }
    override fun onConnected(endpoint: String,deviceId: String) {
        connected[endpoint]=deviceId; event("Connected to ${deviceId.take(8)}")
        viewModelScope.launch { flush(endpoint) }
    }
    override fun onDisconnected(endpoint: String) {
        connected.remove(endpoint)
        inFlight.keys.removeAll { it.first==endpoint }; attempts.keys.removeAll { it.first==endpoint }
    }
    private suspend fun flush(endpoint: String) {
        val peer=connected[endpoint] ?: return
        val packets=withContext(Dispatchers.IO) {
            store.all().filter { Routing.canForward(it,peer) && !store.delivered(it.messageId,peer) }
        }
        if(connected[endpoint]!=peer) return
        // One outstanding packet per peer. Priority ordering controls the next send.
        val now=System.currentTimeMillis()
        val pending=inFlight.entries.firstOrNull { it.key.first==endpoint }
        if(pending!=null) {
            if(now-pending.value<15000) return
            inFlight.remove(pending.key)
            if((attempts[pending.key] ?: 0)>=3) event("No storage confirmation from ${peer.take(8)}. Reconnect to retry; SOS remains saved.")
        }
        val p=packets.firstOrNull { (attempts[endpoint to it.messageId] ?: 0)<3 } ?: return
        val key=endpoint to p.messageId
        inFlight[key]=now; attempts[key]=(attempts[key] ?: 0)+1
        transport.send(endpoint,byteArrayOf(1)+Codec.encode(p)) { inFlight.remove(key) }
    }
    override fun onBytes(endpoint: String,bytes: ByteArray) {
        val peer=connected[endpoint] ?: return
        if(bytes.size !in 2..(Codec.MAX_BYTES+1)) return
        viewModelScope.launch {
            runCatching {
                when(bytes[0].toInt()) {
                    1 -> {
                        val packet=Codec.decode(bytes.copyOfRange(1,bytes.size))
                        val received=Routing.receive(packet,peer,deviceId) ?: return@launch
                        val inserted=withContext(Dispatchers.IO) { store.insert(received) }
                        // INSERT OR IGNORE completed: both a new packet and a duplicate are durable.
                        if(connected[endpoint]==peer) transport.send(endpoint,byteArrayOf(2)+packet.messageId.toByteArray(Charsets.UTF_8))
                        if(inserted) {
                            refresh(); event("Emergency packet received • hop ${received.hopCount} • saved")
                            connected.keys.toList().forEach { flush(it) }
                        }
                    }
                    2 -> {
                        val id=bytes.copyOfRange(1,bytes.size).toString(Charsets.UTF_8)
                        if(!Packet.isUuid(id)) return@launch
                        val key=endpoint to id
                        if(!inFlight.containsKey(key) && !attempts.containsKey(key)) return@launch
                        withContext(Dispatchers.IO) { store.acknowledge(id,peer) }
                        inFlight.remove(key)
                        refresh(); event("Stored by ${peer.take(8)} • ${id.take(8)}")
                        flush(endpoint)
                    }
                }
            }.onFailure { event("Packet rejected or storage failed: ${it.message}") }
        }
    }
    override fun onCleared() { transport.stop(); store.close(); super.onCleared() }
}
