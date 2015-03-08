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
 * Created by Ling Jin on 05/03/2015.
 */
public class AWSService extends IntentService {
    private static final String BUCKET_NAME = "ca.uottawa.ljin027.mappad";
    private static final String KEY_NAME = "notes";
    private static final String TAG = "<<<<< AWS Service >>>>>";
    private static final String ACCESS_KEY = "AKIAJWAGGRMXROXFWDEQ";
    private static final String SECRET_ACCESS_KEY = "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";


    private TransferManager mTransferManager = null;
    private IntentService mContext = null;

    public AWSService() {
        super(TAG);
        mContext = this;
    }

    public void onCreate() {
        super.onCreate();
        mTransferManager = new TransferManager(new BasicAWSCredentials(ACCESS_KEY, SECRET_ACCESS_KEY));
    }

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

    private void sendResult(int result) {
        Intent postAWSIntent = new Intent(AWSManager.INTENT_PROCESS_RESULT);
        postAWSIntent.putExtra(AWSManager.EXTRA_AWS_RESULT, result);
        mContext.sendBroadcast(postAWSIntent);
    }
}
