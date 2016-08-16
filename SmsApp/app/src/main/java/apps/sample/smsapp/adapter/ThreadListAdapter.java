package apps.sample.smsapp.adapter;

/**
 * Created by bhushan.raut on 8/12/2016.
 */

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import apps.sample.smsapp.R;
import apps.sample.smsapp.util.CommonUtils;
import apps.sample.smsapp.util.DateUtility;


/**
 * ThreadListAdapter Class Populates data from database to list view in ThreadActivity
 */
public class ThreadListAdapter extends CursorAdapter {
    private LayoutInflater mLayoutInflater;
    private Context mContext;
    private Cursor mCursor;
    String[] columns = new String[]{"_id", "snippet", "date", "recipient_ids"};


    public ThreadListAdapter(Context context, Cursor c) {
        super(context, c);
        mContext = context;
        mLayoutInflater = LayoutInflater.from(context);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View v = mLayoutInflater.inflate(R.layout.item_thread, parent, false);
        return v;
    }

    /**
     * @param v       The view in which the elements we set up here will be displayed.
     * @param context The running context where this ListView adapter will be active.
     * @param cursor  The Cursor containing the query results we will display.
     */

    @Override
    public void bindView(View v, Context context, Cursor cursor) {

        String threadId, recipient_ids, snippet, date, displayName = "";
        String[] r_ids;
        threadId = cursor.getString(cursor.getColumnIndex(columns[0]));
        snippet = cursor.getString(cursor.getColumnIndex(columns[1]));
        recipient_ids = cursor.getString(cursor.getColumnIndex(columns[3]));
        date = cursor.getString(cursor.getColumnIndex(columns[2]));

        r_ids = recipient_ids.split(" ");
        Cursor c = null;
        for (int i = 0; i < r_ids.length; i++) {
            //finding contact address
            Uri uri = Uri.parse("content://mms-sms/canonical-address/" + r_ids[i]);
            c = mContext.getContentResolver().query(uri, new String[]{"*"}, null, null, null);
            if (c != null) {
                c.moveToFirst();
                String name = CommonUtils.getContactByNumber(c.getString(c.getColumnIndex("address")), mContext);
                if (name != null) {
                    displayName += name + ",";
                } else {
                    displayName += c.getString(c.getColumnIndex("address")) + ",";
                }
            }
        }
        if (c != null) {
            c.close();
        }

        TextView senderNameTextView;
        TextView dateTextView;
        TextView msgTextView;

        senderNameTextView = (TextView) v.findViewById(R.id.tv_sender_name);
        dateTextView = (TextView) v.findViewById(R.id.tv_date);
        msgTextView = (TextView) v.findViewById(R.id.tv_msg);

        senderNameTextView.setText(displayName.substring(0, displayName.length() - 1));
        dateTextView.setText(DateUtility.formatDate(Long.parseLong(date)));
        msgTextView.setText(snippet);

    }
}