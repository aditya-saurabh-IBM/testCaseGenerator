package steps;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.*;
import utils.GenericMethods;
//import cucumber.api.Scenario;
//import cucumber.api.java.Before;
//import cucumber.api.java.After;
import utils.LoadEnvironment;
import utils.RestAssuredUtils_old;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

//import cucumber.api.java.en.Then;
//import cucumber.api.java.en.When;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.Assert;
import utils.LoadEnvironment;
import utils.RestAssuredUtils_old;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ScenarioHooks {
    LoadEnvironment loadEnvironment = new LoadEnvironment();
    RestAssuredUtils_old restAssuredUtils = new RestAssuredUtils_old();
    public static LinkedHashMap<String, String> cookie = new LinkedHashMap<>();
    Response response;
    String loginURL = LoadEnvironment.tenant + "/wmapps/core/login?legacy-error-format=true";
    Map<String, String> creds = new HashMap<>();
    GenericMethods genericMethods = new GenericMethods();

    @Before
    public void beforeAllScenarios(Scenario scenario) throws IOException, InterruptedException {
        // Code to run before all scenarios
        System.out.println("Executing before all scenarios");
        genericMethods.login_to_B2B_And_PP_REST();

        // System.out.println("login url : " + loginURL);
        // creds.put("password", loadEnvironment.ppPwd);
        // creds.put("userName", loadEnvironment.ppUsername);
        // response = restAssuredUtils.sendPostRequest(loginURL, cookie,
        // creds).then().statusCode(200).extract().response();
        // cookie.put("WMAPPSSID",
        // response.then().extract().response().jsonPath().getString("sessionToken").toString());
        // System.out.println("sessionToken : " +
        // response.then().extract().response().jsonPath().getString("sessionToken").toString());

        // System.out.println("Running scenario: " + scenario.getName());
        // System.out.println("Scenario ID: " + scenario.getId());

    }

    @After
    public void afterAllScenarios() {
        // Code to run after all scenarios
        System.out.println("Executing after all scenarios");
    }
}