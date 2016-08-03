
package de.schildbach.wallet.ui.message;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.internal.view.menu.ActionMenuItem;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionConfidence;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.script.ScriptBuilder;

import org.bitcoin.protocols.payments.Protos;

import java.math.BigInteger;
import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import de.schildbach.wallet.Configuration;
import de.schildbach.wallet.Constants;
import de.schildbach.wallet.WalletApplication;
import de.schildbach.wallet.data.PaymentIntent;
import de.schildbach.wallet.integration.android.BitcoinIntegration;
import de.schildbach.wallet.offline.DirectPaymentTask;
import de.schildbach.wallet.ui.AbstractWalletActivity;
import de.schildbach.wallet.ui.DialogBuilder;
import de.schildbach.wallet.ui.send.SendCoinsOfflineTask;
import de.schildbach.wallet.util.Bluetooth;
import de.schildbach.wallet.util.GenericUtils;
import de.schildbach.wallet.util.PaymentProtocol;
import de.schildbach.wallet.util.WalletUtils;
import hashengineering.smileycoin.wallet.R;

/**
 * @author Andrea Bjornsdottir
 */
public final class SendMessageActivity extends AbstractWalletActivity {
    public static final String INTENT_EXTRA_PAYMENT_INTENT = "payment_intent";

    // privates needed for message sending
    private Wallet wallet;
    private Handler backgroundHandler;
    private Configuration config;
    private Activity activity;
    private WalletApplication application;



