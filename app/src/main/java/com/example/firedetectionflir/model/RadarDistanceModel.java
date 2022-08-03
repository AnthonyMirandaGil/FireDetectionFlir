package com.example.firedetectionflir.model;

public class RadarDistanceModel {
    String distance;

    public RadarDistanceModel(){

    }

    public RadarDistanceModel(String distance) {
        this.distance = distance;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }
}
