package steps;


import io.cucumber.java.en.*;
import io.cucumber.datatable.DataTable;
import io.restassured.response.Response;
import org.testng.Assert;
import framework.TestBase;
import payloads.PartnerIdentityPayload;
import resources.AddPartnerIdentity;
import utils.LoadEnvironment;
import utils.PPRRESTEndpoints;
import utils.RestAssuredUtils;
import com.google.gson.Gson;

import java.util.*;

public class PartnerIdentity extends TestBase {

    RestAssuredUtils restAssuredUtils = new RestAssuredUtils();
    ScenarioHooks hooks = new ScenarioHooks();

    PPRRESTEndpoints pprrestEndpoints = new PPRRESTEndpoints();
    PartnerIdentityPayload partnerIdentityPayload = new PartnerIdentityPayload();
    CommonSteps commonSteps = new CommonSteps();
    Response response;
    private String URI = "";
    Response responseFromPOST;
    int recordIDActivityIndex;
    Response responseFromGET;
    Response responseFromPUT;
    Response responseFromDELETE;
    Map<String, String> payload2 = new HashMap<>();
    static String contextRecordID;
    private String payload;
    private int actualStatus;
    String identityId;
    ArrayList<AddPartnerIdentity> arrayListPartnerIdentity = new ArrayList<>();


    @When("get partner contextRecordID of partner portal")
    public void getPartnerContextRecordIDOfPartnerPortal() {
        response = restAssuredUtils.sendGetRequest(pprrestEndpoints.GET_Partner_ContextRecordID, hooks.cookie).then().extract().response();
        contextRecordID = response.then().extract().response().jsonPath().getString("portalInfo.contextRecordId");

    }


    @Then("update partner identity")
    public void updatePartnerIdentity(DataTable table) {

        List<List<String>> data = table.asLists();
        arrayListPartnerIdentity = partnerIdentityPayload.updateIdentityPayload(table);
        Gson gson = new Gson();
        for (int i = 0; i < data.size(); i++) {
            payload = gson.toJson(arrayListPartnerIdentity.get(i));
            responseFromPUT = restAssuredUtils.sendPUTRequest(pprrestEndpoints.PUT_Partner_Identity.replace("{Identity_ID}", commonSteps.getIdentityID(table)),
                            hooks.cookie,
                            payload).
                    then().statusCode(Integer.parseInt(data.get(i).get(4))).extract().response();
            Assert.assertEquals(responseFromPUT.getStatusCode(), Integer.parseInt(data.get(i).get(4)));


        }


    }

    @Then("create partner identity")
    public void createPartnerIdentity(DataTable table) {

        List<List<String>> data = table.asLists();
        arrayListPartnerIdentity = partnerIdentityPayload.addIdentityPayload(table);
        Gson gson = new Gson();
        for (int i = 0; i < data.size(); i++) {
            payload = gson.toJson(arrayListPartnerIdentity.get(i));
            responseFromPUT = restAssuredUtils.sendPostRequest(pprrestEndpoints.POST_Partner_Identities,
                            hooks.cookie,
                            payload).
                    then().statusCode(Integer.parseInt(data.get(i).get(2))).extract().response();
            Assert.assertEquals(responseFromPUT.getStatusCode(), Integer.parseInt(data.get(i).get(2)));


        }
    }

    @Then("get identity with identityType as {string}, identityValue as {string} and status code as {string}")
    public void getIdentityWithIdentityTypeAsIdentityValueAsAndStatusCodeAs(String idType, String idValue, String status) {
        String identityID;
        String identityType;

        response = restAssuredUtils.sendGetRequest(pprrestEndpoints.GET_Partner_ContextRecordID, hooks.cookie).then().extract().response();
        contextRecordID = response.then().extract().response().jsonPath().getString("portalInfo.contextRecordId");

        responseFromGET = restAssuredUtils.sendGetRequest(pprrestEndpoints.GET_Specific_Partner_Identity.replace("{identity_ID}", idValue).replace("{partner_id}", contextRecordID), hooks.cookie).then().statusCode(200).extract().response();
        identityID = responseFromGET.then().extract().response().jsonPath().getString("platform.record[0].identity_value");
        identityType = responseFromGET.then().extract().response().jsonPath().getString("platform.record[0].record_locator");
        System.out.println("identityID : " + identityID);
        System.out.println("responseFromGET.getStatusCode() :: " + responseFromGET.getStatusCode());
        if (Integer.parseInt(status) == 200) {
            Assert.assertEquals(identityID, idValue);
            Assert.assertTrue(identityType.contains(idType));
        } else {
            // since for 400 response we are getting xml as response instead of json so if block is created
        }
    }

