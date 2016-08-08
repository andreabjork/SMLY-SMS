
package de.schildbach.wallet.ui.message;
/**
 * @author Andrea Bjornsdottir
 */
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import javax.annotation.Nonnull;

import de.schildbach.wallet.data.PaymentIntent;
import de.schildbach.wallet.ui.AbstractBindServiceActivity;
import de.schildbach.wallet.ui.HelpDialogFragment;
import hashengineering.smileycoin.wallet.R;

/**
 * @author Andreas Schildbach
 */
public final class SendMessageActivity extends AbstractBindServiceActivity
{
    public static final String INTENT_EXTRA_PAYMENT_INTENT = "payment_intent";

    public static void start(final Context context, @Nonnull PaymentIntent paymentIntent)
    {
        final Intent intent = new Intent(context, SendMessageActivity.class);
        intent.putExtra(INTENT_EXTRA_PAYMENT_INTENT, paymentIntent);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.send_message_content);

        getWalletApplication().startBlockchainService(false);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu)
    {
        getSupportMenuInflater().inflate(R.menu.send_message_activity_options, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
                finish();
                return true;

            case R.id.send_message_options_help:
                HelpDialogFragment.page(getSupportFragmentManager(), R.string.help_send_message);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
