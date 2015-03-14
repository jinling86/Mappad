package ca.uottawa.ljin027.mappad;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

/**
 * This class is implemented for CSI5175 Assignment 2.
 * This class implements an Android Service that manages the communication with Amazon Web Services
 * Simple Storage Service service. The service runs in the background of the applications. The
 * server cannot be communicated directed in Activity unless use an AsyncTask. All the
 * communication is triggered and responded using Intents.
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
     * Bucket/Server name and access keys for the file save in AWS S3 Server. All the applications
     * share the same configuration. This can be improved by introduing AWS Cognito.
     */
    private static final String BUCKET_NAME = "ca.uottawa.ljin027.mappad";
    private static final String ACCESS_KEY = "AKIAJWAGGRMXROXFWDEQ";
    private static final String SECRET_ACCESS_KEY = "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";

    /**
     * String constant for debugging
     */
    private static final String TAG = "<<<<< AWS Service >>>>>";

    /**
     * Reference of AWS S3 client and network connection indicator
     */
    private NetworkInfo mNetworkInfo = null;
    private AmazonS3Client mS3Client = null;

    /**
     * Constructs the Android Service class, nothing to do
     */
    public AWSService() {
        super(TAG);
    }

    /**
     * Creates a AWS S3 client using AWS Keys, initializes the network connection indicator.
     */
    public void onCreate() {
        super.onCreate();
        mS3Client = new AmazonS3Client(new BasicAWSCredentials(ACCESS_KEY, SECRET_ACCESS_KEY));
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        mNetworkInfo = cm.getActiveNetworkInfo();
        Log.d(TAG, "Created");
    }

    /**
     * Receives and dispatches Intents.
     * @param intent Intent from Activity List
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
            } else if (intent.getAction().equals(AWSManager.INTENT_LIST) &&
                    intent.getStringExtra(AWSManager.EXTRA_INTERNAL_FILENAME) != null) {
                list(intent.getStringExtra(AWSManager.EXTRA_INTERNAL_FILENAME));
            }
        }
    }

    /**
     * Downloads a file from AWS S3 server. Not try to download the file if the network does not
     * work. The required file should always exists in the server. It should be fetched if the
     * network works. The downloaded file is added a ".tmp" suffix.
     * @param file_name the name of the downloading file
     */
    private void download(String file_name) {
        if(mNetworkInfo == null || !mNetworkInfo.isConnectedOrConnecting()) {
            Log.d(TAG, "No connection while downloading");
            sendResult(AWSManager.AWS_DOWNLOAD_FAILED, file_name);
            return;
        }
        try {
            String localFilename = NoteManager.getTmpFullName(file_name);
            mS3Client.getObject(new GetObjectRequest(BUCKET_NAME, file_name), new File(localFilename));
            Log.d(TAG, "Download successfully");
            sendResult(AWSManager.AWS_DOWNLOADED, file_name);
        } catch ( AmazonServiceException ase ) {
            Log.d(TAG, "Caught an AmazonServiceException during downloading:");
            Log.d(TAG, "Error Message: " + ase.getMessage()
                    + "HTTP Status Code: " + ase.getStatusCode()
                    + "Error Type: " + ase.getErrorType());
            sendResult(AWSManager.AWS_DOWNLOAD_FAILED, file_name);
        } catch ( AmazonClientException ace ) {
            Log.d(TAG, "Caught an AmazonClientException during downloading:");
            Log.d(TAG, "Error Message: " + ace.getMessage());
            sendResult(AWSManager.AWS_DOWNLOAD_FAILED, file_name);
        }
    }

    /**
     * Uploads a file from AWS S3 server. Not try to upload the file if the network does not work.
     * @param file_name the name of the uploading file
     */
    private void upload(String file_name) {
        if(mNetworkInfo == null || !mNetworkInfo.isConnectedOrConnecting()) {
            sendResult(AWSManager.AWS_UPLOAD_FAILED, file_name);
            Log.d(TAG, "No connection while uploading");
            return;
        }
        try {
            String localFilename = NoteManager.getFullName(file_name);
            mS3Client.putObject(new PutObjectRequest(BUCKET_NAME, file_name, new File(localFilename)));
            Log.d(TAG, "Upload successfully " + file_name);
            sendResult(AWSManager.AWS_UPLOADED, file_name);
        } catch ( AmazonServiceException ase ) {
            Log.d(TAG, "Caught an AmazonServiceException during uploading:");
            Log.d(TAG, "Error Message: " + ase.getMessage()
                    + "HTTP Status Code: " + ase.getStatusCode()
                    + "Error Type: " + ase.getErrorType());
            sendResult(AWSManager.AWS_UPLOAD_FAILED, file_name);
        } catch ( AmazonClientException ace ) {
            Log.d(TAG, "Caught an AmazonClientException during uploading:");
            Log.d(TAG, "Error Message: " + ace.getMessage());
            sendResult(AWSManager.AWS_UPLOAD_FAILED, file_name);
        }
    }

    /**
     * Deletes a file from AWS S3 server. Not try to delete the file if the network does not work.
     * Typically, AWS S3 server always returns success for deletion.
     * @param file_name the name of the deleting file
     */
    private void delete(String file_name) {
        if(mNetworkInfo == null || !mNetworkInfo.isConnectedOrConnecting()) {
            sendResult(AWSManager.AWS_DELETE_FAILED, file_name);
            Log.d(TAG, "No connection while deleting");
            return;
        }
        try {
            mS3Client.deleteObject(BUCKET_NAME, file_name);
            sendResult(AWSManager.AWS_DELETED, file_name);
            Log.d(TAG, "Delete successfully" + file_name);
        } catch ( AmazonServiceException ase ) {
            Log.d(TAG, "Caught an AmazonServiceException during deleting:");
            Log.d(TAG, "Error Message: " + ase.getMessage()
                    + "HTTP Status Code: " + ase.getStatusCode()
                    + "Error Type: " + ase.getErrorType());
            sendResult(AWSManager.AWS_DELETE_FAILED, file_name);
        } catch ( AmazonClientException ace ) {
            Log.d(TAG, "Caught an AmazonClientException during deleting:");
            Log.d(TAG, "Error Message: " + ace.getMessage());
            sendResult(AWSManager.AWS_DELETE_FAILED, file_name);
        }
    }

    /**
     * Reads the file names on the AWS S3 server and uses the names to build a temporary index file,
     * which will be used for determining which files need to be uploaded and downloaded.
     * @param file_name the name of the temporary index file
     */
    private void list(String file_name) {
        if(mNetworkInfo == null || !mNetworkInfo.isConnectedOrConnecting()) {
            sendResult(AWSManager.AWS_DELETE_FAILED, file_name);
            Log.d(TAG, "No connection while deleting");
            return;
        }
        try {
            ArrayList<NoteIndex> noteIndex = new ArrayList<NoteIndex> ();
            ObjectListing objectListing = mS3Client.listObjects(new ListObjectsRequest().withBucketName(BUCKET_NAME));
            for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                // Constructs the note index from the names of the files on the server
                // The names should be in a chronological order and the most newly created file is
                // the last file
                NoteIndex anIndex = new NoteIndex();
                anIndex.mFileName = objectSummary.getKey();
                anIndex.mCreatedTime = NoteIndex.getTimeFromName(anIndex.mFileName);
                anIndex.mModifiedTime = objectSummary.getLastModified().getTime();
                anIndex.mModified = false;
                anIndex.mDeleted = false;
                anIndex.mSynchronized = true;
                noteIndex.add(anIndex);
            }
            ObjectOutputStream i_oos = new ObjectOutputStream(new FileOutputStream(NoteManager.getTmpFullName(file_name)));
            i_oos.writeObject(noteIndex);
            i_oos.close();
            Log.d(TAG, "Note index created");
            sendResult(AWSManager.AWS_LISTED, file_name);
        } catch ( AmazonServiceException ase ) {
            Log.d(TAG, "Caught an AmazonServiceException during reading list:");
            Log.d(TAG, "Error Message: " + ase.getMessage()
                    + "HTTP Status Code: " + ase.getStatusCode()
                    + "Error Type: " + ase.getErrorType());
            sendResult(AWSManager.AWS_LIST_FAILED, file_name);
        } catch ( AmazonClientException ace ) {
            Log.d(TAG, "Caught an AmazonClientException:");
            Log.d(TAG, "Error Message: " + ace.getMessage());
            sendResult(AWSManager.AWS_LIST_FAILED, file_name);
        } catch( IOException | NoSuchFieldException e ) {
            e.printStackTrace();
            Log.d(TAG, "Note index creation failed!");
        }
    }

    /**
     * Constructs an Intent which will be broadcast back to the List Activity
     * @param result result code of the operation
     */
    private void sendResult(int result, String filename) {
        Intent postAWSIntent = new Intent(AWSManager.INTENT_PROCESS_RESULT);
        postAWSIntent.putExtra(AWSManager.EXTRA_AWS_RESULT, result);
        postAWSIntent.putExtra(AWSManager.EXTRA_INTERNAL_FILENAME, filename);
        sendBroadcast(postAWSIntent);
    }

}
