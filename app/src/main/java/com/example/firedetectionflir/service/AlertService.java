package com.example.firedetectionflir.service;

import com.example.firedetectionflir.model.AlertDataModel;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AlertService {
    @POST("alert")
    Call<AlertDataModel> PostAlert(@Body AlertDataModel alertDataModel);
}
