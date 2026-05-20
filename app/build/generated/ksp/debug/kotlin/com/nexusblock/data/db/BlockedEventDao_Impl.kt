package com.nexusblock.`data`.db

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.nexusblock.`data`.model.BlockedEvent
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class BlockedEventDao_Impl(
  __db: RoomDatabase,
) : BlockedEventDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfBlockedEvent: EntityInsertAdapter<BlockedEvent>
  init {
    this.__db = __db
    this.__insertAdapterOfBlockedEvent = object : EntityInsertAdapter<BlockedEvent>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `blocked_events` (`id`,`host`,`appPackage`,`type`,`timestamp`) VALUES (nullif(?, 0),?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: BlockedEvent) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.host)
        val _tmpAppPackage: String? = entity.appPackage
        if (_tmpAppPackage == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpAppPackage)
        }
        statement.bindText(4, entity.type)
        statement.bindLong(5, entity.timestamp)
      }
    }
  }

  public override suspend fun insert(event: BlockedEvent): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfBlockedEvent.insert(_connection, event)
  }

  public override suspend fun insertAll(events: List<BlockedEvent>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfBlockedEvent.insert(_connection, events)
  }

  public override fun observeRecent(limit: Int): Flow<List<BlockedEvent>> {
    val _sql: String = "SELECT * FROM blocked_events ORDER BY timestamp DESC LIMIT ?"
    return createFlow(__db, false, arrayOf("blocked_events")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfHost: Int = getColumnIndexOrThrow(_stmt, "host")
        val _columnIndexOfAppPackage: Int = getColumnIndexOrThrow(_stmt, "appPackage")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _result: MutableList<BlockedEvent> = mutableListOf()
        while (_stmt.step()) {
          val _item: BlockedEvent
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpHost: String
          _tmpHost = _stmt.getText(_columnIndexOfHost)
          val _tmpAppPackage: String?
          if (_stmt.isNull(_columnIndexOfAppPackage)) {
            _tmpAppPackage = null
          } else {
            _tmpAppPackage = _stmt.getText(_columnIndexOfAppPackage)
          }
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          _item = BlockedEvent(_tmpId,_tmpHost,_tmpAppPackage,_tmpType,_tmpTimestamp)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getRecent(limit: Int): List<BlockedEvent> {
    val _sql: String = "SELECT * FROM blocked_events ORDER BY timestamp DESC LIMIT ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfHost: Int = getColumnIndexOrThrow(_stmt, "host")
        val _columnIndexOfAppPackage: Int = getColumnIndexOrThrow(_stmt, "appPackage")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _result: MutableList<BlockedEvent> = mutableListOf()
        while (_stmt.step()) {
          val _item: BlockedEvent
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpHost: String
          _tmpHost = _stmt.getText(_columnIndexOfHost)
          val _tmpAppPackage: String?
          if (_stmt.isNull(_columnIndexOfAppPackage)) {
            _tmpAppPackage = null
          } else {
            _tmpAppPackage = _stmt.getText(_columnIndexOfAppPackage)
          }
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          _item = BlockedEvent(_tmpId,_tmpHost,_tmpAppPackage,_tmpType,_tmpTimestamp)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeTotalCount(): Flow<Int> {
    val _sql: String = "SELECT COUNT(*) FROM blocked_events"
    return createFlow(__db, false, arrayOf("blocked_events")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun countSince(since: Long): Int {
    val _sql: String = "SELECT COUNT(*) FROM blocked_events WHERE timestamp >= ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, since)
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clearAll() {
    val _sql: String = "DELETE FROM blocked_events"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
