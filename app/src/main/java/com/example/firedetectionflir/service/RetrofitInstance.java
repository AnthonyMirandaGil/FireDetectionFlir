package com.example.firedetectionflir.service;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitInstance {
    private static Retrofit retrofit = null;
    private static String BASE_URL = "http://192.168.0.11:5000/api/";
    private static String BASE_URL_RADAR = "http://192.168.1.20:5000/api/";
    private static Retrofit retrofitRadarService;

    public static AlertService getService(){
        if( retrofit == null){
            retrofit = new Retrofit
                    .Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(AlertService.class);
    }

    public static RadarService getServiceRadar(){
        if( retrofitRadarService == null){
            retrofitRadarService = new Retrofit
                    .Builder()
                    .baseUrl(BASE_URL_RADAR)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(RadarService.class);
    }
}
