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
     * Full names of the internal files
     * String constants for debugging and file storage
     */
    private static String TAG = "<<<<< Note Manager >>>>>";
    private static String EXT_TMP_TAG = "_tmp";

    public static String INDEX_FILE_NAME = "notes_file";
    public static String EXT_FILE_DIR = null;
    public static String TMP_INDEX_FILE_NAME = null;

    /**
     * this reference, used by inner classes
     */
    private Context mContext = null;

    /**
     */
    private ArrayList<NoteItem> mAllNotes = null;
    private ArrayList<NoteIndex> mNoteIndex = null;
    private ArrayList<Integer> mRealIndex = null;

    /**
     * Initialize the name of the internal files, which needs the help of the context of the main
     * activity. The files are only saved under the name of one Activity of the application. This
     * helps avoiding operations on external files. But it makes little hard to write the note file
     * in other Activity
     * The construct also read the file into memory
     * @param context context of the main activity
     */
    public NoteManager(Context context) {
        mContext = context;
        read();
        if(mNoteIndex == null) {
            mNoteIndex = new ArrayList<NoteIndex>();
        }
        if(mAllNotes == null) {
            mAllNotes = new ArrayList<NoteItem>();
        }
        if(TMP_INDEX_FILE_NAME == null) {
            TMP_INDEX_FILE_NAME = INDEX_FILE_NAME + EXT_TMP_TAG;
        }
        if(EXT_FILE_DIR == null) {
            EXT_FILE_DIR = mContext.getFilesDir().getPath();
        }
    }

    /**
     * Get the full name of the note index file
     * @return path name and file name of the note index file
     */
    public String getTmpIndexFileName() {
        return mContext.getFilesDir() + "/" + TMP_INDEX_FILE_NAME;
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
        int realIndex = mRealIndex.get(index);
        if(realIndex < mAllNotes.size()) {
            boolean contentChanged = false;
            if(title == null || title.isEmpty()) {
                title = DEFAULT_TITLE;
            }
            NoteItem noteItem = mAllNotes.get(realIndex);
            if(noteItem.mTitle.compareTo(title) != 0) {
                contentChanged = true;
                noteItem.mTitle = title;
            }
            if(noteItem.mContent.compareTo(content) != 0) {
                contentChanged = true;
                noteItem.mContent = content;
            }
            if(!noteItem.mLatitude.equals(latitude)) {
                contentChanged = true;
                noteItem.mLatitude = latitude;
            }
            if(!noteItem.mLongitude.equals(longitude)) {
                contentChanged = true;
                noteItem.mLongitude = longitude;
            }
            if(contentChanged) {
                noteItem.mModifiedTime = System.currentTimeMillis();
                NoteIndex noteIndex = mNoteIndex.get(realIndex);
                noteIndex.mModifiedTime = noteItem.mModifiedTime;
                noteIndex.mModified = true;
                noteIndex.mSynchronized = false;

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
        newNote.mCreatedTime = System.currentTimeMillis();
        newNote.mModifiedTime = newNote.mCreatedTime;
        newNote.mInternalName = newNote.mModifiedTime.toString();
        mAllNotes.add(newNote);

        NoteIndex newIndex = new NoteIndex();
        newIndex.mCreatedTime = newNote.mCreatedTime;
        newIndex.mModifiedTime = newNote.mModifiedTime;
        newIndex.mFileName = newIndex.mModifiedTime.toString();
        newIndex.mModified = true;
        newIndex.mSynchronized = false;
        newIndex.mDeleted = false;
        mNoteIndex.add(newIndex);

        adjustRealIndex();
        save();

        return size() - 1;
    }

    /**
     * @param index note item storage position
     * @return note title
     */
    public String getTitle(int index) {
        int realIndex = mRealIndex.get(index);
        if(realIndex < mAllNotes.size()) {
            return mAllNotes.get(realIndex).mTitle;
        } else {
            return null;
        }
    }

    /**
     * @param index note item storage position
     * @return note content, useless now
     */
    public String getContent(int index) {
        int realIndex = mRealIndex.get(index);
        if(realIndex < mAllNotes.size()) {
            return mAllNotes.get(realIndex).mContent;
        } else {
            return null;
        }
    }

    /**
     * @param index note item storage position
     * @return stored latitude
     */
    public double getLatitude(int index) {
        int realIndex = mRealIndex.get(index);
        if(realIndex < mAllNotes.size()) {
            return mAllNotes.get(realIndex).mLatitude;
        } else {
            return 0.0;
        }
    }

    /**
     * @param index note item storage position
     * @return stored longitude
     */
    public double getLongitude(int index) {
        int realIndex = mRealIndex.get(index);
        if(realIndex < mAllNotes.size()) {
            return mAllNotes.get(realIndex).mLongitude;
        } else {
            return 0.0;
        }
    }

    /**
     * Delete a note, and save the node in file
     * @param index deleting item storage position
     */
    public void deleteNote(int index) {
        int realIndex = mRealIndex.get(index);
        if(realIndex < mNoteIndex.size()) {
            NoteIndex noteIndex = mNoteIndex.get(realIndex);
            NoteItem noteItem = mAllNotes.get(realIndex);

            noteIndex.mDeleted = true;
            noteIndex.mModified = true;
            noteIndex.mModifiedTime = System.currentTimeMillis();
            noteItem.mModifiedTime = noteIndex.mModifiedTime;

            // Delete the note from the list view
            adjustRealIndex();
            save();
        }
    }

    /**
     * Return the number of items of current note, used in iteration
     * @return number of items
     */
    public int size() {
        return mRealIndex.size();
    }

    public int getRealIndex(int index) {
        return mRealIndex.get(index);
    }

    private void adjustRealIndex() {
        if(mRealIndex == null)
            mRealIndex = new ArrayList<Integer>();
        mRealIndex.clear();
        for(int i = 0; i < mNoteIndex.size(); i++) {
            if(!mNoteIndex.get(i).mDeleted) {
                mRealIndex.add(i);
            }
        }
    }
    /**
     * Write current notes on to internal storage
     */
    private void save() {
        if(mAllNotes == null)
            return;
        try {
            for(int i = 0; i < mNoteIndex.size(); i++) {
                if(mNoteIndex.get(i).mModified) {
                    mNoteIndex.get(i).mModified = false;
                    FileOutputStream n_fos = mContext.openFileOutput(mNoteIndex.get(i).mFileName, Context.MODE_PRIVATE);
                    ObjectOutputStream n_oos = new ObjectOutputStream(n_fos);
                    n_oos.writeObject(mAllNotes.get(i));
                    n_oos.close();
                }
            }
            FileOutputStream i_fos = mContext.openFileOutput(INDEX_FILE_NAME, Context.MODE_PRIVATE);
            ObjectOutputStream i_oos = new ObjectOutputStream(i_fos);
            i_oos.writeObject(mNoteIndex);
            i_oos.close();
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
            FileInputStream i_fis = mContext.openFileInput(INDEX_FILE_NAME);
            ObjectInputStream i_ois = new ObjectInputStream(i_fis);
            mNoteIndex = (ArrayList<NoteIndex>)i_ois.readObject();
            i_ois.close();
            if(mNoteIndex != null) {
                mAllNotes = new ArrayList<NoteItem>();
                for (NoteIndex index : mNoteIndex) {
                    FileInputStream n_fis = mContext.openFileInput(index.mFileName);
                    ObjectInputStream n_ois = new ObjectInputStream(n_fis);
                    mAllNotes.add((NoteItem)n_ois.readObject());
                    n_ois.close();
                    Log.d(TAG, "Recover note " + index.mFileName);
                }
                // Prepare for the notes should be displayed
                adjustRealIndex();
            }
        } catch( ClassNotFoundException | IOException | ClassCastException e ) {
            Log.d(TAG, "Recovering notes failed!");
            e.printStackTrace();
            // Discard all notes if any of the read operations fails
            mNoteIndex = null;
            mAllNotes = null;
        }
    }

    /**
     * Return the names of files that need to be uploaded to AWS S3 Server
     * @return file names
     */
    public ArrayList<String> getFileNamesForSending() {
        ArrayList<String> fileNames = new ArrayList<String>();
        for(NoteIndex index : mNoteIndex) {
            if(!index.mSynchronized) {
                fileNames.add(EXT_FILE_DIR + index.mFileName);
            }
        }
        // Add the index file if any file is to be sent
        if(fileNames.size() != 0) {
            fileNames.add(EXT_FILE_DIR + INDEX_FILE_NAME);
        }
        return fileNames;
    }

    /**
     * Return the names of the note files that need to be downloaded
     * @return file names
     */
    public ArrayList<String> getFileNamesForReceiving() {
        ArrayList<String> fileNames = new ArrayList<String>();
        try {
            FileInputStream fis = mContext.openFileInput(TMP_INDEX_FILE_NAME);
            ObjectInputStream ois = new ObjectInputStream(fis);
            ArrayList<NoteIndex> cloudIndex = (ArrayList<NoteIndex>)ois.readObject();
            ois.close();

            int iCloud = 0;
            int iLocal = 0;
            while(iLocal < mNoteIndex.size() && iCloud < cloudIndex.size()) {
                NoteIndex localItem = mNoteIndex.get(iLocal);
                NoteIndex cloudItem = cloudIndex.get(iCloud);
                if(localItem.mCreatedTime.compareTo(cloudItem.mCreatedTime) == 0) {
                    // Same file, compare modification time
                    if(cloudItem.mModifiedTime.compareTo(localItem.mModifiedTime) > 0) {
                        fileNames.add(cloudItem.mFileName);
                    }
                } else if (localItem.mCreatedTime.compareTo(cloudItem.mCreatedTime) > 0) {
                    // A file is deleted/missed in local
                    fileNames.add(cloudItem.mFileName);
                    iCloud++;
                } else {
                    // A file only exists in the cell phone, synchronize it if it is not a deleted
                    // file
                    if(!localItem.mDeleted)
                        localItem.mSynchronized = false;
                }
            }
        } catch( ClassNotFoundException | IOException | ClassCastException e ) {
            Log.d(TAG, "Recovering cloud notes failed!");
            e.printStackTrace();
        }
        return fileNames;
    }

    public ArrayList<String> getFileNamesForDeleting() {
        ArrayList<String> fileNames = new ArrayList<String>();
        for(NoteIndex index : mNoteIndex) {
            if(index.mDeleted) {
                fileNames.add(EXT_FILE_DIR + index.mFileName);
            }
        }
        return fileNames;
    }

    public void confirmSend(String filename) {
        for(NoteIndex index: mNoteIndex) {
            if(filename.compareTo(EXT_FILE_DIR + index.mFileName) == 0) {
                index.mSynchronized = true;
                index.mModified = true;
                save();
            }
        }
    }

    public void addNoteFromExternal(int addPosition, NoteItem noteItem) {
        mAllNotes.add(addPosition, noteItem);
        NoteIndex newIndex = new NoteIndex();
        newIndex.mFileName = noteItem.mInternalName;
        newIndex.mModifiedTime = noteItem.mModifiedTime;
        newIndex.mModifiedTime = noteItem.mModifiedTime;
        newIndex.mModified = true;
        newIndex.mSynchronized = true;
        newIndex.mDeleted = false;
        mNoteIndex.add(addPosition, newIndex);
    }

    public void confirmReceive(String filename) {
        try {
            FileInputStream fis = mContext.openFileInput(filename);
            ObjectInputStream ois = new ObjectInputStream(fis);
            NoteItem aNote = (NoteItem)ois.readObject();
            ois.close();

            boolean addToLast = true;
            for(int i = 0; i < mNoteIndex.size(); i++) {
                if(mNoteIndex.get(i).mCreatedTime.compareTo(aNote.mCreatedTime) == 0) {
                    mAllNotes.set(i, aNote);
                    mNoteIndex.get(i).mModifiedTime = aNote.mModifiedTime;
                    addToLast = false;
                    break;
                } else if(mNoteIndex.get(i).mCreatedTime.compareTo(aNote.mCreatedTime) > 0) {
                    addNoteFromExternal(i, aNote);
                    adjustRealIndex();
                    addToLast = false;
                    break;
                }
            }
            if(addToLast) {
                addNoteFromExternal(mNoteIndex.size(), aNote);
            }
            save();
            File localFile = new File(filename);
            localFile.deleteOnExit();
        } catch( ClassNotFoundException | IOException | ClassCastException e ) {
            Log.d(TAG, "Recovering cloud notes failed!");
            e.printStackTrace();
        }
    }

    public void confirmDelete(String filename) {
        int position = NEW_NODE_POSITION;
        for(int i = 0; i < mNoteIndex.size(); i++) {
            if(mNoteIndex.get(i).mFileName.compareTo(filename) == 0) {
                position = i;
                break;
            }
        }
        mNoteIndex.remove(position);
        mAllNotes.remove(position);
        // Adjust the real position of the note
        adjustRealIndex();
        File localFile = new File(filename);
        localFile.deleteOnExit();
    }

    /**
     * A static method for the Activities other than the main Activity to update the internal file.
     * The path and name of the file is needed in this case.
     * The file needs to be uploaded to AWS S3 Server if it has been really modified.
     * @param realIndex position of the note in the ArrayList
     * @param title note title, modified to "no title" if it is empty
     * @param content note content, will always be empty
     * @param latitude note location
     * @param longitude note location
     * @return indicator of whether the note need to be uploaded to AWS Server
     */
    public static void saveChanges(int realIndex, String title, String content, double latitude, double longitude) {
        try {
            ObjectInputStream i_ois = new ObjectInputStream(new FileInputStream(EXT_FILE_DIR + INDEX_FILE_NAME));
            ArrayList<NoteIndex> allIndex = (ArrayList<NoteIndex>)i_ois.readObject();
            i_ois.close();
            if(realIndex >= allIndex.size()) {
                Log.d(TAG, "Index error in saveChanges()");
                return;
            }
            String noteFilename = EXT_FILE_DIR + allIndex.get(realIndex).mFileName;
            ObjectInputStream n_ois = new ObjectInputStream(new FileInputStream(noteFilename));
            NoteItem aNote = (NoteItem)n_ois.readObject();
            n_ois.close();

            boolean contentChanged = false;
            if(title == null || title.isEmpty()) {
                title = DEFAULT_TITLE;
            }
            if(aNote.mTitle.compareTo(title) != 0) {
                contentChanged = true;
                aNote.mTitle = title;
            }
            if(aNote.mContent.compareTo(content) != 0) {
                contentChanged = true;
                aNote.mContent = content;
            }
            if(!aNote.mLatitude.equals(latitude)) {
                contentChanged = true;
                aNote.mLatitude = latitude;
            }
            if(!aNote.mLongitude.equals(longitude)) {
                contentChanged = true;
                aNote.mLongitude = longitude;
            }
            if(contentChanged) {
                aNote.mModifiedTime = System.currentTimeMillis();
                NoteIndex noteIndex = allIndex.get(realIndex);
                noteIndex.mModifiedTime = aNote.mModifiedTime;
                noteIndex.mModified = true;
                noteIndex.mSynchronized = false;

                ObjectOutputStream i_oos = new ObjectOutputStream(new FileOutputStream(EXT_FILE_DIR + INDEX_FILE_NAME));
                i_oos.writeObject(allIndex);
                i_oos.close();
                ObjectOutputStream n_oos = new ObjectOutputStream(new FileOutputStream(noteFilename));
                n_oos.writeObject(aNote);
                n_oos.close();
            }
        } catch( ClassNotFoundException | IOException | ClassCastException e ) {
            Log.d(TAG, "File saving failed!");
            e.printStackTrace();
        }
    }
}
