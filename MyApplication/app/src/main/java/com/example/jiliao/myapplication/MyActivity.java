package com.example.jiliao.myapplication;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.SyncStateContract;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static com.google.android.gms.common.api.GoogleApiClient.*;


public class MyActivity extends FragmentActivity
        implements LocationListener,
        OnConnectionFailedListener,
        GoogleMap.OnMapClickListener,
        GooglePlayServicesClient.ConnectionCallbacks {

    private static final String TAG = "myApp"+MyActivity.class.getSimpleName();
    private Button buttonGo;
    private TextView textView;
    private TextView locationView;
    private TextView addressView;
    private Location location = null;

    LocationManager locationManager = null;

    private GoogleMap map = null;
    private MapFragment mapFragment=null;
    private LocationClient mLocationClient;
    Marker marker = null;

    // These settings are the same as the settings for the map. They will in fact give you updates
    // at the maximal rates currently possible.
//    private static final LocationRequest REQUEST = LocationRequest.create()
//            .setInterval(5000)         // 5 seconds
//            .setFastestInterval(16)    // 16ms = 60fps
//            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        mapFragment = MapFragment.newInstance();
        FragmentTransaction fragmentTransaction =
                getFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.map_container, mapFragment);
        fragmentTransaction.commit();

        //setVisible(false);

        //centerMapOnMyLocation();


        locationView = (TextView)findViewById(R.id.textLocation);
        addressView = (TextView)findViewById(R.id.textAddress);


        // listen and process button click event
        buttonGo = (Button)findViewById(R.id.button);
        textView = (TextView)findViewById(R.id.textView);

        buttonGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (textView.getCurrentTextColor() == Color.BLUE)
                    textView.setTextColor(Color.RED);
                else
                    textView.setTextColor(Color.BLUE);

                // btw, start location service
                //initLocationService();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        initLocationService();

        setUpMapIfNeeded();

        setUpLocationClientIfNeeded();
        mLocationClient.connect();
    }

    /**
     * @return the last know best location
     */
    private Location getLastBestLocation() {
        Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location locationNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        long GPSLocationTime = 0;
        if (null != locationGPS) { GPSLocationTime = locationGPS.getTime(); }

        long NetLocationTime = 0;

        if (null != locationNet) {
            NetLocationTime = locationNet.getTime();
        }

        if ( 0 < GPSLocationTime - NetLocationTime ) {
            return locationGPS;
        }
        else {
            return locationNet;
        }
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (map == null) {
            map = mapFragment.getMap();

            // Try to obtain the map from the SupportMapFragment.
//            SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
//            map = supportMapFragment.getMap();


            map.setOnMapClickListener(this);
            map.setMyLocationEnabled(true);
            map.setMapType(GoogleMap.MAP_TYPE_HYBRID);

            //map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
              //      .getMap();
            // Check if we were successful in obtaining the map.
            if (map != null) {
                map.setMyLocationEnabled(true);
                //map.setOnMyLocationButtonClickListener(this);
            }

        }

        centerMapOnMyLocation();
    }

    private void centerMapOnMyLocation() {

        if (map!=null) {
            Log.d(TAG, "centerMapOnMyLocation " );
            //Location location = map.getMyLocation();
            LatLng myLocation = null;

            if (location != null) {
                myLocation = new LatLng(location.getLatitude(),
                        location.getLongitude());

                map.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 13));

                displayAddressOnMarker(location);

                //setVisible(true);


//                map.animateCamera(CameraUpdateFactory.newLatLngZoom(
//                        new LatLng(location.getLatitude(), location.getLongitude()), 2));
//
//                CameraPosition cameraPosition = new CameraPosition.Builder()
//                        .target(new LatLng(location.getLatitude(), location.getLongitude()))      // Sets the center of the map to location user
//                        .zoom(17)                   // Sets the zoom
//                        .bearing(90)                // Sets the orientation of the camera to east
//                        .tilt(40)                   // Sets the tilt of the camera to 30 degrees
//                        .build();                   // Creates a CameraPosition from the builder
//                map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));



            }
        }

    }

    private void displayAddressOnMarker(Location myLocation) {

        if (myLocation != null) {
            LatLng myLatLng = new LatLng(location.getLatitude(),
                    location.getLongitude());

            MarkerOptions markerOptions = new MarkerOptions()
                    .title("Current: " + myLatLng.latitude + " " + myLatLng.longitude)
                    .snippet(getAddressFromLocation(location))
                    .position(myLatLng)
                    .visible(true);

            // remove previous marker and add new marker.
            if (marker != null)
                marker.remove();

            marker = map.addMarker(markerOptions);
            marker.showInfoWindow();
        }
    }

    private void setUpLocationClientIfNeeded() {
        if (mLocationClient == null) {
            mLocationClient = new LocationClient(
                    getApplicationContext(),
                    this,  // ConnectionCallbacks
                    this); // OnConnectionFailedListener
        }
    }





    private void initLocationService() {
        // Start location service.
        locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_HIGH);

        String provider = locationManager.getBestProvider(criteria, true);

        Log.d(TAG, "onResume provider=" + provider);

        if (provider != null)
            locationManager.requestLocationUpdates(provider, 2000, 2, this);

        // set the initial location before GPS onChanged Listener calls back.
        location = getLastBestLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationService();

        if (mLocationClient != null) {
            mLocationClient.disconnect();
        }
    }

    private void stopLocationService() {
        locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.removeUpdates(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged " + location.getLatitude() + ", " + location.getLongitude());
        this.location = location;

        // set location and address field
        displayNewLocation(location);

        displayAddressOnMarker(location);
        //centerMapOnMyLocation();

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    private void displayNewLocation(Location location) {
        Log.d(TAG, "displayNewLocation " + location.getLatitude() + ", " + location.getLongitude());
        // display lat and lon;
        locationView.setText("Lat:"+location.getLatitude()+" "+"Lon:" + location.getLongitude());

        //display address
        addressView.setText(getAddressFromLocation(location));
    }

    private String getAddressFromLocation(Location location) {
        Geocoder geocoder = new Geocoder(this.getBaseContext(), Locale.getDefault());
        String fullAddress = null;
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(),1);
            String address = addresses.get(0).getAddressLine(0);
            String city = addresses.get(0).getAddressLine(1);
            String country = addresses.get(0).getAddressLine(2);
            fullAddress = address + ", " + city + ", " + country;
            //NotifyUser("Current address: " + fullAddress);
            Log.d(TAG, "getAddressFromLocation - address is " + fullAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fullAddress;
    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onMapClick(LatLng position) {
        map.addMarker(new MarkerOptions().position(position).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_launcher)));
    }
}
