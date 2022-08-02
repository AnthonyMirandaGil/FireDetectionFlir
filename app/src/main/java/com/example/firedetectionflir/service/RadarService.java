package com.example.firedetectionflir.service;

import com.example.firedetectionflir.model.RadarDistanceModel;

import retrofit2.Call;
import retrofit2.http.GET;

public interface RadarService {
    @GET("distance")
    Call<RadarDistanceModel> getRadarDistance();
}
