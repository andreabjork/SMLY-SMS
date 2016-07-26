
package de.schildbach.wallet.ui.message;


import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.actionbarsherlock.app.ActionBar;

import de.schildbach.wallet.ui.AbstractBindServiceActivity;
import de.schildbach.wallet.ui.AbstractWalletActivity;
import hashengineering.smileycoin.wallet.R;

/**
 * @author Andrea Bjornsdottir
 */
public final class SendMessageActivity extends AbstractWalletActivity {
    public static final String INTENT_EXTRA_PAYMENT_INTENT = "payment_intent";

    private String message;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.send_message_activity);

        getWalletApplication().startBlockchainService(false);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        message = ((EditText)findViewById(R.id.message_view)).getText();

        Button button = (Button) findViewById(R.id.sendBtn);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

    }



    private void sendMessage() {
        Message msg = new Message(message);
        double[] amounts = msg.getAmountsForMessage();
    }


    private void sendMessage() {
    }


    private void sendMessage() {
    }


    private void sendMessage() {
    }


    private void sendMessage() {
    }


    private void sendMessage() {
    }


    private void sendMessage() {
    }


    private void sendMessage() {
    }



}
