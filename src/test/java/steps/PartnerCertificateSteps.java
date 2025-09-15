package steps;

import com.google.gson.Gson;
import io.cucumber.java.en.*;
import features.PartnerCertificate.resources.CertificateRecordID;
import io.cucumber.datatable.DataTable;
import io.restassured.response.Response;
import org.testng.Assert;
import payloads.PartnerCertificatePayload;
import payloads.PartnerContactsPayload;
import utils.ExcelReader;
import utils.PPRRESTEndpoints;
import utils.RestAssuredUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static steps.PartnerContacts.sheetName;
import static steps.PartnerContacts.testDataCompletePath;

public class PartnerCertificateSteps {

    PartnerCertificatePayload partnerCertificatePayload = new PartnerCertificatePayload();
    ArrayList<CertificateRecordID> partnerCert_recordID_list = new ArrayList<CertificateRecordID>();
    String payload;
    private String currentPayload;
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
    public static long certRecordID;

    @When("get certificate recordID with {string} and {string}")
    public void getCertificateRecordIDWithAnd(String usage, String primary) {
        partnerCert_recordID_list = partnerCertificatePayload.getCertificateRecordID(usage, primary);
        Gson gson = new Gson();
        payload = gson.toJson(partnerCert_recordID_list.get(0));
        System.out.println("Payload here : " + payload);
        responseFromPOST = restAssuredUtils.sendPostRequest(pprrestEndpoints.POST_Certificate_RecordID,
                        hooks.cookie,
                        payload).
                then().statusCode(200).extract().response();
        certRecordID = Long.parseLong(responseFromPOST.then().extract().response().
                jsonPath().getString("record.id"));
        ;
        System.out.println("certRecordID : " + certRecordID);

    }

    @Then("create a partner contacts from provided excel row")
    public void createAPartnerContactsFromProvidedExcelRow(DataTable table) throws IOException {

        List<List<String>> data = table.asLists();
//
        List<Map<String, String>> testData = ExcelReader.readTestDataFromExcel(testDataCompletePath, sheetName, data.get(0).get(1));

        Map<String, String> rowData = testData.get(0);
//        for (Map<String, String> rowData : testData) {
        currentPayload = partnerCertificatePayload.createPartnerCertificatePayload(rowData);
        System.out.println("current payload : " + currentPayload);

        responseFromPOST = restAssuredUtils.sendPostRequest(pprrestEndpoints.POST_Certificate_Data,
                        hooks.cookie,
                        currentPayload).
                then().statusCode(200).log().all().extract().response();
//        }

    }

    @When("get partner certificate by {string}")
    public void getPartnerCertificateBy(String usage) {


        responseFromGET = restAssuredUtils.sendGetRequest(pprrestEndpoints.GET_Certificates.replace("{cert_type}", usage).
                                replace("{partner_id}", PartnerIdentity.contextRecordID),
                        hooks.cookie).
                then().statusCode(200).log().all().extract().response();

        Assert.assertEquals(responseFromGET.then().extract().response().jsonPath().getString("platform.record[0].record_locator").toLowerCase(),
                usage);
    }

    @When("delete partner certificate")
    public void deletePartnerCertificate() {
        responseFromDELETE = restAssuredUtils.sendGetRequest(pprrestEndpoints.DELETE_Certificate.replace("{cert_ID}", String.valueOf(certRecordID)),
                        hooks.cookie).
                then().statusCode(200).log().all().extract().response();
    }
}
