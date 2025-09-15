package steps;

import io.cucumber.datatable.DataTable;
import io.restassured.response.Response;
import utils.PPRRESTEndpoints;
import utils.RestAssuredUtils;

import java.util.List;

public class CommonSteps {
    RestAssuredUtils restAssuredUtils = new RestAssuredUtils();
    ScenarioHooks hooks = new ScenarioHooks();

    PPRRESTEndpoints pprrestEndpoints = new PPRRESTEndpoints();
    Response response;
    static String contextRecordID;
    static String identityID;

//    LoadEnvironment loadEnvironment = new LoadEnvironment();
//    RestAssuredUtils restAssuredUtils = new RestAssuredUtils();
//
//   public static LinkedHashMap<String,String> cookie = new LinkedHashMap<>();
//    Response response;
//
//    @When("login to partner portal")
//    public void loginToPartnerPortal() {
//        String loginURL=loadEnvironment.tenant+"/wmapps/core/login?legacy-error-format=true";
//        System.out.println("login url : "+ loginURL);
//        Map<String,String> creds= new HashMap<>();
//        creds.put("password",loadEnvironment.ppPwd);
//        creds.put("userName",loadEnvironment.ppUsername);
//        System.out.println("Cookie before : "+ cookie);
//        response=restAssuredUtils.sendPostRequest(loginURL,cookie,creds).then().statusCode(200).extract().response();
//        cookie.put("sessionToken",response.then().extract().response().jsonPath().getString("sessionToken").toString());
//        System.out.println("Cookie after : "+ cookie);
//        System.out.println("response from extract response : "+ response.then().extract().response().jsonPath().getString("sessionToken").toString());
//    }

    public String partnerContextRecordID()
    {
        response = restAssuredUtils.sendGetRequest(pprrestEndpoints.GET_Partner_ContextRecordID,  hooks.cookie).then().extract().response();
        contextRecordID= response.then().extract().response().jsonPath().getString("portalInfo.contextRecordId");
        return contextRecordID;
    }

    public String getIdentityID(DataTable table) {
        List<List<String>> data = table.asLists();
        response = restAssuredUtils.sendGetRequest(pprrestEndpoints.GET_Specific_Partner_Identity.replace("{identity_ID}",data.get(0).get(1)).replace("{partner_id}", partnerContextRecordID()),
                hooks.cookie).then().statusCode(200).extract().response();
        identityID=response.then().extract().response().jsonPath().getString("platform.record[0].id");
        System.out.println("identityID : "+ identityID);
        return identityID;
    }
}
