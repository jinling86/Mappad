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
    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";
    public static final String DEFAULT_TITLE = "No title";

    public static final int NEW_NODE_POSITION = -1;
    public static final boolean NEED_SYNCHRONIZE = true;
    public static final boolean DO_NOT_NEED_SYNCHRONIZE = false;
    public static final boolean NEED_UPDATE = true;
    public static final boolean DO_NOT_NEED_UPDATE = false;

    private static String TAG = "<<<<< Note Manager >>>>>";
    private static String FILE_NAME = "notes_file";
    private static String TMP_FILE_NAME = "notes_file_tmp";
    public static String EXT_FILE_NAME = null;
    public static String EXT_TMP_FILE_NAME = null;

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
        if(EXT_FILE_NAME == null) {
            EXT_FILE_NAME = mContext.getFilesDir() + "/"+ FILE_NAME;
        }
        if(EXT_TMP_FILE_NAME == null) {
            EXT_TMP_FILE_NAME = mContext.getFilesDir() + "/"+ TMP_FILE_NAME;
        }
    }

    public boolean setNote(int index, String title, String content, double latitude, double longitude) {
        if(index < mAllNotes.size()) {
            boolean contentChanged = false;
            if(title == null || title.isEmpty()) {
                title = DEFAULT_TITLE;
            }
            if(mAllNotes.get(index).mTitle.compareTo(title) != 0) {
                contentChanged = true;
                mAllNotes.get(index).mTitle = title;
            }
            if(mAllNotes.get(index).mContent.compareTo(content) != 0) {
                contentChanged = true;
                mAllNotes.get(index).mContent = title;
            }
            if(!mAllNotes.get(index).mLatitude.equals(latitude)) {
                contentChanged = true;
                mAllNotes.get(index).mLatitude = latitude;
            }
            if(!mAllNotes.get(index).mLongitude.equals(longitude)) {
                contentChanged = true;
                mAllNotes.get(index).mLongitude = longitude;
            }
            if(contentChanged) {
                save();
                return NEED_SYNCHRONIZE;
            } else {
                return DO_NOT_NEED_SYNCHRONIZE;
            }

        } else {
            Log.d(TAG, "Error, trying to write an un-exist note!");
        }
        return DO_NOT_NEED_SYNCHRONIZE;
    }

    public int addNote(String title, String content, double latitude, double longitude) {
        NoteItem newNote = new NoteItem();
        if(title == null || title.isEmpty()) {
            title = DEFAULT_TITLE;
        }
        newNote.mTitle = title;
        newNote.mContent = content;
        newNote.mLongitude = longitude;
        newNote.mLatitude = latitude;
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

    public double getLatitude(int index) {
        if(index < mAllNotes.size()) {
            return mAllNotes.get(index).mLatitude;
        } else {
            return 0.0;
        }
    }

    public double getLongitude(int index) {
        if(index < mAllNotes.size()) {
            return mAllNotes.get(index).mLongitude;
        } else {
            return 0.0;
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
                File localFile = new File(EXT_FILE_NAME);
                localFile.deleteOnExit();
                File tmpFile = new File(EXT_TMP_FILE_NAME);
                if(!tmpFile.renameTo(localFile))
                    Log.d(TAG,"File rename failed!");

                mTimestamp = cloudTimestamp;
                mAllNotes = cloudNotes;

                return NEED_UPDATE;
            }

            return DO_NOT_NEED_UPDATE;

        } catch( ClassNotFoundException | IOException | ClassCastException e ) {
            Log.d(TAG, "Recovering cloud notes failed!");
            e.printStackTrace();
        }
        return DO_NOT_NEED_UPDATE;
    }
}
