package apps.sample.smsapp.ui;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import apps.sample.smsapp.R;
import apps.sample.smsapp.adapter.ConversationListAdapter;
import apps.sample.smsapp.model.IConversation;
import apps.sample.smsapp.util.CommonUtils;
import apps.sample.smsapp.util.Constants;

/**
 * ConversationActivity shows chat conversation and functionality to send messages
 */
public class ConversationActivity extends AppCompatActivity implements IConversation {

    private ListView mListView;
    private ConversationListAdapter mAdapter;
    private ImageButton mBtnSendMessage;
    private EditText mEtMessage;
    private EditText mEtContactNumber;
    private String mThreadId;
    private String mRecipients;
    private String mTitle = "";
    private String mPhoneNumber = "";
    private boolean mDeliverySuccess = false;
    private BroadcastReceiver mSendBroadcastReceiver = null;
    private BroadcastReceiver mDeleiveredBroadcastReceiver = null;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    /**
     * Initializes the Views in activity
     */
    private void initViews() {
        mListView = (ListView) findViewById(R.id.lv_chat_conversation);
        mBtnSendMessage = (ImageButton) findViewById(R.id.btn_send_msg);
        mEtMessage = (EditText) findViewById(R.id.et_send_mesg);
        mListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        mEtContactNumber = (EditText) findViewById(R.id.et_enter_number);
    }

    /**
     * sets title on action bar
     *
     * @param title title to be displayed on action bar
     */
    private void setTitle(String title) {
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);
        initViews();
        //extracting data from intent
        mThreadId = getIntent().getStringExtra(Constants.THREAD_ID);
        mRecipients = getIntent().getStringExtra(Constants.RECIPIENTS);

