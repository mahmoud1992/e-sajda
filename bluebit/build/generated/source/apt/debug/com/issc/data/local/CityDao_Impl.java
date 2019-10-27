package com.issc.data.local;

import android.arch.persistence.db.SupportSQLiteStatement;
import android.arch.persistence.room.EntityInsertionAdapter;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.RoomSQLiteQuery;
import android.database.Cursor;
import com.issc.data.model.City;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.List;

public class CityDao_Impl implements CityDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter __insertionAdapterOfCity;

  public CityDao_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCity = new EntityInsertionAdapter<City>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR ABORT INTO `city`(`id`,`cityname`,`city_ascii`,`latitude`,`longitude`,`pop`,`isotwo`,`isothree`,`country`,`province`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, City value) {
        stmt.bindLong(1, value.id);
        if (value.city == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.city);
        }
        if (value.city_ascii == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.city_ascii);
        }
        stmt.bindDouble(4, value.latitude);
        stmt.bindDouble(5, value.longitude);
        stmt.bindDouble(6, value.pop);
        if (value.isotwo == null) {
          stmt.bindNull(7);
        } else {
          stmt.bindString(7, value.isotwo);
        }
        if (value.isothree == null) {
          stmt.bindNull(8);
        } else {
          stmt.bindString(8, value.isothree);
        }
        if (value.country == null) {
          stmt.bindNull(9);
        } else {
          stmt.bindString(9, value.country);
        }
        if (value.province == null) {
          stmt.bindNull(10);
        } else {
          stmt.bindString(10, value.province);
        }
      }
    };
  }

  @Override
  public void insertAllCity(List<City> cities) {
    __db.beginTransaction();
    try {
      __insertionAdapterOfCity.insert(cities);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public List<City> getAllCity() {
    final String _sql = "SELECT * FROM city";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final Cursor _cursor = __db.query(_statement);
    try {
      final int _cursorIndexOfId = _cursor.getColumnIndexOrThrow("id");
      final int _cursorIndexOfCity = _cursor.getColumnIndexOrThrow("cityname");
      final int _cursorIndexOfCityAscii = _cursor.getColumnIndexOrThrow("city_ascii");
      final int _cursorIndexOfLatitude = _cursor.getColumnIndexOrThrow("latitude");
      final int _cursorIndexOfLongitude = _cursor.getColumnIndexOrThrow("longitude");
      final int _cursorIndexOfPop = _cursor.getColumnIndexOrThrow("pop");
      final int _cursorIndexOfIsotwo = _cursor.getColumnIndexOrThrow("isotwo");
      final int _cursorIndexOfIsothree = _cursor.getColumnIndexOrThrow("isothree");
      final int _cursorIndexOfCountry = _cursor.getColumnIndexOrThrow("country");
      final int _cursorIndexOfProvince = _cursor.getColumnIndexOrThrow("province");
      final List<City> _result = new ArrayList<City>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final City _item;
        _item = new City();
        _item.id = _cursor.getInt(_cursorIndexOfId);
        _item.city = _cursor.getString(_cursorIndexOfCity);
        _item.city_ascii = _cursor.getString(_cursorIndexOfCityAscii);
        _item.latitude = _cursor.getDouble(_cursorIndexOfLatitude);
        _item.longitude = _cursor.getDouble(_cursorIndexOfLongitude);
        _item.pop = _cursor.getDouble(_cursorIndexOfPop);
        _item.isotwo = _cursor.getString(_cursorIndexOfIsotwo);
        _item.isothree = _cursor.getString(_cursorIndexOfIsothree);
        _item.country = _cursor.getString(_cursorIndexOfCountry);
        _item.province = _cursor.getString(_cursorIndexOfProvince);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<City> getCityByCountryName(String countryName) {
    final String _sql = "SELECT * FROM city WHERE country =?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (countryName == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, countryName);
    }
    final Cursor _cursor = __db.query(_statement);
    try {
      final int _cursorIndexOfId = _cursor.getColumnIndexOrThrow("id");
      final int _cursorIndexOfCity = _cursor.getColumnIndexOrThrow("cityname");
      final int _cursorIndexOfCityAscii = _cursor.getColumnIndexOrThrow("city_ascii");
      final int _cursorIndexOfLatitude = _cursor.getColumnIndexOrThrow("latitude");
      final int _cursorIndexOfLongitude = _cursor.getColumnIndexOrThrow("longitude");
      final int _cursorIndexOfPop = _cursor.getColumnIndexOrThrow("pop");
      final int _cursorIndexOfIsotwo = _cursor.getColumnIndexOrThrow("isotwo");
      final int _cursorIndexOfIsothree = _cursor.getColumnIndexOrThrow("isothree");
      final int _cursorIndexOfCountry = _cursor.getColumnIndexOrThrow("country");
      final int _cursorIndexOfProvince = _cursor.getColumnIndexOrThrow("province");
      final List<City> _result = new ArrayList<City>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final City _item;
        _item = new City();
        _item.id = _cursor.getInt(_cursorIndexOfId);
        _item.city = _cursor.getString(_cursorIndexOfCity);
        _item.city_ascii = _cursor.getString(_cursorIndexOfCityAscii);
        _item.latitude = _cursor.getDouble(_cursorIndexOfLatitude);
        _item.longitude = _cursor.getDouble(_cursorIndexOfLongitude);
        _item.pop = _cursor.getDouble(_cursorIndexOfPop);
        _item.isotwo = _cursor.getString(_cursorIndexOfIsotwo);
        _item.isothree = _cursor.getString(_cursorIndexOfIsothree);
        _item.country = _cursor.getString(_cursorIndexOfCountry);
        _item.province = _cursor.getString(_cursorIndexOfProvince);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<City> getCountry() {
    final String _sql = "SELECT  * FROM city GROUP BY country";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final Cursor _cursor = __db.query(_statement);
    try {
      final int _cursorIndexOfId = _cursor.getColumnIndexOrThrow("id");
      final int _cursorIndexOfCity = _cursor.getColumnIndexOrThrow("cityname");
      final int _cursorIndexOfCityAscii = _cursor.getColumnIndexOrThrow("city_ascii");
      final int _cursorIndexOfLatitude = _cursor.getColumnIndexOrThrow("latitude");
      final int _cursorIndexOfLongitude = _cursor.getColumnIndexOrThrow("longitude");
      final int _cursorIndexOfPop = _cursor.getColumnIndexOrThrow("pop");
      final int _cursorIndexOfIsotwo = _cursor.getColumnIndexOrThrow("isotwo");
      final int _cursorIndexOfIsothree = _cursor.getColumnIndexOrThrow("isothree");
      final int _cursorIndexOfCountry = _cursor.getColumnIndexOrThrow("country");
      final int _cursorIndexOfProvince = _cursor.getColumnIndexOrThrow("province");
      final List<City> _result = new ArrayList<City>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final City _item;
        _item = new City();
        _item.id = _cursor.getInt(_cursorIndexOfId);
        _item.city = _cursor.getString(_cursorIndexOfCity);
        _item.city_ascii = _cursor.getString(_cursorIndexOfCityAscii);
        _item.latitude = _cursor.getDouble(_cursorIndexOfLatitude);
        _item.longitude = _cursor.getDouble(_cursorIndexOfLongitude);
        _item.pop = _cursor.getDouble(_cursorIndexOfPop);
        _item.isotwo = _cursor.getString(_cursorIndexOfIsotwo);
        _item.isothree = _cursor.getString(_cursorIndexOfIsothree);
        _item.country = _cursor.getString(_cursorIndexOfCountry);
        _item.province = _cursor.getString(_cursorIndexOfProvince);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }
}
