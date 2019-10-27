package com.issc.Utils;

import com.issc.util.Log;

import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * Created by kanivel.j on 12-03-2018.
 */

public class GpsUtils {


    public static String convertLatitudeToGPS(double Latitude)
    {
        String Direction = "";
        double UnformattedLatitude = Latitude;
        if (Latitude > 0)
        {
            Direction = "N";
        }
        else
        {
            UnformattedLatitude = UnformattedLatitude * -1;
            Direction = "S";
        }

        String GPSString = Direction+" "+roundFourDecimals(UnformattedLatitude);
        return GPSString;
    }


    public static String convertLatitudeToGPSOne(double Latitude)
    {
        String Direction = "";
        double UnformattedLatitude = Latitude;
        if (Latitude > 0)
        {
            Direction = "N";
        }
        else
        {
            UnformattedLatitude = UnformattedLatitude * -1;
            Direction = "S";
        }
        String zero="";

        if(UnformattedLatitude<10)
        {
            zero="0";

        }

        String GPSString = Direction+zero+""+roundFourDecimals(UnformattedLatitude);
        return GPSString;
    }

    public static String convertLongitudeToGPS(double Longitude)
    {
        String Direction = "";
        double UnformattedLongitude = Longitude;
        if (Longitude > 0)
        {
            Direction = "E";
        }
        else
        {
            UnformattedLongitude = UnformattedLongitude * -1;
            Direction = "W";
        }
        String GPSString =  Direction+" "+roundFourDecimals(UnformattedLongitude);
        return GPSString;
    }

    public static String convertLongitudeToGPSOne(double Longitude)
    {
        String Direction = "";
        double UnformattedLongitude = Longitude;
        if (Longitude > 0)
        {
            Direction = "E";
        }
        else
        {
            UnformattedLongitude = UnformattedLongitude * -1;
            Direction = "W";
        }

        String zero="";
        if(UnformattedLongitude<10)
        {
            zero="00";

        }
        else if(UnformattedLongitude<100) {
            zero = "0";
        }



        String GPSString =  Direction+zero+roundFourDecimals(UnformattedLongitude);
        return GPSString;
    }



    public static String getDirectionLatitude(double latitude)
    {

        String Direction = "";
        if (latitude > 0)
        {
            Direction = "N";
        }
        else
        {
            Direction = "S";
        }


        return Direction;
    }


    public static String getDirectionLongitude(double longitude)
    {

        String Direction = "";
        if (longitude > 0)
        {
            Direction = "E";
        }
        else
        {
            Direction = "W";
        }


        return Direction;
    }



    public static String getGpsToSend(double latitude,double longitude)
    {

        String common="%GPSUP";

        String result="";



        result=common+convertLatitudeToGPSOne(latitude)+convertLongitudeToGPSOne(longitude)+"%";



        return result;






    }



    public static String roundFourDecimals(double d)
    {
        /*DecimalFormat twoDForm = new DecimalFormat("##.####");
        twoDForm.setMaximumFractionDigits(4);
        twoDForm.setMinimumFractionDigits(4);
        twoDForm.setRoundingMode(RoundingMode.CEILING);*/

        String value=String.valueOf(d);
        Log.e("Value",value+"**********");
        String arr[]=value.split("\\.");
        int len=arr[1].length();
        int count=0;
        String decimal="";
        for(int i=0;i<len;i++)
        {

            if(i<=3) {
                decimal = decimal + arr[1].charAt(i);
                count++;
            }

        }

        if(count==1)
        {

            decimal=decimal+"000";
        }
        if(count==2)
        {

            decimal=decimal+"00";
        }

        if(count==3)
        {

            decimal=decimal+"0";
        }

        return arr[0]+"."+decimal;
    }


}
