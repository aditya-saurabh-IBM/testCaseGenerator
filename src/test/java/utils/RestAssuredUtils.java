package utils;

import com.jayway.jsonpath.JsonPath;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.io.IOException;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class RestAssuredUtils {


    //    private WebResource wResource;
    private Response response;
    private Response response_csrf;
    private Map<String, String> apiResponseString;
    private static final String HEADER_STAGE = "stage00";
    private static String b2bSessionID = null;
    private static String csrfToken = null;
    //    private ClientConfig basicClientConfig = null;
    private static final String loadBalancerCookieName = "route";
    //    public static Map<String, String> cookies;
    public static Map<String, String> cookies;
    ;
    public static Map<String, String> cookies1;
    public Map<String, String> cookies2 = new HashMap<>();
    public static String cookies_String;
    public static String cookies_String2;
    public static String cookies_new_sessionID;
    public static String  newB2BCookies;
    private static Map<String, String> csrfTokens = new HashMap<>();
    private static Map<String, String> sessionCookies = new HashMap<>();

    //    CommonSteps commonSteps = new CommonSteps();
//     ScenarioHooks hooks = new ScenarioHooks();
    public enum HTTPMethod {
        GET, POST, PUT, DELETE, PATCH, IMPORT_POST
    }

//    static {
//        RestAssured.baseURI = LoadEnvironment.tenant+"/wmapps/apps/"+LoadEnvironment.tname+"/partner-portal"; // Set your base URI here
//    }

//    public static Response sendGetRequest(String endpoint) {
//        return RestAssured.get(endpoint);
//    }

//    public static Response sendPostRequest(String endpoint, Object body) {
//        return RestAssured.given().body(body).post(endpoint);
//    }

//    public static Response sendPutRequest(String endpoint, Object body) {
//        return RestAssured.given().body(body).put(endpoint);
//    }

    public static Response sendDeleteRequest(String endpoint, LinkedHashMap cookie) {
        return RestAssured.given().contentType("application/json").cookies(cookie).delete(endpoint);
    }

    // Add methods for other HTTP methods as needed

    // Example usage:

    //    public  Response sendPostRequest(String endpoint,Object body) {
//
//        if(body.toString().trim().isEmpty())
//        {
//            return RestAssured.given().contentType("application/json").cookies(hooks.cookie).post(endpoint);
//        }
//        if(hooks.cookie==null)
//        {
//            System.out.println("--Cookie is empty--");
//            return RestAssured.given().log().all().contentType("application/json").body(body).post(endpoint);
//        }
//        else
//        {
//            return RestAssured.given().contentType("application/json").cookies(hooks.cookie).body(body).post(endpoint);
//        }
//
//    }
    public Response ExecuteAPI(HTTPMethod methodType, String URI, String uname, String pwd, Object payload,
                               String responseType, String fileName) throws URISyntaxException, InterruptedException, IOException {


        /**** Steps to handle HTTPS calls **/
        String csrfValue = getCSRFValue(uname, pwd);

//        MediaType payloadType = MediaType.APPLICATION_JSON_TYPE;
//        if (payload instanceof FormDataMultiPart) {
//            payloadType = MediaType.MULTIPART_FORM_DATA_TYPE;
//        }


        switch (methodType) {
            case GET: {
                System.out.println("New B2b Cookies : "+ newB2BCookies);
                System.out.println("csrfValue : " + csrfValue);
                response = RestAssured.given().urlEncodingEnabled(false)
                        .contentType("application/json")
                        .header("Accept", "application/json")
                        .header("X-CSRF-TOKEN", csrfValue)
                        .header("Cookie", newB2BCookies)
                        .log().all()
                        .get(URI);
                break;
            }
            case POST: {
                response = RestAssured.given().contentType("application/json").header("x-csrf-token", csrfValue).header("Accept", "application/json").log().all().cookies(cookies1).body(payload).post(URI);
                break;
            }

            case PUT: {
                response = RestAssured.given().contentType("application/json").header("x-csrf-token", csrfValue).header("Accept", "application/json").log().all().cookies(cookies1).body(payload).put(URI);
                break;
            }
            case DELETE: {
                response = RestAssured.given().contentType("application/json").header("x-csrf-token", csrfValue).cookies(cookies1).delete(URI);
                break;

            }
            case PATCH: {
                response = RestAssured.given().contentType("application/json").header("x-csrf-token", csrfValue).header("Accept", "application/json").log().all().cookies(cookies1).body(payload).patch(URI);
                break;
            }
        }
        return response;
    }


    private String getCSRFValue(String userName, String password) throws URISyntaxException, InterruptedException, IOException {
        if (b2bSessionID == null || csrfToken == null) {
            b2bSessionID = getB2BSessionID(userName, password);
            csrfToken = getCSRFToken();
            System.out.println("B2BSessionID is not null. IF condition B2BSESSIONID: " + b2bSessionID + " CSRFTOKEN: " + csrfToken);
        } else {
            System.out.println("B2BSessionID is not null. B2BSESSIONID: " + b2bSessionID + " CSRFTOKEN: " + csrfToken);
        }
        return csrfToken;
    }

    public Response sendPostRequest(String endpoint, LinkedHashMap cookie, Object body) {

        if (body.toString().trim().isEmpty()) {
            return RestAssured.given().contentType("application/json").header("Accept", "application/json").cookies(cookie).post(endpoint);
        }
        if (cookie == null) {
            System.out.println("--Cookie is empty--");
            return RestAssured.given().contentType("application/json").header("Accept", "application/json").log().all().body(body).post(endpoint);
        } else {
            return RestAssured.given().contentType("application/json").header("Accept", "application/json").log().all().cookies(cookie).body(body).post(endpoint);
        }

    }

    public Response sendGetRequest(String endpoint, LinkedHashMap cookie) {
        if (cookie.isEmpty()) {
            System.out.println("--Cookie is empty--");
            return RestAssured.given().urlEncodingEnabled(false).header("Accept", "application/json").log().all().get(endpoint);
        } else {
            return RestAssured.given().urlEncodingEnabled(false).header("Accept", "application/json").log().all().cookies(cookie).get(endpoint);
        }

    }

    public Response sendPUTRequest(String endpoint, LinkedHashMap cookie, Object body) {

        if (body.toString().trim().isEmpty()) {
            return RestAssured.given().contentType("application/json").header("Accept", "application/json").log().all().cookies(cookie).put(endpoint);
        }
        if (cookie == null) {
            System.out.println("--Cookie is empty--");
            return RestAssured.given().contentType("application/json").header("Accept", "application/json").log().all().body(body).put(endpoint);
        } else {
            return RestAssured.given().contentType("application/json").header("Accept", "application/json").log().all().cookies(cookie).body(body).put(endpoint);
        }

    }

    private String getB2BSessionID(String userName, String password) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

        String BASE_URL = "https://"+LoadEnvironment.host;
        // 1st call
        System.out.println(" -------------------------  Started 1st call ---------------------------");

        HttpRequest.Builder requestBuilder = HttpRequest
                .newBuilder()
                .GET()
                .header("Accept", "*/*")
                //.header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:60.0) Gecko")
                .uri(URI.create(BASE_URL + "/b2b/"));

        HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

        List<String> allCookies = response.headers().allValues("set-cookie");
        String targetUri = (response.headers().firstValue("location")).get();
        String b2bCookies = getAllCookies(allCookies);
        System.out.println("Status = " + response.statusCode());
        System.out.println("Target = " + targetUri);
        System.out.println("B2BCookies = " + b2bCookies);

        // 2nd call
        System.out.println(" -------------------------  Started 2nd call ---------------------------");

        requestBuilder = HttpRequest
                .newBuilder()
                .GET()
                .header("Accept", "*/*")
                //.header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:60.0) Gecko")
                .header("Cookie", b2bCookies)
                .uri(URI.create(targetUri));

        response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

        targetUri = (response.headers().firstValue("location")).get();
        System.out.println("Status = " + response.statusCode());
        System.out.println("Target = " + targetUri);


        // 3rd call
        System.out.println(" -------------------------  Started 3rd call ---------------------------");
        requestBuilder = HttpRequest
                .newBuilder()
                .GET()
                .header("Accept", "*/*")
                //.header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:60.0) Gecko/20100101 Firefox/60.0")
                .uri(URI.create(targetUri));

        response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

        String htmlResponse = response.body();
        String sessionCode = getRegexValues("session_code=(.*?)&amp", htmlResponse);
        String execution = getRegexValues("execution=(.*?)&amp", htmlResponse);
        String tabId = getRegexValues("tab_id=(.*?)\"", htmlResponse);
        List<String> cookiesList = response.headers().allValues("set-cookie");
        String targetCookie = "AUTH_SESSION_ID_LEGACY=" + getCookie(cookiesList, "AUTH_SESSION_ID_LEGACY") + ";KC_RESTART=" + getCookie(cookiesList, "KC_RESTART");

        targetUri = "https://" + LoadEnvironment.IDM + "/auth/realms/" + LoadEnvironment.tname + "/login-actions/authenticate?session_code=" + sessionCode + "&execution=" + execution + "&client_id=b2b&tab_id=" + tabId;
        System.out.println("Status = " + response.statusCode());
//        System.out.println("Cookie = " + targetCookie);
        System.out.println("Target = " + targetUri);

        // 4th call
        System.out.println(" -------------------------  Started 4th call ---------------------------");
        Map<String, String> parameters = new HashMap<>();
        parameters.put("username", userName);
        parameters.put("password", password);

        String formData = parameters.entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        requestBuilder = HttpRequest
                .newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .header("Accept", "*/*")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Cookie", targetCookie)
                .header("User-Agent", "Apache-HttpClient/4.5.12 (Java/11.0.15)")
                .uri(URI.create(targetUri));

        response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

        targetUri = (response.headers().firstValue("location")).get();
        System.out.println("Status = " + response.statusCode());
        System.out.println("Target = " + targetUri);

        // 5th call
        System.out.println(" -------------------------  Started 5th call ---------------------------");

        requestBuilder = HttpRequest
                .newBuilder()
                .GET()
                .header("Accept", "*/*")
                //.header("Content-Type", "application/x-www-form-urlencoded")
                .header("Cookie", b2bCookies)
                .header("User-Agent", "Apache-HttpClient/4.5.12 (Java/11.0.15)")
                .uri(URI.create(targetUri));

        response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());


        String newCookies = response.headers().firstValue("set-cookie").get();
        newB2BCookies = replaceCookies(allCookies, newCookies);

        System.out.println("Status = " + response.statusCode());
