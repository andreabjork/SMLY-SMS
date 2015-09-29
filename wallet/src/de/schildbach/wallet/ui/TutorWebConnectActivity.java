package de.schildbach.wallet.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieSyncManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;

import de.schildbach.wallet.util.TutorWebConnectionTask;
import de.schildbach.wallet.util.UserLocalStore;
import hashengineering.smileycoin.wallet.R;


/**
 * Created by andrea on 7.7.2015.
 */
public class TutorWebConnectActivity extends AbstractWalletActivity
{
    private EditText userText = null;
    private EditText pwText = null;
    private TextView message = null;
    private Spinner spinner = null;
    private UserLocalStore store;
    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tutorweb_connect);
        CookieSyncManager.createInstance(this);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        store = new UserLocalStore(getApplicationContext());

        findViewById(R.id.connectBtn).setEnabled(true);
        userText = ((EditText)findViewById(R.id.usernameEdit));
        pwText = ((EditText)findViewById(R.id.passwordEdit));
        message = (TextView)(findViewById(R.id.connect_error_message));

        spinner = ((Spinner)findViewById(R.id.url_spinner));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                store.setBox(i == 1);
                if(store.isKnownUser()) {
                    String[] userData = store.getUserData();
                    userText.setText(userData[0]);
                    pwText.setText("");
                    message.setText(" ");
                }else{
                    userText.setText("");
                    pwText.setText("");
                    message.setText(" ");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        spinner.setSelection((store.usingBox() ? 1 : 0));
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void TutorWebLogin(View view) throws Exception {
        TutorWebConnectionTask task = new TutorWebConnectionTask((AbstractWalletActivity)this) {
            @Override
            protected void onPostExecute(Void aVoid) {
                if(thrownException != null) {
                    handleException(thrownException);
                }
                else handleSuccess();
            }
        };
        int spinnerPos = ((Spinner)findViewById(R.id.url_spinner)).getSelectedItemPosition();
        String usrName = userText.getText().toString();
        String pw = pwText.getText().toString();

        if(!usrName.equals("") && !pw.equals("")) {
            message.setText("Connecting...");
            (findViewById(R.id.connectBtn)).setEnabled(false);
            task.execute("login", usrName, pw, Integer.toString(spinnerPos));
        }
        else message.setText("Please fill out both fields above.");
    }

    public void handleSuccess() {
        // Login might have failed even if POST request was successfully sent:
        if(store.userInSession()) {
            Intent intent = new Intent(this, TutorWebRedeemActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(intent);
        } else {
            message.setText("The username and password you entered don't match.");
            (findViewById(R.id.connectBtn)).setEnabled(true);
        }
    }

    public void handleException(Exception e) {
        if(e instanceof TutorWebConnectionTask.UnauthorizedException) {
            message.setText("The username and password you entered don't match.");
            (findViewById(R.id.connectBtn)).setEnabled(true);

        }
        else if(e instanceof Exception) {
            message.setText("Something went wrong. Please try again later");
        }
    }
}

