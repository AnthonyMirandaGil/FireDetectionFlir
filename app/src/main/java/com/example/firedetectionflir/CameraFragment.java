package com.example.firedetectionflir;

import static com.example.firedetectionflir.utils.Utils.maxArray;
import static org.bytedeco.opencv.global.opencv_imgproc.FONT_HERSHEY_COMPLEX;
import static org.bytedeco.opencv.global.opencv_imgproc.LINE_AA;
import static org.bytedeco.opencv.global.opencv_imgproc.circle;
import static org.bytedeco.opencv.global.opencv_imgproc.putText;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Toast;

import com.example.firedetectionflir.databinding.FragmentCameraBinding;
import com.example.firedetectionflir.model.AlertDataModel;
import com.example.firedetectionflir.service.AlertApi;
import com.example.firedetectionflir.service.DesktopHost;
import com.example.firedetectionflir.service.RadarRxService;
import com.example.firedetectionflir.service.RadarService;
import com.example.firedetectionflir.service.ServiceInstance;
import com.flir.thermalsdk.ErrorCode;
import com.flir.thermalsdk.androidsdk.image.BitmapAndroid;
import com.flir.thermalsdk.androidsdk.live.connectivity.UsbPermissionHandler;
import com.flir.thermalsdk.image.ImageBuffer;
import com.flir.thermalsdk.image.Rectangle;
import com.flir.thermalsdk.image.TemperatureUnit;
import com.flir.thermalsdk.image.ThermalImage;
import com.flir.thermalsdk.image.ThermalValue;
import com.flir.thermalsdk.image.fusion.FusionMode;
import com.flir.thermalsdk.image.palettes.Palette;
import com.flir.thermalsdk.image.palettes.PaletteManager;
import com.flir.thermalsdk.live.Camera;
import com.flir.thermalsdk.live.ConnectParameters;
import com.flir.thermalsdk.live.Identity;
import com.flir.thermalsdk.live.connectivity.ConnectionStatusListener;
import com.flir.thermalsdk.live.remote.OnReceived;
import com.flir.thermalsdk.live.remote.OnRemoteError;
import com.flir.thermalsdk.live.streaming.Stream;
import com.flir.thermalsdk.live.streaming.ThermalStreamer;
import com.flir.thermalsdk.utils.Consumer;

