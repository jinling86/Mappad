package ca.uottawa.ljin027.mappad;

import android.content.Context;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * This class is implemented for CSI5175 Assignment 2.
 * NoteManager stores the data of the note. It is in charge of
 *      1. maintaining an index of the notes in the application;
 *      2. reading notes and note index from application internal storage;
 *      3. saving changes to files;
 *      4. providing supports for user interface facilities;
 *      5. restoring notes from the file downloaded from AWS S3 server;
 *      6. determining which files should be uploaded/downloaded/deleted from the AWS S3 server:
 *          - If a local file does not exist on AWS S3 server, uploads it;
 *          - If a local file is modified, uploads it;
 *          - If a file on AWS S3 server is not in cell phone, downloads it;
 *          - If a note on AWS S3 server is newer than the local one, downloads it;
 *          - If a note on AWS S3 server is older than the local one, uploads it;
 *          - If a note is marked "deleted" or being deleted, then deletes it.
 * The name of a note file consists of "note_", creation time string and ".txt".
 *
 * @author      Ling Jin and Xi Song
 * @version     1.0
 * @since       05/03/2015, LJ
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
     * Default title of the note, used when the user does not specifies a title for a note.
     */
    public static final String DEFAULT_TITLE = "No title";

    /**
     * Indicators of further operations, synchronize gives a hint that the file need to be uploaded
     * to the AWS S3 server.
     */
    public static final int NEW_NODE_POSITION = -1;
    public static final int DO_NOT_NEED_SYNCHRONIZE = 0;
    public static final int NEED_SYNCHRONIZE = 1;

    /**
     * Name components of the note file; full directory path for using by other activities; and
     * string constants for debugging.
     */
    public static String TMP_TAG = ".tmp";
    public static String INDEX_FILE_NAME = "notes_index";
    public static String NOTE_PREFIX = "note_";
    public static String NOTE_SUFFIX = ".txt";
    public static String EXT_FILE_DIR = null;
    private static String TAG = "<<<<< Note Manager >>>>>";

    /**
     * Format of the created/modified time string.
     */
    private static SimpleDateFormat mTimeFormat = new SimpleDateFormat("yyyy.MM.dd kk:mm:ss.SSS");

    /**
     * A time span for comparison of the modified time, for the uploading process introduces some
     * latency so the modified time obtain online should be later than the real modified time.
     */
    private static int TIME_SPAN = 30000;

    /**
     * this reference of the List Activity, used for obtaining the default storage location
     */
    private Context mContext = null;

    /**
     * Array of notes, note indexes and a indicator for jumping off the deleted note items
     */
    private ArrayList<NoteItem> mAllNotes = null;
    private ArrayList<NoteIndex> mNoteIndex = null;
    private ArrayList<Integer> mRealIndex = null;

    /**
     * Initializes the notes, may be called by Map Activity and List Activity
     * @param context reference of the caller Activity
     */
    public NoteManager(Context context) {
        mContext = context;
        read();
        if(mNoteIndex == null || mAllNotes == null) {
            mNoteIndex = new ArrayList<NoteIndex>();
            mAllNotes = new ArrayList<NoteItem>();
            adjustRealIndex();
        }
        if(EXT_FILE_DIR == null) {
            EXT_FILE_DIR = mContext.getFilesDir().getPath();
        }
    }

    /**
     * Gets the temporary name of the note file
     * @param internalName normal file name
     * @return the temporary name
     */
    public static String getTmpName(String internalName) {
        return internalName + TMP_TAG;
    }

    /**
     * @param internalName normal file name
     * @return file name with its full path and a tmp tag
     */
    public static String getTmpFullName(String internalName) {
        return EXT_FILE_DIR + "/" + internalName + TMP_TAG;
    }

    /**
     * @param internalName normal file name
     * @return file name with its full path
     */
    public static String getFullName(String internalName) {
        return EXT_FILE_DIR + "/" + internalName;
    }

    /**
     * Constructs note file name based on the time string
     * @param timeString fixed length time string
     * @return file name
     */
    private String getNoteFilename(String timeString) {
        return (NOTE_PREFIX + timeString + NOTE_SUFFIX);
    }

    /**
     * Update thes note using the passed arguments
     * @param index position of the note in the ArrayList, without the deleted items
     * @param title note title, modified to "no title" if it is empty
     * @param content note content, will always be empty
     * @param latitude note location
     * @param longitude note location
     * @return if the note is modified, saves the note on disk and notifies the caller to upload it
     */
    public int setNote(int index, String title, String content, double latitude, double longitude) {
        Log.d(TAG, "Set note " + index);
        if(index >= mRealIndex.size()) {
            Log.d(TAG, "Error, index does not exist!");
            return DO_NOT_NEED_SYNCHRONIZE;
        }
        int realIndex = mRealIndex.get(index);
        if(realIndex < mAllNotes.size()) {
            // Data to be modified
            NoteIndex noteIndex = mNoteIndex.get(realIndex);
            NoteItem noteItem = mAllNotes.get(realIndex);
            // Check if the note has been changed
            boolean contentChanged = false;
            if(title == null || title.isEmpty()) {
                title = DEFAULT_TITLE;
            }
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
                // Convert time format
                Long currentTime = System.currentTimeMillis();
                noteItem.mModifiedTime = mTimeFormat.format(new Date(currentTime));
                noteIndex.mModifiedTime = currentTime;
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
     * Adds a new note. Although this method always changes of the local file, but as the files is
     * unmodified, it is not necessary to upload the file immediately.
     * @param title note title, modified to "no title" if it is empty
     * @param content note content, will always be empty
     * @param latitude note location
     * @param longitude note location
     * @return the current storage position of the newly added note
     */
    public int addNote(String title, String content, double latitude, double longitude) {
        // Add a note and an index
        NoteItem newNote = new NoteItem();
        NoteIndex newIndex = new NoteIndex();
        // Change the empty title
        if(title == null || title.isEmpty()) {
            title = DEFAULT_TITLE;
        }
        Long currentTime = System.currentTimeMillis();
        newNote.mTitle = title;
        newNote.mContent = content;
        newNote.mLongitude = longitude;
        newNote.mLatitude = latitude;
        newNote.mCreatedTime = mTimeFormat.format(new Date(currentTime));
        newNote.mModifiedTime = newNote.mCreatedTime;
        newNote.mNoteFilename = getNoteFilename(currentTime.toString());
        mAllNotes.add(newNote);

        newIndex.mCreatedTime = currentTime;
        newIndex.mModifiedTime = currentTime;
        newIndex.mFileName = newNote.mNoteFilename;
        newIndex.mModified = true;
        newIndex.mSynchronized = false;
        newIndex.mDeleted = false;
        mNoteIndex.add(newIndex);

        adjustRealIndex();
        save();
        Log.d(TAG, "Add note " + (size() - 1));

        return size() - 1;
    }

    /**
     * @param index note item storage position
     * @return note title
     */
    public String getTitle(int index) {
        if(index >= mRealIndex.size()) {
            Log.d(TAG, "Error, index does not exist!");
            return null;
        }
        int realIndex = mRealIndex.get(index);
        if(realIndex < mAllNotes.size()) {
            return mAllNotes.get(realIndex).mTitle;
        } else {
            Log.d(TAG, "Error, note does not exist!");
            return null;
        }
    }

    /**
     * @param index note item storage position
     * @return note content, useless now
     */
    public String getContent(int index) {
        if(index >= mRealIndex.size()) {
            Log.d(TAG, "Error, index does not exist!");
            return null;
        }
        int realIndex = mRealIndex.get(index);
        if(realIndex < mAllNotes.size()) {
            return mAllNotes.get(realIndex).mContent;
        } else {
            Log.d(TAG, "Error, note does not exist!");
            return null;
        }
    }

    /**
     * @param index note item storage position
     * @return stored latitude
     */
    public double getLatitude(int index) {
        if(index >= mRealIndex.size()) {
            Log.d(TAG, "Error, index does not exist!");
            return 0.0;
        }
        int realIndex = mRealIndex.get(index);
        if(realIndex < mAllNotes.size()) {
            return mAllNotes.get(realIndex).mLatitude;
        } else {
            Log.d(TAG, "Error, note does not exist!");
            return 0.0;
        }
    }

    /**
     * @param index note item storage position
     * @return stored longitude
     */
    public double getLongitude(int index) {
        if(index >= mRealIndex.size()) {
            Log.d(TAG, "Error, index does not exist!");
            return 0.0;
        }
        int realIndex = mRealIndex.get(index);
        if(realIndex < mAllNotes.size()) {
            return mAllNotes.get(realIndex).mLongitude;
        } else {
            Log.d(TAG, "Error, note does not exist!");
            return 0.0;
        }
    }

    /**
     * Deletes a note, and save the node in file
     * @param index deleting item storage position
     */
    public void deleteNote(int index) {
        if(index >= mRealIndex.size()) {
            Log.d(TAG, "Error, index does not exist!");
            return;
        }
        Log.d(TAG, "Delete note " + index);
        int realIndex = mRealIndex.get(index);
        if(realIndex < mNoteIndex.size()) {
            NoteIndex noteIndex = mNoteIndex.get(realIndex);
            NoteItem noteItem = mAllNotes.get(realIndex);

            noteIndex.mDeleted = true;
            noteIndex.mModified = true;
            noteIndex.mSynchronized = false;
            Long currentTime = System.currentTimeMillis();
            noteIndex.mModifiedTime = currentTime;
            noteItem.mModifiedTime = mTimeFormat.format(new Date(currentTime));

            adjustRealIndex();
            save();
        }
    }

    /**
     * Return the number of notes, used in iteration
     * @return number of items
     */
    public int size() {
        return mRealIndex.size();
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
                    DataOutputStream n_dos = new DataOutputStream(
                            mContext.openFileOutput(mNoteIndex.get(i).mFileName, Context.MODE_PRIVATE));
                    mAllNotes.get(i).writeToStream(n_dos);
                    n_dos.close();
                }
            }
            FileOutputStream i_fos = mContext.openFileOutput(INDEX_FILE_NAME, Context.MODE_PRIVATE);
            ObjectOutputStream i_oos = new ObjectOutputStream(i_fos);
            i_oos.writeObject(mNoteIndex);
            i_oos.close();
            Log.d(TAG, "Notes saved");
        } catch( IOException e ) {
            e.printStackTrace();
            Log.d(TAG, "Saving notes failed!");
            errorRecovery();
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
                    NoteItem aNote = new NoteItem();
                    DataInputStream n_dis = new DataInputStream(mContext.openFileInput(index.mFileName));
                    aNote.readFromStream(n_dis);
                    mAllNotes.add(aNote);
                    n_dis.close();
                    Log.d(TAG, "Recover note " + index.mFileName);
                }
                // Prepare for the notes should be displayed
                adjustRealIndex();
            }
        } catch( ClassNotFoundException | IOException | ClassCastException e ) {
            Log.d(TAG, "Recovering notes failed!");
            e.printStackTrace();
            errorRecovery();
        }
        Log.d(TAG, "Notes recovered");
    }

    /**
     * Does somethings when I/O fails. Hope this never happens.
     */
    private void errorRecovery() {
        // What should I do... let's make things worse
        // Discard all notes if any of the read/write operations fails
        mNoteIndex = new ArrayList<NoteIndex>();
        mAllNotes = new ArrayList<NoteItem>();
        mRealIndex = null;
        adjustRealIndex();
    }

    /**
     * Returns the names of files that need to be uploaded to AWS S3 Server
     * @return file names
     */
    public ArrayList<String> getFileNamesForSending() {
        ArrayList<String> fileNames = new ArrayList<String>();
        for(NoteIndex index : mNoteIndex) {
            // Ignore the files should be deleted
            if(!index.mSynchronized && !index.mDeleted) {
                fileNames.add(index.mFileName);
                Log.d(TAG, "Intend to send " + index.mFileName);
            }
        }
        return fileNames;
    }

    /**
     * Returns the names of the note files that need to be downloaded
     * @return file names
     */
    public ArrayList<String> getFileNamesForReceiving() {
        ArrayList<String> fileNames = new ArrayList<String>();
        try {
            FileInputStream fis = mContext.openFileInput(getTmpName(INDEX_FILE_NAME));
            ObjectInputStream ois = new ObjectInputStream(fis);
            ArrayList<NoteIndex> cloudIndex = (ArrayList<NoteIndex>)ois.readObject();
            ois.close();

            int iCloud = 0;
            int iLocal = 0;
            while(iLocal < mNoteIndex.size() && iCloud < cloudIndex.size()) {
                NoteIndex localItem = mNoteIndex.get(iLocal);
                NoteIndex cloudItem = cloudIndex.get(iCloud);
                int cTimeCompResult = localItem.mCreatedTime.compareTo(cloudItem.mCreatedTime);
                if(cTimeCompResult == 0) {
                    // Same file, compare modification time
                    int mTimeCompResult = cloudItem.mModifiedTime.compareTo(localItem.mModifiedTime);
                    if(mTimeCompResult > TIME_SPAN) {
                        // The cloud file is newer, download it
                        fileNames.add(cloudItem.mFileName);
                        Log.d(TAG, "Intend to fetch " + cloudItem.mFileName);
                    } else if(mTimeCompResult < -TIME_SPAN) {
                        // The cloud file is out of date, upload it
                        localItem.mSynchronized = false;
                    }
                    iCloud++;
                    iLocal++;
                } else if (cTimeCompResult > 0) {
                    // A file is deleted/missed in local
                    fileNames.add(cloudItem.mFileName);
                    Log.d(TAG, "Intend to fetch " + cloudItem.mFileName);
                    iCloud++;
                } else {
                    // A file only exists in the cell phone, synchronize it
                    localItem.mSynchronized = false;
                    iLocal++;
                }
            }
            while(iLocal < mNoteIndex.size()) {
                // Miss cloud files
                mNoteIndex.get(iLocal).mSynchronized = false;
                iLocal++;
            }
            while(iCloud < cloudIndex.size()) {
                // Miss local files
                fileNames.add(cloudIndex.get(iCloud).mFileName);
                Log.d(TAG, "Intend to fetch " + cloudIndex.get(iCloud).mFileName);
                iCloud++;
            }
        } catch( ClassNotFoundException | IOException | ClassCastException e ) {
            Log.d(TAG, "Recovering cloud notes failed!");
            e.printStackTrace();
        }
        return fileNames;
    }

    /**
     * Returns the names of the files need to be deleted from the AWS S3 server
     * @return file names
     */
    public ArrayList<String> getFileNamesForDeleting() {
        ArrayList<String> fileNames = new ArrayList<String>();
        for(NoteIndex index : mNoteIndex) {
            if(index.mDeleted) {
                fileNames.add(index.mFileName);
                Log.d(TAG, "Intend to delete " + index.mFileName);
            }
        }
        return fileNames;
    }

    /**
     * Processes the sending confirmation, sets the file to be synchronized.
     * @param filename the name of the sent file
     */
    public void confirmSend(String filename) {
        for(NoteIndex index: mNoteIndex) {
            if(filename.compareTo(index.mFileName) == 0) {
                index.mSynchronized = true;
                Log.d(TAG, "Process sending confirmation of " + index.mFileName);
                save();
            }
        }
    }

    /**
     * A helper method, adds a new node into system.
     * @param addPosition the right position of the note, in chronological manner
     * @param noteItem reference of the note
     */
    public void addNoteFromExternal(int addPosition, NoteItem noteItem) {
        NoteIndex newIndex = new NoteIndex();
        try {
            newIndex.mCreatedTime = mTimeFormat.parse(noteItem.mCreatedTime).getTime();
            newIndex.mModifiedTime = mTimeFormat.parse(noteItem.mModifiedTime).getTime();
        } catch (ParseException e) {
            Log.d(TAG, "Fail to add external note");
            e.printStackTrace();
            return;
        }
        newIndex.mFileName = getNoteFilename(newIndex.mCreatedTime.toString());
        newIndex.mModified = true;
        newIndex.mSynchronized = true;
        newIndex.mDeleted = false;
        mNoteIndex.add(addPosition, newIndex);
        mAllNotes.add(addPosition, noteItem);
    }

    /**
     * Processes the received note file. Adds it into the right place. Saves it onto disk, and
     * deletes the temporary file.
     * @param filename name of the temporary file
     */
    public void confirmReceive(String filename) {
        try {
            NoteItem aNote = new NoteItem();
            DataInputStream n_dis = new DataInputStream(mContext.openFileInput(getTmpName(filename)));
            aNote.readFromStream(n_dis);
            n_dis.close();

            Long noteCreatedTime = mTimeFormat.parse(aNote.mCreatedTime).getTime();
            Long noteModifiedTime = mTimeFormat.parse(aNote.mModifiedTime).getTime();
            Log.d(TAG, "Process receiving confirmation of " + filename);
            boolean addToLast = true;
            for(int i = 0; i < mNoteIndex.size(); i++) {
                int cTimeCompResult = mNoteIndex.get(i).mCreatedTime.compareTo(noteCreatedTime);
                if(cTimeCompResult == 0) {
                    mAllNotes.set(i, aNote);
                    mNoteIndex.get(i).mModifiedTime = noteModifiedTime;
                    mNoteIndex.get(i).mSynchronized = true;
                    mNoteIndex.get(i).mModified = true;
                    addToLast = false;
                    break;
                } else if(cTimeCompResult > 0) {
                    addNoteFromExternal(i, aNote);
                    adjustRealIndex();
                    addToLast = false;
                    break;
                }
            }
            if(addToLast) {
                addNoteFromExternal(mNoteIndex.size(), aNote);
                adjustRealIndex();
            }
            save();
            File localFile = new File(getTmpFullName(filename));
            localFile.deleteOnExit();
        } catch( IOException | ClassCastException |ParseException e ) {
            Log.d(TAG, "Recovering cloud notes failed!");
            e.printStackTrace();
        }
    }

    /**
     * Processes the confirmation of file deletion, delete the note reference in the application
     * and also deletes the note file.
     * @param filename file name
     */
    public void confirmDelete(String filename) {
        int position = NEW_NODE_POSITION;
        for(int i = 0; i < mNoteIndex.size(); i++) {
            if(mNoteIndex.get(i).mFileName.compareTo(filename) == 0) {
                Log.d(TAG, "Process deleting confirmation of " + filename);
                position = i;
                break;
            }
        }
        if(position != NEW_NODE_POSITION) {
            mNoteIndex.remove(position);
            mAllNotes.remove(position);
            // Adjust the real position of the note
            adjustRealIndex();
            File localFile = new File(getFullName(filename));
            localFile.deleteOnExit();
            save();
        }
    }

    /**
     * A static method for the Activities other than the main Activity to update the internal file.
     * The method is firstly implemented for use of Edit Activity which does not has an instance of
     * the notes. As the software being modified, this method is not necessary as List Activity
     * saves the notes every time it modifies the notes. But this method is still kept as a memory.
     * @param index position of the note in the ArrayList
     * @param title note title, modified to "no title" if it is empty
     * @param content note content, will always be empty
     * @param latitude note location
     * @param longitude note location
     */
    public static void saveChanges(int index, String title, String content, double latitude, double longitude) {
        try {
            ObjectInputStream i_ois = new ObjectInputStream(new FileInputStream(getFullName(INDEX_FILE_NAME)));
            ArrayList<NoteIndex> allIndex = (ArrayList<NoteIndex>)i_ois.readObject();
            i_ois.close();

            int realIndex = NEW_NODE_POSITION;
            for(int i = 0; i < allIndex.size(); i++) {
                if(!allIndex.get(i).mDeleted) {
                    if(index > 0) {
                        index--;
                    } else {
                        realIndex = i;
                        break;
                    }
                }
            }
            if(realIndex == NEW_NODE_POSITION) {
                Log.d(TAG, "Index error in saveChanges()");
                return;
            }
            String noteFilename = getFullName(allIndex.get(realIndex).mFileName);
            NoteItem aNote = new NoteItem();
            DataInputStream n_dis = new DataInputStream(new FileInputStream(noteFilename));
            aNote.readFromStream(n_dis);
            n_dis.close();

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
                Long currentTime = System.currentTimeMillis();
                aNote.mModifiedTime = mTimeFormat.format(new Date(currentTime));

                NoteIndex noteIndex = allIndex.get(realIndex);
                noteIndex.mModifiedTime = currentTime;
                noteIndex.mModified = false;
                noteIndex.mSynchronized = false;

                ObjectOutputStream i_oos = new ObjectOutputStream(new FileOutputStream(getFullName(INDEX_FILE_NAME)));
                i_oos.writeObject(allIndex);
                i_oos.close();
                DataOutputStream n_dos = new DataOutputStream(new FileOutputStream(noteFilename));
                aNote.writeToStream(n_dos);
                n_dos.close();
            }
        } catch( ClassNotFoundException | IOException | ClassCastException e ) {
            Log.d(TAG, "File saving failed!");
            e.printStackTrace();
        }
    }
}
