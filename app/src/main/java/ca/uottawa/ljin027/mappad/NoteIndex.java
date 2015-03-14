package ca.uottawa.ljin027.mappad;

import java.io.Serializable;

/**
 * This class is implement for CSI5175 Assignment 2.
 * This class defines the index that is used to track the states of the notes:
 *      File name:      Note file name, both in phone and on AWS S3 Server
 *      Created time:   The unique identifier of the file
 *      Modified time:  Indicates which file is newer and should be kept
 *      Synchronized:   Indicates whether the file should be uploaded to AWS S3 Server
 *      Modified:       Indicates whether the file is changed and should write to internal storage
 *      Deleted:        Indicates whether the file is deleted but the operation has not been
 *                      synchronized to AWS S3 Server
 * This class implements the Serializable interface so as to easily save it by Object read/write
 * stream.
 *
 * @author      Ling Jin
 * @version     1.0
 * @since       11/03/2015
 */
public class NoteIndex implements Serializable {
    /**
     * Index for a note file
     */
    public String mFileName;
    public Long mCreatedTime;
    public Long mModifiedTime;
    public Boolean mSynchronized;
    public Boolean mModified;
    public Boolean mDeleted;

    /**
     * The length of the Long time
     */
    private static int DATE_LENGTH = 13;

    /**
     * Deduces the created time of a note from its file name. Therefore, no files need to be
     * downloaded when the application starts.
     * @param filename the name of a note file
     * @return the created time of the note
     * @throws NoSuchFieldException throws when encounters a wrong file name
     */
    public static Long getTimeFromName(String filename) throws NoSuchFieldException {
        if(!filename.startsWith(NoteManager.NOTE_PREFIX)
            || !filename.endsWith(NoteManager.NOTE_SUFFIX)
            || filename.length() != NoteManager.NOTE_PREFIX.length() + DATE_LENGTH + NoteManager.NOTE_SUFFIX.length()) {
            throw new NoSuchFieldException("Failed to parse filename " + filename);
        } else {
            String createdTimeString = filename.substring(NoteManager.NOTE_PREFIX.length(), NoteManager.NOTE_PREFIX.length()+DATE_LENGTH);
            return Long.getLong(createdTimeString);
        }
    }
}
