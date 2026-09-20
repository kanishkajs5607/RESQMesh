package org.resqmesh.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.resqmesh.core.Codec
import org.resqmesh.core.Packet
import org.resqmesh.core.Routing

/** SQLite commits finish before the receiver sends its acknowledgement. */
class PacketStore(context: Context, name: String = "resqmesh.db") : SQLiteOpenHelper(context, name, null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE packets (id TEXT PRIMARY KEY, body BLOB NOT NULL)")
        db.execSQL("CREATE TABLE deliveries (message_id TEXT NOT NULL, peer_id TEXT NOT NULL, PRIMARY KEY(message_id,peer_id))")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    @Synchronized fun insert(packet: Packet): Boolean {
        val values=ContentValues().apply { put("id",packet.messageId); put("body",Codec.encode(packet)) }
        return writableDatabase.insertWithOnConflict("packets",null,values,SQLiteDatabase.CONFLICT_IGNORE) != -1L
    }
    @Synchronized fun contains(id: String): Boolean = readableDatabase.rawQuery("SELECT 1 FROM packets WHERE id=?",arrayOf(id)).use { it.moveToFirst() }
    @Synchronized fun all(): List<Packet> = readableDatabase.rawQuery("SELECT body FROM packets",null).use { c ->
        buildList { while(c.moveToNext()) add(Codec.decode(c.getBlob(0))) }.sortedWith(Routing.priorityOrder)
    }
    @Synchronized fun acknowledge(id: String, peer: String) {
        writableDatabase.insertWithOnConflict("deliveries",null,ContentValues().apply {
            put("message_id",id); put("peer_id",peer)
        },SQLiteDatabase.CONFLICT_IGNORE)
    }
    @Synchronized fun delivered(id: String, peer: String): Boolean = readableDatabase.rawQuery(
        "SELECT 1 FROM deliveries WHERE message_id=? AND peer_id=?",arrayOf(id,peer)).use { it.moveToFirst() }
    @Synchronized fun deliveryCount(): Int = readableDatabase.rawQuery("SELECT COUNT(*) FROM deliveries",null).use { it.moveToFirst(); it.getInt(0) }
    @Synchronized fun relayCount(self: String): Int = all().filter { it.originDeviceId != self }.count { p ->
        readableDatabase.rawQuery("SELECT 1 FROM deliveries WHERE message_id=? LIMIT 1",arrayOf(p.messageId)).use { it.moveToFirst() }
    }
}
