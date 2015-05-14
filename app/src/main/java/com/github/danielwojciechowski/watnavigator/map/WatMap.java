package com.github.danielwojciechowski.watnavigator.map;

import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.danielwojciechowski.watnavigator.R;
import com.github.danielwojciechowski.watnavigator.datamodel.Building;
import com.github.danielwojciechowski.watnavigator.datamodel.DatabaseRes;
import com.github.danielwojciechowski.watnavigator.directionsmodel.DirectionsResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
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
    private TextView locationTv;
    private boolean mapPrepared = false;
    private Building selectedBuilding;

    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private String mActivityTitle;

    private boolean navigationMode = false;

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

        locationManager.requestLocationUpdates(bestProvider, 5000, 0, this);
//        location = googleMap.getMyLocation();

        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if(locationManager.getLastKnownLocation(bestProvider) != null) {
            location = locationManager.getLastKnownLocation(bestProvider);
        }
        if (location != null) {
            initializeLocation(location);
        }
    }

    public void initializeLocation(Location location) {
        locationTv = (TextView) findViewById(R.id.latlongLocation);
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        LatLng latLng = new LatLng(latitude, longitude);
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(ZOOM_LEVEL));

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
        navigationMode = true;
    }

    @Override
    public void onLocationChanged(Location location) {
        System.out.println("Location CHANGED!!!");
        if(!mapPrepared){
            initializeLocation(location);
            mapPrepared = true;
        }
        updateLocationText(location.getLatitude(), location.getLongitude());
        this.location = location;

        if(navigationMode){
            new DirectionsFetcher(Double.toString(location.getLatitude()) + "," + Double.toString(location.getLongitude()),
                    Double.toString(selectedBuilding.getLatitude()) + "," + Double.toString(selectedBuilding.getLongitude())).execute();
           // googleMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
        }
    }

    private Building setDestinationMarker(int position) {
        selectedBuilding = buildings.get(buildings.keySet().toArray(new String[buildings.size()])[position]);
        googleMap.clear();
        googleMap.addMarker(new MarkerOptions().position(selectedBuilding.getLatLong()));
        mDrawerLayout.closeDrawers();
        return selectedBuilding;
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
        String tableName = "building";

        try {
            myDB = this.openOrCreateDatabase("WatMap", MODE_PRIVATE, null);
            myDB.execSQL("DROP TABLE "+tableName);
            myDB.execSQL("CREATE TABLE " + tableName + " (number VARCHAR(4), latitude DOUBLE, longitude);");

            myDB.execSQL(DatabaseRes.getBuildings());
        } catch (Exception e){
            e.printStackTrace();
        }
        if (myDB == null) throw new Error("Database is null");
        Cursor c = myDB.rawQuery("SELECT * FROM " + tableName , null);
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
        return mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);

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
        ArrayAdapter<String> mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, buildings.keySet().toArray(new String[buildings.size()]));
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

    private List<LatLng> checkpoints = new ArrayList<>();

    private static final HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();
    private static final JsonFactory JSON_FACTORY = new JacksonFactory();

    public void clearMarkers() {
        googleMap.clear();
    }

    public void addPolylineToMap(List<LatLng> checkpoints) {
        PolylineOptions options = new PolylineOptions();
        for (LatLng checkpoint : checkpoints) {
            options.add(checkpoint);
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

    }
        protected void onPostExecute(Void result) {
            addPolylineToMap(checkpoints);
            GoogleMapUtils.fixZoomForLatLngs(googleMap, checkpoints);
            setInitialCameraPosition(checkpoints.get(0), checkpoints.get(1));
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
                checkpoints = PolyUtil.decode(encodedPoints);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return null;
        }
    }

    private void setInitialCameraPosition(LatLng markerPos, LatLng secondPos) {

        float bearing = GoogleMapUtils.bearingBetweenLatLngs(markerPos,secondPos);
        float mapZoom = googleMap.getCameraPosition().zoom >=16 ? googleMap.getCameraPosition().zoom : 16;

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(markerPos)
                .bearing(bearing)
                .tilt(90)
                .zoom(mapZoom)
                .build();

        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }
}
