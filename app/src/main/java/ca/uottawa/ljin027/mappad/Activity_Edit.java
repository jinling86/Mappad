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

import com.google.android.gms.common.SignInButton;
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
 * Created by Ling Jin on 04/03/2015.
 */
public class Activity_Edit extends ActionBarActivity implements OnMapReadyCallback {
    private final String TAG = "<<<<< Activity Edit >>>>>";

    private EditText mView_Title;
    private EditText mView_Content;
    private TextView mView_Latitude;
    private TextView mView_Longitude;
    private ImageButton mButton_Update;

    private boolean mControlledFinish = false;
    private int mPosition;
    private String mTitle;
    private String mContent;
    private double mLatitude;
    private double mLongitude;
    private double mNewLatitude;
    private double mNewLongitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_edit);
        // Set the action bar style
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setTitle(R.string.edit_title);
        actionBar.setIcon(R.drawable.ic_pad);

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        MapFragment mapFragment = new MapFragment();
        fragmentTransaction.add(R.id.locating, mapFragment);
        fragmentTransaction.commit();
        mapFragment.getMapAsync(this);

        mView_Title = (EditText) findViewById(R.id.note_title);
        mView_Content = (EditText) findViewById(R.id.note_content);
        mView_Latitude = (TextView) findViewById(R.id.textView_Latitude);
        mView_Longitude = (TextView) findViewById(R.id.textView_Longitude);
        mButton_Update = (ImageButton) findViewById(R.id.button_Update);
        mButton_Update.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLatitude = mNewLatitude;
                mLongitude = mNewLongitude;
                fillCoordinates();
            }
        });

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mPosition = extras.getInt(NoteManager.INDEX);
            mTitle = extras.getString(NoteManager.TITLE);
            mContent = extras.getString(NoteManager.CONTENT);
            mLatitude = extras.getDouble(NoteManager.LATITUDE);
            mLongitude = extras.getDouble(NoteManager.LONGITUDE);
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

    private void fillViews() {
        if (mPosition != NoteManager.NEW_NODE_POSITION) {
            mView_Title.setText(mTitle);
            mView_Content.setText(mContent);
            fillCoordinates();
        }
    }

    private void fillCoordinates() {
        if(mLatitude > 0) {
            mView_Latitude.setText(String.format("%.4f ° N", mLatitude));
        } else {
            mView_Latitude.setText(String.format("%.4f ° S", mLatitude));
        }
        if(mLongitude > 0) {
            mView_Longitude.setText(String.format("%.4f ° E", mLongitude));
        } else {
            mView_Longitude.setText(String.format("%.4f ° W", mLongitude));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit, menu);
        return true;
    }

    Bundle makeBundle() {
        Bundle bundle = new Bundle();
        bundle.putInt(NoteManager.INDEX, mPosition);
        bundle.putString(NoteManager.TITLE, mTitle);
        bundle.putString(NoteManager.CONTENT, mContent);
        bundle.putDouble(NoteManager.LATITUDE, mLatitude);
        bundle.putDouble(NoteManager.LONGITUDE, mLongitude);
        return bundle;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_done) {
            Log.d(TAG, "Finish button pressed");
            controlledFinish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void controlledFinish() {
        readInput();
        Intent mIntent = new Intent();
        mIntent.putExtras(makeBundle());
        setResult(RESULT_OK, mIntent);
        mControlledFinish = true;
        finish();
    }

    private void readInput() {
        mTitle = mView_Title.getText().toString();
        mContent = mView_Content.getText().toString();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        final GoogleMap mMap = map;

        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.setMyLocationEnabled(true);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLatitude, mLongitude), 15));

        final BitmapDescriptor locatingIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_locating);
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            public void onCameraChange(CameraPosition arg0) {
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(arg0.target).icon(locatingIcon));
            mNewLatitude = arg0.target.latitude;
            mNewLongitude = arg0.target.longitude;
            }
        });
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "Back button pressed");
        controlledFinish();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(!mControlledFinish) {
            readInput();
            if(NoteManager.saveChanges(mPosition, mTitle, mContent,  mLatitude, mLongitude)
                    == NoteManager.NEED_SYNCHRONIZE) {
                AWSManager.upload();
            }
        }

    }
}
