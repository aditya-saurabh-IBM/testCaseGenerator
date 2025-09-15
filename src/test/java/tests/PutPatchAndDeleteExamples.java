package tests;

import com.google.gson.JsonObject;
import org.testng.annotations.Test;
import utils.RestAssuredUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;

import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.given;

public class PutPatchAndDeleteExamples {

    @Test
    public void testPut()
    {

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
                put("/users/2").
                then().
                statusCode(200).log().all();
    }

    @Test
    public void testPatch()
    {

        JsonObject request = new JsonObject();
        JsonObject request2 = new JsonObject();
        request.addProperty("name", "Aditya");
        request.addProperty("job", "leader");
        System.out.println(request);
        baseURI = "https://reqres.in";
//        given().
//                header("Content-type","application/json").
//                header("abcd","text/plain").
//                sessionId("1234").
//                body(request.toString()).
//                when().
//                patch("/api/users/2").
//                then().
//                statusCode(200).log().all();

        // --------------   take header as linkedHashMap to send multiple header at once ----------------
        // --------------   log everything of sending request and response ------------------------------
        LinkedHashMap<String,String> headerMap = new LinkedHashMap<>();
        headerMap.put("Content-type","text/xml");
        headerMap.put("SessionID","asdf3423");
        given().log().all().when().
                headers(headerMap).sessionId("2334").cookies(headerMap).body(request2.toString()).
                when().patch("/api/users/2").then().statusCode(200).log().all();


    }

    @Test
    public void testDelete()
    {
        baseURI = "https://reqres.in";
        given().
                when().
                delete("/api/users/2").
                then().
                statusCode(204).log().all();

    }
}
