package com.mis.francescoandreussi.lukemapswalker;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
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
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
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
    private boolean moving = false;
    private boolean panoramaAvailable = true;
    private boolean exiting = false;
    private Button enterSVButton;
    private Button exitSVButton;
    private Button playPauseButton;

    private ArrayList<LatLng> visitedPath = new ArrayList<>();

    private float[] accVec = new float[3];
    private float[] gyroVec = new float[3];
    private float[] orientVec = new float[3];
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
        visitedPath.add(SYDNEY);

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
                    //viewSwitcher.showNext();

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
                    streetView.setPanningGesturesEnabled(!smartwatchMode);
                    streetView.setZoomGesturesEnabled(!smartwatchMode);
                    streetView.setUserNavigationEnabled(!smartwatchMode);
                    if(smartwatchMode) {
                        //Log.e("listener", dataClient.toString());
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (moving) {
                                    StreetViewPanoramaCamera currentCamera = streetView.getPanoramaCamera();
                                    StreetViewPanoramaCamera nextCamera = new StreetViewPanoramaCamera.Builder()
                                            .zoom(currentCamera.zoom)
                                            .tilt(currentCamera.tilt + deltaTilt)
                                            .bearing(currentCamera.bearing + deltaBearing)
                                            .build();
                                    streetView.animateTo(nextCamera, animDuration);
                                    prevLocation = streetView.getLocation();
                                    streetView.setPosition(move(streetView.getLocation().position, deltaLatLng));
                                    if (streetView.getLocation().position == null) {
                                        streetView.setPosition(prevLocation.position);
                                    }
                                    visitedPath.add(streetView.getLocation().position);
                                }
                            }
                        }, 10);
                    } else {
                        //Wearable.getDataClient(MapsActivity.this).removeListener(MapsActivity.this);
                    }
                } else {
                    Toast.makeText(getApplicationContext(),"ERROR",Toast.LENGTH_SHORT).show();
                }
            }
        });
        //dataClient = Wearable.getDataClient(getApplicationContext());
        //dataClient.addListener(this);
    }


    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        streetViewPanoramaView.onResume();
        Wearable.getDataClient(this).addListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        streetViewPanoramaView.onPause();
        Wearable.getDataClient(this).removeListener(this);
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

    @Override
    public void onMapLongClick(LatLng point){
        Log.d("MapLongClick","Long click performed");
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
        Log.e("debug", "OUTSIDE THE IF " + smartwatchMode);
        if(viewSwitcher.getCurrentView() == streetViewLayout && smartwatchMode) {
            Log.e("debug", "INSIDE THE IF " + dataEventBuffer.getCount());
            for (DataEvent event : dataEventBuffer) {
                if(event.getType() == DataEvent.TYPE_CHANGED) {
                    DataItem item = event.getDataItem();
                    Log.d("check", event.toString() + event.getDataItem().getUri().getPath());
                    if (item.getUri().getPath().compareTo("/wear") == 0 ) {
                        /**
                         * accVec, gyroVec and orientVec are, in the wide majority of the cases, null.
                         * We cannot understand the reason.
                         * Hence, the connected features are almost never working.
                         */
                        accVec = (DataMapItem.fromDataItem(event.getDataItem())).getDataMap().getFloatArray("accVec");
                        if (accVec != null) Log.d("ACC_VEC", "" + accVec.toString());
                        gyroVec = (DataMapItem.fromDataItem(event.getDataItem())).getDataMap().getFloatArray("gyroVec");
                        if (gyroVec != null) Log.d("GYRO_VEC", gyroVec.toString());
                        orientVec = (DataMapItem.fromDataItem(event.getDataItem())).getDataMap().getFloatArray("orientVec");
                        if (orientVec != null) Log.d("ORIENT_VEC", orientVec.toString());
                        Wearable.getDataClient(this).deleteDataItems(event.getDataItem().getUri());
                        //troubleshooting code
                        //Log.d("A","" + (DataMapItem.fromDataItem(event.getDataItem())).getDataMap().getInt("a"));
                        //Log.d("B","" + (DataMapItem.fromDataItem(event.getDataItem())).getDataMap().getInt("b"));
                        //Log.d("C","" + (DataMapItem.fromDataItem(event.getDataItem())).getDataMap().getInt("c"));
                    }
                }
            }
            if(accVec != null && magnitude(accVec) > 0.5f){
                moving = true;
            }
            /*if(zeroOrient == null){
                zeroOrient = orientVec;
            }*/
            if(gyroVec != null){
                //prevOrientVec = orientVec;
                //float[] normalizedRotVec = diff(orientVec, zeroOrient);
                //BEARING around X (+ to the right)
                if(Math.abs(gyroVec[0]) >= 0.1){
                    deltaBearing = 10 * gyroVec[0];
                }
                //TILT around Z (+ to the down)
                if(Math.abs(gyroVec[2]) >= 0.1 && Math.abs(10 * gyroVec[2]) <= 90){
                    deltaBearing = 10 * gyroVec[2];
                }
                //LATLNG around Y (+ to the right)
                float bearing = streetView.getPanoramaCamera().bearing * (float) Math.PI / 180;
                float speed = 0;
                if(Math.abs(gyroVec[1]) >= 0.1){
                    speed = gyroVec[1];
                }
                float c = 0.1f;
                deltaLatLng = new LatLng(speed * c * Math.cos(bearing), speed * c * Math.sin(bearing));

            }
        }
        if(exiting && accVec != null && magnitude(accVec) > 0.1) {
            if(Math.abs(accVec[0]) >= Math.abs(accVec[2]) && accVec[0] < 0) { //up -> simple exit
                //streetViewPanoramaView.setVisibility(View.INVISIBLE);
                //mapView.setVisibility(View.VISIBLE);
                viewSwitcher.showNext();
                Toast.makeText(this, "EXIT STREETVIEW", Toast.LENGTH_SHORT).show();
            } else if(Math.abs(accVec[0]) >= Math.abs(accVec[2]) && accVec[0] >=  0) { //down -> continue streetView
                Toast.makeText(this, "CONTINUE STREETVIEW", Toast.LENGTH_SHORT).show();
            } else if(Math.abs(accVec[2]) >= Math.abs(accVec[0]) && accVec[2] < 0) { //right -> save path & exit
                Polyline visitedPathPolyline = mGoogleMap.addPolyline(new PolylineOptions().addAll(visitedPath));
                //TODO: save visitedPathPolyline
                viewSwitcher.showNext();
                Toast.makeText(this, "SAVE and EXIT STREETVIEW", Toast.LENGTH_SHORT).show();
            } else if(Math.abs(accVec[2]) >= Math.abs(accVec[0]) && accVec[2] >=  0) { //left -> save & to initial position
                streetView.setPosition(marker.getPosition());
                Toast.makeText(this, "RETURN to INITIAL POINT", Toast.LENGTH_SHORT).show();
            }
            exiting = false;
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
