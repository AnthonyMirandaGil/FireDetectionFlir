package com.example.firedetectionflir;

import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.firedetectionflir.databinding.FragmentCameraBinding;
import com.flir.thermalsdk.ErrorCode;
import com.flir.thermalsdk.androidsdk.image.BitmapAndroid;
import com.flir.thermalsdk.image.ImageBuffer;
import com.flir.thermalsdk.image.ThermalImage;
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

import java.io.File;
import java.io.IOException;
import java.util.List;

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
    private FusionMode fusionMode = FusionMode.THERMAL_ONLY;

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

        fragmentCameraBinding.status.setText("Connected to: " + identity.deviceId);

        cameraViewModel.errorInfoLiveData.observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String s) {
                fragmentCameraBinding.status.setText(s);
            }
        });

        fragmentCameraBinding.btnTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });

        return fragmentCameraBinding.getRoot();
        //return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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

    public void takePicture() {
        if (streamer != null) {
            ImageBuffer imageBuffer = streamer.getImage();
            streamer.withThermalImage(new Consumer<ThermalImage>() {
                @Override
                public void accept(ThermalImage thermalImage) {
                    Long timeSeconds = System.currentTimeMillis() / 1000;
                    String stringTs = timeSeconds.toString();
                    File imageSDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                    //String p = Environment.getExternalStorageDirectory(Environment.DIRECTORY_PICTURES);
                    File image = new File(imageSDir, stringTs + ".jpg");

                    thermalImage.setPalette(currentPallete);
                    thermalImage.getFusion().setFusionMode(fusionMode);
                    try {
                        thermalImage.saveAs(image.getAbsolutePath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            Toast.makeText(getContext(), "Image Saved", Toast.LENGTH_SHORT).show();
        }
    }
    private void refreshThermalFrame(){
        streamer.update();
        ImageBuffer imageBuffer = streamer.getImage();

        streamer.withThermalImage(new Consumer<ThermalImage>() {
            @Override
            public void accept(ThermalImage thermalImage) {
                Bitmap msxBitmap;
                {
                    thermalImage.setPalette(currentPallete);
                    thermalImage.getFusion().setFusionMode(fusionMode);
                    msxBitmap = BitmapAndroid.createBitmap(imageBuffer).getBitMap();
                    cameraViewModel.bitmapLiveData.postValue(msxBitmap);
                }

            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        camera.disconnect();
    }
}