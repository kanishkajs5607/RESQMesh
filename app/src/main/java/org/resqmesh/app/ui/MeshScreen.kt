package org.resqmesh.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import org.resqmesh.app.*
import org.resqmesh.app.location.LocationReader
import org.resqmesh.core.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink=Color(0xFF0B1019)
private val Panel=Color(0xFF152031)
private val Coral=Color(0xFFFF746B)
private val Mint=Color(0xFF66DAB2)
private val Muted=Color(0xFFA6B5CC)
private fun time(value: Long)=SimpleDateFormat("dd MMM, HH:mm:ss",Locale.getDefault()).format(Date(value))

@Composable fun ResqMeshApp(vm: MeshViewModel) {
    val state by vm.state.collectAsState()
    var create by rememberSaveable { mutableStateOf(false) }
    val context=LocalContext.current
    val required=remember {
        buildList {
            if(Build.VERSION.SDK_INT>=31) addAll(listOf(Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_ADVERTISE,Manifest.permission.BLUETOOTH_CONNECT))
            if(Build.VERSION.SDK_INT>=33) add(Manifest.permission.NEARBY_WIFI_DEVICES)
            if(Build.VERSION.SDK_INT<=32) addAll(listOf(Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }
    fun granted()=required.all { ContextCompat.checkSelfPermission(context,it)==PackageManager.PERMISSION_GRANTED }
    val permissions=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if(granted()) vm.start() else vm.notice("Nearby permissions are needed to connect. Allow them in App settings, then tap Start mesh.")
    }
    MaterialTheme(colorScheme=darkColorScheme(primary=Coral,secondary=Mint,background=Ink,surface=Panel,onSurface=Color(0xFFF1F5FC))) {
        Surface(Modifier.fillMaxSize(),color=Ink) {
            if(create) SosForm(state.deviceId,{ create=false },{ packet, completed -> vm.create(packet) { ok -> completed(ok); if(ok) create=false } })
            else LazyColumn(Modifier.fillMaxSize().systemBarsPadding(),contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                item {
                    Text("RESQMesh",fontSize=32.sp,fontWeight=FontWeight.ExtraBold,letterSpacing=(-1).sp)
                    Text("Communication when networks fail.",color=Muted,fontSize=14.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("THIS PHONE  •  ${state.deviceId.take(8).uppercase()}",color=Mint,fontSize=12.sp,letterSpacing=1.sp)
                }
                item {
                    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        listOf("CITIZEN","RESCUER").forEach { mode ->
                            FilterChip(selected=state.mode==mode,onClick={ vm.mode(mode) },label={ Text("$mode MODE") })
                        }
                    }
                }
                item {
                    Block {
                        Text(state.network,color=if(state.active) Mint else Muted,fontWeight=FontWeight.Bold,fontSize=13.sp)
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                            Metric(state.peers.count { it.state=="Connected" }.toString(),"Connected")
                            Metric(state.packets.size.toString(),"Saved SOS")
                            Metric(state.relayed.toString(),"Relayed")
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(onClick={
                            if(state.active || state.network.startsWith("Starting")) vm.stop()
                            else if(granted()) vm.start() else permissions.launch(required.toTypedArray())
                        },modifier=Modifier.fillMaxWidth().heightIn(min=50.dp)) {
                            Text(if(state.active || state.network.startsWith("Starting")) "Pause mesh" else "Start mesh")
                        }
                        Text("Keep this app open while relaying. Bluetooth and Wi-Fi must be on; internet is not needed.",color=Muted,fontSize=12.sp)
                        TextButton(onClick={ context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS)) }) { Text("Radio settings") }
                    }
                }
                item {
                    if(state.mode=="CITIZEN") {
                        Button(onClick={ create=true },modifier=Modifier.fillMaxWidth().height(64.dp),
                            colors=ButtonDefaults.buttonColors(containerColor=Coral,contentColor=Ink),shape=RoundedCornerShape(18.dp)) {
                            Text("+  CREATE SOS",fontSize=19.sp,fontWeight=FontWeight.ExtraBold)
                        }
                    } else {
                        Text("Rescue inbox",fontSize=26.sp,fontWeight=FontWeight.Bold)
                        Text("Critical emergencies appear first.",color=Muted)
                    }
                }
                item {
                    Text("NEARBY NODES  •  ${state.peers.size}",color=Muted,fontSize=12.sp,letterSpacing=1.sp)
                    if(state.peers.isEmpty()) Text("No devices discovered. Start mesh on a second phone.",modifier=Modifier.padding(top=8.dp),fontSize=14.sp)
                }
                items(state.peers,key={ it.endpoint }) { peer ->
                    Block {
                        Text("Node ${peer.deviceId.take(8).uppercase()}",fontWeight=FontWeight.Bold)
                        Text(peer.state,color=if(peer.state=="Connected") Mint else Muted)
                        if(peer.state=="Nearby") {
                            OutlinedButton(onClick={ vm.connect(peer.endpoint) }) { Text("Connect") }
                        } else if(peer.code!=null) {
                            Text("Compare this code on both phones",color=Muted,fontSize=13.sp)
                            Text(peer.code,fontSize=32.sp,fontWeight=FontWeight.Bold,letterSpacing=5.sp)
                            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                                Button(onClick={ vm.confirm(peer.endpoint,true) }) { Text("Codes match") }
                                TextButton(onClick={ vm.confirm(peer.endpoint,false) }) { Text("Reject") }
                            }
                        } else {
                            TextButton(onClick={ vm.disconnect(peer.endpoint) }) { Text("Disconnect") }
                        }
                    }
                }
                item {
                    Text(if(state.mode=="CITIZEN") "SAVED EMERGENCIES" else "RECEIVED & LOCAL SOS",color=Muted,fontSize=12.sp,letterSpacing=1.sp)
                    if(state.packets.isEmpty()) Block {
                        Text("Ready to help.",fontSize=22.sp,fontWeight=FontWeight.Bold)
                        Text("No SOS packets saved yet. Received messages stay on this phone, even after a restart.",color=Muted)
                    }
                }
                items(state.packets,key={ it.messageId }) { p -> EmergencyCard(p,state.deviceId) }
                item {
                    if(state.events.isNotEmpty()) Block {
                        Text("LIVE ACTIVITY",fontSize=12.sp,color=Mint,letterSpacing=1.sp)
                        state.events.take(6).forEach { Text("• $it",fontSize=12.sp,color=Muted,modifier=Modifier.padding(top=8.dp)) }
                    }
                }
                item {
                    Text("Storage confirmations: ${state.confirmations}\nA saved or relayed SOS does not mean a rescue team has acted.",fontSize=12.sp,color=Muted)
                }
            }
        }
        state.notice?.let { message ->
            AlertDialog(onDismissRequest={ vm.notice(null) },title={ Text("RESQMesh") },text={ Text(message) },
                confirmButton={ TextButton(onClick={ vm.notice(null) }) { Text("OK") } })
        }
    }
}

