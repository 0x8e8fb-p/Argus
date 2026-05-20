package com.nexusblock.`data`.db

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performInTransactionSuspending
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.nexusblock.`data`.model.BlockedDomain
import javax.`annotation`.processing.Generated
import kotlin.Boolean
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
public class BlockedDomainDao_Impl(
  __db: RoomDatabase,
) : BlockedDomainDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfBlockedDomain: EntityInsertAdapter<BlockedDomain>
  init {
    this.__db = __db
    this.__insertAdapterOfBlockedDomain = object : EntityInsertAdapter<BlockedDomain>() {
      protected override fun createQuery(): String = "INSERT OR IGNORE INTO `blocked_domains` (`id`,`host`,`source`,`enabled`,`isRegex`,`regexPattern`,`insertedAt`) VALUES (nullif(?, 0),?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: BlockedDomain) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.host)
        statement.bindText(3, entity.source)
        val _tmp: Int = if (entity.enabled) 1 else 0
        statement.bindLong(4, _tmp.toLong())
        val _tmp_1: Int = if (entity.isRegex) 1 else 0
        statement.bindLong(5, _tmp_1.toLong())
        val _tmpRegexPattern: String? = entity.regexPattern
        if (_tmpRegexPattern == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpRegexPattern)
        }
        statement.bindLong(7, entity.insertedAt)
      }
    }
  }

  public override suspend fun insertAll(domains: List<BlockedDomain>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfBlockedDomain.insert(_connection, domains)
  }

  public override suspend fun replaceSource(source: String, domains: List<BlockedDomain>): Unit = performInTransactionSuspending(__db) {
    super@BlockedDomainDao_Impl.replaceSource(source, domains)
  }

  public override fun observeEnabled(): Flow<List<BlockedDomain>> {
    val _sql: String = "SELECT * FROM blocked_domains WHERE enabled = 1"
    return createFlow(__db, false, arrayOf("blocked_domains")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfHost: Int = getColumnIndexOrThrow(_stmt, "host")
        val _columnIndexOfSource: Int = getColumnIndexOrThrow(_stmt, "source")
        val _columnIndexOfEnabled: Int = getColumnIndexOrThrow(_stmt, "enabled")
        val _columnIndexOfIsRegex: Int = getColumnIndexOrThrow(_stmt, "isRegex")
        val _columnIndexOfRegexPattern: Int = getColumnIndexOrThrow(_stmt, "regexPattern")
        val _columnIndexOfInsertedAt: Int = getColumnIndexOrThrow(_stmt, "insertedAt")
        val _result: MutableList<BlockedDomain> = mutableListOf()
        while (_stmt.step()) {
          val _item: BlockedDomain
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpHost: String
          _tmpHost = _stmt.getText(_columnIndexOfHost)
          val _tmpSource: String
          _tmpSource = _stmt.getText(_columnIndexOfSource)
          val _tmpEnabled: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfEnabled).toInt()
          _tmpEnabled = _tmp != 0
          val _tmpIsRegex: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsRegex).toInt()
          _tmpIsRegex = _tmp_1 != 0
          val _tmpRegexPattern: String?
          if (_stmt.isNull(_columnIndexOfRegexPattern)) {
            _tmpRegexPattern = null
          } else {
            _tmpRegexPattern = _stmt.getText(_columnIndexOfRegexPattern)
          }
          val _tmpInsertedAt: Long
          _tmpInsertedAt = _stmt.getLong(_columnIndexOfInsertedAt)
          _item = BlockedDomain(_tmpId,_tmpHost,_tmpSource,_tmpEnabled,_tmpIsRegex,_tmpRegexPattern,_tmpInsertedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getEnabled(): List<BlockedDomain> {
    val _sql: String = "SELECT * FROM blocked_domains WHERE enabled = 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfHost: Int = getColumnIndexOrThrow(_stmt, "host")
        val _columnIndexOfSource: Int = getColumnIndexOrThrow(_stmt, "source")
        val _columnIndexOfEnabled: Int = getColumnIndexOrThrow(_stmt, "enabled")
        val _columnIndexOfIsRegex: Int = getColumnIndexOrThrow(_stmt, "isRegex")
        val _columnIndexOfRegexPattern: Int = getColumnIndexOrThrow(_stmt, "regexPattern")
        val _columnIndexOfInsertedAt: Int = getColumnIndexOrThrow(_stmt, "insertedAt")
        val _result: MutableList<BlockedDomain> = mutableListOf()
        while (_stmt.step()) {
          val _item: BlockedDomain
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpHost: String
          _tmpHost = _stmt.getText(_columnIndexOfHost)
          val _tmpSource: String
          _tmpSource = _stmt.getText(_columnIndexOfSource)
          val _tmpEnabled: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfEnabled).toInt()
          _tmpEnabled = _tmp != 0
          val _tmpIsRegex: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsRegex).toInt()
          _tmpIsRegex = _tmp_1 != 0
          val _tmpRegexPattern: String?
          if (_stmt.isNull(_columnIndexOfRegexPattern)) {
            _tmpRegexPattern = null
          } else {
            _tmpRegexPattern = _stmt.getText(_columnIndexOfRegexPattern)
          }
          val _tmpInsertedAt: Long
          _tmpInsertedAt = _stmt.getLong(_columnIndexOfInsertedAt)
          _item = BlockedDomain(_tmpId,_tmpHost,_tmpSource,_tmpEnabled,_tmpIsRegex,_tmpRegexPattern,_tmpInsertedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeCount(): Flow<Int> {
    val _sql: String = "SELECT COUNT(*) FROM blocked_domains WHERE enabled = 1"
    return createFlow(__db, false, arrayOf("blocked_domains")) { _connection ->
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

  public override fun observeSourceStates(): Flow<List<BlocklistSourceState>> {
    val _sql: String = "SELECT source, CASE WHEN MIN(enabled) != 0 THEN 1 ELSE 0 END AS enabled, COUNT(*) AS count FROM blocked_domains GROUP BY source"
    return createFlow(__db, false, arrayOf("blocked_domains")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfSource: Int = 0
        val _columnIndexOfEnabled: Int = 1
        val _columnIndexOfCount: Int = 2
        val _result: MutableList<BlocklistSourceState> = mutableListOf()
        while (_stmt.step()) {
          val _item: BlocklistSourceState
          val _tmpSource: String
          _tmpSource = _stmt.getText(_columnIndexOfSource)
          val _tmpEnabled: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfEnabled).toInt()
          _tmpEnabled = _tmp != 0
          val _tmpCount: Int
          _tmpCount = _stmt.getLong(_columnIndexOfCount).toInt()
          _item = BlocklistSourceState(_tmpSource,_tmpEnabled,_tmpCount)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getBySource(source: String): List<BlockedDomain> {
    val _sql: String = "SELECT * FROM blocked_domains WHERE source = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, source)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfHost: Int = getColumnIndexOrThrow(_stmt, "host")
        val _columnIndexOfSource: Int = getColumnIndexOrThrow(_stmt, "source")
        val _columnIndexOfEnabled: Int = getColumnIndexOrThrow(_stmt, "enabled")
        val _columnIndexOfIsRegex: Int = getColumnIndexOrThrow(_stmt, "isRegex")
        val _columnIndexOfRegexPattern: Int = getColumnIndexOrThrow(_stmt, "regexPattern")
        val _columnIndexOfInsertedAt: Int = getColumnIndexOrThrow(_stmt, "insertedAt")
        val _result: MutableList<BlockedDomain> = mutableListOf()
        while (_stmt.step()) {
          val _item: BlockedDomain
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpHost: String
          _tmpHost = _stmt.getText(_columnIndexOfHost)
          val _tmpSource: String
          _tmpSource = _stmt.getText(_columnIndexOfSource)
          val _tmpEnabled: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfEnabled).toInt()
          _tmpEnabled = _tmp != 0
          val _tmpIsRegex: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsRegex).toInt()
          _tmpIsRegex = _tmp_1 != 0
          val _tmpRegexPattern: String?
          if (_stmt.isNull(_columnIndexOfRegexPattern)) {
            _tmpRegexPattern = null
          } else {
            _tmpRegexPattern = _stmt.getText(_columnIndexOfRegexPattern)
          }
          val _tmpInsertedAt: Long
          _tmpInsertedAt = _stmt.getLong(_columnIndexOfInsertedAt)
          _item = BlockedDomain(_tmpId,_tmpHost,_tmpSource,_tmpEnabled,_tmpIsRegex,_tmpRegexPattern,_tmpInsertedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun countBySource(source: String): Int {
    val _sql: String = "SELECT COUNT(*) FROM blocked_domains WHERE source = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, source)
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

  public override suspend fun getSourceEnabled(source: String): Int {
    val _sql: String = "SELECT COALESCE(MIN(enabled), 1) FROM blocked_domains WHERE source = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, source)
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

  public override suspend fun deleteBySource(source: String) {
    val _sql: String = "DELETE FROM blocked_domains WHERE source = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, source)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun setEnabledBySource(source: String, enabled: Boolean) {
    val _sql: String = "UPDATE blocked_domains SET enabled = ? WHERE source = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: Int = if (enabled) 1 else 0
        _stmt.bindLong(_argIndex, _tmp.toLong())
        _argIndex = 2
        _stmt.bindText(_argIndex, source)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clearAll() {
    val _sql: String = "DELETE FROM blocked_domains"
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
