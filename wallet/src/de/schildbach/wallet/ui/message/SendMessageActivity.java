
package de.schildbach.wallet.ui.message;


import android.app.Activity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.actionbarsherlock.app.ActionBar;

import java.math.BigInteger;
import java.util.Arrays;

import de.schildbach.wallet.Constants;
import de.schildbach.wallet.data.PaymentIntent;
import de.schildbach.wallet.integration.android.BitcoinIntegration;
import de.schildbach.wallet.offline.DirectPaymentTask;
import de.schildbach.wallet.ui.AbstractBindServiceActivity;
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
    private void makeMessagePayments(double[] amounts) {
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

    // Handle a single payment
    private void makePayment(double amount, Callback callbck) {
        state = State.PREPARATION;
        //updateView();

        // final payment intent
        final PaymentIntent finalPaymentIntent = paymentIntent.mergeWithEditedValues(amountCalculatorLink.getAmount(),
                validatedAddress != null ? validatedAddress.address : null);
        final BigInteger finalAmount = finalPaymentIntent.getAmount();

        // prepare send request
        final SendRequest sendRequest = finalPaymentIntent.toSendRequest();
        final Address returnAddress = WalletUtils.pickOldestKey(wallet).toAddress(Constants.NETWORK_PARAMETERS);
        sendRequest.changeAddress = returnAddress;
        sendRequest.emptyWallet = paymentIntent.mayEditAmount() && finalAmount.equals(wallet.getBalance(BalanceType.AVAILABLE));

        new SendCoinsOfflineTask(wallet, backgroundHandler)
        {
            @Override
            protected void onSuccess(final Transaction transaction)
            {
                sentTransaction = transaction;

                state = State.SENDING;
                updateView();

                sentTransaction.getConfidence().addEventListener(sentTransactionConfidenceListener);

                final Payment payment = PaymentProtocol.createPaymentMessage(sentTransaction, returnAddress, finalAmount, null,
                        paymentIntent.payeeData);

                directPay(payment);

                application.broadcastTransaction(sentTransaction);

                final ComponentName callingActivity = activity.getCallingActivity();
                if (callingActivity != null)
                {
                    log.info("returning result to calling activity: {}", callingActivity.flattenToString());
                    final Intent result = new Intent();
                    BitcoinIntegration.transactionHashToResult(result, sentTransaction.getHashAsString());
                    if (paymentIntent.standard == PaymentIntent.Standard.BIP70)
                        BitcoinIntegration.paymentToResult(result, payment.toByteArray());
                    activity.setResult(Activity.RESULT_OK, result);

                    if(activity.getIntent().getBooleanExtra("purchase", false)) activity.finish();
                }

                // Make the rest of the payments for this message, if there are any:
                callbck.onFinish();

            }

            private void directPay(final Payment payment)
            {
                if (directPaymentEnableView.isChecked())
                {
                    final DirectPaymentTask.ResultCallback callback = new DirectPaymentTask.ResultCallback()
                    {
                        @Override
                        public void onResult(final boolean ack) {
                            directPaymentAck = ack;

                            if (state == State.SENDING) {
                                state = State.SENT;
                            }

                            updateView();
                        }

                        @Override
                        public void onFail(final int messageResId, final Object... messageArgs)
                        {
                            final DialogBuilder dialog = DialogBuilder.warn(activity, R.string.send_coins_fragment_direct_payment_failed_title);
                            dialog.setMessage(paymentIntent.paymentUrl + "\n" + getString(messageResId, messageArgs) + "\n\n"
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

                    if (paymentIntent.isHttpPaymentUrl())
                    {
                        new DirectPaymentTask.HttpPaymentTask(backgroundHandler, callback, paymentIntent.paymentUrl, application.httpUserAgent())
                                .send(payment);
                    }
                    else if (paymentIntent.isBluetoothPaymentUrl() && bluetoothAdapter != null && bluetoothAdapter.isEnabled())
                    {
                        new DirectPaymentTask.BluetoothPaymentTask(backgroundHandler, callback, bluetoothAdapter,
                                Bluetooth.getBluetoothMac(paymentIntent.paymentUrl)).send(payment);
                    }
                }


            }

            @Override
            protected void onInsufficientMoney(@Nullable final BigInteger missing)
            {
                state = State.INPUT;
                updateView();

                final BigInteger estimated = wallet.getBalance(BalanceType.ESTIMATED);
                final BigInteger available = wallet.getBalance(BalanceType.AVAILABLE);
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
                updateView();

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