import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Scalar;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.reactivex.rxjava3.disposables.Disposable;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CameraFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class CameraFragment extends Fragment {
    private Identity identity;
    private FragmentCameraBinding fragmentCameraBinding;
    private CameraViewModel cameraViewModel;
    private Camera camera;
    private Palette currentPallete;
    private ThermalStreamer streamer;
    private VideoRecorder videoRecorder;
    private final int REQUEST_PERMISSION_RECORD = 104;
    private final int REQUEST_PERMISSION_WRITE = 105;
    private ArrayList<String> paletteNames = new ArrayList<>();
    private ArrayAdapter<String> palletesAdapter;
    private ArrayList<String> fusionModes = new ArrayList<>();
    private ArrayAdapter<String> fusionModeAdapter;
    private FusionMode currentFusionMode = FusionMode.THERMAL_ONLY;
    private UsbPermissionHandler usbPermissionHandler = new UsbPermissionHandler();
    private Thread rafagaThread;
    private Handler handler = new Handler();
    private Handler handlerSleep = new Handler();
    private int delay = 50;
    private int fpsTake;
    private final String TAG = "CameraFragment";
    private Boolean saveTemperature = false;
    private final double TEMPERATURE_THRESHOLD = 25.0;
    private Boolean alertSent = false;
    private FireForestDetector fireForestDetector;
    private DesktopHost desktopHost;
    private LocationGPSTracker locationGPSTracker;
    private RadarRxService radarRxService;
    private Disposable disposable;
    Runnable runnable;
    private int seconds;
    private int num_detecs = 0;
    private AlertService fireAlertService;
    private double fligthSpeed = 30.0;
    private double fligthHeight = 10;
    private RadarService radarService;
    private Boolean activatedDetectionFire = false;

    private final String [] recordPermissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
    };

    private final String [] writePermissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static final String[] LOCATION_PERMS= {
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    private static final int LOCATION_REQUEST= 108;


    public static CameraFragment newInstance(Identity identity) {
        CameraFragment fragment = new CameraFragment(identity);
        return fragment;
    }


    public CameraFragment() {
        // Required empty public constructor
    }

    public CameraFragment(Identity identity){
        this.identity = identity;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false);
        cameraViewModel = new ViewModelProvider(this).get(CameraViewModel.class);

        cameraViewModel.bitmapLiveData.observe(getViewLifecycleOwner(), new Observer<Bitmap>() {
            @Override
            public void onChanged(Bitmap bitmap) {
                fragmentCameraBinding.streamingView.setImageBitmap(bitmap);
            }
        });

        //fragmentCameraBinding.status.setText("Connected to: " + identity.deviceId);

        //cameraViewModel.errorInfoLiveData.observe(getViewLifecycleOwner(), new Observer<String>() {
        //    @Override
        //    public void onChanged(String s) {
        //       fragmentCameraBinding.status.setText(s);
        //    }
        //});

        fragmentCameraBinding.btnTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyRequiredPermissionsWrite();
                takePicture(true);
            }
        });

        fragmentCameraBinding.recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyRequiredPermissionsRecord();

                if(videoRecorder == null || videoRecorder.recording == false) {
                    Toast.makeText(getContext(),"Start Video", Toast.LENGTH_SHORT).show();
                    videoRecorder  = new VideoRecorder(getContext());
                    // Store Data Temperatures

                    try {
                        videoRecorder.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    fragmentCameraBinding.recordButton.setText("Stop Video");
                } else {
                    Toast.makeText(getContext(),"Stop Video", Toast.LENGTH_SHORT).show();
                    videoRecorder.stop();
                    videoRecorder = null;
                    fragmentCameraBinding.recordButton.setText("Start Video");

                }
            }
        });

        int numPalettes = PaletteManager.getDefaultPalettes().size();

        for (int i = 0; i < numPalettes; i++ ){
            String paletteName = PaletteManager.getDefaultPalettes().get(i).name.toLowerCase(Locale.ROOT);
            paletteNames.add(paletteName);
        }
        palletesAdapter = new ArrayAdapter<String>(getContext(), androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, paletteNames);

        fragmentCameraBinding.spinnerPalette.setAdapter(palletesAdapter);

        fragmentCameraBinding.spinnerPalette.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    currentPallete = PaletteManager.getDefaultPalettes().get(position);
            };

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentPallete = PaletteManager.getDefaultPalettes().get(0);
            }
        });

        fusionModes = new ArrayList<String>(Arrays.asList(new String[]{
                "Thermal Only",
                "Visual Only",
                "Blending",
                "MSX",
                "Thermal Fusion",
                "Pinture in picture",
                "Color nigth vision"}));

        fusionModeAdapter = new ArrayAdapter<String>(getContext(), androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, fusionModes);
        fragmentCameraBinding.spinnerFusion.setAdapter(fusionModeAdapter);

        fragmentCameraBinding.spinnerFusion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                FusionMode fusionMode = FusionMode.THERMAL_ONLY;
                switch (position){
                    case 0:
                        fusionMode = FusionMode.THERMAL_ONLY;
                        break;
                    case 1:
                        fusionMode = FusionMode.VISUAL_ONLY;
                        break;
                    case 2:
                        fusionMode = FusionMode.BLENDING;
                        break;
                    case 3:
                        fusionMode = FusionMode.MSX;
                        break;
                    case 4:
                        fusionMode = FusionMode.THERMAL_FUSION;
                        break;
                    case 5:
                        fusionMode = FusionMode.PICTURE_IN_PICTURE;
                        break;
                    case 6:
                        fusionMode = FusionMode.COLOR_NIGHT_VISION;
                        break;
                    default:
                        fusionMode = FusionMode.THERMAL_ONLY;
                        break;
                }

                currentFusionMode = fusionMode;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentFusionMode = FusionMode.THERMAL_ONLY;
            }
        });


                /*fragmentCameraBinding.enableRafaga.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    Toast.makeText(getContext(), "Start automatic capture in 3 seconds", Toast.LENGTH_SHORT).show();
                    //waitDelay();

                    handlerSleep.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getContext(), "Starting automatic capture every " + delay / 1000.0 + " seconds", Toast.LENGTH_SHORT).show();
                            handler.postDelayed(runnable = new Runnable() {
                                @Override
                                public void run() {
                                    takePicture(false);
                                    handler.postDelayed(this, delay);
                                }
                            }, delay);
                        }
                    }, 3000);
                } else {
                    handler.removeCallbacks(runnable);
                    handlerSleep.removeCallbacks(runnable);
                    Toast.makeText(getContext(), "Stop automatic capture", Toast.LENGTH_SHORT).show();
                }
            }
        });
        */
        /*fragmentCameraBinding.seekBar.setMax(30);
       */
        //fragmentCameraBinding.seekBar.setMin(1);
        /*fragmentCameraBinding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                fpsTake = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                delay = 1000 / fpsTake;
                Toast.makeText(getContext(),"FPS: " + fpsTake, Toast.LENGTH_SHORT).show();
            }
        });
        */

        fragmentCameraBinding.alertBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(activatedDetectionFire == false) {
                    runTimer();
                    double latitud = locationGPSTracker.getLatitude();
                    double longitude = locationGPSTracker.getLongitude();

                    Log.d(TAG,  "Longitude:" + longitude);
                    Log.d(TAG,  "latitud:" + latitud);

                    num_detecs = 0;
                    activatedDetectionFire = true;

                    String strSpeed = fragmentCameraBinding.editTextVelocity.getText().toString();
                    double speed = 30.0;
                    String strHeight = fragmentCameraBinding.editTextAltura.getText().toString();
                    double height = 10;
                    if (strSpeed != null && strSpeed != ""){
                        fligthSpeed = Double.parseDouble(strSpeed);
                        Log.d(TAG, "FlightSpeed: " + fligthSpeed);
                    }


                    if (strHeight != null && strHeight != ""){
                        fligthHeight = Double.parseDouble(strHeight);
                        Log.d(TAG, "FligtHeight: " + fligthHeight);
                    }

                    fireForestDetector = new FireForestLogicDetector(fligthSpeed, fligthHeight);

                    desktopHost.notifyStartFireDetection();
                    fragmentCameraBinding.alertBtn.setText("Stop Fire Detection");
                } else {
                    /*disposable.dispose();*/
                    activatedDetectionFire = false;
                    desktopHost.notifyStopFireDetection();

                    fragmentCameraBinding.alertBtn.setText("Start Fire Detection");
                }
            }
        });
        // Default fire forest
        fireForestDetector = new FireForestLogicDetector(fligthSpeed, fligthHeight);
        radarService = ServiceInstance.getRadarSocketIOService();

        // Connect to server in desktop app
        desktopHost = new DesktopHost();
        desktopHost.connect();

        // GPS tracker
        if(ActivityCompat.checkSelfPermission( getContext(),Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED) {
            locationGPSTracker = new LocationGPSTracker(getActivity());
        }else {
            ActivityResultLauncher<String[]> locationPermissionRequest =
                    registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
                        @RequiresApi(api = Build.VERSION_CODES.N)
                        @Override
                        public void onActivityResult(Map<String, Boolean> result) {
                            Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                            Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                            if(fineLocationGranted != null && fineLocationGranted){
                                locationGPSTracker = new LocationGPSTracker(getActivity());
                            }else if (coarseLocationGranted != null && coarseLocationGranted){
                                Toast.makeText(getContext(), "Solo Aproximada localizacion garantizada", Toast.LENGTH_SHORT).show();
                                //locationGPSTracker = new LocationGPSTracker(getActivity());
                            }else {
                                Toast.makeText(getContext(), "Gps Permision es requerido", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

            locationPermissionRequest.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }

          //requestPermissions(LOCATION_PERMS, LOCATION_REQUEST);

        fireAlertService = new AlertService(desktopHost);

        return fragmentCameraBinding.getRoot();
        //return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    private void runTimer(){
        final Handler handler = new Handler();

        handler.post(new Runnable() {
            @Override
            public void run() {
                //int hours = seconds / 3600;
                //int minutes = (seconds % 3600) / 60;
                //int secs = seconds % 60;
                if(activatedDetectionFire){
                    seconds++;
                    Log.d(TAG,seconds + "s" );
                    handler.postDelayed(this, 1000);
                }else {
                    seconds = 0;
                }
            }
        });
    }

    private double radarAverage(){

       /* RadarRxService radarRxService = ServiceInstance.getServiceRadar();
        List<Double> distances = new ArrayList<>();
        Observable.timer(200, TimeUnit.MILLISECONDS)
                .flatMap(new Function<Long, ObservableSource<Double>>() {
                    @Override
                    public ObservableSource<Double> apply(Long aLong) throws Throwable {
                        return radarRxService.getDistance();
                    }
                })
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new io.reactivex.rxjava3.functions.Consumer<Double>() {
            @Override
            public void accept(Double aDouble) throws Throwable {

            }
        })*/



       /* Observable.interval(0,200, TimeUnit.MILLISECONDS)
                .take(3)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new io.reactivex.rxjava3.functions.Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Throwable {
                        radarRxService.getDistance()
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new io.reactivex.rxjava3.functions.Consumer<Double>() {
                                    @Override
                                    public void accept(Double distance) throws Throwable {
                                        Log.d("RadarService", "" + distance);
                                        distances.add(distance);
                                    }

                                });
                    }
                })
            */
        ;

        return  0;
    }

    private synchronized void waitDelay(){
        //Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Toast.makeText(getContext(), "Start automatic capture every " + delay / 1000.0 + "seconds", Toast.LENGTH_SHORT).show();
            }
        },15000);
    }

    private void verifyRequiredPermissionsRecord() {
        Boolean allGranted = true;

        for(String permission: recordPermissions){
            if(ActivityCompat.checkSelfPermission(getContext(), permission) != PackageManager.PERMISSION_GRANTED ){
                ActivityCompat.requestPermissions(getActivity(), recordPermissions, REQUEST_PERMISSION_RECORD);
                allGranted = false;
                break;
            }
        }
    }

    private void verifyRequiredPermissionsWrite() {
        Boolean allGranted = true;

        for(String permission: writePermissions){
            if(ActivityCompat.checkSelfPermission(getContext(), permission) != PackageManager.PERMISSION_GRANTED ){
                ActivityCompat.requestPermissions(getActivity(), recordPermissions, REQUEST_PERMISSION_WRITE);
                allGranted = false;
                break;
            }
        }
    }



    private synchronized void connectCamera(){
        camera = new Camera();
        try {
            camera.connect(identity, new ConnectionStatusListener() {
                @Override
                public void onDisconnected(@org.jetbrains.annotations.Nullable ErrorCode errorCode) {
                    cameraViewModel.errorInfoLiveData.postValue(errorCode.toString());
                }
            }, new ConnectParameters());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(UsbPermissionHandler.isFlirOne(identity)){
            usbPermissionHandler.requestFlirOnePermisson(identity, getContext(),  new UsbPermissionHandler.UsbPermissionListener(){

                @Override
                public void permissionGranted(@NonNull Identity identity) {
                    connectCamera();
                }

                @Override
                public void permissionDenied(@NonNull Identity identity) {
                    Toast.makeText(getContext(), "Permission was denied for identity: " + identity, Toast.LENGTH_LONG).show();
                }

                @Override
                public void error(ErrorType errorType, Identity identity) {
                    Toast.makeText(getContext(), "Error when asking for permission for FLIR ONE, error:" + errorType + " identity:" + identity, Toast.LENGTH_LONG).show();
                }
            });
        } else{
            connectCamera();
       }

        List<Stream> streams = camera.getStreams();

        Stream thermalStream = null;

        for(Stream stream: streams){
            if(stream.isThermal()){
                thermalStream = stream;
            }
            break;
        }

        if(currentPallete == null) {
            currentPallete = PaletteManager.getDefaultPalettes().get(0);
        }

        if(thermalStream!= null){
            streamer = new ThermalStreamer(thermalStream);
            thermalStream.start(new OnReceived<Void>() {
                @Override
                public void onReceived(Void unused) {
                    refreshThermalFrame();
                }
            }, new OnRemoteError() {
                @Override
                public void onRemoteError(ErrorCode errorCode) {
                    cameraViewModel.errorInfoLiveData.postValue(errorCode.toString());
                }
            });
        } else {
            cameraViewModel.errorInfoLiveData.postValue("No thermal stream available for this camera.");
        }
    }

    public void takePicture(Boolean verbose) {
        if (streamer != null) {
            ImageBuffer imageBuffer = streamer.getImage();

            streamer.withThermalImage(new Consumer<ThermalImage>() {
                @Override
                public void accept(ThermalImage thermalImage) {

                    Long timeSeconds = System.currentTimeMillis() / 10;
                    String stringTs = timeSeconds.toString();
                    File imageSDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                    //String p = Environment.getExternalStorageDirectory(Environment.DIRECTORY_PICTURES);
                    File image = new File(imageSDir,  "flir_image_" + stringTs + ".jpg");

                    thermalImage.setPalette(currentPallete);
                    thermalImage.getFusion().setFusionMode(currentFusionMode);
                    /*double [] temperatures = thermalImage.getValues(new Rectangle(0, 0, thermalImage.getWidth(), thermalImage.getHeight()));*/

                    try {
                        thermalImage.saveAs(image.getAbsolutePath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                }
            });

            if(verbose == true){
                Toast.makeText(getContext(), "Image Saved", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private double getRadarDistance(){
        //RadarApi radarService = RetrofitInstance.getServiceRadar();

       // Call<RadarDistanceModel> call = radarService.getRadarDistance();
       // call.enqueue(new Callback<RadarDistanceModel>() {
       //     @Override
        //    public void onResponse(Call<RadarDistanceModel> call, Response<RadarDistanceModel> response) {
        //        RadarDistanceModel radarDistance = response.body();
        //        Log.d("Radar", radarDistance.toString());
         //   }

        //    @Override
        //    public void onFailure(Call<RadarDistanceModel> call, Throwable t) {
        //
        //   }
        //});
        return 10.0;
    }

    private void sendAlert(double maxTemperatureC, double latutud, double longitud, double distanceM, String time){
        AlertApi alertService = ServiceInstance.getService();
        AlertDataModel alertDataModel = new AlertDataModel(maxTemperatureC + " C", latutud, longitud, distanceM + " m", time, 1.0);
        Call<AlertDataModel> call = alertService.PostAlert(alertDataModel);
        call.enqueue(new Callback<AlertDataModel>() {
            @Override
            public void onResponse(Call<AlertDataModel> call, Response<AlertDataModel> response) {
                AlertDataModel resp = response.body();
                Toast.makeText(getContext(), "Envio Alerta", Toast.LENGTH_SHORT).show();

                alertSent = true;
                Handler handler = new Handler();

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        alertSent = false;
                        Log.d(TAG, "New Alert");
                    }
                }, 10000);
            }

            @Override
            public void onFailure(Call<AlertDataModel> call, Throwable t) {
                Log.i("TAG", t.toString());
                Toast.makeText(getContext(), "Error Alerta: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendAlertSocket(double maxTemperatureC, double latutud, double longitud, double distanceM, String time, double areaFire){
        AlertApi alertService = ServiceInstance.getService();
        AlertDataModel alertDataModel = new AlertDataModel(maxTemperatureC + " C", latutud, longitud, distanceM + " m", time, areaFire);
        desktopHost.alertFire(alertDataModel);

        alertSent = true;
        Handler handler = new Handler();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                alertSent = false;
                Log.d(TAG, "New Alert");
            }
        }, 10000);
    }

    private void fireDetection(Frame frame, double [] temperatures){
        // if alert already have been sent
        //Log.d(TAG, "activatedDetectionFire: " + activatedDetectionFire );
        if(activatedDetectionFire == false) return;
        double distance = 10.0;

       fireForestDetector
               .detectFire(frame, temperatures)
               .subscribe(new io.reactivex.rxjava3.functions.Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean fire) throws Throwable {
                        num_detecs++;
                        if (fire){
                            Log.d(TAG, "Warning: Fuegooooooooooooooooo!");
                            // Enviar alerta
                            if (fireAlertService.alreadySent)
                                return;

                            if (fireAlertService.getRegister()!= null){
                                Log.d(TAG, "Sent Warning: Fuegooooooooooooooooo!");
                                fireAlertService.dispatch();
                            }else {
                                Log.d(TAG, "Register Warning: Fuegooooooooooooooooo!");
                                // Toast.makeText(getContext(), "Fuego", Toast.LENGTH_LONG).show();

                                // Get Max Temperature
                                double maxTemperature = maxArray(temperatures, 2);

                                Log.d(TAG, "Warning: Alta temperatura detectada, N:" + num_detecs);
                                //Toast.makeText(getContext(),"Fire detected", Toast.LENGTH_SHORT ).show();

                                // Get localization
                                double latitude = locationGPSTracker.getLatitude();
                                double longitude = locationGPSTracker.getLongitude();
                                // Get distance

                                // Get area
                                double areaFire = fireForestDetector.getAreaFire();

                                // Get time
                                Date date = Calendar.getInstance().getTime();
                                DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
                                String srtDate = dateFormat.format(date);

                                AlertDataModel alertDataModel = new AlertDataModel(maxTemperature + " C", latitude, longitude, distance + " m", srtDate, areaFire);
                                fireAlertService.registerDetection(alertDataModel);
                            }

                        }
                    }
                });


        //activatedDetectionFire = false;
        // Find max temperature
    }

    private void refreshThermalFrame(){
        streamer.update();
        ImageBuffer imageBuffer = streamer.getImage();

        streamer.withThermalImage(new Consumer<ThermalImage>() {
            @Override
            public void accept(ThermalImage thermalImage) {
                Bitmap msxBitmap;
                double [] temperatures;
                {
                    thermalImage.setPalette(currentPallete);
                    thermalImage.getFusion().setFusionMode(currentFusionMode);
                    msxBitmap = BitmapAndroid.createBitmap(imageBuffer).getBitMap();

                    thermalImage.setTemperatureUnit(TemperatureUnit.CELSIUS);

                    //double [] temperatures = thermalImage.getValues(new Rectangle(0, 0, thermalImage.getWidth(), thermalImage.getHeight()));
                    ThermalValue centerTemp = thermalImage.getValueAt(new com.flir.thermalsdk.image.Point(thermalImage.getWidth()/ 2, thermalImage.getHeight()/2));

                    //Log.d(TAG, "adding images to cache");
                    //Log.d(TAG, "Thermal Image Size: (" + thermalImage.getWidth() + "," + thermalImage.getHeight() + ")");
                    //Log.d(TAG, "MsxBitMap Image Size: (" + msxBitmap.getWidth() + "," + msxBitmap.getHeight() + ")");
                    int centerY = msxBitmap.getHeight() / 2;
                    int centerX  = msxBitmap.getWidth() / 2;

                    AndroidFrameConverter converterToFrame = new AndroidFrameConverter();
                    Frame frame = converterToFrame.convert(msxBitmap);

                    // Umbral para detectar objetos
                    temperatures = thermalImage.getValues(new Rectangle(0, 0, thermalImage.getWidth(), thermalImage.getHeight()));


                    // Fire deteccion and Alert
                    fireDetection(frame, temperatures);

                    OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
                    Mat matImage = converterToMat.convert(frame);

                    putText(matImage,centerTemp.asCelsius() + "", new Point(centerX + 20, centerY), FONT_HERSHEY_COMPLEX, 1.0, new Scalar(0, 255, 0, 0.2));
                    circle(matImage,  new Point(centerX,centerY), 4, new Scalar(255,0,0, 0.2), 4, LINE_AA,0);


                    //Log.v(TAG, "Writing Frame");
                    frame = converterToMat.convert(matImage);
                    msxBitmap = converterToFrame.convert(frame);

                    cameraViewModel.bitmapLiveData.postValue(msxBitmap);
                }

                if(videoRecorder!= null && videoRecorder.recording == true){
                    try {
                        videoRecorder.recordImage(msxBitmap);
                        /*new Thread(() -> {
                            try {
                               // ThermalCSVWriter thermalCSVWriter = new ThermalCSVWriter(getContext(), "image_" + stringTs);
                                thermalCSVWriter.saveThermalValues(temperatures, 0);
                                //thermalCSVWriter.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }).start();
                        */
                    } catch (FFmpegFrameRecorder.Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        });
    }

    @Override

    public void onPause() {
        handler.removeCallbacks(runnable);
        handlerSleep.removeCallbacks(runnable);
        //fragmentCameraBinding.enableRafaga.setChecked(false);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
        camera.disconnect();
    }
}