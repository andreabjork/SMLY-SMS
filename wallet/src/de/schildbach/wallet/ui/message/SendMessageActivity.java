/**
 * @author Andrea Bjornsdottir
 */
public final class SendCoinsActivity extends AbstractBindServiceActivity
{
    public static final String INTENT_EXTRA_PAYMENT_INTENT = "payment_intent";

    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.send_message_activity);

        getWalletApplication().startBlockchainService(false);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

    }
