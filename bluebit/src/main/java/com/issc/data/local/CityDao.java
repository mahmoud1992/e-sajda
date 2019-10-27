package com.issc.data.local;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.RoomWarnings;


import com.issc.data.model.City;

import java.util.List;

/**
 * Created by kanivel.j on 10-03-2018.
 */

@Dao
public interface CityDao {



    @Query("SELECT * FROM city")
    List<City> getAllCity();

    @Query("SELECT * FROM city WHERE country =:countryName")
    List<City> getCityByCountryName(String countryName);


    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT  * FROM city GROUP BY country")
    List<City> getCountry();


    @Insert
    void insertAllCity(List<City> cities);



}
