package ca.uottawa.ljin027.mappad;

import android.content.Context;
import android.content.Intent;

/**
 * Created by Ling Jin on 05/03/2015.
 */
public class AWSManager {

    public static void upload(Context context, String file_name) {
        Intent intent = new Intent(context, AWSService.class);
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(AWSService.FILE_NAME, file_name);
        context.startService(intent);
    }

    public static void download(Context context, String file_name) {
        Intent intent = new Intent(context, AWSService.class);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.putExtra(AWSService.FILE_NAME, file_name);
        context.startService(intent);
    }

}