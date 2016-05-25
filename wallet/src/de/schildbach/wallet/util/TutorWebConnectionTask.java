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

import javax.net.ssl.HttpsURLConnection;

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
            e.printStackTrace();
        }

        return null;
    }

    private int reqPOST_login(String url, String usr, String pw, String spinnerVal) throws Exception {
        HttpURLConnection con;
        URL obj = new URL(url);
        con = (HttpURLConnection) obj.openConnection();
        con.setInstanceFollowRedirects(false);

        // This needs to be done here because the body of the request needs to be written
        // before checking the response. Do this again if we need to redirect.
        con.setDoOutput(true);
        con.setRequestMethod("POST");
        // Now that we've handled redirect, write to body of request:
        String requestValues="form.submitted=1&pwd_empty="+(pw.length()>0 ? 0 : 1)+"&__ac_name="+usr+"&__ac_password="+pw;
        // removed from here
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(requestValues);
        wr.flush();
        wr.close();


        boolean redirect = false;

        // normally, 3xx is redirect
        int status = con.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            if (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER)
                redirect = true;
        }

        if (redirect) {
            // get redirect url from "location" header field
            String newUrl = con.getHeaderField("Location");
            Log.d("REDEEM", newUrl);
            // get the cookie if need, for login
            String cookies = con.getHeaderField("Set-Cookie");

            // open the new connnection again
            con = (HttpURLConnection) new URL(newUrl).openConnection();
            con.setRequestProperty("Cookie", cookies);
            con.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            // Now that we've handled redirect, write to body of request:
            wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(requestValues);
            wr.flush();
            wr.close();

            System.out.println("Redirect to URL : " + newUrl);
        }



        // Store cookie:
        store.clearSessionData(); // Clears cookies and coin balance before logging in
        String cookie = this.getCookie(con);
        if(cookie!=null && !cookie.contains("deleted")) {
            store.clearUserData();
            store.storeUserData(usr, cookie, spinnerVal);
        }

        switch(con.getResponseCode()) {
            case 200:
                Log.d("REDEEM", "Case 200, breaking...");
                break;
            case 401:
                Log.d("REDEEM", "Case 401 -> exception!");
                throw new UnauthorizedException();
            case 403:
                Log.d("REDEEM", "Case 403, unauthorized...");
                throw new UnauthorizedException();
            case 500:
                Log.d("REDEEM", "Case 500, internal");
                throw new InternalErrorException();
            default:
                Log.d("REDEEM", "Default?");
                throw new Exception("An unknown exception has ocurred.");
        }

        return con.getResponseCode();
    }

    private int reqPOST_redeem(String url) throws Exception {
        HttpURLConnection con;
        URL obj = new URL(url);
        con = (HttpURLConnection) obj.openConnection();
        con.setInstanceFollowRedirects(false);
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

        // Handle redirect.
        boolean redirect = false;

        // normally, 3xx is redirect
        int status = con.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            if (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER)
                redirect = true;
        }

        if (redirect) {
            // get redirect url from "location" header field
            String newUrl = con.getHeaderField("Location");
            Log.d("REDEEM", newUrl);
            // get the cookie if need, for login
            String cookies = con.getHeaderField("Set-Cookie");

            // open the new connnection again
            con = (HttpURLConnection) new URL(newUrl).openConnection();
            CookieHandler.setDefault(new CookieManager());;
            // Add request header
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            con.setRequestProperty("Cookie", store.getUserCookie());
            // Send post request
            con.setDoOutput(true);
            wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(requestValues);
            wr.flush();
            wr.close();

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
        String cookieHeader = urlCon.getHeaderField("Set-Cookie");
        if(cookieHeader == null) return null;
        else return (urlCon.getHeaderField("Set-Cookie").split(Pattern.quote(";")))[0];
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