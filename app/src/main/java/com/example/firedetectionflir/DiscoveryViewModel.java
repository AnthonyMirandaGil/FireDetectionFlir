package com.example.firedetectionflir;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.flir.thermalsdk.ErrorCode;
import com.flir.thermalsdk.live.CommunicationInterface;
import com.flir.thermalsdk.live.Identity;
import com.flir.thermalsdk.live.discovery.DiscoveryEventListener;
import com.flir.thermalsdk.live.discovery.DiscoveryFactory;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class DiscoveryViewModel extends ViewModel {
    private final String TAG = "DiscoveryViewModel";
    private CommunicationInterface [] communicationInterfaces = {
            CommunicationInterface.EMULATOR,
            CommunicationInterface.USB
    };

    private HashSet<Identity> foundIdentities = new HashSet<Identity>();
    public MutableLiveData<ArrayList<Identity>> foundIdentitiesLiveData = new MutableLiveData<ArrayList<Identity>>();
    public MutableLiveData<String> statusInfoLiveData = new MutableLiveData<String>();

    public void startDiscovery(){
        statusInfoLiveData.postValue("Discovery in progress");
        DiscoveryFactory.getInstance().scan(new DiscoveryEventListener() {
            @Override
            public void onCameraFound(Identity identity) {
                Log.d(TAG, "onCameraFound identity:" + identity);
                Boolean newItemAdded = foundIdentities.add(identity);
                if(newItemAdded){
                    foundIdentitiesLiveData.postValue(new ArrayList(foundIdentities));
                }
            }

            @Override
            public void onDiscoveryError(CommunicationInterface communicationInterface, ErrorCode errorCode) {
                statusInfoLiveData.postValue(errorCode.toString());
            }
        },communicationInterfaces[0], communicationInterfaces[1]);
    }

    private void stopDiscovery(){
        DiscoveryFactory.getInstance().stop(communicationInterfaces);
    }
}
