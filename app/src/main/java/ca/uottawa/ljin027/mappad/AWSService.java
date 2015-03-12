package ca.uottawa.ljin027.mappad;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobileconnectors.s3.transfermanager.Download;
import com.amazonaws.mobileconnectors.s3.transfermanager.TransferManager;
import com.amazonaws.mobileconnectors.s3.transfermanager.Upload;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.ProgressEvent;
import com.amazonaws.services.s3.model.ProgressListener;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.IOUtils;

import java.io.File;
import java.io.FileOutputStream;

/**
 * This class is implemented for CSI5175 Assignment 2.
 * This class implements an Android Service that manages the communication with Amazon Web Services
 * Simple Storage Service service. The service runs in the background of the applications. The
 * server cannot be communicated directed in Activity unless use an AsyncTask. Here, the
 * TransferManager is used to manage the connection. The TransferManager is also suitable for
 * transmitting large files.
 * The file uploading or downloading is triggered by Intent of the Activities, the transmission
 * results are sent back to the activities by broadcast the result Intent.
 *
 * PLEASE KEY THE ACCESS KEY AND SECRET ACCESS KEY SAFE!
 *
 * Here is some how-to documents:
 * SDK Set-up: http://docs.aws.amazon.com/mobile/sdkforandroid/developerguide/setup.html
 * SDK Developer guide: http://docs.aws.amazon.com/AmazonS3/latest/dev/Welcome.html
 * IAM User Management: http://docs.aws.amazon.com/IAM/latest/UserGuide/IAM_Introduction.html
 * An example of using AWS S3 Service in Java: https://github.com/aws/aws-sdk-java
 * An example of using AWS S3 Service for developing Android: https://github.com/awslabs/aws-sdk-android-samples
 * The latter example uses the IntentService class, but is more complicated.
 *
 * @author      Ling Jin
 * @version     1.0
 * @since       05/03/2015
 */
public class AWSService extends IntentService {
    /**
     * Bucket (Server) name, key (file) name and access keys for the file save in AWS S3 Server
     */
    private static final String BUCKET_NAME = "ca.uottawa.ljin027.mappad";
    private static final String ACCESS_KEY = "AKIAJWAGGRMXROXFWDEQ";
    private static final String SECRET_ACCESS_KEY = "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";
    /**
     * String constant for debugging
     */
    private static final String TAG = "<<<<< AWS Service >>>>>";
    /**
     * Reference of current TransferManager and current object, used in inner classes
     */
    private NetworkInfo mNetworkInfo = null;
    private IntentService mContext = null;
    private AmazonS3Client mS3Client = null;

    /**
     * Construct the Android Service class
     */
    public AWSService() {
        super(TAG);
        mContext = this;
    }

    /**
     * Create a TransferManager using AWS Keys
     */
    public void onCreate() {
        super.onCreate();
        mS3Client = new AmazonS3Client(new BasicAWSCredentials(ACCESS_KEY, SECRET_ACCESS_KEY));
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        mNetworkInfo = cm.getActiveNetworkInfo();
        Log.d(TAG, "Created");
    }