    private String message;
    private enum State
    {
        INPUT, PREPARATION, SENDING, SENT, FAILED
    }
    private State state;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.send_message_activity);
        getWalletApplication().startBlockchainService(false);
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        state = State.INPUT;
        message = ((EditText)findViewById(R.id.message_view)).getText().toString();

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
        makeMessagePayments(amounts);
    }


    // Handle sending of all payments necessary for the
    // sending of the message
    private void makeMessagePayments(final double[] amounts) {
        if(amounts.length == 1) {
            makePayment(amounts[0], new Callback() {
                public void onFinish() {
                    Log.i("MESSAGE", "Successfully sent the message it seems");
                }
            });
            return;
        }

        makePayment(amounts[0], new Callback() {
            public void onFinish() {
                makeMessagePayments(tail(amounts));
            }

        });
    }

    // All but first element of A returned.
    private double[] tail(double[] A) {
        return Arrays.copyOfRange(A, 1, A.length);
    }
    private static PaymentIntent.Output[] buildSimplePayTo(final BigInteger amount, final Address address)
    {
        return new PaymentIntent.Output[] { new PaymentIntent.Output(amount, ScriptBuilder.createOutputScript(address)) };
    }
    // Handle a single payment
    private void makePayment(double amount, final Callback callbck) {
        state = State.PREPARATION;
        //updateView();

        String addr="stringaddress?";
        String label="testlabel";
        Long coins_long = new Long(100);
        Long coins_mult = new Long(100000000);
        long coins_big = coins_long * coins_mult;
        BigInteger bigintAmount = BigInteger.valueOf(coins_big);
        Address address = null;
        try {
            address = new Address(Constants.NETWORK_PARAMETERS, addr);
            PaymentIntent pi = new PaymentIntent(PaymentIntent.Standard.BIP70, null, null, null, buildSimplePayTo(bigintAmount, address), label, null, null, null);



        }catch(Exception e) {
            e.printStackTrace();
        }

        // final payment intent
        /*final PaymentIntent finalPaymentIntent = paymentIntent.mergeWithEditedValues(amountCalculatorLink.getAmount(),
                validatedAddress != null ? validatedAddress.address : null);*/
        final PaymentIntent finalPaymentIntent = new PaymentIntent(PaymentIntent.Standard.BIP70, null, null, null, buildSimplePayTo(bigintAmount, address), label, null, null, null);
        final BigInteger finalAmount = finalPaymentIntent.getAmount();

        // prepare send request
        final Wallet.SendRequest sendRequest = finalPaymentIntent.toSendRequest();
        final Address returnAddress = WalletUtils.pickOldestKey(wallet).toAddress(Constants.NETWORK_PARAMETERS);
        sendRequest.changeAddress = returnAddress;
        sendRequest.emptyWallet = finalAmount.equals(wallet.getBalance(Wallet.BalanceType.AVAILABLE));


        new SendCoinsOfflineTask(wallet, backgroundHandler)
        {
            @Override
            protected void onSuccess(final Transaction transaction)
            {
                Transaction sentTransaction = transaction;

                state = State.SENDING;
                //updateView();

                TransactionConfidence.Listener sentTransactionConfidenceListener = null;
                sentTransaction.getConfidence().addEventListener(sentTransactionConfidenceListener);

                final Protos.Payment payment = PaymentProtocol.createPaymentMessage(sentTransaction, returnAddress, finalAmount, null,
                        finalPaymentIntent.payeeData);

                directPay(payment);


                application.broadcastTransaction(sentTransaction);

                final ComponentName callingActivity = activity.getCallingActivity();
                if (callingActivity != null)
                {
                    log.info("returning result to calling activity: {}", callingActivity.flattenToString());
                    final Intent result = new Intent();
                    BitcoinIntegration.transactionHashToResult(result, sentTransaction.getHashAsString());
                    if (finalPaymentIntent.standard == PaymentIntent.Standard.BIP70)
                        BitcoinIntegration.paymentToResult(result, payment.toByteArray());
                    activity.setResult(Activity.RESULT_OK, result);

                    if(activity.getIntent().getBooleanExtra("purchase", false)) activity.finish();
                }

                // Make the rest of the payments for this message, if there are any:
                callbck.onFinish();

            }

            private void directPay(final Protos.Payment payment)
            {
                ActionMenuItem directPaymentEnableView = null;
                if (directPaymentEnableView.isChecked())
                {
                    final DirectPaymentTask.ResultCallback callback = new DirectPaymentTask.ResultCallback()
                    {
                        @Override
                        public void onResult(final boolean ack) {
                            boolean directPaymentAck = ack;

                            if (state == State.SENDING) {
                                state = State.SENT;
                            }

                            //updateView();
                        }

                        @Override
                        public void onFail(final int messageResId, final Object... messageArgs)
                        {
                            final DialogBuilder dialog = DialogBuilder.warn(activity, R.string.send_coins_fragment_direct_payment_failed_title);
                            dialog.setMessage(finalPaymentIntent.paymentUrl + "\n" + getString(messageResId, messageArgs) + "\n\n"
                                    + getString(R.string.send_coins_fragment_direct_payment_failed_msg));
                            dialog.setPositiveButton(R.string.button_retry, new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(final DialogInterface dialog, final int which)
                                {
                                    directPay(payment);
                                }
                            });
                            dialog.setNegativeButton(R.string.button_dismiss, null);
                            dialog.show();
                        }
                    };

                    if (finalPaymentIntent.isHttpPaymentUrl())
                    {
                        new DirectPaymentTask.HttpPaymentTask(backgroundHandler, callback, finalPaymentIntent.paymentUrl, application.httpUserAgent())
                                .send(payment);
                    }/*
                    else if (finalPaymentIntent.isBluetoothPaymentUrl() && bluetoothAdapter != null && bluetoothAdapter.isEnabled())
                    {
                        new DirectPaymentTask.BluetoothPaymentTask(backgroundHandler, callback, bluetoothAdapter,
                                Bluetooth.getBluetoothMac(paymentIntent.paymentUrl)).send(payment);
                    }*/
                }


            }

            @Override
            protected void onInsufficientMoney(@Nullable final BigInteger missing)
            {
                state = State.INPUT;
                //updateView();

                final BigInteger estimated = wallet.getBalance(Wallet.BalanceType.ESTIMATED);
                final BigInteger available = wallet.getBalance(Wallet.BalanceType.AVAILABLE);
                final BigInteger pending = estimated.subtract(available);


                final int btcShift = config.getBtcShift();
                final int btcPrecision = config.getBtcMaxPrecision();
                final String btcPrefix = config.getBtcPrefix();

                final DialogBuilder dialog = DialogBuilder.warn(activity, R.string.send_coins_fragment_insufficient_money_title);
                final StringBuilder msg = new StringBuilder();
                if (missing != null)
                    msg.append(
                            getString(R.string.send_coins_fragment_insufficient_money_msg1,
                                    btcPrefix + ' ' + GenericUtils.formatValue(missing, btcPrecision, btcShift))).append("\n\n");
                if (pending.signum() > 0)
                    msg.append(
                            getString(R.string.send_coins_fragment_pending,
                                    btcPrefix + ' ' + GenericUtils.formatValue(pending, btcPrecision, btcShift))).append("\n\n");
                //msg.append(getString(R.string.send_coins_fragment_insufficient_money_msg2));
                dialog.setMessage(msg);
                dialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        dialog.cancel();//handleEmpty();
                    }
                });
                //dialog.setNegativeButton(R.string.button_cancel, null);
                dialog.show();
            }

            @Override
            protected void onFailure(@Nonnull Exception exception)
            {
                state = State.FAILED;
                //updateView();

                final DialogBuilder dialog = DialogBuilder.warn(activity, R.string.send_coins_error_msg);
                dialog.setMessage(exception.toString());
                dialog.setNeutralButton(R.string.button_dismiss, null);
                dialog.show();
            }
        }.sendCoinsOffline(sendRequest); // send asynchronously
    }

/*
    validateReceivingAddress(true);
    isAmountValid();

    if (everythingValid())
    handleGo();
    else
    requestFocusFirst();*/

    private interface Callback {
        void onFinish();
    }



}
