package de.schildbach.wallet.util;

import android.os.AsyncTask;
import android.util.Log;

import com.google.bitcoin.core.Address;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Pattern;

import de.schildbach.wallet.Configuration;
import de.schildbach.wallet.WalletApplication;
import de.schildbach.wallet.ui.AbstractWalletActivity;


public class TutorWebConnectionTask extends AsyncTask<String, String, Void> {
    // URLS:
    private final String login_url = "http://tutor-web.net/login_form";
    private final String box_login_url = "http://box.tutor-web.net/login_form";
    private final String redeem_url = "http://tutor-web.net/@@quizdb-student-award";
    private final String box_redeem_url = "http://box.tutor-web.net/@@quizdb-student-award";
    private boolean box = false;

    // For storing and retrieving user's data:
    private AbstractWalletActivity activity;
    private WalletApplication application;
    private Configuration config;
    private Address address;
    private UserLocalStore store;
    protected Exception thrownException;

    public TutorWebConnectionTask(AbstractWalletActivity myActivity) {
        // Get default wallet address:
        this.activity = myActivity;
        application = (WalletApplication) activity.getApplication();
        config = application.getConfiguration();
        address = application.determineSelectedAddress();
        thrownException = null;
        // Initialize userLocalStore:
        store = new UserLocalStore(activity.getApplicationContext());
    }

    @Override
    protected Void doInBackground(String... strings) {
        try {
            if (strings[0].equals("login")) { // strings = {username, password, spinnerValue}
                if(strings[3].equals("1")) box=true;
                this.reqPOST_login((box ? box_login_url : login_url), strings[1], strings[2], strings[3]);
                if(store.userInSession()) this.reqGET_balance((box ? box_redeem_url : redeem_url));
            }
            else if(strings[0].equals("redeem") && store.userInSession()) this.reqPOST_redeem((store.usingBox() ? box_redeem_url : redeem_url));
            else if(strings[0].equals("balance") && store.userInSession()) this.reqGET_balance((store.usingBox() ? box_redeem_url : redeem_url));
        } catch(UnauthorizedException e) {
            thrownException = e;
        } catch(InternalErrorException e) {
            thrownException = e;
        } catch(Exception e) {
            thrownException = e;
        }

        return null;
    }

    private int reqPOST_login(String url, String usr, String pw, String spinnerVal) throws Exception {
        HttpURLConnection con;
        URL obj = new URL(url);
        con = (HttpURLConnection) obj.openConnection();

        // Add request header
        con.setRequestMethod("POST");
        // Adjust request depending on login/redeeming coins.
        String requestValues="form.submitted=1&pwd_empty="+(pw.length()>0 ? 1 : 0)+"&__ac_name="+usr+"&__ac_password="+pw;

        // Send post request
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(requestValues);
        wr.flush();
        wr.close();

        store.clearSessionData(); // Clears cookies and coin balance before logging in

        String cookie = this.getCookie(con);
        if(!cookie.contains("deleted")) {
            store.clearUserData();
            store.storeUserData(usr, cookie, spinnerVal);
        }

        switch(con.getResponseCode()) {
            case 200: break;
            case 401: throw new UnauthorizedException();
            case 403: throw new UnauthorizedException();
            case 500: throw new InternalErrorException();
            default: throw new Exception("An unknown exception has ocurred.");
        }

        return con.getResponseCode();
    }

    private int reqPOST_redeem(String url) throws Exception {
        HttpURLConnection con;
        URL obj = new URL(url);
        con = (HttpURLConnection) obj.openConnection();

        CookieHandler.setDefault(new CookieManager());
        String requestValues = "{\"walletId\":\""+address.toString()+"\"}";
        // Add request header
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        con.setRequestProperty("Cookie", store.getUserCookie());

        // Send post request
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(requestValues);
        wr.flush();
        wr.close();

        switch(con.getResponseCode()) {
            case 200: break;
            case 401: throw new UnauthorizedException();
            case 403: throw new UnauthorizedException();
            case 500: throw new InternalErrorException();
            default: throw new Exception("An unknown exception has ocurred.");
        }

        return con.getResponseCode();
    }

    private int reqGET_balance(String url) throws Exception {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        //add request header
        con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        con.setRequestProperty("Cookie", store.getUserCookie());
        switch(con.getResponseCode()) {
            case 200: break;
            case 401: throw new UnauthorizedException();
            case 403: throw new UnauthorizedException();
            case 500: throw new InternalErrorException();
            default: throw new Exception("An unknown exception has ocurred.");
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine = in.readLine();
        in.close();

        int coinBalance = ((Integer.parseInt(((((inputLine.split(Pattern.quote(",")))[2]).split(Pattern.quote(":")))[1]).replaceAll("\\s", ""))) / 1000);
        store.setUserBalance(coinBalance);


        return con.getResponseCode();
    }

    // Returns the Set-Cookie header from post request with HttpURLConnection
    private String getCookie(HttpURLConnection urlCon) {
        return (urlCon.getHeaderField("Set-Cookie").split(Pattern.quote(";")))[0];
    }

    // 401, 403
    public class UnauthorizedException extends Exception {
        public UnauthorizedException() {super();}
        public UnauthorizedException(String message) {super(message);}
        public UnauthorizedException(String message, Throwable cause) {super(message, cause);}
        public UnauthorizedException(Throwable cause) {super(cause);}
    }

    // 500
    public class InternalErrorException extends Exception {
        public InternalErrorException() {super();}
        public InternalErrorException(String message) {super(message);}
        public InternalErrorException(String message, Throwable cause) {super(message, cause);}
        public InternalErrorException(Throwable cause) {super(cause);}
    }
}