    /**
     * Receive and process Intents
     * @param intent Intent from Activities
     */
    @Override
    public void onHandleIntent(Intent intent) {
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(AWSManager.INTENT_DOWNLOAD) &&
                    intent.getStringExtra(AWSManager.EXTRA_INTERNAL_FILENAME) != null) {
                download(intent.getStringExtra(AWSManager.EXTRA_INTERNAL_FILENAME));
            } else if (intent.getAction().equals(AWSManager.INTENT_UPLOAD) &&
                    intent.getStringExtra(AWSManager.EXTRA_INTERNAL_FILENAME) != null) {
                upload(intent.getStringExtra(AWSManager.EXTRA_INTERNAL_FILENAME));
            } else if (intent.getAction().equals(AWSManager.INTENT_DELETE) &&
                    intent.getStringExtra(AWSManager.EXTRA_INTERNAL_FILENAME) != null) {
                delete(intent.getStringExtra(AWSManager.EXTRA_INTERNAL_FILENAME));
            }
        }
    }

    private void download(final String file_name) {
        if(!mNetworkInfo.isConnectedOrConnecting()) {
            Log.d(TAG, "No connection while downloading");
            sendResult(AWSManager.AWS_DOWNLOAD_FAILED, file_name);
            return;
        }
        try {
            String cloudFilename = NoteManager.getTmpName(file_name);
            String localFilename = NoteManager.getTmpFullName(file_name);
            mS3Client.getObject(new GetObjectRequest(BUCKET_NAME, cloudFilename), new File(localFilename));
            Log.d(TAG, "Download successfully");
            sendResult(AWSManager.AWS_DOWNLOADED, file_name);
        } catch ( AmazonServiceException ase ) {
            Log.d(TAG, "Caught an AmazonServiceException:");
            Log.d(TAG, "Error Message: " + ase.getMessage()
                    + "HTTP Status Code: " + ase.getStatusCode()
                    + "Error Type: " + ase.getErrorType());
            if(ase.getStatusCode() == 404)
                sendResult(AWSManager.AWS_DOWNLOAD_NO_FILE, file_name);
            else
                sendResult(AWSManager.AWS_DOWNLOAD_FAILED, file_name);
        } catch ( AmazonClientException ace ) {
            Log.d(TAG, "Caught an AmazonClientException:");
            Log.d(TAG, "Error Message: " + ace.getMessage());
            sendResult(AWSManager.AWS_DOWNLOAD_FAILED, file_name);
        }
    }

    private void upload(final String file_name) {
        if(!mNetworkInfo.isConnectedOrConnecting()) {
            sendResult(AWSManager.AWS_UPLOAD_FAILED, file_name);
            Log.d(TAG, "No connection while uploading");
            return;
        }
        try {
            String cloudFilename = NoteManager.getTmpName(file_name);
            String localFilename = NoteManager.getFullName(file_name);
            mS3Client.putObject(new PutObjectRequest(BUCKET_NAME, cloudFilename, new File(localFilename)));
            Log.d(TAG, "Upload successfully");
            sendResult(AWSManager.AWS_UPLOADED, file_name);
        } catch ( AmazonServiceException ase ) {
            Log.d(TAG, "Caught an AmazonServiceException:");
            Log.d(TAG, "Error Message: " + ase.getMessage()
                    + "HTTP Status Code: " + ase.getStatusCode()
                    + "Error Type: " + ase.getErrorType());
            sendResult(AWSManager.AWS_UPLOAD_FAILED, file_name);
        } catch ( AmazonClientException ace ) {
            Log.d(TAG, "Caught an AmazonClientException:");
            Log.d(TAG, "Error Message: " + ace.getMessage());
            sendResult(AWSManager.AWS_UPLOAD_FAILED, file_name);
        }
    }

    private void delete(final String file_name) {
        if(!mNetworkInfo.isConnectedOrConnecting()) {
            sendResult(AWSManager.AWS_DELETE_FAILED, file_name);
            Log.d(TAG, "No connection while deleting");
            Log.d(TAG, "Delete successfully");
            return;
        }
        try {
            String cloudFilename = NoteManager.getTmpName(file_name);
            mS3Client.deleteObject(BUCKET_NAME, cloudFilename);
            sendResult(AWSManager.AWS_DELETED, file_name);
        } catch ( AmazonServiceException ase ) {
            Log.d(TAG, "Caught an AmazonServiceException:");
            Log.d(TAG, "Error Message: " + ase.getMessage()
                    + "HTTP Status Code: " + ase.getStatusCode()
                    + "Error Type: " + ase.getErrorType());
            sendResult(AWSManager.AWS_DELETE_FAILED, file_name);
        } catch ( AmazonClientException ace ) {
            Log.d(TAG, "Caught an AmazonClientException:");
            Log.d(TAG, "Error Message: " + ace.getMessage());
            sendResult(AWSManager.AWS_DELETE_FAILED, file_name);
        }
    }

    /**
     * A helper method for constructing Intent
     * The Intent will be broadcast to system, the List Activity will listen and process it
     * @param result result code of the uploading/downloading operation
     */
    private void sendResult(int result, String filename) {
        Intent postAWSIntent = new Intent(AWSManager.INTENT_PROCESS_RESULT);
        postAWSIntent.putExtra(AWSManager.EXTRA_AWS_RESULT, result);
        postAWSIntent.putExtra(AWSManager.EXTRA_INTERNAL_FILENAME, filename);
        mContext.sendBroadcast(postAWSIntent);
    }

}
