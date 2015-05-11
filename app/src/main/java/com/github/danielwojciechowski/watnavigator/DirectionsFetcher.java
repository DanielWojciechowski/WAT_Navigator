/*
package com.github.danielwojciechowski.watnavigator;

import android.os.AsyncTask;

import com.google.android.gms.maps.model.LatLng;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.maps.android.PolyUtil;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

class DirectionsFetcher extends AsyncTask<URL, Integer, String> {

    private List<LatLng> latLngs = new ArrayList<>();
    private static final HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();
    private static final JsonFactory JSON_FACTORY = new JacksonFactory();

    private String origin;
    private String destination;

    public DirectionsFetcher(String origin,String destination) {
        this.origin = origin;
        this.destination = destination;
    }

*/
/*    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        clearMarkers();
        getActivity().setProgressBarIndeterminateVisibility(Boolean.TRUE);

    }*//*


    protected void onPostExecute(Void result) {
        directionsFetched=true;
        System.out.println("Adding polyline");
        addPolylineToMap(latLngs);
        System.out.println("Fix Zoom");
        GoogleMapUtis.fixZoomForLatLngs(googleMap, latLngs);
        System.out.println("Start anim");
        animator.startAnimation(false, latLngs);
        updateNavigationStopStart();
        getActivity().setProgressBarIndeterminateVisibility(Boolean.FALSE);
    }

    @Override
    protected String doInBackground(URL... params) {
        try {
            HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
                @Override
                public void initialize(HttpRequest request) {
                    request.setParser(new JsonObjectParser(JSON_FACTORY));
                }
            });

            GenericUrl url = new GenericUrl("http://maps.googleapis.com/maps/api/directions/json");
            url.put("origin", "Chicago,IL");
            url.put("destination", "Los Angeles,CA");
            url.put("sensor",false);

            HttpRequest request = requestFactory.buildGetRequest(url);
            HttpResponse httpResponse = request.execute();
            DirectionsResult directionsResult = httpResponse.parseAs(DirectionsResult.class);
            String encodedPoints = directionsResult.routes.get(0).overviewPolyLine.points;
            latLngs = PolyUtil.decode(encodedPoints);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}*/
