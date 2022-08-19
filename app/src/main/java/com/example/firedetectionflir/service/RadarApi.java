package com.example.firedetectionflir.service;

import com.example.firedetectionflir.model.RadarDistanceModel;

;
import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.GET;

public interface RadarApi {
    @GET("distance")
    Observable<RadarDistanceModel> getRadarDistance();
}
