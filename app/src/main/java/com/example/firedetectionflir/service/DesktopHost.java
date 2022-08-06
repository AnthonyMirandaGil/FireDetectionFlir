package com.example.firedetectionflir.service;


import com.example.firedetectionflir.model.AlertDataModel;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;

public class DesktopHost {
    private Socket mSocket;
    private String HOST_URL = "http://192.168.13.109:5000";

    DesktopHost(String host_url){
        this.HOST_URL = host_url;
    }

    public DesktopHost(){}

    public void connect(){
        try {
            mSocket = IO.socket(HOST_URL);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        mSocket.connect();
    }
    public void alertFire(AlertDataModel data){
        Gson gson = new Gson();
        try {
            JSONObject Obj = new JSONObject(gson.toJson(data));
            mSocket.emit("fireDetected", Obj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void notifyStartFireDetection(){
        mSocket.emit("startFireDetection", "Comienzar Detecction");
    }

    public void notifyStopFireDetection(){
        mSocket.emit("stopFireDetection", "Detener Detecction");
    }
}
