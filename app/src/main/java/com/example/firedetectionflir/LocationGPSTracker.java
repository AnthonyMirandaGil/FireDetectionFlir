package com.example.firedetectionflir;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

public class LocationGPSTracker extends Service implements LocationListener {
    Location location;
    double latitude;
    double longitude;
    private Context context;
    protected LocationManager locationManager;
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10;
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1;

    public Location getLocation() {
        return location;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }


    public LocationGPSTracker(Context context) {
        this.context = context;
        this.startLocation();
    }

    public void startLocation() {
        locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        Boolean checkNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        Boolean checkGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!checkGps) {
            Toast.makeText(context, "No service Provider GPS is avalible", Toast.LENGTH_SHORT).show();
        } else {
            /// FALTA CHECK PERMISION
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.

                return;
            }

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES,
                    MIN_TIME_BW_UPDATES, this
            );

            if(locationManager != null){
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if(location != null){
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                }
            }
        }
    }

    @Nullable
    @Override

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
    }

    public void stopListener(){
        if(locationManager != null){
            locationManager.removeUpdates(this);
        }
    }
}
