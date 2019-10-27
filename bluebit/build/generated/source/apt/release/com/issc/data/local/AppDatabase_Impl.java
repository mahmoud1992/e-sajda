package com.issc.data.local;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.db.SupportSQLiteOpenHelper;
import android.arch.persistence.db.SupportSQLiteOpenHelper.Callback;
import android.arch.persistence.db.SupportSQLiteOpenHelper.Configuration;
import android.arch.persistence.room.DatabaseConfiguration;
import android.arch.persistence.room.InvalidationTracker;
import android.arch.persistence.room.RoomOpenHelper;
import android.arch.persistence.room.RoomOpenHelper.Delegate;
import android.arch.persistence.room.util.TableInfo;
import android.arch.persistence.room.util.TableInfo.Column;
import android.arch.persistence.room.util.TableInfo.ForeignKey;
import android.arch.persistence.room.util.TableInfo.Index;
import java.lang.IllegalStateException;
import java.lang.Override;
import java.lang.String;
import java.util.HashMap;
import java.util.HashSet;

public class AppDatabase_Impl extends AppDatabase {
  private volatile CityDao _cityDao;

  protected SupportSQLiteOpenHelper createOpenHelper(DatabaseConfiguration configuration) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(configuration, new RoomOpenHelper.Delegate(1) {
      public void createAllTables(SupportSQLiteDatabase _db) {
        _db.execSQL("CREATE TABLE IF NOT EXISTS `city` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `cityname` TEXT, `city_ascii` TEXT, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `pop` REAL NOT NULL, `isotwo` TEXT, `isothree` TEXT, `country` TEXT, `province` TEXT)");
        _db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        _db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, \"4c01789a93dfed936e153f5f4a02de19\")");
      }

      public void dropAllTables(SupportSQLiteDatabase _db) {
        _db.execSQL("DROP TABLE IF EXISTS `city`");
      }

      protected void onCreate(SupportSQLiteDatabase _db) {
        if (mCallbacks != null) {
          for (int _i = 0, _size = mCallbacks.size(); _i < _size; _i++) {
            mCallbacks.get(_i).onCreate(_db);
          }
        }
      }

      public void onOpen(SupportSQLiteDatabase _db) {
        mDatabase = _db;
        internalInitInvalidationTracker(_db);
        if (mCallbacks != null) {
          for (int _i = 0, _size = mCallbacks.size(); _i < _size; _i++) {
            mCallbacks.get(_i).onOpen(_db);
          }
        }
      }

      protected void validateMigration(SupportSQLiteDatabase _db) {
        final HashMap<String, TableInfo.Column> _columnsCity = new HashMap<String, TableInfo.Column>(10);
        _columnsCity.put("id", new TableInfo.Column("id", "INTEGER", true, 1));
        _columnsCity.put("cityname", new TableInfo.Column("cityname", "TEXT", false, 0));
        _columnsCity.put("city_ascii", new TableInfo.Column("city_ascii", "TEXT", false, 0));
        _columnsCity.put("latitude", new TableInfo.Column("latitude", "REAL", true, 0));
        _columnsCity.put("longitude", new TableInfo.Column("longitude", "REAL", true, 0));
        _columnsCity.put("pop", new TableInfo.Column("pop", "REAL", true, 0));
        _columnsCity.put("isotwo", new TableInfo.Column("isotwo", "TEXT", false, 0));
        _columnsCity.put("isothree", new TableInfo.Column("isothree", "TEXT", false, 0));
        _columnsCity.put("country", new TableInfo.Column("country", "TEXT", false, 0));
        _columnsCity.put("province", new TableInfo.Column("province", "TEXT", false, 0));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCity = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCity = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCity = new TableInfo("city", _columnsCity, _foreignKeysCity, _indicesCity);
        final TableInfo _existingCity = TableInfo.read(_db, "city");
        if (! _infoCity.equals(_existingCity)) {
          throw new IllegalStateException("Migration didn't properly handle city(com.issc.data.model.City).\n"
                  + " Expected:\n" + _infoCity + "\n"
                  + " Found:\n" + _existingCity);
        }
      }
    }, "4c01789a93dfed936e153f5f4a02de19");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(configuration.context)
        .name(configuration.name)
        .callback(_openCallback)
        .build();
    final SupportSQLiteOpenHelper _helper = configuration.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  protected InvalidationTracker createInvalidationTracker() {
    return new InvalidationTracker(this, "city");
  }

  @Override
  public CityDao cityDao() {
    if (_cityDao != null) {
      return _cityDao;
    } else {
      synchronized(this) {
        if(_cityDao == null) {
          _cityDao = new CityDao_Impl(this);
        }
        return _cityDao;
      }
    }
  }
}
