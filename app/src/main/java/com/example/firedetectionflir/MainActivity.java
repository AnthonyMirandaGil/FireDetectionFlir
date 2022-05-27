package com.example.firedetectionflir;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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
import com.flir.thermalsdk.androidsdk.ThermalSdkAndroid;
import com.flir.thermalsdk.androidsdk.live.connectivity.UsbPermissionHandler;
import com.flir.thermalsdk.live.CommunicationInterface;
import com.flir.thermalsdk.live.Identity;
import com.flir.thermalsdk.live.connectivity.ConnectionStatusListener;
import com.flir.thermalsdk.live.discovery.DiscoveryEventListener;
import com.flir.thermalsdk.log.ThermalLog;



import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private TextView discoveryStatus;

    private ImageView msxImage;
    private Button btnTakePicture;
    private Button recordButton;

    private Spinner spinnerFusion;
    private Spinner spinner;

    private PermissionHandler permissionHandler;
    private CameraHandler cameraHandler;
    private UsbPermissionHandler usbPermissionHandler = new UsbPermissionHandler();

    private Identity connectedIdentity = null;

    private static final String TAG = "MainActivity";
    /**
     * Show message on the screen
     */
    public interface ShowMessage {
        void show(String message);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setupViews();
        ThermalLog.LogLevel enableLoggingInDebug = BuildConfig.DEBUG ? ThermalLog.LogLevel.DEBUG : ThermalLog.LogLevel.NONE;
        ThermalSdkAndroid.init(getApplicationContext(), enableLoggingInDebug);
        permissionHandler = new PermissionHandler(showMessage, MainActivity.this);
        cameraHandler = new CameraHandler();

        btnTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                cameraHandler.takePicture();
            }
        });

    }

    private final ShowMessage showMessage = new ShowMessage() {
        @Override
        public void show(String message) {
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
        }
    };

    private final DiscoveryEventListener cameraDiscoveryListener = new DiscoveryEventListener() {
        @Override
        public void onCameraFound(Identity identity) {
            Log.d(TAG, "onCameraFound identity:" + identity);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    cameraHandler.add(identity);
                }
            });
        }

        @Override
        public void onDiscoveryError(CommunicationInterface communicationInterface, ErrorCode errorCode) {
            Log.d(TAG, "onDiscoveryError communicationInterface:" + communicationInterface + " errorCode:" + errorCode);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    stopDiscovery();
                    MainActivity.this.showMessage.show("onDiscoveryError communicationInterface:" + communicationInterface + " errorCode:" + errorCode);
                }
            });
        }
    };

    private final CameraHandler.DiscoveryStatus discoveryStatusListener = new CameraHandler.DiscoveryStatus() {
        @Override
        public void started() {
            discoveryStatus.setText(getString(R.string.connection_status_text, "discovering"));
        }

        @Override
        public void stopped() {
            discoveryStatus.setText(getString(R.string.connection_status_text, "not discovering"));
        }
    };

    public void startDiscovery(View view){
        startDiscovery();
    }

    public void stopDiscovery(View view){
        stopDiscovery();
    }

    private void startDiscovery(){
        cameraHandler.startDiscovery(cameraDiscoveryListener, discoveryStatusListener);
    }

    private void stopDiscovery(){
        cameraHandler.stopDiscovery(discoveryStatusListener);
    }

    private void connect(Identity identity){
        cameraHandler.stopDiscovery(discoveryStatusListener);
        if(connectedIdentity != null){
            Log.d(TAG, "connect(), in *this* code sample we only support one camera connection at the time");
            showMessage.show("connect(), in *this* code sample we only support one camera connection at the time");
            return;
        }

        if(identity == null){
            Log.d(TAG,"connect(), can't connect, no camera available" );
            showMessage.show("connect(), can't connect, no camera available" );
            return;
        }

        connectedIdentity = identity;

        if(UsbPermissionHandler.isFlirOne(identity)){
            usbPermissionHandler.requestFlirOnePermisson(identity, this, new UsbPermissionHandler.UsbPermissionListener() {
                @Override
                public void permissionGranted(@NonNull Identity identity) {
                    doConnect(identity);
                }

                @Override
                public void permissionDenied(@NonNull Identity identity) {
                    MainActivity.this.showMessage.show("Permission was denied for identity ");
                }

                @Override
                public void error(ErrorType errorType, Identity identity) {
                    MainActivity.this.showMessage.show("Error when asking for permission for FLIR ONE, error:" + errorType + " identity:" + identity);
                }
            });
        } else {
            doConnect(identity);
        }
    }

    private final CameraHandler.StreamDataListener streamDataListener = new CameraHandler.StreamDataListener() {
        @Override
        public void images(Bitmap msxBitmap) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    msxImage.setImageBitmap(msxBitmap);
                }
            });
        }
    };

    private void doConnect(Identity identity){
        new Thread(()-> {
            try {
                cameraHandler.connect(identity, new ConnectionStatusListener() {
                    @Override
                    public void onDisconnected(@Nullable ErrorCode errorCode) {
                        Log.d(TAG, "onDisconnected errorCode:" + errorCode);
                    }
                }, streamDataListener);

                /*runOnUiThread(()-> {

                });*/
                //cameraHandler.startStream(streamDataListener);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void connectFlirOne(View view){
        connect(cameraHandler.getFlirOne());
    }

    public void connectFlirOneEmulator(View view){
        connect(cameraHandler.getFlirOneEmulator());
    }


    private void setupViews() {
        // connectionStatus = findViewById(R.id.connection_status_text);
        discoveryStatus = findViewById(R.id.discovery_status);
        // deviceInfo = findViewById(R.id.device_info_text);

        msxImage = findViewById(R.id.msx_image);
        btnTakePicture = findViewById(R.id.btnTakePicture);
        recordButton = findViewById(R.id.recordButton);
        spinner = findViewById(R.id.spinner);
        spinnerFusion = findViewById(R.id.spinnerFusion);
    }
}