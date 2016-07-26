
package de.schildbach.wallet.ui.message;


import android.os.Bundle;

import com.actionbarsherlock.app.ActionBar;

import de.schildbach.wallet.ui.AbstractWalletActivity;
import hashengineering.smileycoin.wallet.R;

/**
 * @author Andrea Bjornsdottir
 */
public final class ViewMessageActivity extends AbstractWalletActivity {
    public static final String INTENT_EXTRA_PAYMENT_INTENT = "payment_intent";


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.view_message_content);

        getWalletApplication().startBlockchainService(false);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

    }
}