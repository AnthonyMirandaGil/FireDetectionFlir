package com.example.firedetectionflir.service;

import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import io.reactivex.rxjava3.core.Observable;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class RadarSocketIOService implements RadarService{

    RadarSocketIOService(){

    }
    @Override
    public Observable<Double> getDistance() {
        double distance = RPiSocketIO.getDistance();
        return Observable.just(distance);
    }
}
