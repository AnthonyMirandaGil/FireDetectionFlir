package com.example.firedetectionflir.service;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ServiceInstance {
    private static Retrofit retrofit = null;
    private static String BASE_URL = "http://192.168.0.11:5000/api/";
    private static  RadarService radarRxService;
    private static  RadarService radarSocketIoService;

    public static AlertApi getService(){
        if( retrofit == null){
            retrofit = new Retrofit
                    .Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(AlertApi.class);
    }

    public static RadarService getServiceRxRadar(){
        if(radarRxService == null){
            radarRxService = new RadarRxService();
        }
        return radarRxService;
    }

    public static  RadarService getRadarSocketIOService(){
        if(radarSocketIoService == null){
            radarSocketIoService = new RadarSocketIOService();
        }
        return radarSocketIoService;
    }
}
