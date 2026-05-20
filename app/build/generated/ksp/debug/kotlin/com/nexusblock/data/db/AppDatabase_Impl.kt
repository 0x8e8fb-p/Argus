package com.nexusblock.`data`.db

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AppDatabase_Impl : AppDatabase() {
  private val _blockedDomainDao: Lazy<BlockedDomainDao> = lazy {
    BlockedDomainDao_Impl(this)
  }

  private val _blockedEventDao: Lazy<BlockedEventDao> = lazy {
    BlockedEventDao_Impl(this)
  }

  private val _customRuleDao: Lazy<CustomRuleDao> = lazy {
    CustomRuleDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(2, "22ae43e6ab2448a28bff086b1cac1553", "5476accb4cc576606e83470b62c3097a") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `blocked_domains` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `host` TEXT NOT NULL, `source` TEXT NOT NULL, `enabled` INTEGER NOT NULL, `isRegex` INTEGER NOT NULL, `regexPattern` TEXT, `insertedAt` INTEGER NOT NULL)")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_blocked_domains_host` ON `blocked_domains` (`host`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_blocked_domains_source` ON `blocked_domains` (`source`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `blocked_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `host` TEXT NOT NULL, `appPackage` TEXT, `type` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_blocked_events_timestamp` ON `blocked_events` (`timestamp`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_blocked_events_host` ON `blocked_events` (`host`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_blocked_events_appPackage` ON `blocked_events` (`appPackage`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `custom_rules` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `rule` TEXT NOT NULL, `isAllow` INTEGER NOT NULL, `enabled` INTEGER NOT NULL, `description` TEXT, `createdAt` INTEGER NOT NULL)")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_custom_rules_rule` ON `custom_rules` (`rule`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '22ae43e6ab2448a28bff086b1cac1553')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `blocked_domains`")
        connection.execSQL("DROP TABLE IF EXISTS `blocked_events`")
        connection.execSQL("DROP TABLE IF EXISTS `custom_rules`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
        val _columnsBlockedDomains: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsBlockedDomains.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBlockedDomains.put("host", TableInfo.Column("host", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBlockedDomains.put("source", TableInfo.Column("source", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBlockedDomains.put("enabled", TableInfo.Column("enabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBlockedDomains.put("isRegex", TableInfo.Column("isRegex", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBlockedDomains.put("regexPattern", TableInfo.Column("regexPattern", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBlockedDomains.put("insertedAt", TableInfo.Column("insertedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysBlockedDomains: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesBlockedDomains: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesBlockedDomains.add(TableInfo.Index("index_blocked_domains_host", true, listOf("host"), listOf("ASC")))
        _indicesBlockedDomains.add(TableInfo.Index("index_blocked_domains_source", false, listOf("source"), listOf("ASC")))
        val _infoBlockedDomains: TableInfo = TableInfo("blocked_domains", _columnsBlockedDomains, _foreignKeysBlockedDomains, _indicesBlockedDomains)
        val _existingBlockedDomains: TableInfo = read(connection, "blocked_domains")
        if (!_infoBlockedDomains.equals(_existingBlockedDomains)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |blocked_domains(com.nexusblock.data.model.BlockedDomain).
              | Expected:
              |""".trimMargin() + _infoBlockedDomains + """
              |
              | Found:
              |""".trimMargin() + _existingBlockedDomains)
        }
        val _columnsBlockedEvents: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsBlockedEvents.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBlockedEvents.put("host", TableInfo.Column("host", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBlockedEvents.put("appPackage", TableInfo.Column("appPackage", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBlockedEvents.put("type", TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBlockedEvents.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysBlockedEvents: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesBlockedEvents: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesBlockedEvents.add(TableInfo.Index("index_blocked_events_timestamp", false, listOf("timestamp"), listOf("ASC")))
        _indicesBlockedEvents.add(TableInfo.Index("index_blocked_events_host", false, listOf("host"), listOf("ASC")))
        _indicesBlockedEvents.add(TableInfo.Index("index_blocked_events_appPackage", false, listOf("appPackage"), listOf("ASC")))
        val _infoBlockedEvents: TableInfo = TableInfo("blocked_events", _columnsBlockedEvents, _foreignKeysBlockedEvents, _indicesBlockedEvents)
        val _existingBlockedEvents: TableInfo = read(connection, "blocked_events")
        if (!_infoBlockedEvents.equals(_existingBlockedEvents)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |blocked_events(com.nexusblock.data.model.BlockedEvent).
              | Expected:
              |""".trimMargin() + _infoBlockedEvents + """
              |
              | Found:
              |""".trimMargin() + _existingBlockedEvents)
        }
        val _columnsCustomRules: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsCustomRules.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCustomRules.put("rule", TableInfo.Column("rule", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCustomRules.put("isAllow", TableInfo.Column("isAllow", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCustomRules.put("enabled", TableInfo.Column("enabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCustomRules.put("description", TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCustomRules.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysCustomRules: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesCustomRules: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesCustomRules.add(TableInfo.Index("index_custom_rules_rule", true, listOf("rule"), listOf("ASC")))
        val _infoCustomRules: TableInfo = TableInfo("custom_rules", _columnsCustomRules, _foreignKeysCustomRules, _indicesCustomRules)
        val _existingCustomRules: TableInfo = read(connection, "custom_rules")
        if (!_infoCustomRules.equals(_existingCustomRules)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |custom_rules(com.nexusblock.data.model.CustomRule).
              | Expected:
              |""".trimMargin() + _infoCustomRules + """
              |
              | Found:
              |""".trimMargin() + _existingCustomRules)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "blocked_domains", "blocked_events", "custom_rules")
  }

  public override fun clearAllTables() {
    super.performClear(false, "blocked_domains", "blocked_events", "custom_rules")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(BlockedDomainDao::class, BlockedDomainDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(BlockedEventDao::class, BlockedEventDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(CustomRuleDao::class, CustomRuleDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun blockedDomainDao(): BlockedDomainDao = _blockedDomainDao.value

  public override fun blockedEventDao(): BlockedEventDao = _blockedEventDao.value

  public override fun customRuleDao(): CustomRuleDao = _customRuleDao.value
}
