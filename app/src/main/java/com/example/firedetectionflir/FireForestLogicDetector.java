package com.example.firedetectionflir;

import static org.bytedeco.opencv.global.opencv_core.CV_8UC1;
import static org.bytedeco.opencv.global.opencv_core.CV_8UC3;
import static org.bytedeco.opencv.global.opencv_core.CV_8U;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_CHAIN_APPROX_SIMPLE;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_RETR_EXTERNAL;
import static org.bytedeco.opencv.global.opencv_imgproc.*;



import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import org.bytedeco.javacpp.indexer.Indexer;
import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.MatVector;
//import org.opencv.core.Mat;
import org.bytedeco.opencv.opencv_core.Mat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class FireForestLogicDetector implements FireForestDetector{
    private float temperatureThreshold = 120.0F;
    private float areaThreshold = 1.0F;
    private final String TAG = "FireForestLogicDetector";
    public FireForestLogicDetector() {
    }

    public FireForestLogicDetector(float temperatureThreshold, float areaThreshold) {
        this.temperatureThreshold = temperatureThreshold;
        this.areaThreshold = areaThreshold;
    }

    @Override
    public boolean detectFire(Frame frame, double[] temperatures) {
        int width = 480 ;
        int heigth = 640;
        final int[] WHITE = {255, 255, 255};
        final int[] BLACK = {0, 0, 0};
        int n;
        double valueTemp;
        // Mask hot regions
        OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
        Mat matFrame = converterToMat.convert(frame);
        //Mat maskRGB = matFrame.clone(); //new Mat(heigth, width, CV_8UC3);
        //matFrame.convertTo(maskRGB, CV_8UC3);
        Mat mask = new Mat(heigth,width, CV_8UC1);
        cvtColor(matFrame, mask, CV_BGR2GRAY);

        UByteIndexer maskIndexer = mask.createIndexer();
        for (int x =0 ; x < width ; x++){
            for(int y =0; y< heigth; y++) {
                n = x + (y * width);
                valueTemp = temperatures[n];
                //int pixel = BLACK;
                if(valueTemp >= this.temperatureThreshold) {
                    maskIndexer.put(y, x, 255);
                }else {
                    maskIndexer.put(y, x, 0);
                }
            }
        }

        //Mat mask = Mat.zeros(maskRGB.size(), CV_8UC1).asMat();

        //maskRGB.convertTo(mask, CV_8UC1);


        // save mask
        AndroidFrameConverter converterToFrame = new AndroidFrameConverter();
        Frame maskFrame = converterToMat.convert(mask);
        Bitmap imageMask = converterToFrame.convert(maskFrame);

        Long timeSeconds = System.currentTimeMillis() / 10;
        String stringTs = timeSeconds.toString();
        File imageSDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        //String p = Environment.getExternalStorageDirectory(Environment.DIRECTORY_PICTURES);
        File image = new File(imageSDir,  "flir_image_mask" + stringTs + ".jpg");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(image);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        imageMask.compress(Bitmap.CompressFormat.JPEG, 100, fos);

        MatVector contours = new MatVector();
        findContours(mask, contours, CV_RETR_EXTERNAL, CV_CHAIN_APPROX_SIMPLE);
        // Calcule Areas
        //Mat dist_8u = new Mat();
        //mask.convertTo(dist_8u, CV_8U);


        Log.d(TAG,"Contrours size:" + contours.size());

        double areas [] = new double[(int) contours.size()];


        for (int i=0; i < contours.size(); i++){
            areas[i] = contourArea(contours.get(i));
        }

        // Decision Logic
        boolean exceedThresholdArea = false;

        for(int i = 0; i < areas.length ; i++){
            if (areas[i]>= this.areaThreshold)
                exceedThresholdArea = true;
            break;
        }

        return  exceedThresholdArea;
    }

    public float getTemperatureThreshold() {
        return temperatureThreshold;
    }

    public void setTemperatureThreshold(float temperatureThreshold) {
        this.temperatureThreshold = temperatureThreshold;
    }

    public float getAreaThreshold() {
        return areaThreshold;
    }

    public void setAreaThreshold(float areaThreshold) {
        this.areaThreshold = areaThreshold;
    }
}
