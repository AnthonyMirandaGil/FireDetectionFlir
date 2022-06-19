package com.example.firedetectionflir;

import org.bytedeco.javacv.Frame;

public class FireColorDetection implements FireDetectorRGB{

    @Override
    public boolean detectFire(Frame frame) {
        return false;
    }
}
