package com.mis.francescoandreussi.lukemapswalker;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.StreetViewPanoramaView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.StreetViewPanoramaCamera;
import com.google.android.gms.maps.model.StreetViewPanoramaLocation;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.Timer;

public class MainActivity extends Activity implements  OnMapReadyCallback,
                                                                GoogleMap.OnMapLongClickListener,
                                                                OnStreetViewPanoramaReadyCallback,
                                                                DataClient.OnDataChangedListener {

    private ViewSwitcher viewSwitcher;
    private View mapLayout;
    private View streetViewLayout;

    private MapView mapView;
    private GoogleMap mGoogleMap;

    //private DataClient dataClient;

    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";

    private static final LatLng SYDNEY = new LatLng(-33.87365, 151.20689);

    private StreetViewPanoramaView streetViewPanoramaView;
    private static final String STREETVIEW_BUNDLE = "StreetViewBundle";
    private StreetViewPanorama streetView;

    //private static final String STREETVIEW_BUNDLE_KEY = "StreetViewBundleKey";

    private Wearable.WearableOptions wearableOptions = new Wearable.WearableOptions.Builder().setLooper(Looper.myLooper()).build();
    private DataClient dataClient;// = Wearable.getDataClient(this);

    private Marker marker;
    private boolean smartwatchMode = false;
    private boolean panoramaAvailable = true;
    private boolean exiting = false;
    private Button enterSVButton;
    private Button exitSVButton;
    private Button playPauseButton;

    private ArrayList<LatLng> visitedPath = new ArrayList<>();

    private float[] accVec;
    private float[] gyroVec;
    private float[] orientVec;
    private float[] prevOrientVec = new float[3];
    private StreetViewPanoramaLocation prevLocation;

    private int animDuration = 0;
    private float[] zeroOrient;// = {?, 0f, (float) Math.PI/2}; //???
    private float orientThreshold = (float) Math.PI/10;
    private float deltaTilt = 0;
    private float deltaBearing = 0;
    private LatLng deltaLatLng = new LatLng(0,0);

    private Timer timer = new Timer(true);
    private final Handler handler = new Handler();
    private Bundle mapViewBundle = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        viewSwitcher = findViewById(R.id.switcher);

        mapLayout = findViewById(R.id.map_layout);
        streetViewLayout = findViewById(R.id.streetView_layout);

        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }

        mapView = findViewById(R.id.map_view);
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);

        /*LinearLayout linearLayout = findViewById(R.id.streetView_layout);

        final StreetViewPanoramaOptions options = new StreetViewPanoramaOptions();
        if (savedInstanceState == null) {
            options.position(SYDNEY);
        }

        streetViewPanoramaView = new StreetViewPanoramaView(this, options);
        streetViewPanoramaView.getStreetViewPanoramaAsync(this);

        linearLayout.addView(streetViewPanoramaView,
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));*/

        streetViewPanoramaView = findViewById(R.id.streetView_view);

        Bundle mStreetViewBundle = null;
        if (savedInstanceState != null) {
            mStreetViewBundle = savedInstanceState.getBundle(STREETVIEW_BUNDLE);
        }

        streetViewPanoramaView.onCreate(mStreetViewBundle);
        streetViewPanoramaView.getStreetViewPanoramaAsync(this);
        //streetViewPanoramaView.setVisibility(View.INVISIBLE);

        enterSVButton   = findViewById(R.id.enterSV);
        exitSVButton    = findViewById(R.id.exitSV);
        playPauseButton = findViewById(R.id.playPause);

        enterSVButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(viewSwitcher.getCurrentView() == mapLayout && panoramaAvailable){
                    //streetViewPanoramaView.setVisibility(View.VISIBLE);
                    //mapView.setVisibility(View.INVISIBLE);
                    mapView.onStop();
                    streetViewPanoramaView.onStart();
                    viewSwitcher.showNext();
                }
                else{
                    //streetViewPanoramaView.setVisibility(View.INVISIBLE);
                    //mapView.setVisibility(View.VISIBLE);
                    //viewSwitcher.showPrevious();
                    Toast.makeText(getApplicationContext(),"ERROR",Toast.LENGTH_SHORT).show();
                }
                //playPauseButton.setVisibility(streetViewPanoramaView.getVisibility());
            }
        });

        exitSVButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(viewSwitcher.getCurrentView() == streetViewLayout){
                    //streetViewPanoramaView.setVisibility(View.INVISIBLE);
                    //mapView.setVisibility(View.VISIBLE);
                    viewSwitcher.showNext();
                    mapView.onStart();
                    streetViewPanoramaView.onDestroy();

                    exiting = true;
                }
                else{
                    //streetViewPanoramaView.setVisibility(View.INVISIBLE);
                    //mapView.setVisibility(View.VISIBLE);
                    //viewSwitcher.showPrevious();
                    Toast.makeText(getApplicationContext(),"ERROR",Toast.LENGTH_SHORT).show();
                }
                //playPauseButton.setVisibility(streetViewPanoramaView.getVisibility());
            }
        });

        //playPauseButton.setVisibility(streetViewPanoramaView.getVisibility());
        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(viewSwitcher.getCurrentView() == streetViewLayout){
                    smartwatchMode = !smartwatchMode;
                    streetView.setPanningGesturesEnabled(smartwatchMode);
                    streetView.setZoomGesturesEnabled(smartwatchMode);
                    streetView.setUserNavigationEnabled(smartwatchMode);
                    if(smartwatchMode) {
                        // = Wearable.getDataClient(MapsActivity.this);//.addListener(MapsActivity.this);
                        //Log.e("listener", dataClient.toString());
                        //dataClient.addListener(MapsActivity.this);
                        /*timer.scheduleAtFixedRate(new TimerTask() {
                            @Override
                            public void run() {
                                StreetViewPanoramaCamera currentCamera = streetView.getPanoramaCamera();
                                StreetViewPanoramaCamera nextCamera = new StreetViewPanoramaCamera.Builder()
                                        .zoom(currentCamera.zoom)
                                        .tilt(currentCamera.tilt + deltaTilt)
                                        .bearing(currentCamera.bearing + deltaBearing)
                                        .build();
                                streetView.animateTo(nextCamera, animDuration);
                                streetView.setPosition(move(streetView.getLocation().position, deltaLatLng));
                                visitedPath.add(streetView.getLocation().position);
                            }
                        }, 0, 1000);*/
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                StreetViewPanoramaCamera currentCamera = streetView.getPanoramaCamera();
                                StreetViewPanoramaCamera nextCamera = new StreetViewPanoramaCamera.Builder()
                                        .zoom(currentCamera.zoom)
                                        .tilt(currentCamera.tilt + deltaTilt)
                                        .bearing(currentCamera.bearing + deltaBearing)
                                        .build();
                                streetView.animateTo(nextCamera, animDuration);
                                prevLocation = streetView.getLocation();
                                streetView.setPosition(move(streetView.getLocation().position, deltaLatLng));
                                if(streetView.getLocation().position == null){
                                    streetView.setPosition(prevLocation.position);
                                }
                                visitedPath.add(streetView.getLocation().position);
                            }
                        }, 1000);
                    } else {
                        //Wearable.getDataClient(MapsActivity.this).removeListener(MapsActivity.this);
                    }
                } else {
                    Toast.makeText(getApplicationContext(),"ERROR",Toast.LENGTH_SHORT).show();
                }
            }
        });
        dataClient = Wearable.getDataClient(getApplicationContext());
        dataClient.addListener(this);
    }


    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        streetViewPanoramaView.onResume();
        //dataClient.addListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        streetViewPanoramaView.onPause();
        //dataClient.removeListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
        streetViewPanoramaView.onStart();
        //dataClient.addListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
        streetViewPanoramaView.onStop();
        //dataClient.removeListener(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        super.onCreate(mapViewBundle);
        mGoogleMap = googleMap;
        LatLng ny = SYDNEY;
        marker = mGoogleMap.addMarker(new MarkerOptions().position(ny));
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(ny));
    }

    //@Override
    public void onMapLongClick(LatLng point){
        mGoogleMap.clear();
        marker = mGoogleMap.addMarker(new MarkerOptions().position(point));
        streetView.setPosition(marker.getPosition());
        /*Log.e("streetViewPos", streetView.toString());
        if(streetView.getLocation() != null && streetView.getLocation().links != null){
            panoramaAvailable = true;
        } else {
            Toast.makeText(this,"StreetView is not available here!", Toast.LENGTH_LONG).show();
            panoramaAvailable = false;
        }*/
    }

    @Override
    public void onStreetViewPanoramaReady(final StreetViewPanorama streetViewPanorama) {
        streetView = streetViewPanorama;
        streetView.setPosition(SYDNEY);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle);
        }

        mapView.onSaveInstanceState(mapViewBundle);

        Bundle mStreetViewBundle = outState.getBundle(STREETVIEW_BUNDLE);
        if (mStreetViewBundle == null) {
            mStreetViewBundle = new Bundle();
            outState.putBundle(STREETVIEW_BUNDLE, mStreetViewBundle);
        }
    }

    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {

        Log.e("debug", "OUTSIDE THE IF");
        if(viewSwitcher.getCurrentView() == streetViewLayout && smartwatchMode) {
            Log.e("debug", "INSIDE THE IF");
            for (DataEvent event : dataEventBuffer) {
                Log.v("check", event.toString());
                if(event.getDataItem().getUri().getPath() != null && event.getDataItem().getUri().getPath() == "/data") {
                    accVec = DataMapItem.fromDataItem(event.getDataItem()).getDataMap().getFloatArray("accVec");
                    Log.e("ACC_VEC", accVec.toString());
                    gyroVec = DataMapItem.fromDataItem(event.getDataItem()).getDataMap().getFloatArray("gyroVec");
                    orientVec = DataMapItem.fromDataItem(event.getDataItem()).getDataMap().getFloatArray("orientVec");
                    Log.e("ORIENT_VEC", orientVec.toString());
                    dataClient.deleteDataItems(event.getDataItem().getUri());
                }
            }
            if(zeroOrient == null){
                zeroOrient = orientVec;
            }
            if(magnitude(diff(prevOrientVec,orientVec)) > orientThreshold){
                prevOrientVec = orientVec;
                float[] normalizedRotVec = diff(orientVec, zeroOrient);
                //TODO: map normalizedRotVec to deltaTilt, deltaBearing and deltaLatLng
            }
        }
        if(exiting){
            //TODO: implement exiting options (up, down, right, left)
            /*streetViewPanoramaView.setVisibility(View.INVISIBLE);
            mapView.setVisibility(View.VISIBLE);
            viewSwitcher.showNext();
            exiting = false;*/
        }
    }

    private static LatLng move(LatLng start, LatLng delta){
        return new LatLng(start.latitude + delta.latitude, start.longitude + delta.longitude);
    }

    private static float magnitude(float[] vector){
        float sum = 0;
        for (float elem : vector) {
            sum += Math.pow(elem, 2);
        }
        return (float) Math.sqrt(sum);
    }

    private static float[] diff(float[] v1, float[] v2){
        float[] diff = new float[v1.length];
        if(v1.length == v2.length){
            for(int i=0; i<v1.length; i++){
                diff[i] = v1[i] - v2[i];
            }
        }
        return diff;
    }
}
