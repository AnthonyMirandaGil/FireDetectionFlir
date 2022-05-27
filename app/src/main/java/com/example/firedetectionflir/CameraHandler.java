package com.example.firedetectionflir;


import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.Nullable;

import com.flir.thermalsdk.ErrorCode;
import com.flir.thermalsdk.androidsdk.image.BitmapAndroid;
import com.flir.thermalsdk.image.ImageBuffer;
import com.flir.thermalsdk.image.ThermalImage;
import com.flir.thermalsdk.image.fusion.FusionMode;
import com.flir.thermalsdk.image.palettes.Palette;
import com.flir.thermalsdk.image.palettes.PaletteManager;
import com.flir.thermalsdk.live.Camera;
import com.flir.thermalsdk.live.CommunicationInterface;
import com.flir.thermalsdk.live.ConnectParameters;
import com.flir.thermalsdk.live.Identity;
import com.flir.thermalsdk.live.connectivity.ConnectionStatusListener;
import com.flir.thermalsdk.live.discovery.DiscoveryEventListener;
import com.flir.thermalsdk.live.discovery.DiscoveryFactory;
import com.flir.thermalsdk.live.remote.OnReceived;
import com.flir.thermalsdk.live.remote.OnRemoteError;
import com.flir.thermalsdk.live.streaming.Stream;
import com.flir.thermalsdk.live.streaming.Streamer;
import com.flir.thermalsdk.live.streaming.ThermalStreamer;
import com.flir.thermalsdk.utils.Consumer;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class CameraHandler {
    private static final String TAG = "CameraHandler";
    private Camera camera;
    private ThermalStreamer streamer;
    private StreamDataListener streamDataListener;
    LinkedList<Identity> foundCameraIdentities = new LinkedList<>();
    private Palette palette;
    public FusionMode fusionMode = FusionMode.THERMAL_ONLY;

    public interface DiscoveryStatus {
        void started();

        void stopped();
    }

    public interface StreamDataListener {
        void images(Bitmap msxBitmap);
    }
    /**
     * Start discovery of USB and Emulators
     */

    public void startDiscovery(DiscoveryEventListener cameraDiscoveryListener, DiscoveryStatus discoveryStatus){
        DiscoveryFactory.getInstance().scan(cameraDiscoveryListener, CommunicationInterface.EMULATOR, CommunicationInterface.USB);
        discoveryStatus.started();
    }

    /**
     * Stop discovery of USB and Emulators
     */

    public void stopDiscovery(DiscoveryStatus discoveryStatus){
        DiscoveryFactory.getInstance().stop(CommunicationInterface.EMULATOR, CommunicationInterface.USB);
        discoveryStatus.stopped();
    }
    public void add(Identity identity){
        foundCameraIdentities.add(identity);
    }

    public synchronized void connect(
            Identity identity,
            ConnectionStatusListener connectionStatusListener,
            StreamDataListener streamDataListener) throws IOException {
        Log.d(TAG, "connect identity: " + identity);
        camera = new Camera();
        camera.connect(identity, connectionStatusListener, new ConnectParameters());

        List<Stream> streams = camera.getStreams();
        Stream thermalStream = null;

        for(Stream stream: streams){
            if(stream.isThermal()){
                thermalStream = stream;
            }
            break;
        }

        if(palette == null) {
            palette = PaletteManager.getDefaultPalettes().get(0);
        }

        if(thermalStream != null){
            streamer = new ThermalStreamer(thermalStream);
            this.streamDataListener = streamDataListener;
            thermalStream.start(new OnReceived<Void>() {
                @Override
                public void onReceived(Void unused) {
                    refreshThermalFrame();
                }
            }, new OnRemoteError() {
                @Override
                public void onRemoteError(ErrorCode errorCode) {

                }
            });
        }else {
            Log.d(TAG,"No se encontro stremear");
        }

    }

    public synchronized void startStream(StreamDataListener listener) {
        this.streamDataListener = listener;

    }

    private void refreshThermalFrame(){
        streamer.update();
        ImageBuffer imageBuffer = streamer.getImage();
        streamer.withThermalImage(new Consumer<ThermalImage>() {
            @Override
            public void accept(ThermalImage thermalImage) {
                Bitmap msxBitmap;
                {
                    thermalImage.setPalette(palette);
                    thermalImage.getFusion().setFusionMode(fusionMode);
                    msxBitmap = BitmapAndroid.createBitmap(imageBuffer).getBitMap();
                    if(msxBitmap != null){
                        streamDataListener.images(msxBitmap);
                    }
                }

            }
        });
    }

    @Nullable
    public Identity getFlirOne(){
        for(Identity foundCameraIdentity: foundCameraIdentities) {
            boolean isFlirOneEmulator = foundCameraIdentity.deviceId.contains("EMULATED FLIR ONE");
            boolean isCppEmulator = foundCameraIdentity.deviceId.contains("C++ Emulator");
            if(!isFlirOneEmulator && !isCppEmulator){
                return  foundCameraIdentity;
            }
        }
        return null;
    }

    @Nullable
    public Identity getFlirOneEmulator(){
        for(Identity foundCameraIdentity : foundCameraIdentities){
            if(foundCameraIdentity.deviceId.contains("EMULATED FLIR ONE")){
                return foundCameraIdentity;
            }
        }
        return null;
    };
}
