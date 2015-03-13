package ca.uottawa.ljin027.mappad;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;

/**
 * This class is implement for CSI5175 Assignment 2.
 * This class defines the items of the notes.
 * A note consists of a title, a content and the location information provided by the author.
 * The content field is invisible in the user interface, it remains in the code so that we have
 * easily switch back to the notepad that does not store location information. The items should
 * implements Serializable Interface, so it can be automatically written to an output stream. In
 * order to use the output stream, this class cannot be declared as an inner class.
 * This class is put in an individual file so as to be displayed in a good manner.
 *
 * @author      Ling Jin
 * @version     1.0
 * @since       05/03/2015
 */
public class NoteItem {
    /**
     * Note Items
     */
    public String mTitle;
    public String mContent;
    public Double mLongitude;
    public Double mLatitude;
    public String mNoteFilename;
    public String mCreatedTime;
    public String mModifiedTime;

    private static String FILE_NAME_PREFIX     = "File name     : ";
    private static String CREATED_TIME_PREFIX  = "Created Time  : ";
    private static String MODIFIED_TIME_PREFIX = "Modified Time : ";
    private static String LABEL_PREFIX         = "Label         : ";
    private static String CONTENT_PREFIX       = "Content       : ";
    private static String LONGITUDE_PREFIX     = "Longitude     : ";
    private static String LATITUDE_PREFIX      = "Latitude      : ";

    public void writeToStream(DataOutputStream outputStream) {
        PrintWriter writer = new PrintWriter(outputStream);
        writer.print(FILE_NAME_PREFIX       + mNoteFilename + "\n");
        writer.print(CREATED_TIME_PREFIX    + mCreatedTime  + "\n");
        writer.print(MODIFIED_TIME_PREFIX   + mModifiedTime + "\n");
        writer.print(LABEL_PREFIX           + mTitle        + "\n");
        writer.print(CONTENT_PREFIX         + mContent      + "\n");
        writer.print(LONGITUDE_PREFIX       + mLongitude    + "\n");
        writer.print(LATITUDE_PREFIX        + mLatitude     + "\n");
        writer.close();
    }

    public void readFromStream(DataInputStream inputStream) throws IOException {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            mNoteFilename   = (String)getContent(reader.readLine());
            mCreatedTime    = (String)getContent(reader.readLine());
            mModifiedTime   = (String)getContent(reader.readLine());
            mTitle          = (String)getContent(reader.readLine());
            mContent        = (String)getContent(reader.readLine());
            mLongitude      = (Double)getContent(reader.readLine());
            mLatitude       = (Double)getContent(reader.readLine());
        } catch (NoSuchFieldException | IOException e) {
            if(e instanceof IOException)
                throw new IOException("File read failure");
            else
                throw new IOException("File content error");
        }
    }

    Object getContent(String aLine) throws NoSuchFieldException {
        if(aLine.startsWith(FILE_NAME_PREFIX) && aLine.length() >= FILE_NAME_PREFIX.length()) {
            return aLine.substring(FILE_NAME_PREFIX.length());
        }
        if(aLine.startsWith(CREATED_TIME_PREFIX) && aLine.length() >= CREATED_TIME_PREFIX.length()) {
            return aLine.substring(CREATED_TIME_PREFIX.length());
        }
        if(aLine.startsWith(MODIFIED_TIME_PREFIX) && aLine.length() >= MODIFIED_TIME_PREFIX.length()) {
            return aLine.substring(MODIFIED_TIME_PREFIX.length());
        }
        if(aLine.startsWith(LABEL_PREFIX) && aLine.length() >= LABEL_PREFIX.length()) {
            return aLine.substring(LABEL_PREFIX.length());
        }
        if(aLine.startsWith(CONTENT_PREFIX) && aLine.length() >= CONTENT_PREFIX.length()) {
            return aLine.substring(CONTENT_PREFIX.length());
        }
        if(aLine.startsWith(LONGITUDE_PREFIX) && aLine.length() > LONGITUDE_PREFIX.length()) {
            return Double.valueOf(aLine.substring(LONGITUDE_PREFIX.length()));
        }
        if(aLine.startsWith(LATITUDE_PREFIX) && aLine.length() > LATITUDE_PREFIX.length()) {
            return Double.valueOf(aLine.substring(LATITUDE_PREFIX.length()));
        }
        throw new NoSuchFieldException("Failed to parse line " + aLine);
    }
}