//        System.out.println("Headers = " + response.headers());
        String targetB2BLocation = (response.headers().firstValue("location")).get();
        System.out.println("Target = " + targetB2BLocation);

        // 6th call
        System.out.println(" -------------------------  Started 6th call ---------------------------");

        requestBuilder = HttpRequest
                .newBuilder()
                .GET()
                .header("Accept", "*/*")
                .header("User-Agent", "Apache-HttpClient/4.5.12 (Java/11.0.15)")
                .header("Cookie", newB2BCookies)
                .uri(URI.create(targetB2BLocation));

        response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        System.out.println("Status = " + response.statusCode());
//        System.out.println("Headers = " + response.headers());
        targetB2BLocation = (response.headers().firstValue("location")).get();
        System.out.println("Target = " + targetB2BLocation);
        b2bSessionID=newB2BCookies;
        System.out.println("newB2BCookies : "+ newB2BCookies);

        // 7th call
        System.out.println(" -------------------------  Started 7th call ---------------------------");

        requestBuilder = HttpRequest
                .newBuilder()
                .GET()
                .header("Accept", "*/*")
                .header("User-Agent", "Apache-HttpClient/4.5.12 (Java/11.0.15)")
                .header("Cookie", newB2BCookies)
                .uri(URI.create(targetB2BLocation));

