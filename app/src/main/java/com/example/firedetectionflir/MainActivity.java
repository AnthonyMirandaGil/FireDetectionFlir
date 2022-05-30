package com.example.firedetectionflir;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.flir.thermalsdk.ErrorCode;
import com.flir.thermalsdk.androidsdk.BuildConfig;
import com.flir.thermalsdk.androidsdk.ThermalSdkAndroid;
import com.flir.thermalsdk.androidsdk.live.connectivity.UsbPermissionHandler;
import com.flir.thermalsdk.live.CommunicationInterface;
import com.flir.thermalsdk.live.Identity;
import com.flir.thermalsdk.live.connectivity.ConnectionStatusListener;
import com.flir.thermalsdk.live.discovery.DiscoveryEventListener;
import com.flir.thermalsdk.log.ThermalLog;


import org.bytedeco.javacv.FFmpegFrameRecorder;

import java.io.IOException;
import java.util.Objects;
import java.util.zip.Inflater;

public class MainActivity extends AppCompatActivity {
    private TextView discoveryStatus;

    private ImageView msxImage;
    private Button btnTakePicture;
    private Button recordButton;

    private Spinner spinnerFusion;
    private Spinner spinner;

    //private PermissionHandler permissionHandler;
    private CameraHandler cameraHandler;
    private UsbPermissionHandler usbPermissionHandler = new UsbPermissionHandler();
    private VideoRecorder videoRecorder;

    private Identity connectedIdentity = null;

    private static final String TAG = "MainActivity";
    private final  int REQUEST_PERMISSION_WRITE_EXTERNAL_STORE = 101;
    private final  int REQUEST_PERMISSION_APP = 102;
    private final String [] permissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        verifyRequiredPermissions();
    }

    private void verifyRequiredPermissions() {
        Boolean allGranted = true;

        for(String permission: permissions){
            if(ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED ){
                ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION_APP);
                allGranted = false;
                break;
            }
        }

        if(allGranted){
            initializeApp();
        }
    }

    private void initializeApp() {
        ThermalSdkAndroid.init(this);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mainContainer, MainFragment.newInstance())
                .commitNow();
    }

    public void switchToCameraView(Identity identity) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mainContainer, CameraFragment.newInstance(identity))
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_PERMISSION_APP){
            if(grantResults!= null){
                for(int grantResult: grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(this, "In order to use the app you have to grant permissions.", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    initializeApp();
                }
            }

        }
    }
}