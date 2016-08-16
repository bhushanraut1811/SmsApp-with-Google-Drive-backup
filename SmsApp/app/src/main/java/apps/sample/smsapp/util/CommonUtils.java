package apps.sample.smsapp.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

/**
 * CommonUtils holds common methods used in project
 */
public class CommonUtils {
    public static String getContactByNumber(final String number, Context context) {
        String contactName;
        try {

            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(number));

            Cursor cur = context.getContentResolver().query(uri,
                    new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup._ID},
                    null, null, null);

            if (cur != null) {
                if (cur.moveToFirst()) {
                    int nameIdx = cur.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
                    contactName = cur.getString(nameIdx);
                    cur.close();
                    return contactName;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }
}
