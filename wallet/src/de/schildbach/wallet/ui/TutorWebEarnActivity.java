package de.schildbach.wallet.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Spinner;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;

import hashengineering.smileycoin.wallet.R;

/**
 * Created by andrea on 7.7.2015.
 */
public class TutorWebEarnActivity extends AbstractWalletActivity
{
    private Spinner spinner;

    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tutorweb_earn);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        spinner = ((Spinner)findViewById(R.id.link_spinner));
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

    public void openTutorWeb(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(spinner.getSelectedItemPosition()==0 ? "http://tutor-web.net/math" : "http://box.tutor-web.net/math")));
    }

}

