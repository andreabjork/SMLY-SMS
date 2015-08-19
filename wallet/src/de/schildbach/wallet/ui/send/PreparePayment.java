package de.schildbach.wallet.ui.send;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.script.ScriptBuilder;

import java.math.BigInteger;

import de.schildbach.wallet.Constants;
import de.schildbach.wallet.data.PaymentIntent;

/**
 * Created by andrea on 7.8.2015.
 */
public class PreparePayment extends Activity {

    public static final String INTENT_EXTRA_PAYMENT_INTENT = "payment_intent";

    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve data from the intent
        Intent extAppIntent = getIntent();
        String addr = extAppIntent.getStringExtra("Address");
        String label = extAppIntent.getStringExtra("AddressLabel");
        int amount = extAppIntent.getIntExtra("Amount", 0);

        // Build a payment intent from the data
        Address address = null;
        try {
            address = new Address(Constants.NETWORK_PARAMETERS, addr);
            Long coins_long = new Long(amount);
            Long coins_mult = new Long(100000000);
            long coins_big = coins_long*coins_mult;
            BigInteger bigintAmount = BigInteger.valueOf(coins_big);
            PaymentIntent pi = new PaymentIntent(PaymentIntent.Standard.BIP70, null, null, null, buildSimplePayTo(bigintAmount, address), label, null, null, null);

            // Start the send coins activity with the payment intent:
            final Intent intent = new Intent(this, SendCoinsActivity.class);
            intent.putExtra("purchase", true);
            intent.putExtra(INTENT_EXTRA_PAYMENT_INTENT, pi);
            this.startActivityForResult(intent, 1);

        }catch(com.google.bitcoin.core.AddressFormatException e) {Log.e("AddressError", "Wrong format"); }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.i("intent", "anything happening whatsoever?");
        if (requestCode == 1) {

            Log.i("intent", "request code is 1");
            if(resultCode == RESULT_OK){
                Log.i("intent", "setting the intent result in PreparePayment");
                setResult(RESULT_OK);
                finish();
            }
            if (resultCode == RESULT_CANCELED) {
                Log.i("Result", "No result or result canceled");//Write your code if there's no result
                setResult(RESULT_CANCELED);
                finish();
            }
        }
    }

    private static PaymentIntent.Output[] buildSimplePayTo(final BigInteger amount, final Address address)
    {
        return new PaymentIntent.Output[] { new PaymentIntent.Output(amount, ScriptBuilder.createOutputScript(address)) };
    }

}

