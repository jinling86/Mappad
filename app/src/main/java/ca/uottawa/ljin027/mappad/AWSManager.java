package ca.uottawa.ljin027.mappad;

import android.content.Context;
import android.content.Intent;

/**
 * This class is implement for CSI5175 Assignment 2.
 * This class provides interface between AWS S3 Manager and the Mappad Application.
 * This class defines the name of Android Intents and Bundle Extras.
 * This class defines shortcut methods for using the AWSService.
 * See S3_TransferManager in https://github.com/awslabs/aws-sdk-android-samples
 *
 * @author      Ling Jin
 * @version     1.0
 * @since       05/03/2015
 */
public class AWSManager {
    /**
     * Identifiers and constants for Android Intents
     */
    public static final String EXTRA_INTERNAL_FILENAME = "file_name";
    public static final String EXTRA_AWS_RESULT = "action";
    public static final int AWS_UPLOADED = 0;
    public static final int AWS_UPLOAD_FAILED = 1;
    public static final int AWS_DOWNLOADED = 2;
    public static final int AWS_DOWNLOAD_FAILED = 3;
    public static final int AWS_DELETED = 4;
    public static final int AWS_DELETE_FAILED = 5;
    public static final int AWS_FAILED = 6;
    public static final String INTENT_UPLOAD = "upload";
    public static final String INTENT_DOWNLOAD = "download";
    public static final String INTENT_DELETE = "delete";
    public static final String INTENT_PROCESS_RESULT = "process_aws_result";

    /**
     * Handle of the main activity
     * All the activities use the main activity context to invoke the intent, so the result will
     * sent back to the main activity
     */
    private static Context MainActivityContext;

    /**
     * Initialize the context, called by the main activity (List Activity)
     * @param context this pointer of the main activity
     */
    public static void setContext(Context context) {
        if(context instanceof Activity_List) {
            MainActivityContext = context;
        }
    }

    /**
     * Send intent to the AWS service, uploads notes
     * The intent contains an extra, the name of the file that is used to store the notes
     */
    public static void upload(String filename) {
        if(MainActivityContext != null) {
            Intent intent = new Intent(MainActivityContext, AWSService.class);
            intent.setAction(INTENT_UPLOAD);
            intent.putExtra(EXTRA_INTERNAL_FILENAME, filename);
            MainActivityContext.startService(intent);
        }
    }

    /**
     * Send intent to the AWS service, downloads notes
     * The intent contain an extra, the name of the file that will be used to store the file from
     * S3 Server
     */
    public static void download(String filename) {
        if(MainActivityContext != null) {
            Intent intent = new Intent(MainActivityContext, AWSService.class);
            intent.setAction(INTENT_DOWNLOAD);
            intent.putExtra(EXTRA_INTERNAL_FILENAME, filename);
            MainActivityContext.startService(intent);
        }
    }

    public static void delete(String filename) {
        if(MainActivityContext != null) {
            Intent intent = new Intent(MainActivityContext, AWSService.class);
            intent.setAction(INTENT_DELETE);
            intent.putExtra(EXTRA_INTERNAL_FILENAME, filename);
            MainActivityContext.startService(intent);
        }
    }

}