package payloads;

import com.google.gson.Gson;
import features.PartnerContact.resources.Contacts;
import features.PartnerEndpoint.resources.Endpoint;
import io.cucumber.datatable.DataTable;
import io.restassured.response.Response;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.List;

public class PartnerEndpointPayload {
    ArrayList<Endpoint> partnerEndpoint_list = new ArrayList<Endpoint>();

    public ArrayList<Endpoint> addPartnerEndpointPayload(DataTable table) {
        Endpoint endpoint = new Endpoint();
        List<List<String>> data = table.asLists();
        Gson gson = new Gson();

        endpoint.setType(data.get(0).get(0));
        endpoint.setUrl(data.get(0).get(1));
        endpoint.setIsPreferred(data.get(0).get(2));
        endpoint.setUsername(data.get(0).get(3));
        endpoint.setPassword(data.get(0).get(4));

        partnerEndpoint_list.add(endpoint);
        System.out.println("Arraylist  :: " + gson.toJson(partnerEndpoint_list.get(0)));

        return partnerEndpoint_list;
    }

    public void verifyEndpointFields(Response responseFromGET, DataTable table) {
        List<List<String>> data = table.asLists();
        Assert.assertEquals(responseFromGET.then().extract().response().jsonPath().getString("record.type"), data.get(0).get(0));
        Assert.assertEquals(responseFromGET.then().extract().response().jsonPath().getString("record.url"), data.get(0).get(1));
        Assert.assertEquals(responseFromGET.then().extract().response().jsonPath().getString("record.is_preferred"), data.get(0).get(2));
        Assert.assertEquals(responseFromGET.then().extract().response().jsonPath().getString("record.username"), data.get(0).get(3));

    }
}