    @Then("delete partner identity")
    public void deletePartnerIdentity(DataTable table) {
        List<List<String>> data = table.asLists();
        for (int i = 0; i < data.size(); i++) {
            if (commonSteps.getIdentityID(table) != null) {
                responseFromDELETE = restAssuredUtils.sendDeleteRequest(pprrestEndpoints.DELETE_Partner_Identity.replace("{Identity_ID}", commonSteps.getIdentityID(table)),
                                hooks.cookie).
                        then().statusCode(Integer.parseInt(data.get(i).get(2))).extract().response();
            } else {
                responseFromDELETE = restAssuredUtils.sendDeleteRequest(pprrestEndpoints.DELETE_Partner_Identity.replace("{Identity_ID}", "64674664"),
                                hooks.cookie).
                        then().statusCode(Integer.parseInt(data.get(i).get(2))).extract().response();
            }
            System.out.println("responseFromDELETE status : " + responseFromDELETE.getStatusCode());
            Assert.assertEquals(responseFromDELETE.getStatusCode(), Integer.parseInt(data.get(i).get(2)));


        }
    }



    @Then("approve all the assets in b2b")
    public void approveAllTheAssetsInBB() {
        response = restAssuredUtils.sendGetRequest(pprrestEndpoints.GET_Partner_ContextRecordID, hooks.cookie).then().extract().response();
        contextRecordID = response.then().extract().response().jsonPath().getString("portalInfo.contextRecordId");
        URI = pprrestEndpoints.b2bconnections;
        responseFromGET = restAssuredUtils.ExecuteAPI(RestAssuredUtils.HTTPMethod.GET, URI, LoadEnvironment.B2BUser, LoadEnvironment.B2BPwd, "", "", "");


        List activitykey = responseFromGET.then().extract().response().jsonPath().getList("connections");

        for (int i = 0; i < activitykey.size(); i++) {
            if ((activitykey.get(i).toString().split(","))[0].equals("recordId=" + contextRecordID)) ;
            {
                recordIDActivityIndex = i;
            }
        }

        String ActivitykeyID = (responseFromGET.then().extract().response().jsonPath().getString("connections[" + recordIDActivityIndex + "].activityKeys").split(":"))[0].substring(1);
        String latestID = (responseFromGET.then().extract().response().jsonPath().getString("connections[" + recordIDActivityIndex + "].activityKeys." + ActivitykeyID + ".latestId"));
        String ids = responseFromGET.then().extract().response().jsonPath().getString("connections[" + recordIDActivityIndex + "].activityKeys." + ActivitykeyID + ".ids");
        String action = responseFromGET.then().extract().response().jsonPath().getString("connections[" + recordIDActivityIndex + "].activityKeys." + ActivitykeyID + ".action");

        String ActivityPayload = "{\"activityKeys\":{" + "\"" + ActivitykeyID + "\"" + ": {\"latestId\":" + latestID + ",\"ids\":" + ids + ",\"action\":" + "\"" + action + "\"" + "}}}";

        URI = pprrestEndpoints.b2bActivities.replace("{contextrecordID}", contextRecordID);

        responseFromPOST = restAssuredUtils.ExecuteAPI(RestAssuredUtils.HTTPMethod.POST, URI, LoadEnvironment.B2BUser, LoadEnvironment.B2BPwd, ActivityPayload, "", "");


        String jsonformt = responseFromPOST.then().extract().body().asString();
        String activityResponse = responseFromPOST.then().extract().body().asString();
        String reviewPayload = "{\"action\": \"Approve\",\"comment\": \"automation ids are approved\"," + activityResponse.substring(1, activityResponse.length() - 1) + "," + ActivityPayload.substring(1, ActivityPayload.length() - 1) + "}";
        URI = pprrestEndpoints.b2bReview.replace("{contextrecordID}", contextRecordID);
        responseFromPOST = restAssuredUtils.ExecuteAPI(RestAssuredUtils.HTTPMethod.POST, URI, LoadEnvironment.B2BUser, LoadEnvironment.B2BPwd, reviewPayload, "", "");
        String statusmsg = responseFromPOST.then().extract().response().jsonPath().getString("statusMessages");

        System.out.println(activityResponse);
        System.out.println(reviewPayload);
        System.out.println(statusmsg);
    }
}
