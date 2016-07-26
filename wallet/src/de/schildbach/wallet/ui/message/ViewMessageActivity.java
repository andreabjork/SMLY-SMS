/**
 * @author Andrea Bjornsdottir
 */
public final class SendCoinsActivity extends AbstractWalletActivity
{
    public static final String INTENT_EXTRA_PAYMENT_INTENT = "payment_intent";


    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.view_message_content);

        getWalletApplication().startBlockchainService(false);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

    }
