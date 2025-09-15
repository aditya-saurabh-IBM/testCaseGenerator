package tests;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestsExamples {

    @Test
    public void test_1()
    {
        Response response = RestAssured.get("https://reqres.in/api/users?page=2");
        System.out.println("Response Code : "+response.getStatusCode());
        System.out.println("Response Time : "+response.getTime());
        System.out.println("Response as string : "+response.asString());
        System.out.println("Response status line : "+response.getStatusLine());
        System.out.println("Response headers : "+response.getHeader("Content-Type"));

        Assert.assertEquals(response.getStatusCode(),200);
    }
}
