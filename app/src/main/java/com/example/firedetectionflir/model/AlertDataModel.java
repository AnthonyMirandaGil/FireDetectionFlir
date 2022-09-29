package com.example.firedetectionflir.model;

import com.example.firedetectionflir.FireForestLogicDetector;

public class AlertDataModel {
    String maxTemperature;
    String distance;
    String time;
    double areaFire;

    FireForestLogicDetector.LevelAlert levelAlert;


    public double getAreaFire() {
        return areaFire;
    }

    public void setAreaFire(double areaFire) {
        this.areaFire = areaFire;
    }

    public double getLatitud() {
        return latitud;
    }

    public void setLatitud(double latitud) {
        this.latitud = latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public void setLongitud(double longitud) {
        this.longitud = longitud;
    }

    double latitud;
    double longitud;

    public AlertDataModel(){

    }

    public AlertDataModel(String maxTemperature, double latitud, double longitud , String distance, String time, double areaFire, FireForestLogicDetector.LevelAlert levelAlert) {
        this.maxTemperature = maxTemperature;
        this.distance = distance;
        this.time = time;
        this.areaFire = areaFire;
        this.longitud = longitud;
        this.latitud = latitud;
        this.levelAlert = levelAlert;
    }

    public String getMaxTemperature() {
        return maxTemperature;
    }

    public void setMaxTemperature(String maxTemperature) {
        this.maxTemperature = maxTemperature;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
