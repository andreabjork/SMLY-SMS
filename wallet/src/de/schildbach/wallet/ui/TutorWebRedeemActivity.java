package de.schildbach.wallet.ui;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;
import com.google.bitcoin.core.Address;

import java.math.BigInteger;

import de.schildbach.wallet.Configuration;
import de.schildbach.wallet.Constants;
import de.schildbach.wallet.ExchangeRatesProvider;
import de.schildbach.wallet.WalletApplication;
import de.schildbach.wallet.util.TutorWebConnectionTask;
import de.schildbach.wallet.util.UserLocalStore;
import hashengineering.smileycoin.wallet.R;


/**
 * Created by andrea on 7.7.2015.
 */
public class TutorWebRedeemActivity extends AbstractWalletActivity {

    private AbstractWalletActivity activity;
    private WalletApplication application;
    private Configuration config;
    private Address address = null;
    private UserLocalStore store;
    private TextView message;

    private LoaderManager loaderManager;
    private static final int ID_RATE_LOADER = 0;
    private CurrencyCalculatorLink amountCalculatorLink;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tutorweb_redeem);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        // Initialize application, configuration, activity, address, etc.
        // Fill in the address field.
        application = (WalletApplication) getApplication();
        config = application.getConfiguration();
        address = application.determineSelectedAddress();
        activity = this;
        ((AutoCompleteTextView) findViewById(R.id.send_coins_receiving_address)).setText(address.toString());

        // Set up config so amount can be converted.
        final CurrencyAmountView btcAmountView = (CurrencyAmountView) findViewById(R.id.redeem_coins_amount_btc);
        btcAmountView.setCurrencySymbol(config.getBtcPrefix());
        btcAmountView.setInputPrecision(config.getBtcMaxPrecision());
        btcAmountView.setHintPrecision(config.getBtcPrecision());
        btcAmountView.setShift(config.getBtcShift());
        final CurrencyAmountView localAmountView = (CurrencyAmountView) findViewById(R.id.redeem_coins_amount_local);
        localAmountView.setInputPrecision(Constants.LOCAL_PRECISION);
        localAmountView.setHintPrecision(Constants.LOCAL_PRECISION);
        amountCalculatorLink = new CurrencyCalculatorLink(btcAmountView, localAmountView);
        amountCalculatorLink.setEnabled(false);
        loaderManager = getSupportLoaderManager();
        loaderManager.initLoader(ID_RATE_LOADER, null, rateLoaderCallbacks);

        // Initialize store so we can access user's data.
        store = new UserLocalStore(getApplicationContext());
        message = (TextView)(findViewById(R.id.redeem_coins_message));
    }

    @Override
    protected void onRestart() {
        super.onRestart();
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

    public void TutorWebRedeem(View view) throws Exception {
        TutorWebConnectionTask task = new TutorWebConnectionTask((AbstractWalletActivity) this) {
            @Override
            protected void onPostExecute(Void aVoid) {
                if(thrownException != null) handleRedeemException(thrownException);
                else handleRedeemSuccess();
            }
        };
        task.execute("redeem", store.getUserCookie(), address.toString());
    }

    public void handleRedeemSuccess() {
        TutorWebConnectionTask task = new TutorWebConnectionTask((AbstractWalletActivity) this) {
            @Override
            protected void onPostExecute(Void aVoid) {
                if(thrownException != null) handleBalanceException(thrownException);
                else handleBalanceSuccess();
            }
        };
        task.execute("balance", store.getUserCookie());
    }

    public void handleRedeemException(Exception e) {
        if(e instanceof TutorWebConnectionTask.UnauthorizedException) {
            message.setText("Unauthorized. Please try logging in again.");
        }
        else if(e instanceof TutorWebConnectionTask.InternalErrorException) {
            message.setText("The server is out of coins. Please try again later.");
        } else if(e instanceof Exception) {
            message.setText("An unknown error has ocurred. We could not redeem your coins.");
        }
    }

    public void handleBalanceSuccess() {
        int coins = store.getUserBalance();
        Long coins_long = new Long(coins);
        Long coins_mult = new Long(100000000);
        long coins_big = coins_long*coins_mult;
        BigInteger bigCoins = BigInteger.valueOf(coins_big);
        amountCalculatorLink.setBtcAmount(bigCoins);

        if(coins==0) {
            message.setText("You have successfully redeemed your SMLY coins. They will appear in your wallet in a short while.");
            findViewById(R.id.redeemBtn).setEnabled(false);
        }
    }

    public void handleBalanceException(Exception e) {
        if(e instanceof TutorWebConnectionTask.UnauthorizedException) {
            message.setText("Unauthorized. Please try logging in again.");
        }
        else if(e instanceof Exception) {
            message.setText("Something went wrong. Please try again later.");
        }
    }

    public void TutorWebLogout(View view) throws Exception {
        store.clearSessionData();
        Intent intent = new Intent(this, TutorWebConnectActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }

    public void handleSuccess() {
        Intent intent = new Intent(this, TutorWebRedeemActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }

    public void handleException(Exception e) {
        if(e instanceof TutorWebConnectionTask.UnauthorizedException);
        else if(e instanceof TutorWebConnectionTask.InternalErrorException) ;
    }

    // LoaderCallbacks functions:
    private final LoaderCallbacks<Cursor> rateLoaderCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
            return new ExchangeRateLoader(activity, config);
        }

        @Override
        public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
            if (data != null && data.getCount() > 0) {
                data.moveToFirst();
                final ExchangeRatesProvider.ExchangeRate exchangeRate = ExchangeRatesProvider.getExchangeRate(data);

                amountCalculatorLink.setExchangeRate(exchangeRate);
            }
        }

        @Override
        public void onLoaderReset(final Loader<Cursor> loader) {
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        loaderManager.initLoader(ID_RATE_LOADER, null, rateLoaderCallbacks);
    }

    @Override
    public void onPause() {
        loaderManager.destroyLoader(ID_RATE_LOADER);
        super.onPause();
    }
}