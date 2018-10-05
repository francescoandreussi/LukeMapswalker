package com.mis.francescoandreussi.lukemapswalker;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.StreetViewPanoramaFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.StreetViewPanoramaView;

public class StreetViewActivity extends FragmentActivity implements OnStreetViewPanoramaReadyCallback {
    StreetViewPanorama panorama;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_streetview);
        StreetViewPanoramaFragment mStreetViewPanoramaFragment = (StreetViewPanoramaFragment) getFragmentManager().findFragmentById(R.id.streetviewpanorama);

        mStreetViewPanoramaFragment.getStreetViewPanoramaAsync(this);
    }

    @Override
    public void onStreetViewPanoramaReady (StreetViewPanorama streetViewPanorama) {
        Bundle extras = getIntent().getExtras();
        panorama = streetViewPanorama;
        LatLng point = new LatLng(extras.getDouble("startingLatitude", 46.233333), extras.getDouble("startingLongitude", 13.15));
        streetViewPanorama.setPosition(point);
    }
}
