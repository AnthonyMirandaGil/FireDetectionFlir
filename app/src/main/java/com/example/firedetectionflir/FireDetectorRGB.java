package com.example.firedetectionflir;

import org.bytedeco.javacv.Frame;

public interface FireDetectorRGB {
    boolean detectFire(Frame frame);
}
