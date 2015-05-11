package com.github.danielwojciechowski.watnavigator;

import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.danielwojciechowski.watnavigator.datamodel.Building;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class WatMap extends AppCompatActivity implements LocationListener {

    private static final int ZOOM_LEVEL = 17;


    private GoogleMap googleMap;
    private Location location;
    private Marker destinationMarker;
    private TextView locationTv;
    private boolean mapPrepared = false;

    private ListView mDrawerList;
    private ArrayAdapter<String> mAdapter;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private String mActivityTitle;

    private Map<String, Building> buildings = new TreeMap<>(new Comparator<String>() {
        @Override public int compare(String s1, String s2) {
            return Integer.valueOf(Integer.parseInt(s1)).compareTo(Integer.parseInt(s2));
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prepareDatabase();
        prepareMap();
        prepareMenuPanel();
    }

    private void prepareMenuPanel() {
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        mActivityTitle = getTitle().toString();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        mDrawerList = (ListView)findViewById(R.id.navList);
        addDrawerItems();
        setupDrawer();
    }

    private void prepareMap() {
        if (!isGooglePlayServicesAvailable()) {
            finish();
        }
        setContentView(R.layout.activity_wat_map);
        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.googleMap);
        googleMap = supportMapFragment.getMap();
        googleMap.setMyLocationEnabled(true);
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String bestProvider = locationManager.getBestProvider(criteria, true);
        location = locationManager.getLastKnownLocation(bestProvider);
//        location = googleMap.getMyLocation();
        if (location != null) {
            initializeLocation(location);
        }
        locationManager.requestLocationUpdates(bestProvider, 0, 0, this);
    }

    public void initializeLocation(Location location) {
        locationTv = (TextView) findViewById(R.id.latlongLocation);
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        LatLng latLng = new LatLng(latitude, longitude);
/*        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(ZOOM_LEVEL));*/

        updateLocationText(latitude, longitude);
    }

    private void updateLocationText(double latitude, double longitude) {
        locationTv.setText("Latitude:" + latitude + ", Longitude:" + longitude);
    }

    public void onMarkerSet(LatLng latLng) {
        /*googleMap.addMarker(new MarkerOptions().position(latLng));
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                new CameraPosition.Builder().target(latLng).zoom(ZOOM_LEVEL).build()));*/

        /* przełączenie do nawigacji google
        Uri gmmIntentUri = Uri.parse("google.navigation:q="+latLng.latitude +"," + latLng.longitude);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);*/

        new DirectionsFetcher(Double.toString(location.getLatitude()) + "," + Double.toString(location.getLongitude()),
                Double.toString(latLng.latitude) + "," + Double.toString(latLng.longitude)).execute();
    }

    @Override
    public void onLocationChanged(Location location) {
/*        if(!mapPrepared){
            initializeLocation(location);
            mapPrepared = true;
        }*/
        updateLocationText(location.getLatitude(), location.getLongitude());
    }

    private Building setDestinationMarker(int position) {
        Building building = buildings.get(buildings.keySet().toArray(new String[buildings.size()])[position]);
        googleMap.clear();
        destinationMarker = googleMap.addMarker(new MarkerOptions().position(building.getLatLong()));
        mDrawerLayout.closeDrawers();
        return building;
    }

    private LatLng currentLatLng() {
        if (location != null){
            return new LatLng(location.getLatitude(), location.getLongitude());
        }
        else return null;
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    private void prepareDatabase() {
        SQLiteDatabase myDB = null;
        String TableName = "building";

        try {
            myDB = this.openOrCreateDatabase("WatMap", MODE_PRIVATE, null);

            myDB.execSQL("CREATE TABLE IF NOT EXISTS " + TableName + " (number VARCHAR(4), latitude DOUBLE, longitude);");

            myDB.execSQL("INSERT INTO "
                    + TableName
                    + " VALUES ('100', 52.253200, 20.900204),('65', 52.255337, 20.903774),('61', 52.254157, 20.902425);");
        } catch (Exception e){
            e.printStackTrace();
        }
        if (myDB == null) throw new Error("Database is null");
        Cursor c = myDB.rawQuery("SELECT * FROM " + TableName , null);
        if (c.moveToFirst()) {
            while (!c.isAfterLast()) {
                String numberCursor = c.getString(c.getColumnIndex("number"));
                buildings.put(numberCursor, new Building(numberCursor,
                        c.getDouble(c.getColumnIndex("latitude")),
                        c.getDouble(c.getColumnIndex("longitude"))));
                c.moveToNext();
            }
        }
        c.close();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    private boolean isGooglePlayServicesAvailable() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == status) {
            return true;
        } else {
            GooglePlayServicesUtil.getErrorDialog(status, this, 0).show();
            return false;
        }
    }

    private void addDrawerItems() {
        mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, buildings.keySet().toArray(new String[buildings.size()]));
        mDrawerList.setAdapter(mAdapter);

        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Building building = setDestinationMarker(position);
                Toast.makeText(WatMap.this, "Selected:" + building.getNumber(), Toast.LENGTH_SHORT).show();
                onMarkerSet(building.getLatLong());
            }
        });
    }

    private void setupDrawer() {
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getSupportActionBar().setTitle("Lista budynków");
                invalidateOptionsMenu();
            }
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getSupportActionBar().setTitle(mActivityTitle);
                invalidateOptionsMenu();
            }
        };
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