        if (mThreadId.equalsIgnoreCase(Constants.NEW_MESSAGE)) {
            mEtContactNumber.setVisibility(View.VISIBLE);
            setTitle(getString(R.string.create_message));
        } else {
            populateChatList();
        }
        //Send Message
        mBtnSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String sms = mEtMessage.getText().toString().trim();
                if (sms.isEmpty()) {
                    displayToast(getString(R.string.empty_message));
                } else {
                    if (mThreadId.equalsIgnoreCase(Constants.NEW_MESSAGE)) {
                        String contactNumber = mEtContactNumber.getText().toString().trim();
                        if (contactNumber.isEmpty()) {
                            displayToast(getString(R.string.empty_contact));
                        } else {
                            createNewSMS(sms, contactNumber);
                        }
                    } else {
                        sendSMS(sms);
                        mEtMessage.setText("");
                        mListView.setStackFromBottom(true);
                    }
                }

            }
        });
    }

    /**
     * Populates ListView with data from Db
     */
    private void populateChatList() {
        mEtContactNumber.setVisibility(View.GONE);

        if (!mRecipients.equalsIgnoreCase(Constants.NEW_MESSAGE)) {
            findContactNameById(mRecipients);
            setTitle(mTitle.substring(0, mTitle.length() - 1));
        } else {
            setTitle(CommonUtils.getContactByNumber(mPhoneNumber.substring(0, mPhoneNumber.length() - 1), this));
        }

        Uri uri = Uri.parse("content://sms");
        Cursor cursor = getContentResolver().query(
                uri,
                new String[]{"_id", "thread_id", "address", "date", "body",
                        "type"}, "thread_id=?", new String[]{mThreadId}, "date ASC");
        //setting listView
        mAdapter = new ConversationListAdapter(this, cursor);
        mListView.setAdapter(mAdapter);
    }

    /**
     * finds contact address from recipient ids
     *
     * @param recipient_ids id received from sms content provider
     */
    private void findContactNameById(String recipient_ids) {
        String[] r_ids = recipient_ids.split(" ");
        Cursor c = null;
        String CONTENT_URI = "content://mms-sms/canonical-address/";
        for (int i = 0; i < r_ids.length; i++) {
            Uri uri = Uri.parse(CONTENT_URI + r_ids[i]);
            c = getContentResolver().query(uri, new String[]{"*"}, null, null, null);
            if (c != null) {
                c.moveToFirst();
                mPhoneNumber += c.getString(c.getColumnIndex("address")) + ";";
                String name = CommonUtils.getContactByNumber(c.getString(c.getColumnIndex("address")), this);
                if (name != null) {
                    mTitle += name + ",";
                } else {
                    mTitle += c.getString(c.getColumnIndex("address")) + ",";
                }
            }
        }
        if (c != null) {
            c.close();
        }
    }

    /**
     * Sends Sms to new contact numbers
     *
     * @param sms    sms to send
     * @param number contact number
     */
    @Override
    public void createNewSMS(String sms, String number) {
        //creates new thread for chat and handles only one recipients
        SmsManager smsMgr = SmsManager.getDefault();
        String SMS_SENT = "SMS_SENT";
        String SMS_DELIVERED = "SMS_DELIVERED";
        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SMS_SENT), 0);
        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0, new Intent(SMS_DELIVERED), 0);
        registerBroadCastReceiver(SMS_SENT, SMS_DELIVERED);

        smsMgr.sendTextMessage(number, null, sms, sentPI, deliveredPI);
        mEtMessage.setText("");

    }

    /**
     * Sends Message to already created  message thread
     *
     * @param sms sms to send
     */
    @Override
    public void sendSMS(String sms) {
        SmsManager smsMgr = SmsManager.getDefault();
        String SMS_SENT = "SMS_SENT";
        String SMS_DELIVERED = "SMS_DELIVERED";
        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SMS_SENT), 0);
        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0, new Intent(SMS_DELIVERED), 0);
        registerBroadCastReceiver(SMS_SENT, SMS_DELIVERED);

        // Send a text based SMS
        String[] recipientsNumber = mPhoneNumber.substring(0, mPhoneNumber.length() - 1).split(";");
        for (int i = 0; i < recipientsNumber.length; i++) {
            smsMgr.sendTextMessage(recipientsNumber[i], null, sms, sentPI, deliveredPI);
        }

    }

    private void registerBroadCastReceiver(String SMS_SENT, String SMS_DELIVERED) {
        // For when the SMS has been sent
        mSendBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        // for new message from create new message fAB
                        if (mRecipients.equalsIgnoreCase(Constants.NEW_MESSAGE)) {
                            mDeliverySuccess = true;
                            Uri uri = Uri.parse("content://sms");
                            Cursor cursor = getContentResolver().query(
                                    uri,
                                    new String[]{"_id", "thread_id", "address", "date", "body",
                                            "type"}, null, null, "date DESC" + " LIMIT 1");
                            String threadId = null, address = null;
                            if (cursor != null) {
                                cursor.moveToFirst();
                                threadId = cursor.getString(cursor.getColumnIndex("thread_id"));
                                address = cursor.getString(cursor.getColumnIndex("address"));
                                mPhoneNumber = address + ";";
                                mThreadId = threadId;
                                cursor.close();
                                //populate List with thread id details
                                populateChatList();
                            }
                        }
                        displayToast("SMS sent successfully");
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        displayToast("Generic failure cause");
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        displayToast("Service is currently unavailable");
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        displayToast("No pdu provided");
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        displayToast("Radio was explicitly turned off");
                        break;
                }
            }
        };
        mDeleiveredBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        if (!mDeliverySuccess) {
                            // for new message from create new message fAB
                            if (mRecipients.equalsIgnoreCase(Constants.NEW_MESSAGE)) {
                                mDeliverySuccess = true;
                                Uri uri = Uri.parse("content://sms");
                                Cursor cursor = getContentResolver().query(
                                        uri,
                                        new String[]{"_id", "thread_id", "address", "date", "body",
                                                "type"}, null, null, "date DESC" + " LIMIT 1");
                                String threadId = null, address = null;
                                if (cursor != null) {
                                    cursor.moveToFirst();
                                    threadId = cursor.getString(cursor.getColumnIndex("thread_id"));
                                    address = cursor.getString(cursor.getColumnIndex("address"));
                                    mPhoneNumber = address + ";";
                                    mThreadId = threadId;
                                    cursor.close();
                                    //populate List with thread id details
                                    populateChatList();
                                }
                            }
                        }
                        displayToast("SMS delivered");
                        break;
                    case Activity.RESULT_CANCELED:
                        displayToast("SMS not delivered");
                        break;
                }
            }
        }

        ;

        registerReceiver(mDeleiveredBroadcastReceiver, new IntentFilter(SMS_DELIVERED)

        );

    }

    /**
     * shows toast message with specified message
     *
     * @param msg message to be displayed in toast
     */

    private void displayToast(String msg) {
        Toast.makeText(ConversationActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mSendBroadcastReceiver != null)
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mSendBroadcastReceiver);

        if (mDeleiveredBroadcastReceiver != null)
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mDeleiveredBroadcastReceiver);
    }
}

