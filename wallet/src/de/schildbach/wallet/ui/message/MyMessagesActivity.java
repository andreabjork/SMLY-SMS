
package de.schildbach.wallet.ui.message;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.actionbarsherlock.app.ActionBar;

import de.schildbach.wallet.ui.AbstractWalletActivity;
import de.schildbach.wallet.ui.WalletActivity;
import hashengineering.smileycoin.wallet.R;

/**
 * @author Andrea Bjornsdottir
 */
public final class MyMessagesActivity extends AbstractWalletActivity {
    public static final String INTENT_EXTRA_PAYMENT_INTENT = "payment_intent";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.my_messages_activity);

        getWalletApplication().startBlockchainService(false);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);


        Button button = (Button) findViewById(R.id.msgBtn);
        final Intent sendMsgIntent = new Intent(this, SendMessageActivity.class);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("MESSAGE", "Let me prove that this is actually logging something");
                startActivity(sendMsgIntent);
            }
        });

    }

}
