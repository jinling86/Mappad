package ca.uottawa.ljin027.mappad;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

/**
 * Created by Ling Jin on 05/03/2015.
 */
public class NoteManager {

    public static final String INDEX = "index";
    public static final String TITLE = "title";
    public static final String CONTENT = "content";
    public static final int NEW_NODE_POSITION = -1;

    private final String TAG = "<<<<< Note Manager >>>>>";
    private final String FILE_NAME = "notes_file";
    private final String TMP_FILE_NAME = "notes_file_tmp";
    public static String EXT_NAME = null;
    public static String TMP_NAME = null;

    private Context mContext;

    private ArrayList<NoteItem> mAllNotes;
    private long mTimestamp;

    public NoteManager(Context context) {
        mContext = context;
        read();
        if(mAllNotes == null) {
            mAllNotes = new ArrayList<NoteItem>();
            mTimestamp = 0;
        }
        if(EXT_NAME == null) {
            EXT_NAME = mContext.getFilesDir() + "/"+ FILE_NAME;
        }
        if(TMP_NAME == null) {
            TMP_NAME = mContext.getFilesDir() + "/"+ FILE_NAME + "_tmp";
        }
    }

    public void setNote(int index, String title, String content) {
        if(index < mAllNotes.size()) {
            mAllNotes.get(index).mTitle = title;
            mAllNotes.get(index).mContent = content;
        } else if(index == mAllNotes.size()) {
            addNote(title, content);
        }
        save();
    }

    public int addNote(String title, String content) {
        NoteItem newNote = new NoteItem();
        newNote.mTitle = title;
        newNote.mContent = content;
        newNote.mLongitude = "";
        newNote.mLatitude = "";
        mAllNotes.add(newNote);
        return mAllNotes.size() - 1;
    }

    public String getTitle(int index) {
        if(index < mAllNotes.size()) {
            return mAllNotes.get(index).mTitle;
        } else {
            return null;
        }
    }

    public String getContent(int index) {
        if(index < mAllNotes.size()) {
            return mAllNotes.get(index).mContent;
        } else {
            return null;
        }
    }

    public void deleteNote(int index) {
        if(index < mAllNotes.size()) {
            mAllNotes.remove(index);
            save();
        }
    }

    public int size() {
        return mAllNotes.size();
    }

    private void save() {
        if(mAllNotes == null)
            return;
        try {
            FileOutputStream fos = mContext.openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            mTimestamp = System.currentTimeMillis();
            oos.writeLong(mTimestamp);
            oos.writeObject(mAllNotes);
            oos.close();
            Log.d(TAG, "List item saved");
        } catch( IOException e ) {
            e.printStackTrace();
            Log.d(TAG, "Saving notes failed!");
        }
    }

    private void read() {
        try {
            FileInputStream fis = mContext.openFileInput(FILE_NAME);
            ObjectInputStream ois = new ObjectInputStream(fis);
            mTimestamp = ois.readLong();
            mAllNotes = (ArrayList<NoteItem>)ois.readObject();
            ois.close();
            if(mAllNotes != null) {
                for (NoteItem note : mAllNotes) {
                    Log.d(TAG, "Recovered item " + note.mTitle);
                }
            }
        } catch( ClassNotFoundException | IOException | ClassCastException e ) {
            Log.d(TAG, "Recovering notes failed!");
            e.printStackTrace();
        }
    }

    public boolean updateFromTmpFile() {
        try {
            FileInputStream fis = mContext.openFileInput(TMP_FILE_NAME);
            ObjectInputStream ois = new ObjectInputStream(fis);
            long cloudTimestamp = ois.readLong();
            ArrayList<NoteItem> cloudNotes = (ArrayList<NoteItem>)ois.readObject();
            ois.close();

            if(cloudTimestamp > mTimestamp) {
                File localFile = new File(EXT_NAME);
                localFile.deleteOnExit();
                File tmpFile = new File(TMP_NAME);
                if(!tmpFile.renameTo(localFile))
                    Log.d(TAG,"File rename failed!");

                mTimestamp = cloudTimestamp;
                mAllNotes = cloudNotes;

                return true;
            }

            return false;

        } catch( ClassNotFoundException | IOException | ClassCastException e ) {
            Log.d(TAG, "Recovering cloud notes failed!");
            e.printStackTrace();
        }
        return false;
    }
}
