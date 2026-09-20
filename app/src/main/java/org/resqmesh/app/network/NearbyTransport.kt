package org.resqmesh.app.network

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import org.resqmesh.core.Packet

data class Peer(val endpoint: String, val deviceId: String, val state: String = "Nearby", val code: String? = null)

@SuppressLint("MissingPermission")
class NearbyTransport(context: Context, private val self: String, private val listener: Listener) {
    interface Listener {
        fun onPeers(peers: List<Peer>)
        fun onState(state: String, active: Boolean)
        fun onConnected(endpoint: String, deviceId: String)
        fun onDisconnected(endpoint: String)
        fun onBytes(endpoint: String, bytes: ByteArray)
        fun onError(message: String)
    }
    private val app=context.applicationContext
    private val client=Nearby.getConnectionsClient(app)
    private val peers=linkedMapOf<String,Peer>()
    private var running=false
    private var generation=0
    private var advertising=false
    private var discovering=false
    private val service="org.resqmesh.app.offline.v1"
    private fun emit()=listener.onPeers(peers.values.toList())
    private fun error(prefix: String, e: Exception)=listener.onError("$prefix: ${e.message ?: e.javaClass.simpleName}")
    private fun publishState() {
        if(running) listener.onState(if(advertising && discovering) "OFFLINE MESH ACTIVE" else "Starting nearby radios…",advertising && discovering)
    }
    fun start() {
        if(running) return
        if(GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(app)!=ConnectionResult.SUCCESS) {
            listener.onState("Google Play services unavailable or needs update",false); return
        }
        running=true
        val token=++generation
        publishState()
        client.startAdvertising(self,service,lifecycle,AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build())
            .addOnSuccessListener { if(running && generation==token) { advertising=true; publishState() } }
            .addOnFailureListener { if(generation==token) { stop(); listener.onState("Could not advertise. Check permissions and radios.",false); error("Advertising",it) } }
        client.startDiscovery(service,discovery,DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build())
            .addOnSuccessListener { if(running && generation==token) { discovering=true; publishState() } }
            .addOnFailureListener { if(generation==token) { stop(); listener.onState("Could not discover. Check permissions and radios.",false); error("Discovery",it) } }
    }
    fun stop() {
        if(!running) {
            listener.onState("Mesh paused • messages saved",false)
            return
        }
        running=false; generation++; advertising=false; discovering=false
        client.stopDiscovery(); client.stopAdvertising(); client.stopAllEndpoints()
        peers.keys.toList().forEach(listener::onDisconnected)
        peers.clear(); emit(); listener.onState("Mesh paused • messages saved",false)
    }
    fun connect(endpoint: String) {
        val peer=peers[endpoint] ?: return
        if(peer.state!="Nearby") return
        peers[endpoint]=peer.copy(state="Connecting"); emit()
        client.requestConnection(self,endpoint,lifecycle).addOnFailureListener {
            peers[endpoint]?.let { p -> peers[endpoint]=p.copy(state="Nearby",code=null); emit() }
            error("Connection request",it)
        }
    }
    fun confirm(endpoint: String, accept: Boolean) {
        if(accept) {
            peers[endpoint]?.let { peers[endpoint]=it.copy(state="Waiting for peer",code=null); emit() }
            client.acceptConnection(endpoint,payloads).addOnFailureListener { error("Accept connection",it); disconnect(endpoint) }
        } else {
            client.rejectConnection(endpoint)
            peers.remove(endpoint); emit()
        }
    }
    fun disconnect(endpoint: String) {
        client.disconnectFromEndpoint(endpoint)
        peers.remove(endpoint); listener.onDisconnected(endpoint); emit()
    }
    fun send(endpoint: String, bytes: ByteArray, failed: () -> Unit = {}) {
        if(peers[endpoint]?.state!="Connected") { failed(); return }
        client.sendPayload(endpoint,Payload.fromBytes(bytes)).addOnFailureListener {
            error("Send failed; packet remains saved",it); failed()
        }
    }
    private val discovery=object: EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            if(!running || !Packet.isUuid(info.endpointName) || info.endpointName==self) return
            if(!peers.containsKey(endpointId)) peers[endpointId]=Peer(endpointId,info.endpointName)
            emit()
        }
        override fun onEndpointLost(endpointId: String) {
            if(peers[endpointId]?.state=="Nearby") { peers.remove(endpointId); emit() }
        }
    }
    private val lifecycle=object: ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            if(!running || !Packet.isUuid(info.endpointName) || info.endpointName==self) {
                client.rejectConnection(endpointId); return
            }
            peers[endpointId]=Peer(endpointId,info.endpointName,"Verify code",info.authenticationDigits)
            emit()
        }
        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            if(!running) return
            val peer=peers[endpointId] ?: return
            if(resolution.status.isSuccess) {
                peers[endpointId]=peer.copy(state="Connected",code=null); emit()
                listener.onConnected(endpointId,peer.deviceId)
            } else {
                peers[endpointId]=peer.copy(state="Nearby",code=null); emit()
                listener.onError("Connection not established (${resolution.status.statusCode}). Tap Connect to retry.")
            }
        }
        override fun onDisconnected(endpointId: String) {
            peers.remove(endpointId); listener.onDisconnected(endpointId); emit()
        }
    }
    private val payloads=object: PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if(running && peers[endpointId]?.state=="Connected") payload.asBytes()?.let { listener.onBytes(endpointId,it) }
        }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            if(update.status==PayloadTransferUpdate.Status.FAILURE) listener.onError("Radio transfer interrupted; waiting to retry.")
        }
    }
}
