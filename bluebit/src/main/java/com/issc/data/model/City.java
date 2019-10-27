package com.issc.data.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

/**
 * Created by kanivel.j on 10-03-2018.
 */


@Entity(tableName = "city")
public class City {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "cityname")
    public String city;

    @ColumnInfo(name = "city_ascii")
    public String city_ascii;

    @ColumnInfo(name = "latitude")
    public double latitude;

    @ColumnInfo(name = "longitude")
    public double longitude;

    @ColumnInfo(name = "pop")
    public double pop;

    @ColumnInfo(name = "isotwo")
    public String isotwo;

    @ColumnInfo(name = "isothree")
    public String isothree;

    @ColumnInfo(name = "country")
    public String country;

    @ColumnInfo(name = "province")
    public String province;


    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCity_ascii() {
        return city_ascii;
    }

    public void setCity_ascii(String city_ascii) {
        this.city_ascii = city_ascii;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getPop() {
        return pop;
    }

    public void setPop(double pop) {
        this.pop = pop;
    }

    public String getIsotwo() {
        return isotwo;
    }

    public void setIsotwo(String isotwo) {
        this.isotwo = isotwo;
    }

    public String getIsothree() {
        return isothree;
    }

    public void setIsothree(String isothree) {
        this.isothree = isothree;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }
}
