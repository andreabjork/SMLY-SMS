package de.schildbach.wallet.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;

import de.schildbach.wallet.util.TutorWebConnectionTask;
import de.schildbach.wallet.util.UserLocalStore;
import hashengineering.smileycoin.wallet.R;

/**
 * Created by andrea on 7.7.2015.
 */
public class TutorWebActivity extends AbstractWalletActivity
{

    private UserLocalStore store;

    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tutorweb);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
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

    public void openAbout(View view) {
        startActivity(new Intent(this, TutorWebAboutActivity.class));
    }

    public void openEarnSMLY(View view) {
        startActivity(new Intent(this, TutorWebEarnActivity.class));
    }

    // If there is a known user for this app, try to connect to tutor-web with his cookie
    // If it has expired, redirect the user to the login screen.
    // If there is no known user, open login screen.
    public void openRedeemSMLY(View view) {
        store = new UserLocalStore(getApplicationContext());
        if (store.userInSession()) {
            forwardToRedeemActivity();
        } else {
            Intent intent = new Intent(this, TutorWebConnectActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(intent);
        }
    }

    public void openSpendSMLY(View view) {
        startActivity(new Intent(this, TutorWebSpendActivity.class));
    }

    // This functions handles forwarding to the next page if cookie is still valid.
    private void forwardToRedeemActivity() {
        TutorWebConnectionTask task = new TutorWebConnectionTask((AbstractWalletActivity)this) {
            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                if (this.getResponseCode()==200) {
                    TutorWebActivity.this.handleSuccessfulForward();
                } else {
                    TutorWebActivity.this.redirectToLogin();
                }
            }
        };
        task.execute("balance", store.getUserCookie());
    }

    public void handleSuccessfulForward() {
        Intent intent = new Intent(this, TutorWebRedeemActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }

    public void redirectToLogin() {
        Intent intent = new Intent(this, TutorWebConnectActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }
    
}

