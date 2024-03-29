package com.app.geofancing;


import android.app.PendingIntent;
import android.app.Service;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;


import java.util.HashMap;


/**
 * Created by kartik on 22-Mar-17.
 */

public class GeoFencingService extends Service implements GoogleApiClient.ConnectionCallbacks, LocationListener, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {

    private GoogleApiClient googleApiClient;
    private static final long GEO_DURATION = 60 * 60 * 1000;
    boolean isStart = false;
    GeoFenceObject geoFenceObject;
    static GeoFencingService demo;
    public MyThread myThread;
    HashMap<String, GeoFenceObject> lisFenceObjectHashMap = new HashMap<>();
    MyLocation myLocation;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        myThread = new MyThread(this);
        Thread thread = new Thread(myThread);
        thread.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        demo = this;
        return START_STICKY;
    }

    public static GeoFencingService getInstance() {
        if (demo != null) {
            return demo;
        }
        return null;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        myLocation.getLastKnownLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        googleApiClient.connect();

    }

    // Create a Geofence
    public void createGeofence(GeoFenceObject geoFenceObject) {

        this.geoFenceObject = geoFenceObject;
        lisFenceObjectHashMap.put(geoFenceObject.getId(), geoFenceObject);
        Log.d("Tag", "createGeofence");
        Geofence geofence = new Geofence.Builder()
                .setRequestId(geoFenceObject.getId())
                .setCircularRegion(geoFenceObject.getLatitude(),
                        geoFenceObject.getLongitude(),
                        geoFenceObject.getRadius())
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build();
        createGeofenceRequest(geofence);
    }

    private void createGeofenceRequest(Geofence geofence) {
        Log.d("TAG", "createGeofenceRequest");
        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();
        addGeofence(geofencingRequest);
    }

    // Add the created GeofenceRequest to the device's monitoring list
    private void addGeofence(GeofencingRequest request) {
        Log.d("TAG", "addGeofence");
        isStart = true;
        if (checkPermission()) {
            LocationServices.GeofencingApi.addGeofences(
                    googleApiClient,
                    request,
                    createGeofencePendingIntent()
            ).setResultCallback(this);
        } else {
            Log.i("TAG", "enabling()");
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            addGeofence(request);
        }
    }

    // Check for permission to access LocationObject
    private boolean checkPermission() {
        Log.d("TAG", "checkPermission()");
        return (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED);
    }


    private PendingIntent createGeofencePendingIntent() {
        Log.d("TAG", "createGeofencePendingIntent");

        Intent intent = new Intent(this, GeofenceTrasitionService.class);
        return PendingIntent.getService(
                this,geoFenceObject.getId().hashCode() , intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    @Override
    public void onResult(@NonNull Status status) {

    }

    public HashMap<String, GeoFenceObject> getList() {
        return lisFenceObjectHashMap;
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("Location chaanged", String.valueOf(location));

        if (DDPClient.getInstance()!=null)
            DDPClient.getInstance().subscribeForData(location);
        else
        {
            Log.d("Location chaanged"," called again");
            onLocationChanged(location);
        }
    }


    public class MyThread implements Runnable {
        GeoFencingService demoService;

        public MyThread(GeoFencingService demoService) {
            this.demoService = demoService;
            demo = demoService;
        }

        @Override
        public void run() {
            myLocation = new MyLocation(demoService.getBaseContext(),demoService);
            googleApiClient = myLocation.createGoogleApi(demoService);
            googleApiClient.connect();
        }
    }

}
