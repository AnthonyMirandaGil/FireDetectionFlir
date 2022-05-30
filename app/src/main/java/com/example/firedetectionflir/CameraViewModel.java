package com.example.firedetectionflir;

import android.graphics.Bitmap;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class CameraViewModel extends ViewModel {
    public MutableLiveData<String> errorInfoLiveData = new MutableLiveData<String>();
    public MutableLiveData<Bitmap> bitmapLiveData = new MutableLiveData<Bitmap>();

}
