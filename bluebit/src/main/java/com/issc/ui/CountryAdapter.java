package com.issc.ui;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;


import com.issc.R;
import com.issc.data.model.City;

import java.util.List;

/**
 * Created by kanivel.j on 11-03-2018.
 */

public class CountryAdapter extends ArrayAdapter<City> {
    Context context;
    List<City> cities;
    LayoutInflater inflter;
    LayoutInflater inflter1;

    boolean isCountry=true;

    public CountryAdapter(Context applicationContext,int layout, List<City> cities,boolean isCountry) {

        super(applicationContext, layout, cities);
        this.context = applicationContext;
        this.cities = cities;
        inflter = (LayoutInflater.from(applicationContext));
        inflter1 = (LayoutInflater.from(applicationContext));
        this.isCountry=isCountry;

    }

    @Override
    public int getCount() {
        return cities.size();
    }


    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        Log.e("getView",i+"");

        LayoutInflater inflater =(LayoutInflater.from(context));;
        View view1 = inflater.inflate(R.layout.item_spinner, viewGroup, false);
        TextView names = (TextView) view1.findViewById(R.id.text1);

        if(i==0)
        {
            names.setTextColor(context.getResources().getColor(R.color.md_blue_grey_100));
        }
        else
        {
            names.setTextColor(context.getResources().getColor(R.color.black));
        }

        if(isCountry) {
            names.setText(cities.get(i).getCountry());
        }
        else
        {
            names.setText(cities.get(i).getCity());

        }
        return view1;
    }

    @Override
    public View getDropDownView(int position, View convertView,
                                ViewGroup parent) {

        Log.e("DropDownView",position+"");
        LayoutInflater inflater =(LayoutInflater.from(context));;
        View view1 = inflater.inflate(R.layout.item_spinner_language, parent, false);
        TextView names = (TextView) view1.findViewById(R.id.txt_country);

        if(position == 0){
            // Set the hint text color gray
            names.setTextColor(context.getResources().getColor(R.color.md_blue_grey_100));
        }
        else {
            names.setTextColor(context.getResources().getColor(R.color.black));
        }

        if(isCountry) {
            names.setText(cities.get(position).getCountry());
        }
        else
        {
            names.setText(cities.get(position).getCity());

        }

        // convertView.setBackgroundColor(context.getResources().getColor(R.color.transparent));
        return view1;
    }

}
