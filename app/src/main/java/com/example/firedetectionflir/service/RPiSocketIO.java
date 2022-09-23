package com.example.firedetectionflir.service;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public final class RPiSocketIO {
    //private static  RPiSocketIO rPiSocketIO;
    private static Socket mSocket;
    private static String HOST_URL = "http://192.168.43.133:5055";
    private RPiSocketIO(){ }
    private static final String Tag = "RPiSocketIO";
    private static double distance = 0.0;

    private static void connect(){
        try {
            mSocket = IO.socket(HOST_URL);
        } catch (URISyntaxException e) {
            Log.d(Tag, "Error: " + e.getMessage());
            e.printStackTrace();
        }

        mSocket.on("radarDistance", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(Tag, "Distance Lecture");
                JSONObject data = (JSONObject) args[0];
                Double radarDistance;
                try {
                    radarDistance = data.getDouble("distance");
                    distance = radarDistance;
                    Log.d(Tag, "Radar Distance: " + radarDistance);
                } catch (JSONException e){
                    Log.d(Tag, e.getMessage());
                }
            }
        });

        mSocket.connect();

        if(mSocket.connected()){
            Log.d(Tag, "Estamos Ready!!!");
        }else {
            Log.d(Tag, "No estamos ready :(");
        }
    }

    public static double getDistance(){
        return distance;
    }

    public static Socket getConnection(){
        if(mSocket  == null || !mSocket.connected()){
            connect();
        }
        return mSocket;
    }
}