@Composable private fun Block(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(20.dp))
        .border(1.dp,Color(0xFF26354A),RoundedCornerShape(20.dp)).padding(18.dp),content=content)
}
@Composable private fun Metric(value: String,label: String) {
    Column { Text(value,fontSize=28.sp,fontWeight=FontWeight.Bold); Text(label,fontSize=12.sp,color=Muted) }
}
@Composable private fun EmergencyCard(p: Packet,self: String) {
    Block {
        val accent=when(p.priority) { Priority.CRITICAL -> Coral; Priority.HIGH -> Color(0xFFFFC16B); Priority.NORMAL -> Mint }
        Text("${p.priority} SOS   •   ${p.emergencyType.uppercase()}",color=accent,fontSize=12.sp,fontWeight=FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(p.message,fontSize=21.sp,fontWeight=FontWeight.SemiBold)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(28.dp)) {
            Metric(p.peopleCount.toString(),"PEOPLE"); Metric(p.injuredCount.toString(),"INJURED"); Metric(p.hopCount.toString(),"HOPS")
        }
        HorizontalDivider(Modifier.padding(vertical=14.dp),color=Color(0xFF30415A))
        Text(if(p.latitude==null) "Location unavailable" else "${p.latitude}, ${p.longitude}",fontWeight=FontWeight.Bold)
        Text(p.locationSource+(p.locationTimestamp?.let { " • fix ${time(it)}" } ?: ""),color=Muted,fontSize=12.sp)
        Text("Created ${time(p.timestamp)}",color=Muted,fontSize=12.sp)
        Spacer(Modifier.height(12.dp))
        Text(if(p.originDeviceId==self) "ORIGINATED HERE" else "RELAY PATH",color=Mint,fontSize=11.sp,letterSpacing=1.sp)
        Text(p.path.joinToString(" → ") { it.take(8).uppercase() },fontSize=13.sp,modifier=Modifier.padding(top=4.dp))
        Text("Origin: ${p.originDeviceId}\nPacket: ${p.messageId}\nHops remaining: ${p.ttl}",color=Muted,fontSize=11.sp,modifier=Modifier.padding(top=8.dp))
        Text(if(p.ttl==0) "Hop limit reached • retained for rescue" else "Stored • forwards to eligible connected nodes",color=accent,fontSize=12.sp,modifier=Modifier.padding(top=8.dp))
    }
}

