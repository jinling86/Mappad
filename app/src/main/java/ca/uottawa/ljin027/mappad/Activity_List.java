package ca.uottawa.ljin027.mappad;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Activity_List extends ActionBarActivity {

    private static final int DELETE_ID = Menu.FIRST;
    private static final int ACTIVITY_EDIT = 0;

    private NoteManager mNotes = null;
    private ListView mView_NoteList = null;
    private TextView mView_NoteHint = null;
    private final Activity_List mActivity_List = this;

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
                intent.putExtra(NoteManager.INDEX, position);
                intent.putExtra(NoteManager.TITLE, mNotes.getTitle(position));
                intent.putExtra(NoteManager.CONTENT, mNotes.getContent(position));
                startActivityForResult(intent, ACTIVITY_EDIT);
            }
        });

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        this.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int action = intent.getIntExtra(AWSService.ACTION, AWSService.ACTION_FAILED);
                if (action == AWSService.ACTION_UPLOADED) {
                    Toast.makeText(context, "Synchronized", Toast.LENGTH_LONG).show();
                } else if (action == AWSService.ACTION_DOWNLOADED) {
                    if(mNotes.updateFromTmpFile())
                        fillList();
                }
            }
        }, intentFilter);

        // Recover the saved notes
        mNotes = new NoteManager(this);
        fillList();

        AWSManager.download(this, mNotes.TMP_NAME);
        Log.d(TAG, "Activity created");
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
            Intent intent = new Intent(mActivity_List, Activity_Edit.class);
            int newPosition = mNotes.addNote("", "");
            intent.putExtra(NoteManager.INDEX, newPosition);
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
                return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        Bundle extras = intent.getExtras();
        int position = extras.getInt(NoteManager.INDEX);
        String title = extras.getString(NoteManager.TITLE);
        String content = extras.getString(NoteManager.CONTENT);
        mNotes.setNote(position, title, content);
        AWSManager.upload(this, mNotes.EXT_NAME);
        fillList();
    }

}
