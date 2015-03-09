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
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class is implemented for CSI5175 Assignment 2
 * This class displays the note list to user. It also supports a set of notes operations, which are
 * adding new note by clicking add button, modifying existing note by clicking the list item,
 * deleting a single note item when long touching the item, deleting mass notes by clicking the
 * trash can button.
 * This class sends Intents to Android AWS Service to synchronize the notes file.
 * This class initializes the location service to receive location for editing.
 *
 * A note is immediately created and saved when a user clicks the add button. When he/she finishes
 * editing, the contents of the modified note are sent back to current activity via Intent. The
 * pre-save mechanism is implemented in case the application is put to background when in the Edit
 * activity. If not being implemented, the index of the note item will turn out an error.
 * When being created, the application tries to download the notes file from AWS S3 server. it will
 * use the internal notes file if it fails to download the file or the downloaded file is older
 * than the current file. When the notes have been modified, the Activity will wait until it is
 * being stopped to upload the modified file.
 * When the internet is out of usage, the application works as normal. However, it can neither
 * upload the notes file to the AWS S3 server nor receive Google Maps figures. It will upload the
 * notes file when the internet recovers.
 * When current Android system does not support Google Play service, very noticing hints will be
 * displayed to alert some services cannot be used. However, the application still works.
 *
 * The URL of the notepad example (the SQLite DB is abandoned):
 * http://developer.android.com/training/notepad/index.html
 *
 * @author      Ling Jin
 * @version     1.0
 * @since       04/03/2015
 */
public class Activity_List extends ActionBarActivity {

    private static final int DELETE_ID = Menu.FIRST;
    private static final int ACTIVITY_EDIT = 0;
    private static final int ACTIVITY_MAP = ACTIVITY_EDIT + 1;

    private NoteManager mNotes = null;
    private ListView mView_NoteList = null;
    private TextView mView_NoteHint = null;
    private TextView mView_GoogleHint = null;
    private Button mButton_ConfirmDeletion = null;
    private Button mButton_CancelDeletion = null;
    private Button mButton_ShowMap = null;

    private final Activity_List mActivity_List = this;
    private GoogleApiClient mGoogleApiClient = null;
    private Location mLastLocation = null;
    private AWSMessageReceiver mBroadcastReceiver = null;

    private boolean mPendingUpload = false;
    private boolean mPendingLocating = true;
    private boolean mIsVisible = true;
    private boolean mIsDeleting = false;
    private boolean mGoogleServiceAvailable = true;

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
        mView_GoogleHint = (TextView)findViewById(R.id.textView_GoogleHint);
        registerForContextMenu(mView_NoteList);
        mView_NoteList.setOnItemClickListener(new OnItemClickListener_Edit());

        AWSManager.setContext(this);
        mBroadcastReceiver = new AWSMessageReceiver();
        IntentFilter intentFilter = new IntentFilter(AWSManager.INTENT_PROCESS_RESULT);
        this.registerReceiver(mBroadcastReceiver, intentFilter);

        mButton_ShowMap = (Button) findViewById(R.id.button_ShowMap);
        mButton_ShowMap.setOnClickListener(new ButtonListener_ShowMap());
        mButton_ConfirmDeletion = (Button) findViewById(R.id.button_ConfirmDeletion);
        mButton_ConfirmDeletion.setOnClickListener(new ButtonListener_ConfirmDeletion());
        mButton_CancelDeletion = (Button) findViewById(R.id.button_CancelDeletion);
        mButton_CancelDeletion.setOnClickListener(new ButtonListener_CancelDeletion());

        // Recover the saved notes
        mNotes = new NoteManager(this);
        fillList();

        mLastLocation = new Location("default");
        mLastLocation.setLatitude(Activity_Map.OTTAWA_COORDINATES.latitude);
        mLastLocation.setLongitude(Activity_Map.OTTAWA_COORDINATES.longitude);

