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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
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
 * This class is implemented for CSI5175 Assignment 2
 *
 * @author      Ling Jin
 * @version     1.0
 * @since       04/03/2015
 */
public class Activity_Edit extends ActionBarActivity implements OnMapReadyCallback {
    private final String TAG = "<<<<< Activity Edit >>>>>";

    private EditText mView_Title = null;
    //private EditText mView_Content = null;
    private TextView mView_Latitude = null;
    private TextView mView_Longitude = null;
    private TextView mView_RefLatitude = null;
    private TextView mView_RefLongitude = null;
    private TextView mView_GoogleHint = null;

    private boolean mControlledFinish = false;
    private boolean mFirstCameraChange = true;
    private int mPosition;
    private String mTitle;
    private final String  mContent = "";
    private double mLatitude;
    private double mLongitude;
    private double mNewLatitude;
    private double mNewLongitude;
    private boolean mGoogleServiceAvailable = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_edit);
        // Set the action bar style
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setTitle(R.string.edit_title);
        actionBar.setIcon(R.drawable.ic_pad);

        mView_Title = (EditText) findViewById(R.id.note_title);
        //mView_Content = (EditText) findViewById(R.id.note_content);
        mView_Latitude = (TextView) findViewById(R.id.textView_Latitude);
        mView_Longitude = (TextView) findViewById(R.id.textView_Longitude);
        mView_RefLatitude = (TextView) findViewById(R.id.textView_RefLatitude);
        mView_RefLongitude = (TextView) findViewById(R.id.textView_RefLongitude);
        mView_GoogleHint = (TextView) findViewById(R.id.textView_GoogleHint);

        ImageButton mButton_Update = (ImageButton) findViewById(R.id.button_Update);
        mButton_Update.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLatitude = mNewLatitude;
                mLongitude = mNewLongitude;
                mView_Latitude.setTextColor(getResources().getColor(R.color.black));
                mView_Longitude.setTextColor(getResources().getColor(R.color.black));
                fillCoordinates();
            }
        });

        detectGoogleService();
        if(mGoogleServiceAvailable) {
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            MapFragment mapFragment = new MapFragment();
            fragmentTransaction.add(R.id.locating, mapFragment);
            fragmentTransaction.commit();
            mapFragment.getMapAsync(this);
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mPosition = extras.getInt(NoteManager.EXTRA_INDEX);
            mTitle = extras.getString(NoteManager.EXTRA_TITLE);
            //mContent = extras.getString(NoteManager.EXTRA_CONTENT);
            mLatitude = extras.getDouble(NoteManager.EXTRA_LATITUDE);
            mLongitude = extras.getDouble(NoteManager.EXTRA_LONGITUDE);
        } else {
            Log.d(TAG, "Receive a null Bundle, please check!");
        }
        mNewLatitude = mLatitude;
        mNewLongitude = mLongitude;

        if(mPosition == NoteManager.NEW_NODE_POSITION) {
            Log.d(TAG, "Activity received wrong intent, notes Passing Failure !");
        } else {
            fillViews();
        }
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "Back button pressed");
        controlledFinish();
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        readInput();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(!mControlledFinish) {
            if (NoteManager.saveChanges(mPosition, mTitle, mContent, mLatitude, mLongitude)
                    == NoteManager.NEED_SYNCHRONIZE) {
                AWSManager.upload();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_done) {
            Log.d(TAG, "Finish button pressed");
            controlledFinish();
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        final GoogleMap mMap = map;

        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.setMyLocationEnabled(true);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLatitude, mLongitude), Activity_Map.DEFAULT_CAMERA_ZOOM));

        final BitmapDescriptor locatingIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_locating);
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            public void onCameraChange(CameraPosition arg0) {
                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(arg0.target).icon(locatingIcon));
                mNewLatitude = arg0.target.latitude;
                mNewLongitude = arg0.target.longitude;
                if(!mFirstCameraChange) {
                    mView_Latitude.setTextColor(getResources().getColor(R.color.grey));
                    mView_Longitude.setTextColor(getResources().getColor(R.color.grey));
                }
                mFirstCameraChange = false;
                fillRefCoordinates();
            }
        });

        Log.d(TAG, "Google map is ready");
    }

    private void fillViews() {
        if (mPosition != NoteManager.NEW_NODE_POSITION) {
            if(mTitle.compareTo(NoteManager.DEFAULT_TITLE) == 0)
                mTitle = "";
            mView_Title.setText(mTitle);
            //mView_Content.setText(mContent);
            fillCoordinates();
        }
    }

    private String getLatitude(double latitude) {
        if(latitude > 0) {
            return String.format("%.5f ° N", latitude);
        } else {
            return String.format("%.5f ° S", latitude);
        }
    }

    private String getLongitude(double longitude) {
        if(longitude > 0) {
            return String.format("%.5f ° E", longitude);
        } else {
            return String.format("%.5f ° W", longitude);
        }
    }

    private void fillCoordinates() {
        mView_Latitude.setText(getLatitude(mLatitude));
        mView_Longitude.setText(getLongitude(mLongitude));
    }

    private void fillRefCoordinates() {
        mView_RefLatitude.setText(getLatitude(mNewLatitude));
        mView_RefLongitude.setText(getLongitude(mNewLongitude));
    }


    Bundle makeBundle() {
        Bundle bundle = new Bundle();
        bundle.putInt(NoteManager.EXTRA_INDEX, mPosition);
        bundle.putString(NoteManager.EXTRA_TITLE, mTitle);
        bundle.putString(NoteManager.EXTRA_CONTENT, mContent);
        bundle.putDouble(NoteManager.EXTRA_LATITUDE, mLatitude);
        bundle.putDouble(NoteManager.EXTRA_LONGITUDE, mLongitude);
        return bundle;
    }

    private void controlledFinish() {
        readInput();
        Intent mIntent = new Intent();
        mIntent.putExtras(makeBundle());
        setResult(RESULT_OK, mIntent);
        mControlledFinish = true;
    }

    private void readInput() {
        mTitle = mView_Title.getText().toString();
        //mContent = mView_Content.getText().toString();
    }

    void detectGoogleService() {
        int status= GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(status == ConnectionResult.SUCCESS) {
            mGoogleServiceAvailable = true;
            mView_GoogleHint.setVisibility(View.GONE);
        } else {
            mGoogleServiceAvailable = false;
            mView_GoogleHint.setVisibility(View.VISIBLE);
            String errorMsg = "Google Play Service Error :\n";
            errorMsg += GooglePlayServicesUtil.getErrorString(status);
            errorMsg += "\nDisable All Google Maps Functions";
            mView_GoogleHint.setText(errorMsg);
        }
    }
}
