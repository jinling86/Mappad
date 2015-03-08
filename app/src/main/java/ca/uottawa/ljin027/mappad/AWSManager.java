package ca.uottawa.ljin027.mappad;

import android.content.Context;
import android.content.Intent;

/**
 * Created by Ling Jin on 05/03/2015.
 */
public class AWSManager {

    public static final String EXTRA_INTERNAL_FILENAME = "file_name";
    public static final String EXTRA_AWS_RESULT = "action";
    public static final int AWS_UPLOADED = 0;
    public static final int AWS_UPLOAD_FAILED = 1;
    public static final int AWS_DOWNLOADED = 2;
    public static final int AWS_DOWNLOAD_FAILED = 3;
    public static final int AWS_FAILED = 4;
    public static final String INTENT_UPLOAD = "upload";
    public static final String INTENT_DOWNLOAD = "download";
    public static final String INTENT_PROCESS_RESULT = "process_aws_result";

    private static Context MainActivityContext;

    public static void setContext(Context context) {
        if(context instanceof Activity_List) {
            MainActivityContext = context;
        }
    }

    public static void upload() {
        if(MainActivityContext != null) {
            Intent intent = new Intent(MainActivityContext, AWSService.class);
            intent.setAction(INTENT_UPLOAD);
            intent.putExtra(EXTRA_INTERNAL_FILENAME, NoteManager.EXT_FILE_NAME);
            MainActivityContext.startService(intent);
        }
    }

    public static void download() {
        if(MainActivityContext != null) {
            Intent intent = new Intent(MainActivityContext, AWSService.class);
            intent.setAction(INTENT_DOWNLOAD);
            intent.putExtra(EXTRA_INTERNAL_FILENAME, NoteManager.EXT_TMP_FILE_NAME);
            MainActivityContext.startService(intent);
        }
    }

}