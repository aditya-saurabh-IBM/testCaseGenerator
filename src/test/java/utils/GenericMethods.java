package utils;
//////////////////////only REST calls ... login to some here

import io.restassured.RestAssured;
import io.restassured.response.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GenericMethods {

    Response response;
    Response responseFromGET;
    TokenStore tokenStore = new TokenStore();

    public Response B2B_GET_Request(String endpoint) {
        response = RestAssured.given().urlEncodingEnabled(false)
                .contentType("application/json")
                .header("Accept", "application/json")
                .header("X-CSRF-TOKEN", TokenFetcher.b2b_csrf_token)
                .header("Cookie", TokenFetcher.b2b_cookie)
                .log().all()
                .get(endpoint);

        return response;
    }

    public Response B2B_POST_Request(String endpoint, Object payload) {
        response = RestAssured.given().urlEncodingEnabled(false)
                .contentType("application/json")
                .header("x-csrf-token", TokenFetcher.b2b_csrf_token)
                .header("Accept", "application/json")
                .log().all()
                .header("Cookie", TokenFetcher.b2b_cookie)
                .body(payload)
                .post(endpoint);

        return response;
    }

    public Response B2B_POST_Request(String endpoint) {
        response = RestAssured.given().urlEncodingEnabled(false)
                .contentType("application/json")
                .header("x-csrf-token", TokenFetcher.b2b_csrf_token)
                .header("Accept", "application/json")
                .log().all()
                .header("Cookie", TokenFetcher.b2b_cookie)
                .post(endpoint);

        return response;
    }

    public Response B2B_PUT_Request(String endpoint, Object payload) {
        response = RestAssured.given().urlEncodingEnabled(false)
                .contentType("application/json")
                .header("x-csrf-token", TokenFetcher.b2b_csrf_token)
                .header("Accept", "application/json")
                .log().all()
                .header("Cookie", TokenFetcher.b2b_cookie)
                .body(payload)
                .put(endpoint);

        return response;
    }

    public Response B2B_PATCH_Request(String endpoint, Object payload) {
        response = RestAssured.given().urlEncodingEnabled(false)
                .contentType("application/json")
                .header("x-csrf-token", TokenFetcher.b2b_csrf_token)
                .header("Accept", "application/json")
                .log().all()
                .header("Cookie", TokenFetcher.b2b_cookie)
                .body(payload)
                .patch(endpoint);

        return response;
    }

    public Response B2B_DELETE_Request(String endpoint) {
        response = RestAssured.given()
                .contentType("application/json")
                .header("x-csrf-token", TokenFetcher.b2b_csrf_token)
                .header("Cookie", TokenFetcher.b2b_cookie)
                .delete(endpoint);

        return response;
    }

    public Response PP_POST_Request(String endpoint, String cookie, Object body) {

        if (body.toString().trim().isEmpty()) {
            return RestAssured.given().contentType("application/json")
                    .header("Accept", "application/json")
                    .header("Cookie", cookie)
                    .post(endpoint);
        }
        if (cookie == null || cookie.isEmpty()) {
            System.out.println("--Cookie is empty--");
            return RestAssured.given().contentType("application/json")
                    .header("Accept", "application/json")
                    .log().all()
                    .body(body)
                    .post(endpoint);
        } else {
            return RestAssured.given().contentType("application/json")
                    .header("Accept", "application/json")
                    .log().all()
                    .cookies(TokenFetcher.pp_cookie)
                    .body(body)
                    .post(endpoint);
        }

    }

    public Response PP_POST_Request(String endpoint, Object body) {

        if (body.toString().trim().isEmpty()) {
            return RestAssured.given().contentType("application/json")
                    .header("Accept", "application/json")
                    .header("Cookie", TokenFetcher.pp_cookie)
                    .post(endpoint);
        } else {
            return RestAssured.given().contentType("application/json")
                    .header("Accept", "application/json")
                    .log().all()
                    .cookies(TokenFetcher.pp_cookie)
                    .body(body)
                    .post(endpoint);
        }

    }

    public Response PP_GET_Request(String endpoint) {

        return RestAssured.given().urlEncodingEnabled(false)
                .header("Accept", "application/json")
                .log().all()
                .cookies(TokenFetcher.pp_cookie)
                .get(endpoint);

    }

    public Response PP_PUT_Request(String endpoint, Object body) {

        if (body.toString().trim().isEmpty()) {
            return RestAssured.given()
                    .contentType("application/json")
                    .header("Accept", "application/json")
                    .log().all()
                    .cookies(TokenFetcher.pp_cookie)
                    .put(endpoint);
        } else {
            return RestAssured.given()
                    .contentType("application/json")
                    .header("Accept", "application/json")
                    .log().all()
                    .cookies(TokenFetcher.pp_cookie)
                    .body(body)
                    .put(endpoint);
        }

    }

    public Response PP_DELETE_Request(String endpoint) {
        return RestAssured.given()
                .contentType("application/json")
                .cookies(TokenFetcher.pp_cookie)
                .delete(endpoint);
    }

    public void login_to_B2B_And_PP_REST() throws IOException, InterruptedException {
        // tokenStore.set_pp_creds();
        tokenStore.set_B2B_creds();
        TokenFetcher.loginAndGetB2bCookieAndTokens();
        // TokenFetcher.loginAndGetPOCookieAndTokens();
        // TokenFetcher.loginAndGetPPTokens();
    }
}
