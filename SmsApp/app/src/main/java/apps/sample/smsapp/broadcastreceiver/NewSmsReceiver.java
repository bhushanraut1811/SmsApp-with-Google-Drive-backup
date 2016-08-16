package apps.sample.smsapp.broadcastreceiver;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import apps.sample.smsapp.R;
import apps.sample.smsapp.ui.ConversationActivity;
import apps.sample.smsapp.util.CommonUtils;
import apps.sample.smsapp.util.Constants;

/**
 * NewSmsReceiver invokes as soon as new message arrives and shows notification
 */
public class NewSmsReceiver extends BroadcastReceiver {

    private static final String TAG = NewSmsReceiver.class.getSimpleName();
    private static final int NOTIFY_ID = 100;
    static final String ACTION = "android.provider.Telephony.SMS_RECEIVED";

    @Override
    public void onReceive(Context context, Intent intent) {
        //check intent once before doing operation
        if (ACTION.equals(intent.getAction())) {
            getThreadIdFromCP(context);
        }
    }

    /**
     * Extracts the thread Id from sms Content provider and show up notifications
     *
     * @param context app context
     */
    private void getThreadIdFromCP(Context context) {
        String[] columns = new String[]{"address", "thread_id", "date",
                "body", "type"};

        Uri uri = Uri.parse("content://sms");
        Cursor cursor = context.getContentResolver().query(
                uri,
                new String[]{"_id", "thread_id", "address", "date", "body",
                        "type"}, null, null, "date DESC" + " LIMIT 1");
        String threadId = null, msg = null, displayName = null, address = null;
        if (cursor != null) {
            cursor.moveToFirst();
            threadId = cursor.getString(cursor.getColumnIndex("thread_id"));
            msg = cursor.getString(cursor.getColumnIndex(columns[3]));
            address = cursor.getString(cursor
                    .getColumnIndex(columns[0]));

            if (address.length() > 0) {
                String contactData = CommonUtils.getContactByNumber(address, context);
                if (contactData != null) {
                    displayName = contactData;
                } else {
                    displayName = address;
                }
            } else {
                address = null;
            }
            cursor.close();
        }
        showNotification(context, msg, displayName, threadId);

    }

    /**
     * Show notification with messages details
     *
     * @param context      app context
     * @param message      message to be show in notification
     * @param senderNumber contact information
     * @param threadId     conversation thread Id
     */
    private void showNotification(Context context, String message, String senderNumber, String threadId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        builder.setContentTitle(senderNumber);
        builder.setContentText(message);
        builder.setAutoCancel(true);
        builder.setSmallIcon(R.mipmap.ic_launcher);


        Intent intent = new Intent(context, ConversationActivity.class);
        intent.putExtra(Constants.THREAD_ID, threadId);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, 0);
        builder.setContentIntent(pendingIntent);
        notificationManager.notify(NOTIFY_ID, builder.build());
    }
}