//-----------------------------------DIRECTIONS FETCHER---------------------

    private List<LatLng> latLngs = new ArrayList<>();
    private List<Marker> markers = new ArrayList<>();
    private boolean directionsFetched = false;
    private Animator animator = new Animator();

    private static final HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();
    private static final JsonFactory JSON_FACTORY = new JacksonFactory();

    public void clearMarkers() {
        googleMap.clear();
        markers.clear();
    }

    public void addPolylineToMap(List<LatLng> latLngs) {
        PolylineOptions options = new PolylineOptions();
        for (LatLng latLng : latLngs) {
            options.add(latLng);
        }
        googleMap.addPolyline(options);
    }

    private class DirectionsFetcher extends AsyncTask<URL, Integer, Void> {

        private String origin;
        private String destination;

        public DirectionsFetcher(String origin,String destination) {
            this.origin = origin;
            this.destination = destination;
        }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        clearMarkers();
        String[] destination = this.destination.split(",");
        googleMap.addMarker(new MarkerOptions().position(new LatLng(Double.parseDouble(destination[0]), Double.parseDouble(destination[1]))));
//        getActivity().setProgressBarIndeterminateVisibility(Boolean.TRUE);

    }
        protected void onPostExecute(Void result) {
            directionsFetched=true;
            addPolylineToMap(latLngs);
            GoogleMapUtils.fixZoomForLatLngs(googleMap, latLngs);
            animator.setInitialCameraPosition(latLngs.get(0), latLngs.get(1));
           // animator.startAnimation(false, latLngs);
            //updateNavigationStopStart();
            //getActivity().setProgressBarIndeterminateVisibility(Boolean.FALSE);
        }

        @Override
        protected Void doInBackground(URL... params) {
            try {
                HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
                    @Override
                    public void initialize(HttpRequest request) {
                        request.setParser(new JsonObjectParser(JSON_FACTORY));
                    }
                });

                GenericUrl url = new GenericUrl("http://maps.googleapis.com/maps/api/directions/json");
                url.put("origin", origin);
                url.put("destination", destination);
                url.put("sensor",false);
                url.put("mode", "walking");

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
    }

