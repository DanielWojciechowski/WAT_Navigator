package com.github.danielwojciechowski.watnavigator.datamodel;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Daniel on 2015-05-02.
 */
public class Building {
    private String number;
    private double latitude;
    private double longitude;

    public LatLng getLatLong(){
        return new LatLng(latitude, longitude);
    }

    public Building() {
    }

    public Building(String number, double latitude, double longitude) {
        this.number = number;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
