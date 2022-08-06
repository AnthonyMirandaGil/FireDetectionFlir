package com.example.firedetectionflir;

import org.bytedeco.javacv.Frame;

public interface FireForestDetector {

    boolean detectFire(Frame frame, double[] temperatures, double distance);
    double getAreaFire();
}