@Composable private fun SosForm(self: String,back: () -> Unit,submit: (Packet, (Boolean) -> Unit) -> Unit) {
    val context=LocalContext.current
    var emergency by rememberSaveable { mutableStateOf("Collapse") }
    var message by rememberSaveable { mutableStateOf("") }
    var people by rememberSaveable { mutableStateOf("1") }
    var injured by rememberSaveable { mutableStateOf("0") }
    var priority by rememberSaveable { mutableStateOf("CRITICAL") }
    var latitude by rememberSaveable { mutableStateOf("") }
    var longitude by rememberSaveable { mutableStateOf("") }
    var source by rememberSaveable { mutableStateOf("Unavailable") }
    var fixTime by rememberSaveable { mutableStateOf<Long?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }
    fun readFix() {
        val fix=LocationReader.lastKnown(context)
        if(fix==null) { error="No cached location. You can enter coordinates or send without location."; return }
        latitude=fix.latitude.toString(); longitude=fix.longitude.toString(); fixTime=fix.time
        source="Last-known device location"; error=null
    }
    val locationPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { readFix() }
    Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(22.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        TextButton(onClick=back) { Text("← Back") }
        Text("Request help",fontSize=32.sp,fontWeight=FontWeight.Bold)
        Text("Saved here first. Shared when a nearby phone connects.",color=Muted)
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("Collapse","Flood","Medical","Fire","Other").forEach { type ->
                FilterChip(selected=emergency==type,onClick={ emergency=type },label={ Text(type) })
            }
        }
        OutlinedTextField(value=message,onValueChange={ message=it.take(280) },label={ Text("What happened?") },
            minLines=3,modifier=Modifier.fillMaxWidth(),supportingText={ Text("${message.length}/280") })
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value=people,onValueChange={ people=it.filter(Char::isDigit).take(3) },label={ Text("People") },
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.weight(1f))
            OutlinedTextField(value=injured,onValueChange={ injured=it.filter(Char::isDigit).take(3) },label={ Text("Injured") },
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.weight(1f))
        }
        Text("PRIORITY",fontSize=12.sp,color=Muted)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Priority.values().forEach { p -> FilterChip(selected=priority==p.name,onClick={ priority=p.name },label={ Text(p.name) }) }
        }
        OutlinedButton(onClick={ locationPermission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION)) }) { Text("Use last-known location") }
        Text("Optional coordinates • $source",color=Muted,fontSize=12.sp)
        OutlinedTextField(value=latitude,onValueChange={ latitude=it; source="Manually entered"; fixTime=null },label={ Text("Latitude") },modifier=Modifier.fillMaxWidth())
        OutlinedTextField(value=longitude,onValueChange={ longitude=it; source="Manually entered"; fixTime=null },label={ Text("Longitude") },modifier=Modifier.fillMaxWidth())
        TextButton(onClick={
            message="Trapped after building collapse"; emergency="Collapse"; people="3"; injured="1"; priority="CRITICAL"
            latitude="10.7905"; longitude="78.7047"; source="DEMO coordinates • not GPS"; fixTime=null
        }) { Text("Fill hackathon example (demo location)") }
        error?.let { Text(it,color=Coral) }
        Button(onClick={
            val n=people.toIntOrNull(); val i=injured.toIntOrNull()
            val lat=latitude.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
            val lon=longitude.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
            if(message.isBlank() || n==null || n !in 1..999 || i==null || i !in 0..n) {
                error="Add a message, 1–999 people, and an injured count no greater than people."; return@Button
            }
            if((latitude.isNotBlank() || longitude.isNotBlank()) && (lat==null || lon==null || !lat.isFinite() || !lon.isFinite() || lat !in -90.0..90.0 || lon !in -180.0..180.0)) {
                error="Enter both valid coordinates, or leave both empty."; return@Button
            }
            sending=true
            submit(Packet(originDeviceId=self,timestamp=System.currentTimeMillis(),latitude=lat,longitude=lon,
                priority=Priority.valueOf(priority),message=message.trim(),peopleCount=n,injuredCount=i,
                emergencyType=emergency,locationSource=if(lat==null) "Unavailable" else source,locationTimestamp=fixTime)) { sending=false }
        },enabled=!sending,modifier=Modifier.fillMaxWidth().height(60.dp)) { Text("SAVE & SHARE SOS",fontWeight=FontWeight.Bold,fontSize=17.sp) }
        Text("This shares the emergency details and coordinates with connected participants.",color=Muted,fontSize=12.sp)
    }
}
