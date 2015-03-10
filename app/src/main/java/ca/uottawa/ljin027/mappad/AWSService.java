package ca.uottawa.ljin027.mappad;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobileconnectors.s3.transfermanager.Download;
import com.amazonaws.mobileconnectors.s3.transfermanager.TransferManager;
import com.amazonaws.mobileconnectors.s3.transfermanager.Upload;
import com.amazonaws.services.s3.model.ProgressEvent;
import com.amazonaws.services.s3.model.ProgressListener;

import java.io.File;

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
    private static final String KEY_NAME = "notes";
    private static final String ACCESS_KEY = "AKIAJWAGGRMXROXFWDEQ";
    private static final String SECRET_ACCESS_KEY = "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";
    /**
     * String constant for debugging
     */
    private static final String TAG = "<<<<< AWS Service >>>>>";
    /**
     * Reference of current TransferManager and current object, used in inner classes
     */
    private TransferManager mTransferManager = null;
    private IntentService mContext = null;

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
        mTransferManager = new TransferManager(new BasicAWSCredentials(ACCESS_KEY, SECRET_ACCESS_KEY));
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
            }
        }
    }

    /**
     * Download the note file from the AWS S3 Server
     * Send an Intent back to Activities as the result of the downloading
     * The download failures can be captured by catching the exceptions
     * @param file_name name of the file to be saved
     */
    private void download(String file_name) {
        try {
            Download mDownload = mTransferManager.download(BUCKET_NAME, KEY_NAME, new File(file_name));
            mDownload.addProgressListener(new ProgressListener() {
                @Override
                public void progressChanged(ProgressEvent event) {
                    if (event.getEventCode() == ProgressEvent.COMPLETED_EVENT_CODE) {
                        Log.d(TAG, "Downloaded!");
                        sendResult(AWSManager.AWS_DOWNLOADED);
                    }
                }
            });
        } catch ( AmazonServiceException ase ) {
            Log.d(TAG, "Caught an AmazonServiceException, which means your request made it "
                    + "to Amazon S3, but was rejected with an error response for some reason.");
            Log.d(TAG, "Error Message:    " + ase.getMessage());
            Log.d(TAG, "HTTP Status Code: " + ase.getStatusCode());
            Log.d(TAG, "AWS Error Code:   " + ase.getErrorCode());
            Log.d(TAG, "Error Type:       " + ase.getErrorType());
            Log.d(TAG, "Request ID:       " + ase.getRequestId());
            sendResult(AWSManager.AWS_DOWNLOAD_FAILED);
        } catch ( AmazonClientException ace ) {
            Log.d(TAG, "Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with S3, "
                    + "such as not being able to access the network.");
            Log.d(TAG, "Error Message: " + ace.getMessage());
            sendResult(AWSManager.AWS_DOWNLOAD_FAILED);
        }
    }

    /**
     * Upload the note file to the AWS S3 Server
     * Send an Intent back to Activities as the result of the uploading
     * The upload failures can be captured by catching the exceptions
     * @param file_name name of the file to be read from
     */
    private void upload(String file_name) {
        try {
            Upload mUpload = mTransferManager.upload(BUCKET_NAME, KEY_NAME, new File(file_name));
            mUpload.addProgressListener(new ProgressListener() {
                @Override
                public void progressChanged(ProgressEvent event) {
                    if (event.getEventCode() == ProgressEvent.COMPLETED_EVENT_CODE) {
                        Log.d(TAG, "Uploaded!");
                        sendResult(AWSManager.AWS_UPLOADED);
                    }
                }
            });
        } catch ( AmazonServiceException ase ) {
            Log.d(TAG, "Caught an AmazonServiceException, which means your request made it "
                    + "to Amazon S3, but was rejected with an error response for some reason.");
            Log.d(TAG, "Error Message:    " + ase.getMessage());
            Log.d(TAG, "HTTP Status Code: " + ase.getStatusCode());
            Log.d(TAG, "AWS Error Code:   " + ase.getErrorCode());
            Log.d(TAG, "Error Type:       " + ase.getErrorType());
            Log.d(TAG, "Request ID:       " + ase.getRequestId());
            sendResult(AWSManager.AWS_UPLOAD_FAILED);
        } catch ( AmazonClientException ace ) {
            Log.d(TAG, "Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with S3, "
                    + "such as not being able to access the network.");
            Log.d(TAG, "Error Message: " + ace.getMessage());
            sendResult(AWSManager.AWS_UPLOAD_FAILED);
        }
    }

    /**
     * A helper method for constructing Intent
     * The Intent will be broadcast to system, the List Activity will listen and process it
     * @param result result code of the uploading/downloading operation
     */
    private void sendResult(int result) {
        Intent postAWSIntent = new Intent(AWSManager.INTENT_PROCESS_RESULT);
        postAWSIntent.putExtra(AWSManager.EXTRA_AWS_RESULT, result);
        mContext.sendBroadcast(postAWSIntent);
    }
}
