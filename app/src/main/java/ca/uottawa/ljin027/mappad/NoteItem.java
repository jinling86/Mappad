package ca.uottawa.ljin027.mappad;

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
public class NoteItem implements Serializable{
    /**
     * Note Items
     */
    public String mTitle;
    public String mContent;
    public Double mLongitude;
    public Double mLatitude;
}
