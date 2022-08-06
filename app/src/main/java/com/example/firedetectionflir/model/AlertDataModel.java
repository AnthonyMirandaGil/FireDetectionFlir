package com.example.firedetectionflir.model;

public class AlertDataModel {
    String maxTemperature;
    String position;
    String distance;
    String time;
    double areaFire;

    public AlertDataModel(){

    }
    public AlertDataModel(String maxTemperature, String position, String distance, String time, double areaFire) {
        this.maxTemperature = maxTemperature;
        this.position = position;
        this.distance = distance;
        this.time = time;
        this.areaFire = areaFire;
    }

    public String getMaxTemperature() {
        return maxTemperature;
    }

    public void setMaxTemperature(String maxTemperature) {
        this.maxTemperature = maxTemperature;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
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
