package com.nexusblock.`data`.db

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.nexusblock.`data`.model.CustomRule
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
public class CustomRuleDao_Impl(
  __db: RoomDatabase,
) : CustomRuleDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfCustomRule: EntityInsertAdapter<CustomRule>

  private val __deleteAdapterOfCustomRule: EntityDeleteOrUpdateAdapter<CustomRule>

  private val __updateAdapterOfCustomRule: EntityDeleteOrUpdateAdapter<CustomRule>
  init {
    this.__db = __db
    this.__insertAdapterOfCustomRule = object : EntityInsertAdapter<CustomRule>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `custom_rules` (`id`,`rule`,`isAllow`,`enabled`,`description`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: CustomRule) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.rule)
        val _tmp: Int = if (entity.isAllow) 1 else 0
        statement.bindLong(3, _tmp.toLong())
        val _tmp_1: Int = if (entity.enabled) 1 else 0
        statement.bindLong(4, _tmp_1.toLong())
        val _tmpDescription: String? = entity.description
        if (_tmpDescription == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpDescription)
        }
        statement.bindLong(6, entity.createdAt)
      }
    }
    this.__deleteAdapterOfCustomRule = object : EntityDeleteOrUpdateAdapter<CustomRule>() {
      protected override fun createQuery(): String = "DELETE FROM `custom_rules` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: CustomRule) {
        statement.bindLong(1, entity.id)
      }
    }
    this.__updateAdapterOfCustomRule = object : EntityDeleteOrUpdateAdapter<CustomRule>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `custom_rules` SET `id` = ?,`rule` = ?,`isAllow` = ?,`enabled` = ?,`description` = ?,`createdAt` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: CustomRule) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.rule)
        val _tmp: Int = if (entity.isAllow) 1 else 0
        statement.bindLong(3, _tmp.toLong())
        val _tmp_1: Int = if (entity.enabled) 1 else 0
        statement.bindLong(4, _tmp_1.toLong())
        val _tmpDescription: String? = entity.description
        if (_tmpDescription == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpDescription)
        }
        statement.bindLong(6, entity.createdAt)
        statement.bindLong(7, entity.id)
      }
    }
  }

  public override suspend fun insert(rule: CustomRule): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfCustomRule.insert(_connection, rule)
  }

  public override suspend fun delete(rule: CustomRule): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfCustomRule.handle(_connection, rule)
  }

  public override suspend fun update(rule: CustomRule): Unit = performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfCustomRule.handle(_connection, rule)
  }

  public override fun observeEnabled(): Flow<List<CustomRule>> {
    val _sql: String = "SELECT * FROM custom_rules WHERE enabled = 1"
    return createFlow(__db, false, arrayOf("custom_rules")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfRule: Int = getColumnIndexOrThrow(_stmt, "rule")
        val _columnIndexOfIsAllow: Int = getColumnIndexOrThrow(_stmt, "isAllow")
        val _columnIndexOfEnabled: Int = getColumnIndexOrThrow(_stmt, "enabled")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _result: MutableList<CustomRule> = mutableListOf()
        while (_stmt.step()) {
          val _item: CustomRule
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpRule: String
          _tmpRule = _stmt.getText(_columnIndexOfRule)
          val _tmpIsAllow: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsAllow).toInt()
          _tmpIsAllow = _tmp != 0
          val _tmpEnabled: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfEnabled).toInt()
          _tmpEnabled = _tmp_1 != 0
          val _tmpDescription: String?
          if (_stmt.isNull(_columnIndexOfDescription)) {
            _tmpDescription = null
          } else {
            _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _item = CustomRule(_tmpId,_tmpRule,_tmpIsAllow,_tmpEnabled,_tmpDescription,_tmpCreatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getEnabled(): List<CustomRule> {
    val _sql: String = "SELECT * FROM custom_rules WHERE enabled = 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfRule: Int = getColumnIndexOrThrow(_stmt, "rule")
        val _columnIndexOfIsAllow: Int = getColumnIndexOrThrow(_stmt, "isAllow")
        val _columnIndexOfEnabled: Int = getColumnIndexOrThrow(_stmt, "enabled")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _result: MutableList<CustomRule> = mutableListOf()
        while (_stmt.step()) {
          val _item: CustomRule
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpRule: String
          _tmpRule = _stmt.getText(_columnIndexOfRule)
          val _tmpIsAllow: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsAllow).toInt()
          _tmpIsAllow = _tmp != 0
          val _tmpEnabled: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfEnabled).toInt()
          _tmpEnabled = _tmp_1 != 0
          val _tmpDescription: String?
          if (_stmt.isNull(_columnIndexOfDescription)) {
            _tmpDescription = null
          } else {
            _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _item = CustomRule(_tmpId,_tmpRule,_tmpIsAllow,_tmpEnabled,_tmpDescription,_tmpCreatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeAll(): Flow<List<CustomRule>> {
    val _sql: String = "SELECT * FROM custom_rules ORDER BY createdAt DESC"
    return createFlow(__db, false, arrayOf("custom_rules")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfRule: Int = getColumnIndexOrThrow(_stmt, "rule")
        val _columnIndexOfIsAllow: Int = getColumnIndexOrThrow(_stmt, "isAllow")
        val _columnIndexOfEnabled: Int = getColumnIndexOrThrow(_stmt, "enabled")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _result: MutableList<CustomRule> = mutableListOf()
        while (_stmt.step()) {
          val _item: CustomRule
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpRule: String
          _tmpRule = _stmt.getText(_columnIndexOfRule)
          val _tmpIsAllow: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsAllow).toInt()
          _tmpIsAllow = _tmp != 0
          val _tmpEnabled: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfEnabled).toInt()
          _tmpEnabled = _tmp_1 != 0
          val _tmpDescription: String?
          if (_stmt.isNull(_columnIndexOfDescription)) {
            _tmpDescription = null
          } else {
            _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _item = CustomRule(_tmpId,_tmpRule,_tmpIsAllow,_tmpEnabled,_tmpDescription,_tmpCreatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeCount(): Flow<Int> {
    val _sql: String = "SELECT COUNT(*) FROM custom_rules WHERE enabled = 1"
    return createFlow(__db, false, arrayOf("custom_rules")) { _connection ->
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

  public override suspend fun deleteById(id: Long) {
    val _sql: String = "DELETE FROM custom_rules WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clearAll() {
    val _sql: String = "DELETE FROM custom_rules"
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
