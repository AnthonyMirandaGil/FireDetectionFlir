package com.example.firedetectionflir.service;


import com.example.firedetectionflir.model.AlertDataModel;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;

public class DesktopHost implements HostService {
    private Socket mSocket;
    //private String HOST_URL = "http://192.168.43.184:5000";

    //DesktopHost(String host_url){
    //    this.HOST_URL = host_url;
    //}

    public DesktopHost(){}

    public void connect(){
        mSocket = RPiSocketIO.getConnection();
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

    @Override
    public void notifyFire(AlertDataModel data) {
        Gson gson = new Gson();
        try {
            JSONObject Obj = new JSONObject(gson.toJson(data));
            mSocket.emit("fireDetected", Obj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void registerDetection(AlertDataModel data) {

    }
}
