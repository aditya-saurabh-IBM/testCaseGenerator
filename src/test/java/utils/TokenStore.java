package utils;

import java.util.HashMap;
import java.util.Map;

/////////////////   This class contains getters and setter of B2B and Appdev tokens ////////////////////
public class TokenStore {

    public static Map<String, String> pp_creds = new HashMap<>();
    public static Map<String, String> b2b_creds = new HashMap<>();
    public static Map<String, String> pp_cookie = new HashMap<>();
    public static HashMap<String, String> po_cookie = new HashMap<>();
    public static String b2b_cookie;
    public static String b2b_csrfToken;


    public void set_pp_creds() {
        pp_creds.put("username", LoadEnvironment.ppUsername);
        pp_creds.put("password", LoadEnvironment.ppPwd);
    }
    public void set_pp_creds(String username,String password) {
        pp_creds.put("username", username);
        pp_creds.put("password", password);
    }

    public void set_B2B_creds() {

        b2b_creds.put("username", LoadEnvironment.B2BUser);
        b2b_creds.put("password", LoadEnvironment.B2BPwd);
    }

    public String get_B2B_creds(String parameter) {
        return b2b_creds.get(parameter);
    }

    public Map<String, String> get_pp_creds() {
        return pp_creds;
    }

    public void set_b2b_cookie(String cookie) {
        b2b_cookie = cookie;

    }

    public String get_b2b_cookie() {
        return b2b_cookie;
    }

    public void set_b2b_csrfToken(String csrf) {
        b2b_csrfToken = csrf;

    }

    public String get_b2b_csrfToken() {
        return b2b_csrfToken;
    }

    public void set_pp_cookie(String sessionID) {
        pp_cookie.put("WMAPPSSID", sessionID);

    }

    public Map<String, String> get_pp_cookie() {
        return pp_cookie;
    }

    public String get_pp_cookie_string() {
        return "WMAPPSSID=" + pp_cookie.get("WMAPPSSID");
    }

    public void set_po_cookie(String sessionID) {
        po_cookie.put("WMAPPSSID", sessionID);

    }

    public Map<String, String> get_po_cookie() {
        return po_cookie;
    }
}
