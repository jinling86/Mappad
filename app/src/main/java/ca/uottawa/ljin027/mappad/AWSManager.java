package ca.uottawa.ljin027.mappad;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

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
    public static final int AWS_LISTED = 6;
    public static final int AWS_LIST_FAILED = 7;
    public static final int AWS_FAILED = 8;
    public static final int AWS_RETRY_TIMEOUT = 10; // second
    public static final String INTENT_UPLOAD = "upload";
    public static final String INTENT_DOWNLOAD = "download";
    public static final String INTENT_LIST = "list";
    public static final String INTENT_DELETE = "delete";
    public static final String INTENT_PROCESS_RESULT = "process_aws_result";

    /**
     * Handle of the main activity
     * All the activities use the main activity context to invoke the intent, so the result will
     * sent back to the main activity
     */
    private static Context MainActivityContext;
    private static String TAG = "<<<<< AWS Manager >>>>>";

    /**
     * Initialize the context, called by the main activity (List Activity)
     * @param context this pointer of the main activity
     */
    public static void setContext(Context context) {
        if(context instanceof Activity_List) {
            MainActivityContext = context;
        }
    }

    public static void list(String filename) {
        getImmediateService(INTENT_LIST, filename);
    }
    public static void listLater(String filename) {
        getLatentService(INTENT_LIST, filename);
    }

    public static void upload(String filename) {
        getImmediateService(INTENT_UPLOAD, filename);
    }
    public static void uploadLater(String filename) {
        getLatentService(INTENT_UPLOAD, filename);
    }

    public static void download(String filename) {
        getImmediateService(INTENT_DOWNLOAD, filename);
    }
    public static void downloadLater(String filename) {
        getLatentService(INTENT_DOWNLOAD, filename);
    }

    public static void delete(String filename) {
        getImmediateService(INTENT_DELETE, filename);
    }
    public static void deleteLater(String filename) {
        getLatentService(INTENT_DELETE, filename);
    }

    public static void getImmediateService(String action, String filename) {
        if(MainActivityContext != null) {
            Log.d(TAG, "Immediately " + action + " " + filename);
            Intent intent = new Intent(MainActivityContext, AWSService.class);
            intent.setAction(action);
            intent.putExtra(EXTRA_INTERNAL_FILENAME, filename);
            MainActivityContext.startService(intent);
        }
    }

    public static void getLatentService(String action, String filename) {

        if(MainActivityContext != null) {
            Log.d(TAG, "After " + AWS_RETRY_TIMEOUT + " seconds " + action + " " + filename);
            AlarmManager alarmMgr = (AlarmManager) MainActivityContext.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(MainActivityContext, AWSService.class);
            intent.setAction(action);
            intent.putExtra(EXTRA_INTERNAL_FILENAME, filename);
            PendingIntent alarmIntent = PendingIntent.getService(MainActivityContext, 0, intent, PendingIntent.FLAG_ONE_SHOT);
            alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + AWS_RETRY_TIMEOUT * 1000, alarmIntent);
        }
    }
}