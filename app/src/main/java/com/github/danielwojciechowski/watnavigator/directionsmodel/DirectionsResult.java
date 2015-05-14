package com.github.danielwojciechowski.watnavigator.directionsmodel;

import com.google.api.client.util.Key;

import java.util.List;

/**
 * Created by Daniel on 2015-05-03.
 */
public class DirectionsResult {
    @Key("routes")
    public List<Route> routes;
}
