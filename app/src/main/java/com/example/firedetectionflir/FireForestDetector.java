package com.example.firedetectionflir;

import org.bytedeco.javacv.Frame;

import io.reactivex.rxjava3.core.Observable;

public interface FireForestDetector {

    Observable<Boolean> detectFire(Frame frame, double[] temperatures);
    double getAreaFire();
    void setFlightSpeed(double flightSpeed);
    void setFligtHeight(double fligtHeight);
}
