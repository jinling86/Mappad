package ca.uottawa.ljin027.mappad;

import android.content.Context;
import android.content.Intent;

/**
 * Created by Ling Jin on 05/03/2015.
 */
public class AWSManager {

    private static Context MainActivityContext;

    public static void setContext(Context context) {
        if(context instanceof Activity_List) {
            MainActivityContext = context;
        }
    }

    public static void upload() {
        if(MainActivityContext != null) {
            Intent intent = new Intent(MainActivityContext, AWSService.class);
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(AWSService.FILE_NAME, NoteManager.EXT_FILE_NAME);
            MainActivityContext.startService(intent);
        }
    }

    public static void download() {
        if(MainActivityContext != null) {
            Intent intent = new Intent(MainActivityContext, AWSService.class);
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.putExtra(AWSService.FILE_NAME, NoteManager.EXT_TMP_FILE_NAME);
            MainActivityContext.startService(intent);
        }
    }

}