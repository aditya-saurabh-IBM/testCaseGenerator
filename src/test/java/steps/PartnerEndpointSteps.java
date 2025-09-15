package steps;

import com.google.gson.Gson;
import io.cucumber.java.en.*;
import features.PartnerEndpoint.resources.Endpoint;
import io.cucumber.datatable.DataTable;
import io.restassured.response.Response;
import org.testng.Assert;
import payloads.PartnerContactsPayload;
import payloads.PartnerEndpointPayload;
import utils.ExcelReader;
import utils.PPRRESTEndpoints;
import utils.RestAssuredUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static utils.FileReference.testdataFiles;

public class PartnerEndpointSteps {

    ArrayList<Endpoint> contactEndpoint = new ArrayList<>();
    PartnerEndpointPayload partnerEndpointPayload = new PartnerEndpointPayload();
    RestAssuredUtils restAssuredUtils = new RestAssuredUtils();
    PPRRESTEndpoints pprrestEndpoints = new PPRRESTEndpoints();
    ScenarioHooks hooks = new ScenarioHooks();
    private String payload;
    Response responseFromPOST;
    Response responseFromPUT;
    Response responseFromGET;
    Response responseFromDELETE;
    static long endpointID;

    @Then("create partner endpoint")
    public void createPartnerEndpoint(DataTable table) {
        List<List<String>> data = table.asLists();
        contactEndpoint = partnerEndpointPayload.addPartnerEndpointPayload(table);
        Gson gson = new Gson();
        payload = gson.toJson(contactEndpoint.get(0));
        System.out.println("Payload here : " + payload);

        responseFromPOST = restAssuredUtils.sendPostRequest(pprrestEndpoints.POST_Partner_Endpoint,
                        hooks.cookie,
                        payload).
                then().statusCode(Integer.parseInt(data.get(0).get(5))).extract().response();
        Assert.assertEquals(responseFromPOST.getStatusCode(), Integer.parseInt(data.get(0).get(5)));
    }

    @Given("get endpoint id of partner whose email is {string}")
    public void getEndpointIdOfPartnerWhoseEmailIs(String url) {

        responseFromGET = restAssuredUtils.sendGetRequest(pprrestEndpoints.GET_Partner_Endpoint_ID.replace("{email}", url).
                                replace("{partner_id}", PartnerIdentity.contextRecordID),
                        hooks.cookie).
                then().statusCode(200).log().all().extract().response();
        endpointID = Long.parseLong(responseFromGET.then().extract().response().
                jsonPath().getString("platform.record[0].id"));
        System.out.println("endpointID : " + endpointID);
    }

    @Then("get and assert endpoint details")
    public void getAndAssertEndpointDetails(DataTable table) {
        List<List<String>> data = table.asLists();
        responseFromGET = restAssuredUtils.sendGetRequest(pprrestEndpoints.GET_Partner_Endpoint.replace("{endpoint_ID}", String.valueOf(endpointID)),
                        hooks.cookie).
                then().statusCode(Integer.parseInt(data.get(0).get(5))).extract().response();
        partnerEndpointPayload.verifyEndpointFields(responseFromGET, table);
    }

    @Then("update partner endpoint")
    public void updatePartnerEndpoint(DataTable table) {
        List<List<String>> data = table.asLists();
        contactEndpoint = partnerEndpointPayload.addPartnerEndpointPayload(table);
        Gson gson = new Gson();
        payload = gson.toJson(contactEndpoint.get(0));
        System.out.println("Payload here : " + payload);

        responseFromPUT = restAssuredUtils.sendPUTRequest(pprrestEndpoints.PUT_Partner_Endpoint.replace("{endpoint_ID}", String.valueOf(endpointID)),
                        hooks.cookie,
                        payload).
                then().statusCode(Integer.parseInt(data.get(0).get(5))).extract().response();
        Assert.assertEquals(responseFromPUT.getStatusCode(), Integer.parseInt(data.get(0).get(5)));
    }

    @Then("delete partner endpoint")
    public void deletePartnerEndpoint(DataTable table) {
        List<List<String>> data = table.asLists();
        responseFromDELETE = restAssuredUtils.sendDeleteRequest(pprrestEndpoints.DELETE_Partner_Endpoint.replace("{endpoint_ID}", String.valueOf(endpointID)),
                        hooks.cookie).
                then().statusCode(Integer.parseInt(data.get(0).get(0))).extract().response();
    }
}
