package com.example.firedetectionflir.service;

import com.example.firedetectionflir.model.AlertDataModel;

public interface HostService {
    void notifyFire(AlertDataModel data);
    void registerDetection(AlertDataModel data);
}
