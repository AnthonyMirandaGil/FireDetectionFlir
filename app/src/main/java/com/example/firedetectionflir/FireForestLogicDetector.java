package com.example.firedetectionflir;

import static org.bytedeco.opencv.global.opencv_core.CV_8UC1;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_CHAIN_APPROX_SIMPLE;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_RETR_EXTERNAL;
import static org.bytedeco.opencv.global.opencv_imgproc.*;


import android.util.Log;

import com.example.firedetectionflir.service.RadarService;
import com.example.firedetectionflir.service.ServiceInstance;

import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.MatVector;
//import org.opencv.core.Mat;
import org.bytedeco.opencv.opencv_core.Mat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class FireForestLogicDetector implements FireForestDetector{
    private double temperatureThreshold = 50.0;
    private double areaThreshold = 0.01;
    private final String TAG = "FireForestLogicDetector";
   // RGB camera fov
    final double hfov = 38;
    final double vfov = 50;
    final double Sw = 0.620;
    final double Sh = 0.6136;
    final int width = 480 ;
    final int heigth = 640;
    private double frameRate = 2;
    private double flightSpeed;
    private double flightHeight;
    private double overlap;
    private Frame currentFrame;
    private double Dv;
    private double currentAltura = 0.0;
    private double gridHeigth = 1.0;
    private double prevTime = 0.0;
    private double areaFire;

    public int getNumCurrentHotRegions() {
        return numCurrentHotRegions;
    }

    private int numCurrentHotRegions;
    public enum LevelAlert {
        ORANGE,
        RED
    }
    private LevelAlert levelAlert;

    public LevelAlert getLevelAlert() {
        return levelAlert;
    }

    public double getCurrentAltura() {
        return currentAltura;
    }


    // to thermal image scel
    public FireForestLogicDetector() {
        this.overlap = 0.75;
        // Initial Fps
        this.frameRate = calculateFps(flightHeight);
    }

    public FireForestLogicDetector(double flightSpeed,  double flightHeight) {
        this.overlap = 0.75;
        this.flightSpeed = flightSpeed;
        this.flightHeight = flightHeight;
        // Initial Fps
        this.frameRate = calculateFps(this.flightHeight);
    }

    public FireForestLogicDetector(float temperatureThreshold, float areaThreshold) {
        this.temperatureThreshold = temperatureThreshold;
        this.areaThreshold = areaThreshold;
        this.overlap = 0.75;
        // Initial Fps
        this.frameRate = calculateFps(flightHeight);
    }

    private double calculateFps(double altura){
        double dv =  calculateLongitudeVFov(altura);
        return  flightSpeed / ((1 - overlap) * dv);
    }

    private double calculateLongitudeVFov(double altura){
        double Vdv = 2 * altura * Math.tan(0.5 * vfov * (Math.PI / 180.0));

        double Tdv = Vdv * Sh;
        return Tdv;
    }

    public boolean detectFire(Frame frame, double[] temperatures, double altura ){
        final int[] WHITE = {255, 255, 255};
        final int[] BLACK = {0, 0, 0};
        int n;
        double valueTemp;
        // Mask hot regions
        Log.d(TAG, "Analizando, altura:" + altura);

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

        /*Bitmap imageMask = converterToFrame.convert(maskFrame);
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
        */

        MatVector contours = new MatVector();
        findContours(mask, contours, CV_RETR_EXTERNAL, CV_CHAIN_APPROX_SIMPLE);

        // Calcule Areas
        //Mat dist_8u = new Mat();
        //mask.convertTo(dist_8u, CV_8U);

        int nRegions = (int) contours.size();
        Log.d(TAG,"Num regions:" + nRegions);
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
            break;
        }


        // Logs
        areaFire = this.getMaxArea(areas);

        if (areaFire > areaThreshold ) {
            exceedThresholdArea = true;
        }

        Log.d(TAG, "exceedThresholdArea: " + Boolean.toString(exceedThresholdArea));

        if (areaFire >= 0.25 && areaFire < 1.0){
            levelAlert = LevelAlert.ORANGE;
        }else if (areaFire > 1.0) {
            levelAlert = LevelAlert.RED;
        }

        numCurrentHotRegions = areas.length;
        currentAltura = altura;

        return  exceedThresholdArea;
    }

    private boolean radarServiceIsActive(){
        RadarService radarService = ServiceInstance.getRadarSocketIOService();
        return  radarService.isActive();
    }

    private Observable<Double> estimationMaxAltura(long numSamples, long period){
        List<ObservableSource<Double>> observables = new ArrayList<>();
        RadarService radarService = ServiceInstance.getRadarSocketIOService();

        for(int i = 0; i< numSamples; i++) {
            observables.add(Observable.timer(period * i, TimeUnit.MILLISECONDS)
                    .flatMap(new Function<Long, ObservableSource<Double>>() {
                        @Override
                        public ObservableSource<Double> apply(Long aLong) throws Throwable {
                            return radarService.getDistance();
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
        RadarService radarService = ServiceInstance.getRadarSocketIOService();

        for(int i = 0; i< numSamples; i++) {
            observables.add(Observable.timer( period * i, TimeUnit.MILLISECONDS)
                    .flatMap(new Function<Long, ObservableSource<Double>>() {
                        @Override
                        public ObservableSource<Double> apply(Long aLong) throws Throwable {
                            return radarService.getDistance();
                        }
                    }));
        }
        
        return  Observable.zip(observables, new Function<Object[], Double>() {
            @Override
            public Double apply(Object[] objects) throws Throwable {
                Double [] distances = Arrays.copyOf(objects, objects.length, Double[].class);

                Double sumAlturas = 0.0;
                int numNZeros = 0;

                for (int i = 0; i < distances.length ; i++){
                    if (distances[i] != 0.0) {
                        numNZeros += 1;
                        sumAlturas += distances[i];
                    }
                }

                if (numNZeros == 0)
                    return 0.0;

                double meanAltura = sumAlturas / (double) numNZeros;

                Log.d(TAG,"Mean Altura:" + meanAltura);
                return  meanAltura;
            }
        }).observeOn(Schedulers.io());
    }

    private int calculateNumSampleRadar(double altura){
        double Vdh = 2 * altura * Math.tan(0.5 * hfov * (Math.PI / 180.0));
        /* Scale to thermal distances */
        double Tdh = Vdh * Sh;
        int numSamplesRadar = (int) Math.ceil(Tdh / gridHeigth);
        return numSamplesRadar;
    }


    @Override
    public Observable<Boolean> detectFire(Frame frame, double[] temperatures) {
        Observable<Double> estimationAltura;

        double time_elapsed = System.currentTimeMillis() - prevTime;
        if (time_elapsed > ( 1 / frameRate) * 1000) {
            prevTime = System.currentTimeMillis();
            // Update Frame
            currentFrame = frame;
            Log.d("CameraFragment", "time_elapsed: " + time_elapsed + "ms");
            boolean radarIsActive = radarServiceIsActive();
            Log.d(TAG,"Radar status: " +  Boolean.toString(radarIsActive));
            if (radarIsActive) {
                if (currentAltura == 0.0) {
                    currentAltura = flightHeight;
                    estimationAltura = estimationMeanAltura(3, 50)
                            .flatMap(new Function<Double, Observable<Double>>() {
                                @Override
                                public Observable<Double> apply(Double meanDistance) throws Throwable {
                                    double newAltura = currentAltura;
                                    if (meanDistance != null && meanDistance != 0.0) {
                                        newAltura = meanDistance;
                                        currentAltura = meanDistance;
                                    }
                                    int numSamplesRadar = calculateNumSampleRadar(newAltura);
                                    long intervalPeriod = (long) (1.0 / frameRate) * (1 / numSamplesRadar);
                                    return estimationMaxAltura(numSamplesRadar, intervalPeriod);
                                }
                            });
                } else {
                    double altura = flightHeight;
                    if (currentAltura != 0.0)
                        altura = currentAltura;
                    int numSamplesRadar = calculateNumSampleRadar(altura);
                    long intervalPeriod = (long) (1.0 / frameRate) * (1 / numSamplesRadar);
                    estimationAltura = estimationMaxAltura(numSamplesRadar, intervalPeriod);
                }

                return estimationAltura
                        .map(new Function<Double, Boolean>() {
                            @Override
                            public Boolean apply(Double altura) throws Throwable {
                                if (altura != 0.0) {
                                    currentAltura = altura;
                                }
                                boolean fire = detectFire(currentFrame, temperatures, currentAltura);
                                Log.d("CameraFragment", "Result Detection: " + Boolean.toString(fire));
                                return fire;
                            }
                        });
            } else {
                if (currentAltura == 0.0) {
                    currentAltura = flightHeight;
                }
                boolean fire = detectFire(currentFrame, temperatures, currentAltura);
                return Observable.just(fire);
            }
        }

        return  Observable.just(false);
    }

    private double getMaxArea(double areas []){
        double maxArea = 0.0;
        for(double area : areas){
            if(area > maxArea){
                maxArea = area;
                maxArea = Math.round(maxArea * 10000.0) / 10000.0;
            }
        }
        return maxArea;
    }

    public double getAreaFire() {
        return areaFire;
    }

    @Override
    public void setFlightSpeed(double flightSpeed) {
        this.flightSpeed = flightSpeed;
    }

    @Override
    public void setFligtHeight(double fligtHeight) {
        this.flightHeight = fligtHeight;
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

    public double getTemperatureThreshold() {
        return temperatureThreshold;
    }

    public void setTemperatureThreshold(float temperatureThreshold) {
        this.temperatureThreshold = temperatureThreshold;
    }

    public double getAreaThreshold() {
        return areaThreshold;
    }

    public void setAreaThreshold(float areaThreshold) {
        this.areaThreshold = areaThreshold;
    }
}
