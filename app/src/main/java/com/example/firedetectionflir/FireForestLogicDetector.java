package com.example.firedetectionflir;

import static org.bytedeco.opencv.global.opencv_core.CV_8UC1;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_CHAIN_APPROX_SIMPLE;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_RETR_EXTERNAL;
import static org.bytedeco.opencv.global.opencv_imgproc.*;



import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import com.example.firedetectionflir.service.RadarRxService;
import com.example.firedetectionflir.service.ServiceInstance;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class FireForestLogicDetector implements FireForestDetector{
    private float temperatureThreshold = 50.0F;
    private float areaThreshold = 0.8F;
    private final String TAG = "FireForestLogicDetector";
   // RGB camera fov
    final double hfov = 50;
    final double vfov = 38;
    final double Sw = 0.620;
    final double Sh = 0.6136;
    final int width = 480 ;
    final int heigth = 640;
    final int frameRate = 2;
    final double droneSpeed = 30;
    private Frame currentFrame;
    private double currentHeigth = 0.0;
    private double gridHeigth = 0.5;
    private double prevTime = 0.0;
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

        // Decision Logic
        boolean exceedThresholdArea = false;

        for(int i = 0; i < areas.length ; i++){
            if (areas[i]>= this.areaThreshold)
                exceedThresholdArea = true;
            areaFire = this.getMaxArea(areas);
            break;
        }
        return  exceedThresholdArea;
    }

    private Observable<Double> estimationMaxAltura(long numSamples, long period){
        List<ObservableSource<Double>> observables = new ArrayList<>();
        RadarRxService radarRxService = ServiceInstance.getServiceRadar();

        for(int i = 0; i< numSamples; i++) {
            observables.add(Observable.timer(period * i, TimeUnit.MILLISECONDS)
                    .flatMap(new Function<Long, ObservableSource<Double>>() {
                        @Override
                        public ObservableSource<Double> apply(Long aLong) throws Throwable {
                            return radarRxService.getDistance();
                        }
                    }));
        }

        return  Observable.zip(observables, new Function<Object[], Double>() {
            @Override
            public Double apply(Object[] objects) throws Throwable {
                Double [] distances = Arrays.copyOf(objects, objects.length, Double[].class);
                Double maxDistance = Double.NEGATIVE_INFINITY;

                for (int i = 0; i < distances.length ; i++){
                    if (distances[i] > maxDistance){
                        maxDistance = distances[i];
                    }
                    //sumDistances += distances[i];
                }
                return maxDistance;
            }
        }).observeOn(Schedulers.io());
    }

    private Observable<Double> estimationMeanAltura(int numSamples, long period){
        List<ObservableSource<Double>> observables = new ArrayList<>();
        RadarRxService radarRxService = ServiceInstance.getServiceRadar();

        for(int i = 0; i< numSamples; i++) {
            observables.add(Observable.timer( period * i, TimeUnit.MILLISECONDS)
                    .flatMap(new Function<Long, ObservableSource<Double>>() {
                        @Override
                        public ObservableSource<Double> apply(Long aLong) throws Throwable {
                            return radarRxService.getDistance();
                        }
                    }));
        }
        return  Observable.zip(observables, new Function<Object[], Double>() {
            @Override
            public Double apply(Object[] objects) throws Throwable {
                Double [] distances = Arrays.copyOf(objects, objects.length, Double[].class);
                Double sumAlturas = 0.0;
                for (int i = 0; i < distances.length ; i++){
                    sumAlturas += distances[i];
                }
                return sumAlturas;
            }
        }).observeOn(Schedulers.io());
    }

    private int calculateNumSampleRadar(double altura){
        double Vdh = 2 * altura * Math.tan(0.5 * hfov * (Math.PI / 180.0));
        /* Scale to thermal distances */
        double Tdh = Vdh * Sh;
        int numSamplesRadar = (int) (Tdh / gridHeigth);
        return numSamplesRadar;
    }


    @Override
    public Observable<Boolean> detectFire(Frame frame, double[] temperatures) {
        Observable<Double> estimationAltura;

        double time_elapsed = System.currentTimeMillis() - prevTime;
        double overlap = 0.75;
        if (time_elapsed > ((overlap * 1.0) / frameRate) * 1000) {
            prevTime = System.currentTimeMillis();
            // Update Frame
            currentFrame = frame;
            Log.d("CameraFragment", "time_elapsed: " + time_elapsed + "ms" );
            if (currentHeigth == 0.0){
                estimationAltura = estimationMeanAltura(3, 50)
                        .flatMap(new Function<Double, Observable<Double>>() {
                            @Override
                            public Observable<Double> apply(Double meanDistance) throws Throwable {

                                int numSamplesRadar = calculateNumSampleRadar(meanDistance);
                                long intervalPeriod = (long) (1.0 / frameRate) * (1 / numSamplesRadar);

                                return estimationMaxAltura(numSamplesRadar, intervalPeriod);
                            }
                        });

            } else {
                int numSamplesRadar = calculateNumSampleRadar(currentHeigth);
                long intervalPeriod = (long) (1.0 / frameRate) * (1 / numSamplesRadar);
                estimationAltura = estimationMaxAltura(numSamplesRadar, intervalPeriod);
            }

           return estimationAltura
                    .map(new Function<Double, Boolean>() {
                        @Override
                        public Boolean apply(Double altura) throws Throwable {
                            boolean fire = detectFire(currentFrame, temperatures, altura);
                            Log.d("CameraFragment", "Result Detection: " +  Boolean.toString(fire) );
                            return fire;
                    }});

        }
        return  Observable.just(false);
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