//        response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
//
//        // CSRF Token
//        String jsonString = getCSRFToken(newB2BCookies, BASE_URL);
//        String csrfToken = JsonPath.read(jsonString, "$.csrfToken");
//
//        csrfTokens.put(BASE_URL, csrfToken);
//        sessionCookies.put(BASE_URL, newB2BCookies);
//        // Store tokens in TokenStore
//        TokenStore.getInstance().setTokens(BASE_URL, csrfToken, newB2BCookies, wmio_Token, wmioSession);
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
    private static String getAllCookies(List<String> cookies) {
        //List<HttpCookie> httpCookies = HttpCookie.parse(String.join(", ", cookies));
        StringBuilder stringBuilder = new StringBuilder();
        for (String c : cookies) {
            List<HttpCookie> httpCookies = HttpCookie.parse(c);
            stringBuilder.append(httpCookies.get(0).getName()).append("=").append(httpCookies.get(0).getValue()).append(";");
        }
        return stringBuilder.substring(0, stringBuilder.length() - 1);
    }

    private String getCSRFToken() throws InterruptedException, IOException {
        String BASE_URL = "https://"+LoadEnvironment.host;
       HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        System.out.println(" -------------------------  Started CSRF call ---------------------------");
        HttpRequest.Builder requestBuilder = HttpRequest
                .newBuilder()
                .uri(URI.create(BASE_URL + "/b2b/isSessionActive"))
                .GET()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .headers("Cookie", newB2BCookies)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36");

        HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        System.out.println("B2B TOKEN RESPONSE: " + response.body());
        String jsonString = response.body();
        csrfToken = JsonPath.read(jsonString, "$.csrfToken");

        System.out.println("Exiting from CSRF Token");

        return csrfToken;

    }
static String getCookie(List<String> cookies, String name) {
    List<HttpCookie> httpCookies = HttpCookie.parse(String.join(", ", cookies));
    return httpCookies.stream().filter(cookie -> cookie.getName().equals(name)).findFirst().map(cookie -> cookie.getValue()).orElse(null);
}

    private static String replaceCookies(List<String> existingCookies, String newCookies) {
        StringBuilder stringBuilder = new StringBuilder();
        List<HttpCookie> httpCookies = HttpCookie.parse(newCookies);
        for (String c : existingCookies) {
            List<HttpCookie> cookies = HttpCookie.parse(c);
            if (cookies.get(0).getName().equals(httpCookies.get(0).getName())) {
                stringBuilder.append(httpCookies.get(0).getName()).append("=").append(httpCookies.get(0).getValue()).append(";");
            } else {
                stringBuilder.append(cookies.get(0).getName()).append("=").append(cookies.get(0).getValue()).append(";");
            }
        }
        return stringBuilder.substring(0, stringBuilder.length() - 1);
    }
}