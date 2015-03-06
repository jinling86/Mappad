package ca.uottawa.ljin027.mappad;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

/**
 * Created by Ling Jin on 04/03/2015.
 */
public class Activity_Edit extends ActionBarActivity {
    private final String TAG = "<<<<< Activity Edit >>>>>";

    private EditText mView_Title;
    private EditText mView_Content;
    private int mPosition;
    private String mTitle;
    private String mContent;

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
        mView_Content = (EditText) findViewById(R.id.note_content);

        mPosition = NoteManager.NEW_NODE_POSITION;
        if(savedInstanceState != null) {
            mPosition = (Integer) savedInstanceState.getSerializable(NoteManager.INDEX);
            mTitle = (String) savedInstanceState.getSerializable(NoteManager.TITLE);
            mContent = (String) savedInstanceState.getSerializable(NoteManager.CONTENT);
        } else {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                mPosition = extras.getInt(NoteManager.INDEX);
                mTitle = extras.getString(NoteManager.TITLE);
                mContent = extras.getString(NoteManager.CONTENT);
            }
        }

        if(mPosition == NoteManager.NEW_NODE_POSITION) {
            Log.d(TAG, "Notes Passing Failure !");
        } else {
            populateFields();
        }
    }

    private void populateFields() {
        if (mPosition != NoteManager.NEW_NODE_POSITION) {
            mView_Title.setText(mTitle);
            mView_Content.setText(mContent);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        update();
        outState.putSerializable(NoteManager.INDEX, mPosition);
        outState.putSerializable(NoteManager.TITLE, mTitle);
        outState.putSerializable(NoteManager.CONTENT, mContent);
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
            update();
            Bundle bundle = new Bundle();
            bundle.putInt(NoteManager.INDEX, mPosition);
            bundle.putString(NoteManager.TITLE, mTitle);
            bundle.putString(NoteManager.CONTENT, mContent);
            Intent mIntent = new Intent();
            mIntent.putExtras(bundle);

            setResult(RESULT_OK, mIntent);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void update() {
        mTitle = mView_Title.getText().toString();
        mContent = mView_Content.getText().toString();
    }
}
