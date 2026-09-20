package org.resqmesh.core

import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable enum class Priority { CRITICAL, HIGH, NORMAL }
@Serializable data class Packet(
    val messageId: String = UUID.randomUUID().toString(),
    val originDeviceId: String,
    val timestamp: Long,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val priority: Priority,
    val message: String,
    val peopleCount: Int,
    val injuredCount: Int,
    val hopCount: Int = 0,
    val ttl: Int = 8,
    val emergencyType: String = "Other",
    val locationSource: String = "Unavailable",
    val locationTimestamp: Long? = null,
    val path: List<String> = listOf(originDeviceId),
    val version: Int = 1
) {
    fun validate(): Packet {
        require(version == 1)
        require(isUuid(messageId) && isUuid(originDeviceId))
        require(timestamp > 0 && message.isNotBlank() && message.length <= 280)
        require(peopleCount in 1..999 && injuredCount in 0..peopleCount)
        require(hopCount in 0..8 && ttl in 0..8 && hopCount + ttl == 8)
        require(path.size == hopCount + 1 && path.first() == originDeviceId)
        require(path.distinct().size == path.size && path.all(::isUuid))
        require((latitude == null) == (longitude == null))
        require(latitude == null || (latitude.isFinite() && latitude in -90.0..90.0))
        require(longitude == null || (longitude.isFinite() && longitude in -180.0..180.0))
        require(emergencyType.length in 1..40 && locationSource.length in 1..80)
        require(locationTimestamp == null || locationTimestamp > 0)
        return this
    }
    companion object {
        fun isUuid(value: String): Boolean = runCatching { UUID.fromString(value).toString() == value }.getOrDefault(false)
    }
}

object Codec {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    const val MAX_BYTES = 8192
    fun encode(packet: Packet): ByteArray = json.encodeToString(packet.validate()).toByteArray(Charsets.UTF_8)
        .also { require(it.size <= MAX_BYTES) }
    fun decode(bytes: ByteArray): Packet {
        require(bytes.size <= MAX_BYTES)
        return json.decodeFromString<Packet>(bytes.toString(Charsets.UTF_8)).validate()
    }
}

object Routing {
    val priorityOrder = compareBy<Packet> { it.priority.ordinal }.thenByDescending { it.timestamp }
    fun receive(packet: Packet, sender: String, self: String): Packet? {
        packet.validate()
        if (packet.path.last() != sender || self in packet.path || packet.ttl <= 0) return null
        return packet.copy(hopCount = packet.hopCount + 1, ttl = packet.ttl - 1, path = packet.path + self).validate()
    }
    fun canForward(packet: Packet, peer: String): Boolean = packet.ttl > 0 && peer !in packet.path
}
