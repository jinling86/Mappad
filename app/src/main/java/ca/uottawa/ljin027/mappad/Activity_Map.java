package ca.uottawa.ljin027.mappad;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by Ling Jin on 06/03/2015.
 */
public class Activity_Map extends ActionBarActivity implements OnMapReadyCallback {

    public static final LatLng OTTAWA_COORDINATES = new LatLng(45.4214, -75.6919);
    public static final int DEFAULT_CAMERA_ZOOM = 15;
    private final String TAG = "<<<<< Activity Map >>>>>";

    private NoteManager mNotes = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_map);
        // Set the action bar style
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setTitle(R.string.map_title);
        actionBar.setIcon(R.drawable.ic_pad);

        mNotes = new NoteManager(this);

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        MapFragment mapFragment = new MapFragment();
        fragmentTransaction.add(R.id.map, mapFragment);
        fragmentTransaction.commit();

        mapFragment.getMapAsync(this);
        Toast.makeText(this, "Rendering " + mNotes.size() + " markers", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        final BitmapDescriptor markerIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_location);

        map.getUiSettings().setZoomGesturesEnabled(true);
        map.setMyLocationEnabled(true);

        if(mNotes != null && mNotes.size() != 0) {
            for(int i = 0; i < mNotes.size(); i++) {
                MarkerOptions staticMarker = new MarkerOptions()
                    .title(mNotes.getTitle(i))
                    //.snippet(mNotes.getContent(i))
                    .icon(markerIcon)
                    .position(new LatLng(mNotes.getLatitude(i), mNotes.getLongitude(i)));
                map.addMarker(staticMarker);
            }
            LatLng firstLocation = new LatLng(mNotes.getLatitude(0), mNotes.getLongitude(0));
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, DEFAULT_CAMERA_ZOOM-4));
        } else {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(OTTAWA_COORDINATES, DEFAULT_CAMERA_ZOOM));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_map, menu);
        return true;
    }

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
