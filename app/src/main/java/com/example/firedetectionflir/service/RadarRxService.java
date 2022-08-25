package com.example.firedetectionflir.service;


import android.util.Log;


import com.example.firedetectionflir.model.RadarDistanceModel;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class RadarRxService {
    private RadarApi radarApi;
    private final String BASE_URL= "http://192.168.43.133:5000/api/radar/";

    public RadarRxService() {
        Retrofit retrofit = new Retrofit
                .Builder()

                .baseUrl(BASE_URL)
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        radarApi = retrofit.create(RadarApi.class);
    }

    public Observable<Double> getDistance() {
       return radarApi.getRadarDistance()
                .map(result -> mapDistance(result));
    }

    private double mapDistance(RadarDistanceModel result) {
        //Log.d("RadarRxService",  "" +  result.getDistance());
        return result.getDistance();
    }
}
