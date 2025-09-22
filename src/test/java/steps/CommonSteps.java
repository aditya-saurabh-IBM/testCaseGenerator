package steps;

import io.cucumber.datatable.DataTable;
import io.restassured.response.Response;
import utils.GenericMethods;
import utils.LoadEnvironment;
import utils.PPRRESTEndpoints;
import utils.RestAssuredUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public class CommonSteps {
    RestAssuredUtils restAssuredUtils = new RestAssuredUtils();
    ScenarioHooks hooks = new ScenarioHooks();

    PPRRESTEndpoints pprrestEndpoints = new PPRRESTEndpoints();
    GenericMethods genericMethods = new GenericMethods();
    Response response;
    static String contextRecordID;
    static String identityID;

    public String partnerContextRecordID() {
        response = genericMethods.PP_GET_Request(pprrestEndpoints.GET_Partner_ContextRecordID).then().extract()
                .response();
        contextRecordID = response.then().extract().response().jsonPath().getString("portalInfo.contextRecordId");
        return contextRecordID;
    }

    public String getIdentityID(DataTable table) {
        List<List<String>> data = table.asLists();

        response = genericMethods
                .PP_GET_Request(pprrestEndpoints.GET_Specific_Partner_Identity
                        .replace("{identity_ID}", data.get(0).get(1)).replace("{partner_id}", partnerContextRecordID()))
                .then().statusCode(200).extract().response();
        identityID = response.then().extract().response().jsonPath().getString("platform.record[0].id");
        System.out.println("identityID : " + identityID);
        return identityID;
    }

}