        detectGoogleService();
        if(mGoogleServiceAvailable) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(new LocatingSucceedCallBack())
                    .addOnConnectionFailedListener(new LocatingFailedCallBack())
                    .addApi(LocationServices.API)
                    .build();
        }

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
        mIsDeleting = false;
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
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
            if(mIsDeleting) {
                mIsDeleting = false;
                fillList();
            }

            Location tmp = null;
            if(mGoogleServiceAvailable)
                tmp = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if(tmp != null)
                mLastLocation = tmp;

            Intent intent = new Intent(mActivity_List, Activity_Edit.class);
            int newPosition = mNotes.addNote("", "", mLastLocation.getLatitude(), mLastLocation.getLongitude());
            fillList();
            intent.putExtras(makeBundle(newPosition));
            startActivityForResult(intent, ACTIVITY_EDIT);

        } else if (id == R.id.action_delete) {
            if(mIsDeleting) {
                mIsDeleting = false;
                fillList();
            } else {
                if(mNotes.size() != 0) {
                    mIsDeleting = true;
                    fillList();
                }
            }
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
                mNotes = new NoteManager(this);
                Bundle bundle = intent.getExtras();
                if (mNotes.setNote(
                        bundle.getInt(NoteManager.EXTRA_INDEX),
                        bundle.getString(NoteManager.EXTRA_TITLE),
                        bundle.getString(NoteManager.EXTRA_CONTENT),
                        bundle.getDouble(NoteManager.EXTRA_LATITUDE),
                        bundle.getDouble(NoteManager.EXTRA_LONGITUDE))
                        == NoteManager.NEED_SYNCHRONIZE) {
                    mPendingUpload = true;
                    fillList();
                }
            } else {
                Log.d(TAG, "Return from Activity Edit with null Intent, ERROR!");
            }
        } else if (requestCode == ACTIVITY_MAP) {
            Log.d(TAG, "Return from Activity Map");
        }
    }

    private void fillList() {
        if(mIsDeleting) {
            // Update the list
            String[] from = new String[]{"note_title"};
            int[] to = new int[]{R.id.textView_delete};

            // prepare the list of all records
            List<HashMap<String, String>> nodeList = new ArrayList<HashMap<String, String>>();
            for (int i = 0; i < mNotes.size(); i++) {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("note_title", mNotes.getTitle(i));
                nodeList.add(map);
            }

            // fill in the grid_item layout
            SimpleAdapter adapter = new SimpleAdapter(this, nodeList, R.layout.view_delete_item, from, to);
            mView_NoteList.setAdapter(adapter);

            mView_NoteList.setVisibility(View.VISIBLE);
            mView_NoteHint.setVisibility(View.GONE);
            mButton_ShowMap.setVisibility(View.GONE);
            mButton_ConfirmDeletion.setVisibility(View.VISIBLE);
            mButton_CancelDeletion.setVisibility(View.VISIBLE);
        } else {
            if (mNotes.size() != 0) {
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
                SimpleAdapter adapter = new SimpleAdapter(this, nodeList, R.layout.view_list_item, from, to);
                mView_NoteList.setAdapter(adapter);

                // Hide the hind
                mView_NoteHint.setVisibility(View.GONE);
                mView_NoteList.setVisibility(View.VISIBLE);
            } else {
                mView_NoteList.setVisibility(View.GONE);
                mView_NoteHint.setVisibility(View.VISIBLE);
            }

            mButton_ShowMap.setVisibility(View.VISIBLE);
            mButton_ConfirmDeletion.setVisibility(View.GONE);
            mButton_CancelDeletion.setVisibility(View.GONE);
        }
    }


    Bundle makeBundle(int index) {
        Bundle bundle = new Bundle();
        bundle.putInt(NoteManager.EXTRA_INDEX, index);
        bundle.putString(NoteManager.EXTRA_TITLE, mNotes.getTitle(index));
        bundle.putString(NoteManager.EXTRA_CONTENT, mNotes.getContent(index));
        bundle.putDouble(NoteManager.EXTRA_LATITUDE, mNotes.getLatitude(index));
        bundle.putDouble(NoteManager.EXTRA_LONGITUDE, mNotes.getLongitude(index));
        return bundle;
    }

    void toast(String message) {
        if(mIsVisible) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
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

    class AWSMessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getIntExtra(AWSManager.EXTRA_AWS_RESULT, AWSManager.AWS_FAILED)) {
                case AWSManager.AWS_UPLOADED:
                    Log.d(TAG, "Response from AWS service, uploaded");
                    Toast.makeText(mActivity_List, "Uploaded", Toast.LENGTH_SHORT).show();
                    break;
                case AWSManager.AWS_UPLOAD_FAILED:
                    Log.d(TAG, "Response from AWS service, upload failed");
                    toast("Try later");
                    mPendingUpload = true;
                    break;
                case AWSManager.AWS_DOWNLOADED:
                    Log.d(TAG, "Response from AWS service, downloaded");
                    toast("Synchronized");
                    if(mNotes.updateFromTmpFile() == NoteManager.NEED_UPDATE) {
                        fillList();
                    }
                    break;
                case AWSManager.AWS_DOWNLOAD_FAILED:
                case AWSManager.AWS_FAILED:
                    Log.d(TAG, "Response from AWS service, failed");
                    break;
            }
        }
    }

    class ButtonListener_ShowMap implements Button.OnClickListener {
        @Override
        public void onClick(View view) {
            if(mGoogleServiceAvailable) {
                Intent intent = new Intent(mActivity_List, Activity_Map.class);
                startActivityForResult(intent, ACTIVITY_MAP);
            } else {
                toast("Button disabled");
            }
        }
    }

    class ButtonListener_CancelDeletion implements Button.OnClickListener {
        @Override
        public void onClick(View view) {
            mIsDeleting = false;
            fillList();
        }
    }

    class ButtonListener_ConfirmDeletion implements Button.OnClickListener {
        @Override
        public void onClick(View view) {
            for (int position = mView_NoteList.getChildCount() - 1; position >= 0; position--) {
                CheckBox cb = (CheckBox) mView_NoteList.getChildAt(position).findViewById(R.id.checkBox_delete);
                if( cb.isChecked() ) {
                    mNotes.deleteNote(position);
                }
            }
            mPendingUpload = true;
            mIsDeleting = false;
            fillList();
        }
    }

    class LocatingSucceedCallBack implements GoogleApiClient.ConnectionCallbacks {
        @Override
        public void onConnected(Bundle bundle) {
            if(mPendingLocating) {
                mPendingLocating = false;
                toast("Location confirmed");
            }
        }

        @Override
        public void onConnectionSuspended(int i) {
            toast("Location service error");
        }
    }

    class LocatingFailedCallBack implements GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            toast("Location service error");
        }
    }

    class OnItemClickListener_Edit implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> arg0, View view, int position, long rowId) {
            Intent intent = new Intent(mActivity_List, Activity_Edit.class);
            intent.putExtras(makeBundle(position));
            startActivityForResult(intent, ACTIVITY_EDIT);
        }
    }
}
