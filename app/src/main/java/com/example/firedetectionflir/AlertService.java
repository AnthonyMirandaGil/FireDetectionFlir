package com.example.firedetectionflir;

import android.os.Handler;
import android.util.Log;

import com.example.firedetectionflir.model.AlertDataModel;
import com.example.firedetectionflir.service.HostService;

public class AlertService {
    final String TAG = "AlertService";
    AlertDataModel register;
    HostService hostService;
    int timeSleep = 30;
    Boolean alreadySent = false;

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

    private void freezeAlertService(){
        alreadySent = true;
        Handler handler = new Handler();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                alreadySent = false;
                Log.d(TAG, "New Alert");
            }
        }, timeSleep * 1000);
    }

    public void registerDetection(AlertDataModel alertData){
        this.register = alertData;
    }

    //private boolean hasRegister(){
    //    return false;
    //}
}