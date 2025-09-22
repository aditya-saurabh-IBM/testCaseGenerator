package utils;

//import com.sun.jersey.api.client.Client;
//import com.sun.jersey.api.client.ClientResponse;
////import com.sun.jersey.api.client.WebResource;
////import com.sun.jersey.api.client.config.ClientConfig;
////import com.sun.jersey.core.util.MultivaluedMapImpl;
//import com.sun.jersey.core.util.MultivaluedMapImpl;
//import com.sun.jersey.multipart.FormDataMultiPart;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.response.ResponseBody;
//import org.json.JSONObject;
import steps.CommonSteps;
import steps.ScenarioHooks;

//import javax.ws.rs.core.MediaType;
//import javax.ws.rs.core.MultivaluedMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RestAssuredUtils_old {

    // private WebResource wResource;
    private Response response;
    private Map<String, String> apiResponseString;
    private static final String HEADER_STAGE = "stage00";
    private static String b2bSessionID = null;
    private static String csrfToken = null;
    // private ClientConfig basicClientConfig = null;
    private static final String loadBalancerCookieName = "route";
    public static Map<String, String> cookies;
    public static Map<String, String> cookies1;

    // CommonSteps commonSteps = new CommonSteps();
    // ScenarioHooks hooks = new ScenarioHooks();
    public enum HTTPMethod {
        GET, POST, PUT, DELETE, PATCH, IMPORT_POST
    }

    // static {
    // RestAssured.baseURI =
    // LoadEnvironment.tenant+"/wmapps/apps/"+LoadEnvironment.tname+"/partner-portal";
    // // Set your base URI here
    // }

    // public static Response sendGetRequest(String endpoint) {
    // return RestAssured.get(endpoint);
    // }

    // public static Response sendPostRequest(String endpoint, Object body) {
    // return RestAssured.given().body(body).post(endpoint);
    // }

    // public static Response sendPutRequest(String endpoint, Object body) {
    // return RestAssured.given().body(body).put(endpoint);
    // }

    public static Response sendDeleteRequest(String endpoint, LinkedHashMap cookie) {
        return RestAssured.given().contentType("application/json").cookies(cookie).delete(endpoint);
    }

    // Add methods for other HTTP methods as needed

    // Example usage:

    // public Response sendPostRequest(String endpoint,Object body) {
    //
    // if(body.toString().trim().isEmpty())
    // {
    // return
    // RestAssured.given().contentType("application/json").cookies(hooks.cookie).post(endpoint);
    // }
    // if(hooks.cookie==null)
    // {
    // System.out.println("--Cookie is empty--");
    // return
    // RestAssured.given().log().all().contentType("application/json").body(body).post(endpoint);
    // }
    // else
    // {
    // return
    // RestAssured.given().contentType("application/json").cookies(hooks.cookie).body(body).post(endpoint);
    // }
    //
    // }
    public Response ExecuteAPI(HTTPMethod methodType, String URI, String uname, String pwd, Object payload,
            String responseType, String fileName) {

        /**** Steps to handle HTTPS calls **/
        String csrfValue = getCSRFValue(uname, pwd);

        // MediaType payloadType = MediaType.APPLICATION_JSON_TYPE;
        // if (payload instanceof FormDataMultiPart) {
        // payloadType = MediaType.MULTIPART_FORM_DATA_TYPE;
        // }

        switch (methodType) {
            case GET: {
                response = RestAssured.given().urlEncodingEnabled(false).header("x-csrf-token", csrfValue)
                        .header("Accept", "application/json").log().all().cookies(cookies1).get(URI);
                break;
            }
            case POST: {
                response = RestAssured.given().contentType("application/json").header("x-csrf-token", csrfValue)
                        .header("Accept", "application/json").log().all().cookies(cookies1).body(payload).post(URI);
                break;
            }

            case PUT: {
                response = RestAssured.given().contentType("application/json").header("x-csrf-token", csrfValue)
                        .header("Accept", "application/json").log().all().cookies(cookies1).body(payload).put(URI);
                break;
            }
            case DELETE: {
                response = RestAssured.given().contentType("application/json").header("x-csrf-token", csrfValue)
                        .cookies(cookies1).delete(URI);
                break;

            }
            case PATCH: {
                response = RestAssured.given().contentType("application/json").header("x-csrf-token", csrfValue)
                        .header("Accept", "application/json").log().all().cookies(cookies1).body(payload).patch(URI);
                break;
            }
        }
        return response;
    }

    private String getCSRFValue(String userName, String password) {
        if (b2bSessionID == null || csrfToken == null) {
            b2bSessionID = getB2BSessionID(userName, password);
            csrfToken = getCSRFToken();
            System.out.println("B2BSessionID is not null. IF condition B2BSESSIONID: " + b2bSessionID + " CSRFTOKEN: "
                    + csrfToken);
        } else {
            System.out.println("B2BSessionID is not null. B2BSESSIONID: " + b2bSessionID + " CSRFTOKEN: " + csrfToken);
        }
        return csrfToken;
    }

    public Response sendPostRequest(String endpoint, LinkedHashMap cookie, Object body) {

        if (body.toString().trim().isEmpty()) {
            return RestAssured.given().contentType("application/json").header("Accept", "application/json")
                    .cookies(cookie).post(endpoint);
        }
        if (cookie == null) {
            System.out.println("--Cookie is empty--");
            return RestAssured.given().contentType("application/json").header("Accept", "application/json").log().all()
                    .body(body).post(endpoint);
        } else {
            return RestAssured.given().contentType("application/json").header("Accept", "application/json").log().all()
                    .cookies(cookie).body(body).post(endpoint);
        }

    }

    public Response sendGetRequest(String endpoint, LinkedHashMap cookie) {
        if (cookie.isEmpty()) {
            System.out.println("--Cookie is empty--");
            return RestAssured.given().urlEncodingEnabled(false).header("Accept", "application/json").log().all()
                    .get(endpoint);
        } else {
            return RestAssured.given().urlEncodingEnabled(false).header("Accept", "application/json").log().all()
                    .cookies(cookie).get(endpoint);
        }

    }

    public Response sendPUTRequest(String endpoint, LinkedHashMap cookie, Object body) {

        if (body.toString().trim().isEmpty()) {
            return RestAssured.given().contentType("application/json").header("Accept", "application/json").log().all()
                    .cookies(cookie).put(endpoint);
        }
        if (cookie == null) {
            System.out.println("--Cookie is empty--");
            return RestAssured.given().contentType("application/json").header("Accept", "application/json").log().all()
                    .body(body).put(endpoint);
        } else {
            return RestAssured.given().contentType("application/json").header("Accept", "application/json").log().all()
                    .cookies(cookie).body(body).put(endpoint);
        }

    }

    private String getB2BSessionID(String userName, String password) {
        String baseURI = LoadEnvironment.tenant;
        String loadBalancerCookie;

        RestAssured.config().getRedirectConfig().followRedirects(false);
        response = RestAssured.given().urlEncodingEnabled(false).header("Accept", "application/json").log().all()
                .redirects().follow(false).get(baseURI + "/b2b/");
        /*
         * loadBalancerCookie = APIResponseProvider.getCookies(loadBalancerCookieName,
         * response);
         */
        b2bSessionID = response.getCookie("b2bsessionid");
        cookies = response.getCookies();
        cookies1 = response.getCookies();
        String LoginURI2 = response.getHeader("Location");

        // Second API call

        response = RestAssured.given().urlEncodingEnabled(false).redirects().follow(false)
                .header("Accept", "application/json").log().all().cookies(cookies).get(LoginURI2);
        cookies = response.getCookies();

        String htmlResponse = response.getBody().asString();
        String sessionCode = getRegexValues("session_code=(.*?)&amp", htmlResponse);
        String execution = getRegexValues("execution=(.*?)&amp", htmlResponse);
        String tabId = getRegexValues("tab_id=(.*?)\"", htmlResponse);
        // String redirectCookie1 =
        // response.getCookies().get(1).toString().split(";")[0] + ";" +
        // response.getCookies().get(0).toString().split(";")[0];
        String RedirectURI1 = "https://" + LoadEnvironment.IDM + "/auth/realms/" + LoadEnvironment.tname
                + "/login-actions/authenticate?session_code=" + sessionCode + "&execution=" + execution
                + "&client_id=b2b&tab_id=" + tabId;
        System.out.println(RedirectURI1);
        // MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        // formData.add("username", userName);
        // formData.add("password", password);
        // response =
        // RestAssured.given().contentType("multipart/form-data").multiPart("username",
        // userName).multiPart("password",
        // password).redirects().follow(false).accept("application/json").header("Accept","application/json").log().all().cookies(cookies).post(RedirectURI1);
        response = RestAssured.given().contentType("application/x-www-form-urlencoded").formParam("username", userName)
                .formParam("password", password).redirects().follow(false).accept("application/json")
                .header("Accept", "application/json").log().all().cookies(cookies).post(RedirectURI1);
        cookies = response.getCookies();
        String RedirectURI2 = response.getHeader("Location");
        System.out.println("RedirectURI2 : " + RedirectURI2);

        response = RestAssured.given().contentType("application/json").redirects().follow(false)
                .accept("application/json").header("Accept", "application/json").header("Host", LoadEnvironment.host)
                .log().all().cookies(cookies1).get(RedirectURI2);
        cookies = response.getCookies();

        System.out.println("GetheaderLocationC :" + response.getHeaders());

        String RedirectURIb2b2 = response.getHeader("Location");
        System.out.println("RedirectURIb2b2 : " + RedirectURIb2b2);
        response = RestAssured.given().contentType("application/json").redirects().follow(false)
                .accept("application/json").header("Accept", "application/json").header("Host", LoadEnvironment.host)
                .log().all().cookies(cookies1).get(RedirectURIb2b2);
        System.out.println("response status" + response.getStatusCode());

        return b2bSessionID;

    }

    private String getRegexValues(String code, String htmlResponse) {
        Pattern p = Pattern.compile(code);
        Matcher m = p.matcher(htmlResponse);
        if (m.find()) {
            return m.group(1);
        } else {
            return "NomatchFound";
        }
    }

    private String getCSRFToken() {
        String baseURI = LoadEnvironment.tenant;
        response = RestAssured.given().contentType("application/json").accept("application/json")
                .header("Accept", "application/json").log().all().cookies(cookies1)
                .get(baseURI + "/b2b/isSessionActive");
        String csrftoken = response.then().extract().response().jsonPath().getString("csrfToken");
        System.out.println(csrftoken);
        return csrftoken;

    }

    // public static Response sendPutRequest(String endpoint, LinkedHashMap cookie,
    // Object body) {
    //
    // if (body.toString().trim().isEmpty()) {
    // return RestAssured.given().cookies(cookie).put(endpoint);
    // }
    // if (cookie.isEmpty()) {
    // return RestAssured.given().body(body).put(endpoint);
    // } else {
    // return RestAssured.given().cookies(cookie).body(body).put(endpoint);
    // }
    //
    // }
    // public static void main(String[] args) {
    // String endpoint = "/users/1";
    // Response response = sendGetRequest(endpoint);
    //
    // System.out.println("Response Code: " + response.getStatusCode());
    // System.out.println("Response Body: " + response.getBody().asString());
    // }

}