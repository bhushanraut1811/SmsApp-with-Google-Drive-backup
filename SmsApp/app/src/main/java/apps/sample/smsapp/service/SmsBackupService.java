package apps.sample.smsapp.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.MetadataChangeSet;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import apps.sample.smsapp.R;

/**
 * SmsBackupService takes backup of all the Sms and saves them to Google Drive
 */
public class SmsBackupService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {


    private static final String TAG = SmsBackupService.class.getSimpleName();
    private GoogleApiClient mGoogleApiClient;
    private NotificationCompat.Builder mBuilder;
    private NotificationManager mNotificationManager;
    private static final int NOTIFICATION_ID = 1000;

    //writes to a data to a file
    final private ResultCallback<DriveApi.DriveContentsResult> driveContentsCallback = new
            ResultCallback<DriveApi.DriveContentsResult>() {
                @Override
                public void onResult(DriveApi.DriveContentsResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Log.d(TAG, "Error while trying to create new file contents");
                        return;
                    }

                    final DriveContents driveContents = result.getDriveContents();
                    // Perform I/O off the UI thread.
                    new Thread() {
                        @Override
                        public void run() {
                            // write content to DriveContents
                            OutputStream outputStream = driveContents.getOutputStream();
                            Writer writer = new OutputStreamWriter(outputStream);
                            writeSmsData(writer);

                            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                    .setTitle("Sms backUp File")
                                    .setMimeType("text/plain")
                                    .setStarred(true).build();

                            // create a file on root folder in drive
                            Drive.DriveApi.getRootFolder(mGoogleApiClient)
                                    .createFile(mGoogleApiClient, changeSet, driveContents)
                                    .setResultCallback(fileCallback);
                        }

                        /** Writes Sms data to Writer Object
                         * @param writer Writer object
                         */
                        private void writeSmsData(Writer writer) {
                            Uri uri = Uri.parse("content://sms");
                            Cursor cursor = getContentResolver().query(
                                    uri,
                                    new String[]{"_id", "thread_id", " address", "date", "body", "type"},
                                    null, null, null);
                            String[] columns = new String[]{"address", "thread_id", "date",
                                    "body", "type"};
                            if (cursor != null) {
                                cursor.moveToFirst();
                                try {
                                    while (cursor.moveToNext()) {
                                        String address, date = null, msg = null, type = null, threadId = null;

                                        threadId = cursor.getString(cursor.getColumnIndex(columns[1]));
                                        type = cursor.getString(cursor.getColumnIndex(columns[4]));
                                        if (Integer.parseInt(type) == 1 || Integer.parseInt(type) == 2) {

                                            address = cursor.getString(cursor
                                                    .getColumnIndex(columns[0]));
                                            date = cursor.getString(cursor.getColumnIndex(columns[2]));
                                            msg = cursor.getString(cursor.getColumnIndex(columns[3]));
                                            writer.write(threadId + "," + date + "," + msg + "," + type + "," + address + "\n");
                                        }
                                    }
                                    writer.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                cursor.close();
                            }
                        }
                    }.start();

                }
            };
    //callBack to inform about Upload status
    final private ResultCallback<DriveFolder.DriveFileResult> fileCallback = new
            ResultCallback<DriveFolder.DriveFileResult>() {
                @Override
                public void onResult(DriveFolder.DriveFileResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Log.d(TAG, "Error while trying to create the file");
                        return;
                    }
                    showNotification("BackUp Successful", true, false);
                    Log.d(TAG, "Created a file with content: " + result.getDriveFile().getDriveId());
                    stopSelf();
                }
            };


    @Override
    public void onConnected(Bundle bundle) {
        //Creates connection with drive
        Log.d(TAG, "onConnected();");
        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(driveContentsCallback);
    }

    @Override
    public void onConnectionSuspended(int i) {
        //connection suspended
        showNotification("BackUp Failed", true, false);
        Log.d("TAG", "onConnectedSuspended();");
        stopSelf();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //connection failed
        showNotification("BackUp Failed", true, false);
        Log.d(TAG, "onConnectedFailed();" + connectionResult.getErrorMessage());
        stopSelf();
    }

    @Override
    public void onDestroy() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onDestroy();

    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        initNotify();
        if (mGoogleApiClient == null) {

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .useDefaultAccount()
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        Log.d(TAG, "mGoogleApiClient.connect();");
        showNotification("Back Up in Progress", false, true);
        mGoogleApiClient.connect();
    }

    /**
     * preparing notifications objects
     */
    private void initNotify() {
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(this);
    }

    /**
     * shows notifications regarding status of the backup process
     *
     * @param message      message to be displayed
     * @param isAutoCancel to set autoCancel set or not
     * @param isOngoing    to set onGoing set or not
     */
    private void showNotification(String message, boolean isAutoCancel, boolean isOngoing) {
        mBuilder.setContentTitle(getString(R.string.app_name));
        mBuilder.setContentText(message);
        mBuilder.setAutoCancel(isAutoCancel);
        mBuilder.setOngoing(isOngoing);
        mBuilder.setSmallIcon(R.mipmap.ic_launcher);

        Intent intent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);
        mBuilder.setContentIntent(pendingIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
