package apps.sample.smsapp.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import apps.sample.smsapp.R;
import apps.sample.smsapp.util.DateUtility;

/**
 * ConversationListAdapter Populates data from database to list view in Conversation Activity
 */
public class ConversationListAdapter extends CursorAdapter {
    private LayoutInflater mLayoutInflater;
    private Context mContext;
    private Cursor mCursor;
    String[] columns = new String[]{"address", "thread_id", "date",
            "body", "type"};


    public ConversationListAdapter(Context context, Cursor c) {
        super(context, c);
        mContext = context;
        mLayoutInflater = LayoutInflater.from(context);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View v = mLayoutInflater.inflate(R.layout.item_conversation, parent, false);
        return v;
    }

    /**
     * @param v       The view in which the elements we set up here will be displayed.
     * @param context The running context where this ListView adapter will be active.
     * @param cursor  The Cursor containing the query results we will display.
     * @author will
     */

    @Override
    public void bindView(View v, Context context, Cursor cursor) {
        // new String[]{"address", "thread_id", "date", "body", "type"};

        String msg, date, address, type, displayName = "";
        address = cursor.getString(cursor.getColumnIndex(columns[0]));
        msg = cursor.getString(cursor.getColumnIndex(columns[3]));
        date = cursor.getString(cursor.getColumnIndex(columns[2]));
        type = cursor.getString(cursor.getColumnIndex(columns[4]));


        TextView senderNameTextView;
        TextView dateTextView;
        TextView msgTextView;

        senderNameTextView = (TextView) v.findViewById(R.id.tv_conv_sender);
        dateTextView = (TextView) v.findViewById(R.id.tv_conv_date);
        msgTextView = (TextView) v.findViewById(R.id.tv_conv_msg);

        if (type.equalsIgnoreCase("1")) {
            senderNameTextView.setText(R.string.message_received);
        } else if (type.equalsIgnoreCase("2")) {
            senderNameTextView.setText(R.string.message_sent);
        } else if (type.equalsIgnoreCase("3")) {
            senderNameTextView.setText(R.string.message_draft);
        }
        //senderNameTextView.setText(address);
        dateTextView.setText(DateUtility.formatDate(Long.parseLong(date)));
        msgTextView.setText(msg);
/*
        if (Integer.parseInt(type) == 1)//put view to Right Side
        {

            ((RelativeLayout.LayoutParams) msgTextView.getLayoutParams()).alignWithParent = Gravity.RIGHT;
            ((RelativeLayout.LayoutParams) dateTextView.getLayoutParams()).gravity = Gravity.RIGHT;
            ((RelativeLayout.LayoutParams) senderNameTextView.getLayoutParams()).gravity = Gravity.RIGHT;

        }

        if (Integer.parseInt(type) == 2)//put view to Left Side
        {
            ((RelativeLayout.LayoutParams) msgTextView.getLayoutParams()).gravity = Gravity.LEFT;
            ((RelativeLayout.LayoutParams) dateTextView.getLayoutParams()).gravity = Gravity.LEFT;
            ((RelativeLayout.LayoutParams) senderNameTextView.getLayoutParams()).gravity = Gravity.LEFT;

        }*/
    }
}
