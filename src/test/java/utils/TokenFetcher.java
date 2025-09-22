package utils;

import com.jayway.jsonpath.JsonPath;
import io.restassured.response.Response;

import java.io.IOException;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static utils.RestAssuredUtils.getCookie;

public class TokenFetcher {
        static Response response;
        static TokenStore tokenStore = new TokenStore();
        static GenericMethods genericMethods = new GenericMethods();
        public static String BASE_URL = "https://" + LoadEnvironment.host;
        public static String b2b_cookie = tokenStore.get_b2b_cookie();
        public static String b2b_username = tokenStore.get_B2B_creds("username");
        public static String b2b_password = tokenStore.get_B2B_creds("password");
        public static String b2b_csrf_token;
        public static Map<String, String> pp_creds = new HashMap<>();
        public static Map<String, String> pp_cookie = new HashMap<>();

        public static void loginAndGetB2bCookieAndTokens() throws IOException, InterruptedException {
                HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

                // 1st call
                System.out.println(" -------------------------  Started 1st call ---------------------------");

                HttpRequest.Builder requestBuilder = HttpRequest
                                .newBuilder()
                                .GET()
                                .header("Accept", "*/*")
                                // .header("Content-Type", "application/x-www-form-urlencoded")
                                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:60.0) Gecko")
                                .uri(URI.create(BASE_URL + "/b2b/"));

