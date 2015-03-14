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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class is implemented for CSI5175 Assignment 2
 * This class displays the note list to user. It supports a set of notes operations, which are
 * adding new note by clicking add button, modifying existing note by clicking the list item,
 * deleting a single note item when long touching the item, deleting mass notes by clicking the
 * trash can button.
 * This class sends Intents to Android AWS Service to synchronize the notes file.
 * This class initializes the location service to receive location for editing.
 * The screen rotation is forbidden for the Edit Activity is hard to be displayed in landscape mode.
 *
 * A note is immediately created and saved when a user clicks the add button. When he/she finishes
 * editing, the contents of the modified note are sent back to this Activity via Intent. The
 * pre-save mechanism is implemented in case the application is put to background when in the Edit
 * activity. If not being implemented, the index of the note item will turn out an error.
 * When being created, the application tries to synchronize the notes from AWS S3 server. it will
 * first obtain the file list on the server, compares the file names and modified date to
 * determine which files need to be downloaded/uploaded. It uses the internal notes file if it
 * fails to synchronize the notes. When the notes have been really modified, the Activity will
 * upload the file.
 *
 * A transaction concept is introduced in the application. A synchronization transaction always be
 * performed in sequence of:
 *        obtaining the file list and downloading files -> deleting files -> upload files
 * Only current transaction is finished, a new transaction can be initialized, which involves
 * determining the files that to be processed and uploads/deletes them. This transaction helps
 * maintaining the integrity of the synchronization. Moreover, the uploading process maybe cancelled
 * if the file is locally deleted.
 *
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
    /**
     * Constants for resource and Intent
     */
    private static final int DELETE_ID = Menu.FIRST;
    private static final int ACTIVITY_EDIT = 0;
    private static final int ACTIVITY_MAP = ACTIVITY_EDIT + 1;
    /**
     * References for views
     */
    private ListView mView_NoteList = null;
    private TextView mView_NoteHint = null;
    private TextView mView_GoogleHint = null;
    private Button mButton_ConfirmDeletion = null;
    private Button mButton_CancelDeletion = null;
    private Button mButton_ShowMap = null;
    /**
     * References for data
     */
    private NoteManager mNotes = null;
    private GoogleApiClient mGoogleApiClient = null;
    private Location mLastLocation = null;
    private AWSMessageReceiver mBroadcastReceiver = null;
    private final Activity_List mActivity_List = this;
    /**
     * Indicator for uploading the notes file, protected by Atomic mechanism
     */
    private AtomicBoolean mAWSBusy = new AtomicBoolean(true);
    /**
     * Indicator for toasting messages
     */
    private boolean mPendingLocating = true;
    private boolean mIsVisible = true;
    /**
     * Indicator for rendering user interface
     */
    private boolean mIsDeleting = false;
    private boolean mGoogleServiceAvailable = true;
    /**
     * Debugging
     */
    private final String TAG = "<<<<< Activity List >>>>>";
    /**
     * Synchronization file name lists
     */
    private ArrayList<String> mFilesToBeUploaded = new ArrayList<String>();
    private ArrayList<String> mFilesToBeDownloaded = new ArrayList<String>();
    private ArrayList<String> mFilesToBeDeleted = new ArrayList<String>();

    /**
     * Initializes UI, sets buttons listeners, initializes location service, downloads the note list
     * from AWS S3 server. All the callback methods are defined as inner classes for clearance.
     * @param savedInstanceState saved state, do not need to be processed
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize view
        setContentView(R.layout.activity_list);
        // Store the view references
        mView_NoteList = (ListView)findViewById(R.id.note_list);
        mView_NoteHint = (TextView)findViewById(R.id.note_hint);
        mView_GoogleHint = (TextView)findViewById(R.id.textView_GoogleHint);
        mButton_ShowMap = (Button) findViewById(R.id.button_ShowMap);
        mButton_ConfirmDeletion = (Button) findViewById(R.id.button_ConfirmDeletion);
        mButton_CancelDeletion = (Button) findViewById(R.id.button_CancelDeletion);
        // Set the action bar style
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setTitle(R.string.list_title);
        actionBar.setIcon(R.drawable.ic_pad);
        mButton_ShowMap.setOnClickListener(new ButtonListener_ShowMap());
        mButton_ConfirmDeletion.setOnClickListener(new ButtonListener_ConfirmDeletion());
        mButton_CancelDeletion.setOnClickListener(new ButtonListener_CancelDeletion());
        // Initialize context menu
        registerForContextMenu(mView_NoteList);
        mView_NoteList.setOnItemClickListener(new OnItemClickListener_Edit());
        // Initialize AWS S3 Service result listener
        AWSManager.setContext(this);
        mBroadcastReceiver = new AWSMessageReceiver();
        IntentFilter intentFilter = new IntentFilter(AWSManager.INTENT_PROCESS_RESULT);
        this.registerReceiver(mBroadcastReceiver, intentFilter);
        // Recover the saved notes
        mNotes = new NoteManager(this);
        fillList();
        // A default value used when the location service does not work
        mLastLocation = new Location("default");
        mLastLocation.setLatitude(Activity_Map.OTTAWA_COORDINATES.latitude);
        mLastLocation.setLongitude(Activity_Map.OTTAWA_COORDINATES.longitude);
        // Initialize location service and
        detectGoogleService();
        if(mGoogleServiceAvailable) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(new LocatingSucceedCallBack())
                    .addOnConnectionFailedListener(new LocatingFailedCallBack())
                    .addApi(LocationServices.API)
                    .build();
        }
        // Download the notes from AWS S3 Server
        AWSManager.list(NoteManager.INDEX_FILE_NAME);
        mAWSBusy.set(true);
        Log.d(TAG, "Activity created");
    }

    /**
     * Sets the visibility indicator as visible, recovers location service
     */
    @Override
    protected void onStart() {
        super.onStart();
        mIsVisible = true;
        Log.d(TAG, "Activity started");
        if(mGoogleApiClient != null) {
            if(!mGoogleApiClient.isConnected())
                mGoogleApiClient.connect();
        }
    }

    /**
     * Sets the visibility indicator as visible. Sets the list view from deleting state to normal
     * Disconnects the location service. And uploads the notes file if necessary
     */
    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "Activity stopped");
        mIsVisible = false;
        mIsDeleting = false;
        if(mGoogleApiClient != null) {
            if (mGoogleApiClient.isConnected()) {
                mGoogleApiClient.disconnect();
            }
        }
    }

    /**
     * Unregister the broadcast receive registered in onCreate
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Activity destroyed");
        unregisterReceiver(mBroadcastReceiver);
    }

    /**
     * Creates the option menu, includes two buttons, one for adding new note, the other for mass
     * deleting
     * @param menu a menu instance
     * @return always true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_mappad, menu);
        return true;
    }

    /**
     * Adds or mass delete
     * @param item the menu item being clicked
     * @return result from super method
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_new) {
            Log.d(TAG, "Menu NEW clicked");
            // Cancel the deleting state
            if(mIsDeleting) {
                mIsDeleting = false;
                fillList();
            }
            // Get an initial location
            Location tmp = null;
            if(mGoogleServiceAvailable)
                tmp = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if(tmp != null)
                mLastLocation = tmp;
            // Add a new note to memory
            int newPosition = mNotes.addNote("", "", mLastLocation.getLatitude(), mLastLocation.getLongitude());
            // Start the edit Activity
            Intent intent = new Intent(mActivity_List, Activity_Edit.class);
            intent.putExtras(makeBundle(newPosition));
            startActivityForResult(intent, ACTIVITY_EDIT);
            // Update the ListView
            fillList();
        } else if (id == R.id.action_delete) {
            Log.d(TAG, "Menu DELETE clicked");
            // Cancel deleting state if already in
            // Start deleting state if not
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

    /**
     * Adds the delete context menu
     * @param menu menu context
     * @param v view context
     * @param menuInfo menuInfo instance
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(Menu.NONE, DELETE_ID, Menu.NONE, "Delete Note");
    }

    /**
     * Deletes the selected item and prepare to upload the file
     * @param item menu item instance
     * @return always true
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case DELETE_ID:
                Log.d(TAG, "Context menu DELETE clicked");
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
                mNotes.deleteNote(info.position);
                // Update the List View and try to synchronize the notes when modified
                fillList();
                startNewTransmission();
                return true;
        }
        return super.onContextItemSelected(item);
    }

    /**
     * Processes the results from other Activities
     * @param requestCode request type
     * @param resultCode not in use
     * @param intent used for getting extras
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == ACTIVITY_EDIT) {
            Log.d(TAG, "Returned from Activity Edit");
            if (intent != null) {
                Bundle bundle = intent.getExtras();
                if (mNotes.setNote(
                        bundle.getInt(NoteManager.EXTRA_INDEX),
                        bundle.getString(NoteManager.EXTRA_TITLE),
                        bundle.getString(NoteManager.EXTRA_CONTENT),
                        bundle.getDouble(NoteManager.EXTRA_LATITUDE),
                        bundle.getDouble(NoteManager.EXTRA_LONGITUDE))
                        == NoteManager.NEED_SYNCHRONIZE) {
                    // Update the List View and try to synchronize the notes when modified
                    fillList();
                    startNewTransmission();
                }
            } else {
                // Should not happen
                Log.d(TAG, "ERROR, returned from Activity Edit with null Intent");
            }
        } else if (requestCode == ACTIVITY_MAP) {
            // Nothing to do. But it is not a bad idea to leave an interface here.
            Log.d(TAG, "Returned from Activity Map");
        }
    }

    /**
     * Fill the ListView and select which buttons should be rendered
     */
    private void fillList() {
        Log.d(TAG, "Update ListView");
        if(mIsDeleting) {
            // Update the list
            String[] from = new String[]{"note_title"};
            int[] to = new int[]{R.id.textView_delete};
            // Prepare the list of all records
            List<HashMap<String, String>> nodeList = new ArrayList<HashMap<String, String>>();
            for (int i = 0; i < mNotes.size(); i++) {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("note_title", mNotes.getTitle(i));
                nodeList.add(map);
            }
            // Fill in the grid_item layout
            SimpleAdapter adapter = new SimpleAdapter(this, nodeList, R.layout.view_delete_item, from, to);
            mView_NoteList.setAdapter(adapter);
            // Set button views
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
                // Prepare the list of all records
                List<HashMap<String, String>> nodeList = new ArrayList<HashMap<String, String>>();
                for (int i = 0; i < mNotes.size(); i++) {
                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put("note_title", mNotes.getTitle(i));
                    nodeList.add(map);
                }
                // Fill in the grid_item layout
                SimpleAdapter adapter = new SimpleAdapter(this, nodeList, R.layout.view_list_item, from, to);
                mView_NoteList.setAdapter(adapter);
                // Hide the hint
                mView_NoteHint.setVisibility(View.GONE);
                mView_NoteList.setVisibility(View.VISIBLE);
            } else {
                // Show the hint when no notes exist
                mView_NoteList.setVisibility(View.GONE);
                mView_NoteHint.setVisibility(View.VISIBLE);
            }
            // Set button views
            mButton_ShowMap.setVisibility(View.VISIBLE);
            mButton_ConfirmDeletion.setVisibility(View.GONE);
            mButton_CancelDeletion.setVisibility(View.GONE);
        }
    }

    /**
     * Puts a note in a bundle
     * @param index index of the note item
     * @return new bundle extra
     */
    private Bundle makeBundle(int index) {
        Bundle bundle = new Bundle();
        bundle.putInt(NoteManager.EXTRA_INDEX, index);
        bundle.putString(NoteManager.EXTRA_TITLE, mNotes.getTitle(index));
        bundle.putString(NoteManager.EXTRA_CONTENT, mNotes.getContent(index));
        bundle.putDouble(NoteManager.EXTRA_LATITUDE, mNotes.getLatitude(index));
        bundle.putDouble(NoteManager.EXTRA_LONGITUDE, mNotes.getLongitude(index));
        return bundle;
    }

    /**
     * Toasts a message when the Activity is visible
     * @param message message
     */
    private void toast(String message) {
        if(mIsVisible) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Detects whether the location service is available on the cell phone
     * Directly use the location service will make the application crash
     */
    private void detectGoogleService() {
        int status= GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(status == ConnectionResult.SUCCESS) {
            mGoogleServiceAvailable = true;
            mView_GoogleHint.setVisibility(View.GONE);
        } else {
            // Prepare a hint if the location service in unavailable
            mGoogleServiceAvailable = false;
            mView_GoogleHint.setVisibility(View.VISIBLE);
            String errorMsg = "Google Play Service Error :\n";
            errorMsg += GooglePlayServicesUtil.getErrorString(status);
            errorMsg += "\nDisable All Google Maps Functions";
            mView_GoogleHint.setText(errorMsg);
        }
    }

    /**
     * A broadcast receiver class for processing AWS S3 services results
     */
    class AWSMessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String filename = intent.getStringExtra(AWSManager.EXTRA_INTERNAL_FILENAME);
            if(filename == null) {
                Log.d(TAG, "ERROR, AWS response without filename");
                return;
            }
            switch (intent.getIntExtra(AWSManager.EXTRA_AWS_RESULT, AWSManager.AWS_FAILED)) {
                case AWSManager.AWS_UPLOADED:
                    onFileUploaded(filename);
                    break;
                case AWSManager.AWS_DOWNLOADED:
                    onFileDownloaded(filename);
                    fillList();
                    break;
                case AWSManager.AWS_DELETED:
                    onFileDeleted(filename);
                    break;
                case AWSManager.AWS_LISTED:
                    onFileListed(filename);
                    break;
                case AWSManager.AWS_UPLOAD_FAILED:
                    onFileUploadFailure(filename);
                    break;
                case AWSManager.AWS_DOWNLOAD_FAILED:
                    onFileDownloadFailure(filename);
                    break;
                case AWSManager.AWS_DELETE_FAILED:
                    onFileDeleteFailure(filename);
                    break;
                case AWSManager.AWS_LIST_FAILED:
                    onFileListFailure(filename);
                    break;
                case AWSManager.AWS_FAILED:
                    // This should not happen
                    Log.d(TAG, "Response from AWS service, failed");
                    break;
            }
        }
    }

    /**
     * Check whether the response from the AWS Service is right. It should be right any way.
     * @param respFilename responded note file name
     * @param reqFilename request note file name
     * @param reqType operation type, delete/send/receive/list
     * @param resptype true for success, false for failure
     * @return the response is Okay or not
     */
    private boolean isRightResponse(String respFilename, String reqFilename, String reqType, boolean resptype) {
        if(respFilename == null) {
            Log.d(TAG, "ERROR: AWS " + reqType + " response without a filename");
            return false;
        }
        if(reqType.compareTo(AWSManager.INTENT_LIST) != 0) {
            if (reqFilename == null) {
                Log.d(TAG, "ERROR: AWS " + reqType + " response without local filename");
                return false;
            }
            if (reqFilename.compareTo(respFilename) != 0) {
                Log.d(TAG, "ERROR: AWS " + reqType + " response with a miss matched local filename");
                Log.d(TAG, "ERROR: i.e. " + respFilename + " v.s. " + reqFilename);
                return false;
            }
        }
        if(resptype)
            Log.d(TAG, "AWS response, success to " + reqType + " " + respFilename);
        else
            Log.d(TAG, "AWS response, fail to " + reqType + " " + respFilename);
        return true;
    }

    /**
     * Gets the next mission of a new transaction when system is Idle. Deleting mission first,
     * uploading files last.
     * @return whether a new mission is executed.
     */
    private boolean startNewTransmission() {
        if(!mAWSBusy.get()) {
            // When AWS is busy, we should not assign new tasks for it
            if(mFilesToBeDeleted.size() != 0 || mFilesToBeUploaded.size() != 0) {
                Log.d(TAG, "ERROR: AWS mission incomplete");
                return false;
            }
            Log.d(TAG, "AWS server is idle, update file list");
            updateNotesToBeDeletedList();
            updateNotesToBeSendList();
            if(mFilesToBeDeleted.size() != 0) {
                Log.d(TAG, "New AWS deleting mission assigned");
                deleteTopmostNote();
                return true;
            } else if (mFilesToBeUploaded.size() != 0) {
                Log.d(TAG, "New AWS uploading mission assigned");
                sendTopmostNote();
                return true;
            }
            Log.d(TAG, "No new AWS mission assigned");
            return false;
        }
        return false;
    }

    /**
     * Gets the next mission of current transaction, deleting mission first, uploading files last.
     * Delete the uploading mission if the note has been deleted. The deleting message will be sent
     * in next transaction. If a new mission is not executed, should set system to Idle state.
     * @return whether a new mission is executed
     */
    private boolean startNextTransmission() {
        // Send the following files
        Log.d(TAG, "Start a new transmission while in an unfinished session");
        if(mFilesToBeDeleted.size() != 0) {
            Log.d(TAG, "New AWS deleting mission assigned");
            deleteTopmostNote();
            return true;
        } else {
            Log.d(TAG, "Check already deleted files in startNextTransmission");
            ArrayList<String> tmpDeleteNames = mNotes.getFileNamesForDeleting();
            while(mFilesToBeUploaded.size() != 0 && tmpDeleteNames.contains(mFilesToBeUploaded.get(0))) {
                Log.d(TAG, "Cancel uploading the already deleted file " + mFilesToBeUploaded.get(0));
                mFilesToBeUploaded.remove(0);
            }
            if(mFilesToBeUploaded.size() != 0) {
                Log.d(TAG, "New AWS uploading mission assigned");
                sendTopmostNote();
                return true;
            }
        }
        Log.d(TAG, "No new AWS mission assigned");
        return false;
    }

    /**
     * Starts a transmission in current transaction, if fails, begins a new transaction
     */
    private void continueTransmission() {
        if(!startNextTransmission()) {
            mAWSBusy.set(false);
            if(!startNewTransmission()) {
                Log.d(TAG, "All files updated, synchronized");
                toast("Synchronized");
            }
        }
    }

    /**
     * Tries to upload the note file again.
     * @param filename file name for checking and new Intent
     */
    private void onFileUploadFailure(String filename) {
        if(!isRightResponse(
                filename,
                mFilesToBeUploaded.size() != 0 ? mFilesToBeUploaded.get(0): null,
                AWSManager.INTENT_UPLOAD,
                true))
            return;
        ArrayList<String> tmpDeleteNames = mNotes.getFileNamesForDeleting();
        if(tmpDeleteNames.contains(filename)) {
            Log.d(TAG, "Cancel uploading the already deleted file " + mFilesToBeUploaded.get(0));
            mFilesToBeUploaded.remove(0);
            // No file need to receive
            continueTransmission();
        } else {
            AWSManager.uploadLater(filename);
        }
    }

    /**
     * Decides further operations, uploading more files
     * @param filename file name for checking
     */
    private void onFileUploaded(String filename) {
        if(!isRightResponse(
                filename,
                mFilesToBeUploaded.size() != 0 ? mFilesToBeUploaded.get(0): null,
                AWSManager.INTENT_UPLOAD,
                true))
            return;
        mNotes.confirmSend(filename);
        mFilesToBeUploaded.remove(0);
        continueTransmission();
    }

    /**
     * Tries to delete the note file again.
     * @param filename file name for checking and new Intent
     */
    private void onFileDeleteFailure(String filename) {
        if(!isRightResponse(
                filename,
                mFilesToBeDeleted.size() != 0 ? mFilesToBeDeleted.get(0): null,
                AWSManager.INTENT_DELETE,
                false))
            return;
        AWSManager.deleteLater(filename);
    }

    /**
     * Decides further operations, deleting/uploading
     * @param filename file name for checking
     */
    private void onFileDeleted(String filename) {
        if(!isRightResponse(
                filename,
                mFilesToBeDeleted.size() != 0 ? mFilesToBeDeleted.get(0): null,
                AWSManager.INTENT_DELETE,
                true))
            return;

        mNotes.confirmDelete(filename);
        mFilesToBeDeleted.remove(0);
        continueTransmission();
    }

    /**
     * Tries to download the note file again.
     * @param filename file name for checking and new Intent
     */
    private void onFileDownloadFailure(String filename) {
        if(!isRightResponse(
                filename,
                mFilesToBeDownloaded.size() != 0 ? mFilesToBeDownloaded.get(0): null,
                AWSManager.INTENT_DOWNLOAD,
                false))
            return;
        AWSManager.downloadLater(filename);
    }

    /**
     * Decides further operations, downloading/deleting/uploading
     * @param filename file name for checking
     */
    private void onFileDownloaded(String filename) {
        if(!isRightResponse(
                filename,
                mFilesToBeDownloaded.size() != 0 ? mFilesToBeDownloaded.get(0): null,
                AWSManager.INTENT_DOWNLOAD,
                true))
            return;

        // Confirm the received file, update ListView
        Log.d(TAG, "Reading the downloaded note file...");
        mNotes.confirmReceive(filename);
        fillList();
        mFilesToBeDownloaded.remove(0);
        // Try to download next file
        if (mFilesToBeDownloaded.size() != 0) {
            Log.d(TAG, "New AWS downloading mission assigned");
            receiveTopmostNote();
            return;
        }

        // No file need to receive
        continueTransmission();
    }

    /**
     * Decides further operations, downloading/deleting/uploading
     * @param filename file name for checking
     */
    private void onFileListed(String filename) {
        if(!isRightResponse(filename, null, AWSManager.INTENT_LIST, true))
            return;
        if(filename == null) {
            Log.d(TAG, "ERROR: AWS list response without a filename");
            return;
        }
        Log.d(TAG, "AWS response, success to download " + filename);
        mFilesToBeDownloaded = mNotes.getFileNamesForReceiving();
        if(mFilesToBeDownloaded.size() != 0) {
            // Request one more file
            Log.d(TAG, "New AWS downloading mission assigned");
            receiveTopmostNote();
            return;
        }

        // No file need to receive
        continueTransmission();
    }

    /**
     * Tries to obtain file list again.
     * @param filename file name for checking and new Intent
     */
    private void onFileListFailure(String filename) {
        if(!isRightResponse(
                filename,
                mFilesToBeDownloaded.size() != 0 ? mFilesToBeDownloaded.get(0): null,
                AWSManager.INTENT_LIST,
                false))
            return;
        AWSManager.listLater(filename);
    }

    /**
     * Queries the NoteManager for files to be delete, ignores the files already in list
     */
    private void updateNotesToBeDeletedList() {
        if(!mAWSBusy.get()) {
            Log.d(TAG, "Update files to be deleted list");
            ArrayList<String> names = mNotes.getFileNamesForDeleting();
            for(String name: names) {
                if(!mFilesToBeDeleted.contains(name)) {
                    mFilesToBeDeleted.add(name);
                    Log.d(TAG, name + " is added into deleting queue");
                } else {
                    Log.d(TAG, name + " is already in deleting queue, ignore it");
                }
            }
        }
    }

    /**
     * Queries the NoteManager for files to be sent, ignores the files already in list
     */
    private void updateNotesToBeSendList() {
        if(!mAWSBusy.get()) {
            Log.d(TAG, "Update files to be sent list");
            ArrayList<String> names = mNotes.getFileNamesForSending();
            for(String name: names) {
                if(!mFilesToBeUploaded.contains(name)) {
                    mFilesToBeUploaded.add(name);
                    Log.d(TAG, name + " is added into sending queue");
                } else {
                    Log.d(TAG, name + " is already in sending queue, ignore it");
                }
            }
        }
    }

    /**
     * Sends the first file in the to-be-sent list
     */
    private void sendTopmostNote() {
        mAWSBusy.set(true);
        AWSManager.upload(mFilesToBeUploaded.get(0));
    }

    /**
     * Receives the first file in the to-be-received list
     */    private void receiveTopmostNote() {
        mAWSBusy.set(true);
        AWSManager.download(mFilesToBeDownloaded.get(0));
    }

    /**
     * Deletes the first file in the to-be-deleted list
     */
    private void deleteTopmostNote() {
        mAWSBusy.set(true);
        AWSManager.delete(mFilesToBeDeleted.get(0));
    }

    /**
     * A button click listener for starting the map display Activity
     */
    class ButtonListener_ShowMap implements Button.OnClickListener {
        @Override
        public void onClick(View view) {
            if(mGoogleServiceAvailable) {
                Intent intent = new Intent(mActivity_List, Activity_Map.class);
                startActivityForResult(intent, ACTIVITY_MAP);
                Log.d(TAG, "Show map button clicked");
            } else {
                toast("Button disabled");
            }
        }
    }

    /**
     * A button click listener for cancelling the deleting state
     */
    class ButtonListener_CancelDeletion implements Button.OnClickListener {
        @Override
        public void onClick(View view) {
            mIsDeleting = false;
            fillList();
            Log.d(TAG, "Cancel deletion button clicked");
        }
    }

    /**
     * A button click listener for deleting all the selected note items
     */
    class ButtonListener_ConfirmDeletion implements Button.OnClickListener {
        @Override
        public void onClick(View view) {
            Log.d(TAG, "Confirm deletion button clicked");
            for (int position = mView_NoteList.getChildCount() - 1; position >= 0; position--) {
                CheckBox cb = (CheckBox) mView_NoteList.getChildAt(position).findViewById(R.id.checkBox_delete);
                if( cb.isChecked() ) {
                    mNotes.deleteNote(position);
                }
            }
            mIsDeleting = false;
            fillList();
            startNewTransmission();
        }
    }

    /**
     * Location service callback methods. Toast when the location service is connected for the first
     * time.
     */
    class LocatingSucceedCallBack implements GoogleApiClient.ConnectionCallbacks {
        @Override
        public void onConnected(Bundle bundle) {
            if(mPendingLocating) {
                mPendingLocating = false;
                toast("location confirmed");
                Log.d(TAG, "Google Plays Location service confirmed");
            }
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.d(TAG, "Google Plays Location service error");
        }
    }

    /**
     * Location service callback method. Do nothing when it cannot be used.
     */
    class LocatingFailedCallBack implements GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            Log.d(TAG, "Google Plays Location service error");
        }
    }

    /**
     * A list view clicking listener for starting the edit Activity
     */
    class OnItemClickListener_Edit implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> arg0, View view, int position, long rowId) {
            Intent intent = new Intent(mActivity_List, Activity_Edit.class);
            intent.putExtras(makeBundle(position));
            startActivityForResult(intent, ACTIVITY_EDIT);
            Log.d(TAG, "List item clicked, start editing");
        }
    }
}
