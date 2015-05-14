package com.github.danielwojciechowski.watnavigator.map;

import android.location.Location;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.List;

class GoogleMapUtils {

    public static Location convertLatLngToLocation(LatLng latLng) {
        Location loc = new Location("someLoc");
        loc.setLatitude(latLng.latitude);
        loc.setLongitude(latLng.longitude);
        return loc;
    }

    public static float bearingBetweenLatLngs(LatLng begin,LatLng end) {
        Location beginL= convertLatLngToLocation(begin);
        Location endL= convertLatLngToLocation(end);
        return beginL.bearingTo(endL);
    }

    public static void fixZoomForLatLngs(GoogleMap googleMap, List<LatLng> latLngs) {
        if (latLngs!=null && latLngs.size() > 0) {
            LatLngBounds.Builder bc = new LatLngBounds.Builder();

            for (LatLng latLng: latLngs) {
                bc.include(latLng);
            }

            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bc.build(), 50),4000,null);
        }
    }
}