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
 * This class is implement for CSI5175 Assignment 2.
 * NoteManager stores the date of the note. It is in charge of
 *      1. reading data from application internal file;
 *      2. saving the in-memory data to file;
 *      3. providing support for user interface facilities;
 *      4. restore note from the file downloaded from AWS S3 server.
 *
 * @author      Ling Jin
 * @version     1.0
 * @since       05/03/2015
 */
public class NoteManager {
    /**
     * Identifiers of extras used in Android Intent
     */
    public static final String EXTRA_INDEX = "index";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_CONTENT = "content";
    public static final String EXTRA_LATITUDE = "latitude";
    public static final String EXTRA_LONGITUDE = "longitude";

    /**
     * Default title of the note, used when the user does not specifies a title for a note
     */
    public static final String DEFAULT_TITLE = "No title";

    /**
     * Full names of the internal files
     */
    public static String EXT_FILE_NAME = null;
    public static String EXT_TMP_FILE_NAME = null;

    /**
     * Indicators of further operations
     * Update gives a hint that the note list need to be updated
     * Synchronize gives a hint that the file need to be uploaded to the AWS S3 server
     */
    public static final int NEW_NODE_POSITION = -1;
    public static final int DO_NOT_NEED_UPDATE = 0;
    public static final int NEED_UPDATE = 1;
    public static final int DO_NOT_NEED_SYNCHRONIZE = 2;
    public static final int NEED_SYNCHRONIZE = 3;

    /**
     * String constants for debugging and file storage
     */
    private static String TAG = "<<<<< Note Manager >>>>>";
    private static String FILE_NAME = "notes_file";
    private static String TMP_FILE_NAME = "notes_file_tmp";

    /**
     * this reference, used by inner classes
     */
    private Context mContext = null;

    /**
     * The note items are stored in an ArrayList
     * A timestamp gives the modified time of the note file, a nearby time means the file is new,
     * and the newer file will be kept and the older file will be discarded
     */
    private ArrayList<NoteItem> mAllNotes = null;
    private long mTimestamp;

    /**
     * Initializes the name of the internal files, which needs the help of the context of the main
     * activity. The files are only saved under the name of one Activity of the application. This
     * helps avoiding operations on external files. But it makes little hard to write the note file
     * in other Activity
     * The construct also read the file into memory
     * @param context context of the main activity
     */
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

    /**
     * Update the note using the passed arguments
     * @param index position of the note in the ArrayList
     * @param title note title, modified to "no title" if it is empty
     * @param content note content, will always be empty
     * @param latitude note location
     * @param longitude note location
     * @return if the note is modified, saves the note on disk and notifies the caller to upload it
     */
    public int setNote(int index, String title, String content, double latitude, double longitude) {
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
                mAllNotes.get(index).mContent = content;
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

    /**
     * Add a new note
     * This method always causes changes of the local file, so the caller should upload the file
     * without a return value
     * @param title note title, modified to "no title" if it is empty
     * @param content note content, will always be empty
     * @param latitude note location
     * @param longitude note location
     * @return the current storage position of the newly added note
     */
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
        save();
        return mAllNotes.size() - 1;
    }

    /**
     * @param index note item storage position
     * @return note title
     */
    public String getTitle(int index) {
        if(index < mAllNotes.size()) {
            return mAllNotes.get(index).mTitle;
        } else {
            return null;
        }
    }

    /**
     * @param index note item storage position
     * @return note content, useless now
     */
    public String getContent(int index) {
        if(index < mAllNotes.size()) {
            return mAllNotes.get(index).mContent;
        } else {
            return null;
        }
    }

    /**
     * @param index note item storage position
     * @return stored latitude
     */
    public double getLatitude(int index) {
        if(index < mAllNotes.size()) {
            return mAllNotes.get(index).mLatitude;
        } else {
            return 0.0;
        }
    }

    /**
     * @param index note item storage position
     * @return stored longitude
     */
    public double getLongitude(int index) {
        if(index < mAllNotes.size()) {
            return mAllNotes.get(index).mLongitude;
        } else {
            return 0.0;
        }
    }

    /**
     * Delete a note, and save the node in file
     * @param index deleting item storage position
     */
    public void deleteNote(int index) {
        if(index < mAllNotes.size()) {
            mAllNotes.remove(index);
            save();
        }
    }

    /**
     * Returns the number of items of current note, used in iteration
     * @return number of items
     */
    public int size() {
        return mAllNotes.size();
    }

    /**
     * Write current notes on to internal storage
     */
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

    /**
     * Read notes from internal storage
     */
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

    /**
     * Compare the timestamps of files from the AWS S3 server and the internal file, if the
     * downloaded file is new, replace the internal file with it
     * @return indicator of whether the ListView need updated for the newly coming notes
     */
    public int updateFromTmpFile() {
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

    /**
     * A static method for the Activities other than the main Activity to update the internal file.
     * The path and name of the file is needed in this case.
     * The file needs to be uploaded to AWS S3 Server if it has been really modified.
     * @param index position of the note in the ArrayList
     * @param title note title, modified to "no title" if it is empty
     * @param content note content, will always be empty
     * @param latitude note location
     * @param longitude note location
     * @return indicator of whether the note need to be uploaded to AWS Server
     */
    public static int saveChanges(int index, String title, String content, double latitude, double longitude) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(EXT_FILE_NAME));
            long timestamp = ois.readLong();
            ArrayList<NoteItem> allNotes = (ArrayList<NoteItem>)ois.readObject();
            ois.close();

            if(index < allNotes.size()) {
                boolean contentChanged = false;
                if (title == null || title.isEmpty()) {
                    title = DEFAULT_TITLE;
                }
                if (allNotes.get(index).mTitle.compareTo(title) != 0) {
                    contentChanged = true;
                    allNotes.get(index).mTitle = title;
                }
                if (allNotes.get(index).mContent.compareTo(content) != 0) {
                    contentChanged = true;
                    allNotes.get(index).mContent = content;
                }
                if (!allNotes.get(index).mLatitude.equals(latitude)) {
                    contentChanged = true;
                    allNotes.get(index).mLatitude = latitude;
                }
                if (!allNotes.get(index).mLongitude.equals(longitude)) {
                    contentChanged = true;
                    allNotes.get(index).mLongitude = longitude;
                }
                if (contentChanged) {
                    ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(EXT_FILE_NAME));
                    timestamp = System.currentTimeMillis();
                    oos.writeLong(timestamp);
                    oos.writeObject(allNotes);
                    oos.close();
                    return NEED_SYNCHRONIZE;
                } else {
                    return DO_NOT_NEED_SYNCHRONIZE;
                }
            }
        } catch( ClassNotFoundException | IOException | ClassCastException e ) {
            Log.d(TAG, "File saving failed!");
            e.printStackTrace();
        }
        return DO_NOT_NEED_SYNCHRONIZE;
    }
}
