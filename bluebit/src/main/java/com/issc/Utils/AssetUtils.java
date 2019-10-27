package com.issc.Utils;

import android.content.Context;
import android.content.res.AssetManager;

import com.issc.data.local.AppDatabase;
import com.issc.data.model.City;
import com.issc.util.Log;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * Created by kanivel.j on 10-03-2018.
 */

public class AssetUtils {


    public static void insertCity(Context context, AppDatabase database) throws UnsupportedEncodingException {


        List<City> cities = new ArrayList<City>();
        AssetManager assetManager = context.getAssets();
        InputStream is = null;

        try {
            is = assetManager.open("worldcities.csv");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        BufferedReader reader = null;
        reader = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));

        String line = "";
        StringTokenizer st = null;
        boolean first=true;

        try {

            int count=0;

            while ((line = reader.readLine()) != null) {

                count++;
                Log.e("Count",count+"");
                if(!first) {
                    st = new StringTokenizer(line, ",");
                    City city = new City();
                    //your attributes
                    city.setCity(st.nextToken());
                    city.setCity_ascii(st.nextToken());
                    city.setLatitude(Double.parseDouble(st.nextToken()));
                    city.setLongitude(Double.parseDouble(st.nextToken()));
                    city.setPop(Double.parseDouble(st.nextToken()));
                    city.setCountry(st.nextToken());
                    city.setIsotwo(st.nextToken());

                    try {
                        city.setIsothree(st.nextToken());
                    }
                    catch (NoSuchElementException e)
                    {

                    }

                    try {
                        city.setProvince(st.nextToken());
                    }
                    catch (NoSuchElementException e)
                    {
                        e.printStackTrace();
                    }

                    cities.add(city);
                }


                first=false;

            }
        } catch (IOException e) {

            e.printStackTrace();
        }

        database.cityDao().insertAllCity(cities);

    }


}
