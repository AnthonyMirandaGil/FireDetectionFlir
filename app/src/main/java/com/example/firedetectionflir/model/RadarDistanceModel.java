package com.example.firedetectionflir.model;

public class RadarDistanceModel {
    double distance;

    public RadarDistanceModel(){

    }

    public RadarDistanceModel(double distance) {
        this.distance = distance;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }
}
