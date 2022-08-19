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
   // RGB camera fov
    final double hfov = 50;
    final double vfov = 38;
    final double Sw = 0.620;
    final double Sh = 0.6136;
    final int width = 480 ;
    final int heigth = 640;
    final int frameRate = 5;
    // to thermal image scel
    public FireForestLogicDetector() {

    }

    private double areaFire;

    public FireForestLogicDetector(float temperatureThreshold, float areaThreshold) {
        this.temperatureThreshold = temperatureThreshold;
        this.areaThreshold = areaThreshold;
    }

    public boolean detectFire(Frame frame, double[] temperatures, double altura ){
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

        int nRegions = (int) contours.size();
        Log.d(TAG,"Contrours size:" + nRegions);
        if (nRegions  == 0)
            return false;

        double areas [] = new double[nRegions];

        for (int i=0; i < contours.size(); i++){
            double areaPixels = contourArea(contours.get(i));
            areas[i] = this.convertAreaMeters(areaPixels, altura);
        }

        Log.d(TAG,"Area size11:" + areas[0]);
        // Decision Logic
        boolean exceedThresholdArea = false;

        for(int i = 0; i < areas.length ; i++){
            if (areas[i]>= this.areaThreshold)
                exceedThresholdArea = true;
            areaFire = this.getMaxArea(areas);
            break;
        }
        Log.d(TAG,"Area size22:" + areas[0]);
        return  exceedThresholdArea;
    }

    @Override
    public boolean detectFire(Frame frame, double[] temperatures) {
        double prevTime = 0.0;
        double time_elapsed = System.currentTimeMillis() - prevTime;
        if (time_elapsed > (1.0 / frameRate)) {
            prevTime = System.currentTimeMillis();
            // Process image
        }
        return  false;
    }

    private double getMaxArea(double areas []){
        double maxArea = 0.0;
        for(double area : areas){
            if(area > maxArea){
                maxArea = area;
                maxArea = Math.round(maxArea * 100.0) / 100.0;
            }
        }
        return maxArea;
    }

    public double getAreaFire() {
        return areaFire;
    }

    public double convertAreaMeters(double areaPixels, double altura){
        // RGB images distances
        double Vdh = 2 * altura * Math.tan(0.5 * hfov * (Math.PI / 180.0));
        double Vdv = 2 * altura * Math.tan(0.5 * vfov * (Math.PI / 180.0));
        // Scale to thermal distances
        double Tdh = Vdh * Sh;
        double Tdv = Vdv * Sw;

        Log.d(TAG,"Tdh:" + Tdh);
        Log.d(TAG,"Tdv:" + Tdv);
        double pixelSize = (Tdv * Tdh) / (heigth * width);
        Log.d(TAG,"pixelSize:" + pixelSize);
        double areaMeters = areaPixels * pixelSize;
        Log.d(TAG,"areaPixels:" + areaPixels);
        Log.d(TAG,"areaMeters:" + areaMeters);
        return areaMeters;
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
