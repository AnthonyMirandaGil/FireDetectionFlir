package com.example.firedetectionflir;

import android.os.Handler;
import android.util.Log;

import com.example.firedetectionflir.model.AlertDataModel;
import com.example.firedetectionflir.service.HostService;

public class AlertService {
    final String TAG = "AlertService";
    AlertDataModel register;
    HostService hostService;
    int timeSleep = 5;
    Boolean alreadySent = false;
    Handler handler;
    public AlertDataModel getRegister() {
        return register;
    }

    public AlertService(HostService hostService) {
        this.hostService = hostService;
    }

    public void dispatch(){
        hostService.notifyFire(register);
        freezeAlertService();
    }
    public void reset(){
        register = null;
        alreadySent = false;
    }

    public void freezeAlertService(){
        alreadySent = true;
    }

    public void registerDetection(AlertDataModel alertData){
        this.register = alertData;
    }

    //private boolean hasRegister(){
    //    return false;
    //}
}