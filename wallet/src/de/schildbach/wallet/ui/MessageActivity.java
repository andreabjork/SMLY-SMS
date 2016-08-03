package de.schildbach.wallet.ui;

import android.app.Activity;
import android.os.Bundle;

import hashengineering.smileycoin.wallet.R;

/**
 * Created by andrea on 25.7.2016.
 */
public class MessageActivity extends AbstractWalletActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.send_message);
    }
}