                HttpResponse<String> response = client.send(requestBuilder.build(),
                                HttpResponse.BodyHandlers.ofString());

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
                                // .header("Content-Type", "application/x-www-form-urlencoded")
                                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:60.0) Gecko")
                                .header("Cookie", b2bCookies)
                                .uri(URI.create(targetUri));

                response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

                targetUri = (response.headers().firstValue("location")).get();
                System.out.println("Status = " + response.statusCode());
                System.out.println("Target = " + targetUri);
                GetDomain result = extractDomainAndTenant(targetUri);
                System.out.println("IDM: " + result.getIDM());
                System.out.println("Tenant: " + result.getTenant());

                // 3rd call
                System.out.println(" -------------------------  Started 3rd call ---------------------------");
                requestBuilder = HttpRequest
                                .newBuilder()
                                .GET()
                                .header("Accept", "*/*")
                                // .header("Content-Type", "application/x-www-form-urlencoded")
                                .header("User-Agent",
                                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:60.0) Gecko/20100101 Firefox/60.0")
                                .uri(URI.create(targetUri));

                response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

                String htmlResponse = response.body();
                String sessionCode = getRegexValues("session_code=(.*?)&amp", htmlResponse);
                String execution = getRegexValues("execution=(.*?)&amp", htmlResponse);
                String tabId = getRegexValues("tab_id=(.*?)\"", htmlResponse);
                List<String> cookiesList = response.headers().allValues("set-cookie");
                String targetCookie = "AUTH_SESSION_ID_LEGACY=" + getCookie(cookiesList, "AUTH_SESSION_ID_LEGACY")
                                + ";KC_RESTART=" + getCookie(cookiesList, "KC_RESTART");

                targetUri = "https://" + result.getIDM() + "/auth/realms/" + result.getTenant()
                                + "/login-actions/authenticate?session_code=" + sessionCode + "&execution=" + execution
                                + "&client_id=b2b&tab_id=" + tabId;
                System.out.println("Status = " + response.statusCode());
                System.out.println("targetCookie = " + targetCookie);
                System.out.println("Target = " + targetUri);

                // 4th call
                System.out.println(" -------------------------  Started 4th call ---------------------------");
                Map<String, String> parameters = new HashMap<>();

                System.out.println("B2B Username : " + b2b_username);
                System.out.println("B2B Password : " + b2b_password);

                parameters.put("username", b2b_username);
                parameters.put("password", b2b_password);

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

                System.out.println("response : " + response.headers().map());

                targetUri = (response.headers().firstValue("location")).get();
                System.out.println("Status = " + response.statusCode());
                System.out.println("Target = " + targetUri);

                // 5th call
                System.out.println(" -------------------------  Started 5th call ---------------------------");

                requestBuilder = HttpRequest
                                .newBuilder()
                                .GET()
                                .header("Accept", "*/*")
                                // .header("Content-Type", "application/x-www-form-urlencoded")
                                .header("Cookie", b2bCookies)
                                .header("User-Agent", "Apache-HttpClient/4.5.12 (Java/11.0.15)")
                                .uri(URI.create(targetUri));

                response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

                String newCookies = response.headers().firstValue("set-cookie").get();
                tokenStore.set_b2b_cookie(replaceCookies(allCookies, newCookies));
                b2b_cookie = tokenStore.get_b2b_cookie();

                System.out.println("Status = " + response.statusCode());
                // System.out.println("Headers = " + response.headers());
                String targetB2BLocation = (response.headers().firstValue("location")).get();
                System.out.println("Target = " + targetB2BLocation);

                // 6th call
                System.out.println(" -------------------------  Started 6th call ---------------------------");

                requestBuilder = HttpRequest
                                .newBuilder()
                                .GET()
                                .header("Accept", "*/*")
                                .header("User-Agent", "Apache-HttpClient/4.5.12 (Java/11.0.15)")
                                .header("Cookie", b2b_cookie)
                                .uri(URI.create(targetB2BLocation));

                response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
                System.out.println("Status = " + response.statusCode());
                // System.out.println("Headers = " + response.headers());
                targetB2BLocation = (response.headers().firstValue("location")).get();
                System.out.println("Target = " + targetB2BLocation);
                System.out.println("b2b_cookie : " + b2b_cookie);

                // 7th call
                System.out.println(" -------------------------  Started 7th call ---------------------------");

                requestBuilder = HttpRequest
                                .newBuilder()
                                .GET()
                                .header("Accept", "*/*")
                                .header("User-Agent", "Apache-HttpClient/4.5.12 (Java/11.0.15)")
                                .header("Cookie", b2b_cookie)
                                .uri(URI.create(targetB2BLocation));

                response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

                System.out.println(" -------------------------  Started CSRF call ---------------------------");
                requestBuilder = HttpRequest
                                .newBuilder()
                                .uri(URI.create(BASE_URL + "/b2b/isSessionActive"))
                                .GET()
                                .header("Accept", "application/json")
                                .header("Content-Type", "application/json")
                                .headers("Cookie", b2b_cookie)
                                .header("User-Agent",
                                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36");

                response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

                System.out.println("B2B TOKEN RESPONSE: " + response.body());
                String jsonString = response.body();
                tokenStore.set_b2b_csrfToken(JsonPath.read(jsonString, "$.csrfToken"));
                b2b_csrf_token = tokenStore.get_b2b_csrfToken();
                System.out.println("b2b_cookie : " + b2b_cookie);
                System.out.println("csrfTokens : " + b2b_csrf_token);
        }

        private static String getAllCookies(List<String> cookies) {
                // List<HttpCookie> httpCookies = HttpCookie.parse(String.join(", ", cookies));
                StringBuilder stringBuilder = new StringBuilder();
                for (String c : cookies) {
                        List<HttpCookie> httpCookies = HttpCookie.parse(c);
                        stringBuilder.append(httpCookies.get(0).getName()).append("=")
                                        .append(httpCookies.get(0).getValue()).append(";");
                }
                return stringBuilder.substring(0, stringBuilder.length() - 1);
        }

        private static String getRegexValues(String code, String htmlResponse) {
                Pattern p = Pattern.compile(code);
                Matcher m = p.matcher(htmlResponse);
                if (m.find()) {
                        return m.group(1);
                } else {
                        return "NomatchFound";
                }
        }

        private static String replaceCookies(List<String> existingCookies, String newCookies) {
                StringBuilder stringBuilder = new StringBuilder();
                List<HttpCookie> httpCookies = HttpCookie.parse(newCookies);
                for (String c : existingCookies) {
                        List<HttpCookie> cookies = HttpCookie.parse(c);
                        if (cookies.get(0).getName().equals(httpCookies.get(0).getName())) {
                                stringBuilder.append(httpCookies.get(0).getName()).append("=")
                                                .append(httpCookies.get(0).getValue()).append(";");
                        } else {
                                stringBuilder.append(cookies.get(0).getName()).append("=")
                                                .append(cookies.get(0).getValue()).append(";");
                        }
                }
                return stringBuilder.substring(0, stringBuilder.length() - 1);
        }

        public static void loginAndGetPPTokens() {
                pp_creds = tokenStore.get_pp_creds();
                String loginURL = BASE_URL + "/wmapps/core/login?legacy-error-format=true";
                response = genericMethods.PP_POST_Request(loginURL, "", pp_creds).then().statusCode(200).extract()
                                .response();
                response.getBody().prettyPrint();
                tokenStore.set_pp_cookie(
                                response.then().extract().response().jsonPath().getString("sessionToken").toString());
                System.out.println("sessionToken : "
                                + response.then().extract().response().jsonPath().getString("sessionToken").toString());
                pp_cookie = tokenStore.get_pp_cookie();
        }

        private static GetDomain extractDomainAndTenant(String url) {
                // Regular expression to extract the domain and tenant
                String regex = "https://([\\w.-]+)/auth/realms/([\\w-]+)/";

                // Compile the pattern and match it against the URL
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(url);

                if (matcher.find()) {
                        String idm = matcher.group(1); // Extracts the domain
                        String tenant = matcher.group(2); // Extracts the tenant

                        return new GetDomain(idm, tenant);
                }
                return null;
        }

        public static void loginAndGetPOCookieAndTokens() throws IOException, InterruptedException {
                HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
                // 1st call
                System.out.println(" -------------------------  Started 1st PO call ---------------------------");

                HttpRequest.Builder requestBuilder = HttpRequest
                                .newBuilder()
                                .GET()
                                .header("Accept", "*/*")
                                // .header("Content-Type", "application/x-www-form-urlencoded")
                                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:60.0) Gecko")
                                .uri(URI.create(BASE_URL + "/wmapps/sso/login"));

                System.out.println("targetUri1 : " + BASE_URL + "/wmapps/sso/login");

                HttpResponse<String> response = client.send(requestBuilder.build(),
                                HttpResponse.BodyHandlers.ofString());

                List<String> allCookies = response.headers().allValues("set-cookie");
                String targetUri = (response.headers().firstValue("location")).get();
                String b2bCookies = getAllCookies(allCookies);
                // System.out.println("Status = " + response.statusCode());
                // System.out.println("B2BCookies = " + b2bCookies);

                // 2nd call
                System.out.println(" -------------------------  Started 2nd PO call ---------------------------");

                requestBuilder = HttpRequest
                                .newBuilder()
                                .GET()
                                .header("Accept", "*/*")
                                // .header("Content-Type", "application/x-www-form-urlencoded")
                                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:60.0) Gecko")
                                .header("Cookie", b2bCookies)
                                .uri(URI.create(targetUri + "?doneURL=/wmapps/apps/awdevtenant1/partner/"));
                System.out.println("targetUri2 = " + targetUri + "?doneURL=/wmapps/apps/awdevtenant1/partner/");

                response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

                targetUri = (response.headers().firstValue("location")).get();
                System.out.println("Status = " + response.statusCode());
                System.out.println("Target = " + targetUri);

                System.out.println(" -------------------------  Started 3rd PO call ---------------------------");

                requestBuilder = HttpRequest
                                .newBuilder()
                                .GET()
                                .header("Accept", "*/*")
                                // .header("Content-Type", "application/x-www-form-urlencoded")
                                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:60.0) Gecko")
                                .header("Cookie", b2bCookies)
                                .uri(URI.create(targetUri));
                System.out.println("targetUri3 = " + targetUri);

                response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

                targetUri = (response.headers().firstValue("location")).get();
                System.out.println("Status = " + response.statusCode());

                // 3rd call
                System.out.println(" -------------------------  Started 4th PO call ---------------------------");
                requestBuilder = HttpRequest
                                .newBuilder()
                                .GET()
                                .header("Accept", "*/*")
                                // .header("Content-Type", "application/x-www-form-urlencoded")
                                .header("User-Agent",
                                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:60.0) Gecko/20100101 Firefox/60.0")
                                .uri(URI.create(targetUri));

                System.out.println("targetUri4 = " + targetUri);
                GetDomain result = extractDomainAndTenant(targetUri);
                System.out.println("IDM: " + result.getIDM());
                System.out.println("Tenant: " + result.getTenant());

                response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

                String htmlResponse = response.body();
                String sessionCode = getRegexValues("session_code=(.*?)&amp", htmlResponse);
                String execution = getRegexValues("execution=(.*?)&amp", htmlResponse);
                String tabId = getRegexValues("tab_id=(.*?)\"", htmlResponse);
                List<String> cookiesList = response.headers().allValues("set-cookie");
                String targetCookie = "AUTH_SESSION_ID_LEGACY=" + getCookie(cookiesList, "AUTH_SESSION_ID_LEGACY")
                                + ";KC_RESTART=" + getCookie(cookiesList, "KC_RESTART");

                targetUri = "https://" + result.getIDM() + "/auth/realms/" + result.getTenant()
                                + "/login-actions/authenticate?session_code=" + sessionCode + "&execution=" + execution
                                + "&client_id=agileapps&tab_id=" + tabId;
                System.out.println("Status = " + response.statusCode());

                // 4th call
                System.out.println(" -------------------------  Started 5th PO call ---------------------------");
                Map<String, String> parameters = new HashMap<>();
                parameters.put("username", LoadEnvironment.B2BUser);
                parameters.put("password", LoadEnvironment.B2BPwd);

                System.out.println("targetCookie = " + targetCookie);
                System.out.println("targetURI5 = " + targetUri);

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

                System.out.println("response header :: " + response.headers());
                System.out.println("response body :: " + response.body());

                targetUri = (response.headers().firstValue("location")).get();
                System.out.println("Status = " + response.statusCode());
                System.out.println("Target = " + targetUri);

                // 5th call
                System.out.println(" -------------------------  Started 6th PO call ---------------------------");

                requestBuilder = HttpRequest
                                .newBuilder()
                                .GET()
                                .header("Accept", "*/*")
                                // .header("Content-Type", "application/x-www-form-urlencoded")
                                .header("Cookie", b2bCookies)
                                .header("User-Agent", "Apache-HttpClient/4.5.12 (Java/11.0.15)")
                                .uri(URI.create(targetUri));

                response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

                String newCookies = response.headers().firstValue("set-cookie").get();
                String wmappssid = "";
                String prefix = "WMAPPSSID=";
                int start = newCookies.indexOf(prefix);
                if (start != -1) {
                        start += prefix.length();
                        int end = newCookies.indexOf(";", start);
                        wmappssid = (end != -1) ? newCookies.substring(start, end) : newCookies.substring(start);

                } else {
                        System.out.println("WMAPPSSID not found.");
                }
                tokenStore.set_po_cookie(wmappssid);
                System.out.println("tokenStore.get_po_cookie() :: " + tokenStore.get_po_cookie());

                System.out.println("newCookies :: " + newCookies);
                String newPOCookies = replaceCookies(allCookies, newCookies);

                System.out.println("Status = " + response.statusCode());
                String targetPOLocation = (response.headers().firstValue("location")).get();
                System.out.println("Target = " + targetPOLocation);

                // 6th call
                System.out.println(" -------------------------  Started 7th PO call ---------------------------");

                requestBuilder = HttpRequest
                                .newBuilder()
                                .GET()
                                .header("Accept", "*/*")
                                .header("User-Agent", "Apache-HttpClient/4.5.12 (Java/11.0.15)")
                                .header("Cookie", newPOCookies)
                                .uri(URI.create(targetPOLocation));

                response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

                System.out.println("Status = " + response.statusCode());

        }
}
