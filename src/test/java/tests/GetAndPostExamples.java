package tests;

import com.google.gson.JsonObject;
import io.restassured.RestAssured;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

public class GetAndPostExamples {

    @Test
    public void testGet()
    {
        baseURI = "https://reqres.in/api";
        given().
                get("/users?page=2")
                .then()
                .statusCode(200).
                body("data[1].first_name", equalTo("Lindsay")).
                body("data.first_name", hasItems("George","Rachel"));

    }

    @Test
    public void testPost()
    {

        //can be used like this
        /*
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("name", "Aditya");
        map.put("job", "leader");

        System.out.println(map);
        JSONObject request = new JSONObject(map);
        */
//        JSONObject request = new JSONObject();
        JsonObject request = new JsonObject();
        request.addProperty("name", "Aditya");
        request.addProperty("job", "leader");

        System.out.println(request);

        baseURI = "https://reqres.in/api";
        given().
                header("Content-type","application/json").
                header("abcd","text/plain").
                sessionId("1234").
                body(request.toString()).
                when().
                post("/users").
                then().
                statusCode(201).log().all();
    }
}
