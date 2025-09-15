package framework;

import io.restassured.RestAssured;

public class TestBase {
    public TestBase() {
        RestAssured.baseURI = "https://google.com";
        // Add any other common configurations here
    }
}