package steps;

import com.google.gson.Gson;
import io.cucumber.java.en.*;
import io.cucumber.datatable.DataTable;
import io.restassured.response.Response;

import org.testng.Assert;
import payloads.PartnerContactsPayload;
import utils.ExcelReader;
import utils.PPRRESTEndpoints;
import utils.RestAssuredUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static utils.FileReference.testdataFiles;

public class PartnerContacts {
    private String payload;
    private String sheet;
    public static String testDataCompletePath;
    public static String sheetName;
    Response responseFromPOST;
    Response responseFromPUT;
    Response responseFromGET;
    Response responseFromDELETE;
    private int expectedStatusCode;
    PartnerContactsPayload partnerContactsPayload = new PartnerContactsPayload();
    PPRRESTEndpoints pprrestEndpoints = new PPRRESTEndpoints();
    RestAssuredUtils restAssuredUtils = new RestAssuredUtils();
    CommonSteps commonSteps = new CommonSteps();
    ScenarioHooks hooks = new ScenarioHooks();
    private List<Map<String, String>> testData;
    private String currentPayload;
    static long contactID;
    //    String partnerContactPayload;
    ArrayList<PartnerContactsPayload> contactPayload = new ArrayList<>();
    Map<String, Integer> contactIDMap = new HashMap<>();

    @Given("the excel file is {string} and sheet is {string} and row is {string}")
    public void theExcelFileIsAndSheetIs(String testDatapath, String sheetName, String s) throws IOException {
        testDataCompletePath = testdataFiles + testDatapath + ".xlsx";
        this.sheetName = sheetName;
        testData = ExcelReader.readTestDataFromExcel(testDataCompletePath, sheetName, s);

    }

//    @Then("create a partner contacts from provided excel")
//    public void createAPartnerContactsFromProvidedExcel() {
//        List<Map<String, String>> testDataList = ExcelReader.readTestDataFromExcel(testDataCompletePath, sheetName);
//
//        for (Map<String, String> rowData : testDataList) {
//            currentPayload = partnerContactsPayload.addPartnerContactPayload(rowData);
//            System.out.println("current payload : " + currentPayload);
//
//            responseFromPOST = restAssuredUtils.sendPostRequest(pprrestEndpoints.POST_Partner_Contacts,
//                            hooks.cookie,
//                            currentPayload).
//                    then().statusCode(Integer.parseInt(rowData.get("expectedstatuscode"))).log().all().extract().response();
//            Assert.assertEquals(responseFromPOST.getStatusCode(), Integer.parseInt(rowData.get("expectedstatuscode")));
//        }
////        Map<String, String> rowData = testData.remove(0);
//
//    }

//    @Then("create a partner contacts from provided excel row")
//    public void createAPartnerContactsFromProvidedExcelRow(DataTable table) {
//        List<List<String>> data = table.asLists();
//
//        List<Map<String, String>> testData = ExcelReader.readTestDataFromExcel(testDataCompletePath, sheetName, data.get(0).get(1));
//
//        for (Map<String, String> rowData : testData) {
//            currentPayload = partnerContactsPayload.addPartnerContactPayload(rowData);
//            System.out.println("current payload : " + currentPayload);
//
//            responseFromPOST = restAssuredUtils.sendPostRequest(pprrestEndpoints.POST_Partner_Contacts,
//                            hooks.cookie,
//                            currentPayload).
//                    then().statusCode(Integer.parseInt(rowData.get("expectedstatuscode"))).log().all().extract().response();
//            Assert.assertEquals(responseFromPOST.getStatusCode(), Integer.parseInt(rowData.get("expectedstatuscode")));
//        }
////        Map<String, String> rowData = testData.remove(0);
//
//    }

    @Given("get contact id of partner whose email is {string}")
    public void getContactIdOfPartnerWhoseEmailIs(String email) {

        responseFromGET = restAssuredUtils.sendGetRequest(pprrestEndpoints.GET_Specific_Partner_Contact.replace("{email}", email).
                                replace("{partner_id}", PartnerIdentity.contextRecordID),
                        hooks.cookie).
                then().statusCode(200).log().all().extract().response();
        contactID = Long.parseLong(responseFromGET.then().extract().response().
                jsonPath().getString("platform.record[0].id"));
        System.out.println("contactID : " + contactID);
    }

    @Then("create partner contact")
    public void createPartnerContact(DataTable table) {
        contactPayload = partnerContactsPayload.addPartnerContactPayload(table);
        Gson gson = new Gson();
        payload = gson.toJson(contactPayload.get(0));
        System.out.println("Payload here : " + payload);
        responseFromPOST = restAssuredUtils.sendPostRequest(pprrestEndpoints.POST_Partner_Contacts,
                        hooks.cookie,
                        payload).
                then().statusCode(200).extract().response();
        Assert.assertEquals(responseFromPOST.getStatusCode(), 200);


    }


    @Then("get and assert contact details")
    public void getAndAssetContactDetails(DataTable table) {
        System.out.println("contextRecordID2: " + PartnerIdentity.contextRecordID);
        responseFromGET = restAssuredUtils.sendGetRequest(pprrestEndpoints.GET_Partner_Contacts_From_ContactID.replace("{contactID}", String.valueOf(contactID).
                                replace("{partner_id}", PartnerIdentity.contextRecordID)),
                        hooks.cookie).
                then().statusCode(200).extract().response();
        partnerContactsPayload.verifyContactFiels(responseFromGET, table);

    }

    @Then("update partner contact")
    public void updatePartnerContact(DataTable table) {
        contactPayload = partnerContactsPayload.addPartnerContactPayload(table);
        Gson gson = new Gson();
        payload = gson.toJson(contactPayload.get(0));
        System.out.println("Payload here : " + payload);
        responseFromPUT = restAssuredUtils.sendPUTRequest(pprrestEndpoints.PUT_Partner_Contact.replace("{contactID}", String.valueOf(contactID)),
                        hooks.cookie,
                        payload).
                then().statusCode(200).extract().response();
        Assert.assertEquals(responseFromPUT.getStatusCode(), 200);
    }

    @Then("Delete partner contact")
    public void deletePartnerContact(DataTable table) {
        List<List<String>> data = table.asLists();
        responseFromDELETE = restAssuredUtils.sendDeleteRequest(pprrestEndpoints.DELETE_Partner_Contact.replace("{contactID}", String.valueOf(contactID)),
                        hooks.cookie).
                then().statusCode(Integer.parseInt(data.get(0).get(0))).extract().response();
        Assert.assertEquals(responseFromDELETE.getStatusCode(), Integer.parseInt(data.get(0).get(0)));
    }
}