//----------------------------------------ANIMATOR-----------------------------

    private final Handler mHandler = new Handler();

    public class Animator implements Runnable {

        private static final int ANIMATE_SPEEED = 1500;
        private static final int ANIMATE_SPEEED_TURN = 1500;
        private static final int BEARING_OFFSET = 20;

        private final Interpolator interpolator = new LinearInterpolator();

        private boolean animating = false;

        private List<LatLng> latLngs = new ArrayList<>();

        int currentIndex = 0;

        float tilt = 90;
        float zoom = 15.5f;
        boolean upward=true;

        long start = SystemClock.uptimeMillis();

        LatLng endLatLng = null;
        LatLng beginLatLng = null;

        boolean showPolyline = false;

        private Marker trackingMarker;

        public void reset() {
            resetMarkers();
            start = SystemClock.uptimeMillis();
            currentIndex = 0;
            endLatLng = getEndLatLng();
            beginLatLng = getBeginLatLng();

        }

        public void stopAnimation() {
            animating=false;
            mHandler.removeCallbacks(animator);

        }

        public void initialize(boolean showPolyLine) {
            reset();
            this.showPolyline = showPolyLine;

            highLightMarker(0);

            if (showPolyLine) {
                polyLine = initializePolyLine();
            }

            // We first need to put the camera in the correct position for the first run (we need 2 markers for this).....
            LatLng markerPos = latLngs.get(0);
            LatLng secondPos = latLngs.get(1);

            setInitialCameraPosition(markerPos, secondPos);

        }

        private void setInitialCameraPosition(LatLng markerPos, LatLng secondPos) {

            float bearing = GoogleMapUtils.bearingBetweenLatLngs(markerPos,secondPos);

            trackingMarker = googleMap.addMarker(new MarkerOptions().position(markerPos)
                    .title("title")
                    .snippet("snippet"));

            float mapZoom = googleMap.getCameraPosition().zoom >=16 ? googleMap.getCameraPosition().zoom : 16;

            CameraPosition cameraPosition =
                    new CameraPosition.Builder()
                            .target(markerPos)
                            .bearing(bearing + BEARING_OFFSET)
                            .tilt(90)
                            .zoom(mapZoom)
                            .build();

            googleMap.animateCamera(
                    CameraUpdateFactory.newCameraPosition(cameraPosition),
                    ANIMATE_SPEEED_TURN,
                    new GoogleMap.CancelableCallback() {

                        @Override
                        public void onFinish() {
                            System.out.println("finished camera");
                            /*animator.reset();
                            Handler handler = new Handler();
                            handler.post(animator);*/
                        }

                        @Override
                        public void onCancel() {
                            System.out.println("cancelling camera");
                        }
                    }
            );
        }

        private Polyline polyLine;
        private PolylineOptions rectOptions = new PolylineOptions();


        private Polyline initializePolyLine() {
            //polyLinePoints = new ArrayList<LatLng>();
            rectOptions.add(latLngs.get(0));
            return googleMap.addPolyline(rectOptions);
        }

        /**
         * Add the marker to the polyline.
         */
        private void updatePolyLine(LatLng latLng) {
            List<LatLng> points = polyLine.getPoints();
            points.add(latLng);
            polyLine.setPoints(points);
        }

        public void startAnimation(boolean showPolyLine,List<LatLng> latLngs) {
            if (trackingMarker!=null) {
                trackingMarker.remove();
            }
            this.animating = true;
            this.latLngs=latLngs;
            if (latLngs.size()>2) {
                initialize(showPolyLine);
            }

        }

        public boolean isAnimating() {
            return this.animating;
        }


        @Override
        public void run() {

            long elapsed = SystemClock.uptimeMillis() - start;
            double t = interpolator.getInterpolation((float)elapsed/ANIMATE_SPEEED);
            LatLng intermediatePosition = SphericalUtil.interpolate(beginLatLng, endLatLng, t);

            Double mapZoomDouble = 18.5-( Math.abs((0.5- t))*5);
            float mapZoom =  mapZoomDouble.floatValue();

            System.out.println("mapZoom = " + mapZoom);

            trackingMarker.setPosition(intermediatePosition);

            if (showPolyline) {
                updatePolyLine(intermediatePosition);
            }

            if (t< 1) {
                mHandler.postDelayed(this, 16);
            } else {

                System.out.println("Move to next marker.... current = " + currentIndex + " and size = " + latLngs.size());
                // imagine 5 elements -  0|1|2|3|4 currentindex must be smaller than 4
                if (currentIndex<latLngs.size()-2) {

                    currentIndex++;

                    endLatLng = getEndLatLng();
                    beginLatLng = getBeginLatLng();


                    start = SystemClock.uptimeMillis();

                    Double heading = SphericalUtil.computeHeading(beginLatLng, endLatLng);

                    highLightMarker(currentIndex);

                    CameraPosition cameraPosition =
                            new CameraPosition.Builder()
                                    .target(endLatLng)
                                    .bearing(heading.floatValue() /*+ BEARING_OFFSET*/) // .bearing(bearingL  + BEARING_OFFSET)
                                    .tilt(tilt)
                                    .zoom(googleMap.getCameraPosition().zoom)
                                    .build();

                    googleMap.animateCamera(
                            CameraUpdateFactory.newCameraPosition(cameraPosition),
                            ANIMATE_SPEEED_TURN,
                            null
                    );

                    //start = SystemClock.uptimeMillis();
                    mHandler.postDelayed(this, 16);

                } else {
                    currentIndex++;
                    highLightMarker(currentIndex);
                    stopAnimation();
                }

            }
        }



        private LatLng getEndLatLng() {
            return latLngs.get(currentIndex+1);
        }

        private LatLng getBeginLatLng() {
            return latLngs.get(currentIndex);
        }

    }

    private void highLightMarker(int index) {
        if (markers.size()>=index+1) {
            highLightMarker(markers.get(index));
        }
    }

    /**
     * Highlight the marker by marker.
     */
    private void highLightMarker(Marker marker) {

        if (marker!=null) {
            marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
            marker.showInfoWindow();
        }

    }

    private void resetMarkers() {
        for (Marker marker : this.markers) {
            marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        }
    }
}
