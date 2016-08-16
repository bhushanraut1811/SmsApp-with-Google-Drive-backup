package apps.sample.smsapp.ui;

import android.content.Intent;
import android.content.IntentSender;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;

import apps.sample.smsapp.R;
import apps.sample.smsapp.adapter.ConversationListAdapter;
import apps.sample.smsapp.adapter.ThreadListAdapter;
import apps.sample.smsapp.service.SmsBackupService;
import apps.sample.smsapp.util.Constants;

/**
 * ThreadActivity shows list of conversations threads by date and options to send New Sms
 */
public class ThreadActivity extends AppCompatActivity implements SearchView.OnQueryTextListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = ThreadActivity.class.getSimpleName();
    private ListView mListView;
    private ThreadListAdapter mThreadListAdapter;
    private FloatingActionButton mSendSmsFab;
    private FrameLayout mFrameSearchLayout;
    private ListView mListViewSearch;
    private ConversationListAdapter mSearchListAdapter;
    private GoogleApiClient mGoogleApiClient;
    //used for searching
    private Cursor mCursor;

    /**
     * Initializes Views present in activity
     */
    private void initViews() {
        mListView = (ListView) findViewById(R.id.lv_thread_list);
        mSendSmsFab = (FloatingActionButton) findViewById(R.id.fab);
        mFrameSearchLayout = (FrameLayout) findViewById(R.id.fl_search_msg);
        mListViewSearch = (ListView) findViewById(R.id.lv_search_msg);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thread);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        initViews();
        //requesting permissions
        final int REQUEST_CODE_ASK_PERMISSIONS = 123;
        ActivityCompat.requestPermissions(ThreadActivity.this, new String[]{"android.permission.READ_SMS"}, REQUEST_CODE_ASK_PERMISSIONS);

        //Content provider to get Conversations from Sms Db
        final String[] projection = new String[]{"_id", "snippet", "date", "recipient_ids"};
        Uri uri = Uri.parse("content://mms-sms/conversations?simple=true");
        final Cursor cursor = getContentResolver().query(uri, projection, null, null, "date DESC");
        //setting up listView
        mThreadListAdapter = new ThreadListAdapter(this, cursor);
        mListView.setAdapter(mThreadListAdapter);

        //open new to see conversation
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor c = (Cursor) mThreadListAdapter.getItem(position);
                openConversation(c);
            }
        });
        //to send new Sms
        mSendSmsFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFrameSearchLayout.setVisibility(View.GONE);
                Intent sendSmsIntent = new Intent(ThreadActivity.this, ConversationActivity.class);
                sendSmsIntent.putExtra(Constants.THREAD_ID, Constants.NEW_MESSAGE);
                sendSmsIntent.putExtra(Constants.RECIPIENTS, Constants.NEW_MESSAGE);
                startActivity(sendSmsIntent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sms, menu);
        final MenuItem item = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        searchView.setOnQueryTextListener(this);

        //SearchView lsiteners to collapse and expand
        MenuItemCompat.setOnActionExpandListener(item,
                new MenuItemCompat.OnActionExpandListener() {
                    @Override
                    public boolean onMenuItemActionCollapse(MenuItem item) {
                        mFrameSearchLayout.setVisibility(View.GONE);
                        mCursor = null;
                        mListViewSearch.setAdapter(null);
                        return true;
                    }

                    @Override
                    public boolean onMenuItemActionExpand(MenuItem item) {
                        mFrameSearchLayout.setVisibility(View.VISIBLE);
                        return true;
                    }
                });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_backup) {
            //add backUp account here and start service
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            mGoogleApiClient.connect();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (newText.length() != 0) {

            final String[] projection = new String[]{"*"};
            Uri uri = Uri.parse("content://sms");
            mCursor = getContentResolver().query(uri, projection, "body like ?", new String[]{"%" + newText + "%"}, null);
            mSearchListAdapter = new ConversationListAdapter(this, mCursor);
            mListViewSearch.setAdapter(mSearchListAdapter);

            mListViewSearch.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Cursor c = (Cursor) mThreadListAdapter.getItem(position);
                    openConversation(c);
                }
            });

        } else {
            mListViewSearch.setAdapter(null);
            mCursor = null;
        }
        return true;
    }

    /**
     * opens ConversationActivity
     *
     * @param c cursor for item which is clicked
     */
    private void openConversation(Cursor c) {
        String thread = null;
        String recipients = null;
        if (c != null) {
            thread = c.getString(c.getColumnIndex("_id"));
            recipients = c.getString(c.getColumnIndex("recipient_ids"));
        }
        Intent intent = new Intent(ThreadActivity.this, ConversationActivity.class);
        intent.putExtra(Constants.THREAD_ID, thread);
        intent.putExtra(Constants.RECIPIENTS, recipients);
        startActivity(intent);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Intent backupIntent = new Intent(ThreadActivity.this, SmsBackupService.class);
        startService(backupIntent);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, 0);
                Toast.makeText(this, "Start your back after choosing account", Toast.LENGTH_SHORT).show();
            } catch (IntentSender.SendIntentException e) {
                // Unable to resolve, message user appropriately
                Toast.makeText(this, "back up failed", Toast.LENGTH_SHORT).show();
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this, 0).show();
        }
    }
}
