package com.example.firedetectionflir.service;

import io.reactivex.rxjava3.core.Observable;

public interface RadarService {
    Observable<Double> getDistance();
    boolean isActive();
}
