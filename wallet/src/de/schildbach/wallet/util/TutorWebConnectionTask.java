package de.schildbach.wallet.util;

import android.os.AsyncTask;

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
    private int responseCode = 0;
    // For storing and retrieving user's data:
    private AbstractWalletActivity activity;
    private WalletApplication application;
    private Configuration config;
    private Address address;
    private UserLocalStore store;

    public TutorWebConnectionTask(AbstractWalletActivity myActivity) {
        // Get default wallet address:
        this.activity = myActivity;
        application = (WalletApplication) activity.getApplication();
        config = application.getConfiguration();
        address = application.determineSelectedAddress();
        // Initialize userLocalStore:
        store = new UserLocalStore(activity.getApplicationContext());
    }

    @Override
    protected Void doInBackground(String... strings) {
        try {
            if (strings[0].equals("login")) { // strings = {username, password, spinnerValue}
                if(strings[3].equals("1")) box=true;
                this.reqPOST_login((box ? box_login_url : login_url), strings[1], strings[2], strings[3]);
                this.reqGET_balance((box ? box_redeem_url : redeem_url));
            }
            else if(strings[0].equals("redeem")) this.reqPOST_redeem((store.usingBox() ? box_redeem_url : redeem_url));
            else if(strings[0].equals("balance")) this.reqGET_balance((store.usingBox() ? box_redeem_url : redeem_url));
        } catch(Exception e) {e.printStackTrace();}
        return null;
    }

    private int reqPOST_login(String url, String usr, String pw, String spinnerVal) throws Exception {
        HttpURLConnection con;
        URL obj = new URL(url);
        con = (HttpURLConnection) obj.openConnection();

        // Add request header
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

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

        responseCode = con.getResponseCode();
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
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        con.setRequestProperty("Cookie", store.getUserCookie());

        // Send post request
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(requestValues);
        wr.flush();
        wr.close();

        responseCode = con.getResponseCode();
        return con.getResponseCode();
    }

    private int reqGET_balance(String url) throws Exception {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        //add request header
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        con.setRequestProperty("Cookie", store.getUserCookie());

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine = in.readLine();
        in.close();

        responseCode = con.getResponseCode();
        if(responseCode == 200 ) {
            int coinBalance = ((Integer.parseInt(((((inputLine.split(Pattern.quote(",")))[2]).split(Pattern.quote(":")))[1]).replaceAll("\\s", ""))) / 1000);
            store.setUserBalance(coinBalance);
        }
        return responseCode;
    }


    // Returns the Set-Cookie header from post request with HttpURLConnection
    private String getCookie(HttpURLConnection urlCon) {
        return (urlCon.getHeaderField("Set-Cookie").split(Pattern.quote(";")))[0];
    }

    public int getResponseCode() {return responseCode;}
}