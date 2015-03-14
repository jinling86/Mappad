package ca.uottawa.ljin027.mappad;

import java.io.Serializable;

/**
 * This class is implement for CSI5175 Assignment 2.
 * This class defines the index that is used to track the file names of the notes. The created time
 * is used to identify the file name and modified time is used to indicate whether it should be
 * synchronized to the AWS S3 Server.
 *
 * @author      Ling Jin
 * @version     1.0
 * @since       11/03/2015
 */
public class NoteIndex implements Serializable {
    public String mFileName;
    public Long mCreatedTime;
    public Long mModifiedTime;
    public Boolean mSynchronized;
    public Boolean mModified;
    public Boolean mDeleted;

    private static int DATE_LENGTH = 13;

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
