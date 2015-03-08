package ca.uottawa.ljin027.mappad;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Activity_List extends ActionBarActivity {

    private static final int DELETE_ID = Menu.FIRST;
    private static final int ACTIVITY_EDIT = 0;
    private static final int ACTIVITY_MAP = ACTIVITY_EDIT + 1;

    private NoteManager mNotes = null;
    private ListView mView_NoteList = null;
    private TextView mView_NoteHint = null;
    private final Activity_List mActivity_List = this;
    private GoogleApiClient mGoogleApiClient = null;
    private Location mLastLocation = null;
    private AWSMessageReceiver mBroadcastReceiver = null;

    private boolean mPendingUpload = false;
    private boolean mPendingLocating = false;
    private boolean mIsVisible = true;


    private final String TAG = "<<<<< Activity List >>>>>";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_list);
        // Set the action bar style
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setTitle(R.string.list_title);
        actionBar.setIcon(R.drawable.ic_pad);

        // Store the handle
        mView_NoteList = (ListView)findViewById(R.id.note_list);
        mView_NoteHint = (TextView)findViewById(R.id.note_hint);
        registerForContextMenu(mView_NoteList);
        mView_NoteList.setOnItemClickListener( new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int position, long rowId) {
                Intent intent = new Intent(mActivity_List, Activity_Edit.class);
                intent.putExtras(makeBundle(position));
                startActivityForResult(intent, ACTIVITY_EDIT);
            }
        });

        AWSManager.setContext(this);
        mBroadcastReceiver = new AWSMessageReceiver();
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        this.registerReceiver(mBroadcastReceiver, intentFilter);

        Button mapButton = (Button) findViewById(R.id.button_ShowMap);
        mapButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mActivity_List, Activity_Map.class);
                startActivityForResult(intent, ACTIVITY_MAP);
            }
        });

        // Recover the saved notes
        mNotes = new NoteManager(this);
        fillList();

        mLastLocation = new Location("default");
        mLastLocation.setLatitude(Activity_Map.OTTAWA_COORDINATES.latitude);
        mLastLocation.setLongitude(Activity_Map.OTTAWA_COORDINATES.longitude);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
            .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                @Override
                public void onConnected(Bundle bundle) {
                    if(!mPendingLocating) {
                        mPendingLocating = true;
                        toast("Location confirmed");
                    }
                }

                @Override
                public void onConnectionSuspended(int i) {
                    toast("Location service error");
                }
            })
            .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                @Override
                public void onConnectionFailed(ConnectionResult connectionResult) {
                    toast("Location service error");
                }
            })
            .addApi(LocationServices.API)
            .build();

        AWSManager.download();
        Log.d(TAG, "Activity created");
    }


    @Override
    protected void onStart() {
        super.onStart();
        mIsVisible = true;
        if(mGoogleApiClient != null) {
            if(!mGoogleApiClient.isConnected())
                mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mIsVisible = false;
        if(mGoogleApiClient != null) {
            if (mGoogleApiClient.isConnected()) {
                mGoogleApiClient.disconnect();
            }
        }
        if(mPendingUpload) {
            mPendingUpload = false;
            AWSManager.upload();
        }
    }

    private void fillList() {
        if(mNotes.size() != 0) {
            // Hide the hind
            mView_NoteHint.setVisibility(View.GONE);
            mView_NoteList.setVisibility(View.VISIBLE);

            // Update the list
            String[] from = new String[]{"note_title"};
            int[] to = new int[]{R.id.item_title};

            // prepare the list of all records
            List<HashMap<String, String>> nodeList = new ArrayList<HashMap<String, String>>();
            for (int i = 0; i < mNotes.size(); i++) {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("note_title", mNotes.getTitle(i));
                nodeList.add(map);
            }

            // fill in the grid_item layout
            SimpleAdapter adapter = new SimpleAdapter(this, nodeList, R.layout.view_listitem, from, to);
            mView_NoteList.setAdapter(adapter);
        } else {
            mView_NoteList.setVisibility(View.GONE);
            mView_NoteHint.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_mappad, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_new) {
            Location tmp = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if(tmp != null)
                mLastLocation = tmp;

            Intent intent = new Intent(mActivity_List, Activity_Edit.class);
            int newPosition = mNotes.addNote("", "", mLastLocation.getLatitude(), mLastLocation.getLongitude());
            fillList();
            intent.putExtras(makeBundle(newPosition));
            startActivityForResult(intent, ACTIVITY_EDIT);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(Menu.NONE, DELETE_ID, Menu.NONE, "Delete Note");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case DELETE_ID:
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
                mNotes.deleteNote(info.position);
                fillList();
                mPendingUpload = true;
                return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == ACTIVITY_EDIT) {
            Log.d(TAG, "Return from Activity Edit");
            if (intent != null) {
                Bundle bundle = intent.getExtras();
                if (mNotes.setNote(
                        bundle.getInt(NoteManager.INDEX),
                        bundle.getString(NoteManager.TITLE),
                        bundle.getString(NoteManager.CONTENT),
                        bundle.getDouble(NoteManager.LATITUDE),
                        bundle.getDouble(NoteManager.LONGITUDE))
                        == NoteManager.NEED_SYNCHRONIZE) {
                    mPendingUpload = true;
                    fillList();
                }
            } else {
                mPendingUpload = true;
                mNotes = new NoteManager(this);
                fillList();
            }
        } else if (requestCode == ACTIVITY_MAP) {
            Log.d(TAG, "Return from Activity Map");
        }
    }

    Bundle makeBundle(int index) {
        Bundle bundle = new Bundle();
        bundle.putInt(NoteManager.INDEX, index);
        bundle.putString(NoteManager.TITLE, mNotes.getTitle(index));
        bundle.putString(NoteManager.CONTENT, mNotes.getContent(index));
        bundle.putDouble(NoteManager.LATITUDE, mNotes.getLatitude(index));
        bundle.putDouble(NoteManager.LONGITUDE, mNotes.getLongitude(index));
        return bundle;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
    }

    void toast(String message) {
        if(mIsVisible)
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    class AWSMessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getIntExtra(AWSService.ACTION, AWSService.ACTION_FAILED)) {
                case AWSService.ACTION_UPLOADED:
                    Log.d(TAG, "Response from AWS upload service");
                    toast("Uploaded");
                    break;
                case AWSService.ACTION_DOWNLOADED:
                    Log.d(TAG, "Response from AWS download service");
                    toast("Synchronized");
                    if(mNotes.updateFromTmpFile() == NoteManager.NEED_UPDATE) {
                        fillList();
                    }
            }
        }
    }
}
