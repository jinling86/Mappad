package ca.uottawa.ljin027.mappad;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * This class is implemented for CSI5175 Assignment 2.
 * Here a Google Maps Fragment resides in a Fragment view. The OnMapReadyCallback.onMapReady
 * method is overrode as the callback from the Google Maps service. The locations stored in the
 * notes will be read and rendered as markers on the map.
 *
 * PLEASE USE A VALID GOOGLE PLAYS SERVICES KEY TO COMPILE THE APPLICATION!
 *
 * Here are some reference URLs:
 * Google Maps API Set up: https://developers.google.com/maps/documentation/android/start
 * Google Play API Set up: https://developer.android.com/google/play-services/setup.html
 * Examples: https://developers.google.com/maps/documentation/android/
 * Examples: https://developer.android.com/google/auth/api-client.html
 *
 * @author      Ling Jin and Xi Song
 * @version     1.0
 * @since       06/03/2015, LJ
 */
public class Activity_Map extends ActionBarActivity implements OnMapReadyCallback {

    /**
     * Default location (city of ottawa), used when the Google Location service does not work
     */
    public static final LatLng OTTAWA_COORDINATES = new LatLng(45.4214, -75.6919);

    /**
     * Default map camera zoom
     */
    public static final int DEFAULT_CAMERA_ZOOM = 15;

    /**
     * A tag for debug printing
     */
    private final String TAG = "<<< Activity Map >>>";

    /**
     * Notes read from internal file
     */
    private NoteManager mNotes = null;

    /**
     * Initializes all the data and views to be used in the map.
     * @param savedInstanceState saved state, do not need to be processed
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //  Set view of the Activity
        setContentView(R.layout.activity_map);
        // Set the title and icon of action bar
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setTitle(R.string.map_title);
        actionBar.setIcon(R.drawable.ic_pad);
        // Read locations from note
        mNotes = new NoteManager(this);
        // Initialize the map Fragment
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        MapFragment mapFragment = new MapFragment();
        fragmentTransaction.add(R.id.map, mapFragment);
        fragmentTransaction.commit();
        mapFragment.getMapAsync(this);
        // Toast a message
        Toast.makeText(this, "Rendering " + mNotes.size() + " markers", Toast.LENGTH_SHORT).show();
    }

    /**
     * When Maps Service is ready, adds markers into it and customizes the behaviour when a marker
     * is clicked. This gets rids of the button automatically added into the map Fragment.
     * @param map a GoogleMap instance
     */
    @Override
    public void onMapReady(GoogleMap map) {
        // Set the map behaviour
        map.getUiSettings().setZoomGesturesEnabled(true);
        map.setMyLocationEnabled(true);
        // Prepare the icon of the markers
        final BitmapDescriptor markerIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_location);
        // Read titles and locations from notes and add them to the map as markers
        if(mNotes != null && mNotes.size() != 0) {
            for(int i = 0; i < mNotes.size(); i++) {
                MarkerOptions staticMarker = new MarkerOptions()
                    .title(mNotes.getTitle(i))
                    //.snippet(mNotes.getContent(i))
                    .icon(markerIcon)
                    .position(new LatLng(mNotes.getLatitude(i), mNotes.getLongitude(i)));
                map.addMarker(staticMarker);
            }
            // Move the camera to the location of the first note
            LatLng firstLocation = new LatLng(mNotes.getLatitude(0), mNotes.getLongitude(0));
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, DEFAULT_CAMERA_ZOOM-2));
        } else {
            // Move the camera to the default location if there does not exist a note
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(OTTAWA_COORDINATES, DEFAULT_CAMERA_ZOOM));
        }
        // Set a customized GoogleMap.OnMarkerClickListener to get rid of the original buttons
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            Marker currentShown;
            public boolean onMarkerClick(Marker marker) {
                if (marker.equals(currentShown)) {
                    marker.hideInfoWindow();
                    currentShown = null;
                } else {
                    marker.showInfoWindow();
                    currentShown = marker;
                }
                return true;
            }
        });
        Log.d(TAG, "Google map is ready");
    }

    /**
     * Creates the menu which only contains one button for returning to the List Activity.
     * @param menu a menu instance
     * @return always true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_map, menu);
        return true;
    }

    /**
     * Kills current Activity and go back to the List Activity when the back icon is clicked
     * @param item a menu item instance
     * @return always true in the inheriting method
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_back) {
            Intent mIntent = new Intent();
            setResult(RESULT_OK, mIntent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
