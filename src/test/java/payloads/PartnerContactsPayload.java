package payloads;

import com.google.gson.Gson;
import features.PartnerContact.resources.Contacts;
import io.cucumber.datatable.DataTable;
import io.restassured.response.Response;
import org.testng.Assert;
import resources.AddPartnerIdentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PartnerContactsPayload {
    private String payload;
    ArrayList<Contacts> partnerContact_list = new ArrayList<Contacts>();


    public ArrayList addPartnerContactPayload(DataTable table) {
        Contacts contacts = new Contacts();
        List<List<String>> data = table.asLists();
        Gson gson= new Gson();

        contacts.setFirstName(data.get(0).get(0));
        contacts.setLastName(data.get(0).get(1));
        contacts.setContactType(data.get(0).get(2));
        contacts.setRole(data.get(0).get(3));
        contacts.setEmail(data.get(0).get(4));
        contacts.setFaxNumber(data.get(0).get(5));
        contacts.setTelephoneNumber(data.get(0).get(6));
        contacts.setTelephoneExtension(data.get(0).get(7));
        contacts.setAddressLine1(data.get(0).get(8));
        contacts.setAddressLine2(data.get(0).get(9));
        contacts.setAddressLine3(data.get(0).get(10));
        contacts.setAddressType(data.get(0).get(11));
        contacts.setCountry(data.get(0).get(12));
        contacts.setState(data.get(0).get(13));
        contacts.setCity(data.get(0).get(14));
        contacts.setZip(data.get(0).get(15));

//        Gson gson = new Gson();
//        payload = gson.toJson(contacts);
//        System.out.println("payload created : " + payload);
        partnerContact_list.add(contacts);
        System.out.println("Arraylist  :: "+gson.toJson(partnerContact_list.get(0)));


        return partnerContact_list;
    }

    public void verifyContactFiels(Response responseFromGET, DataTable table) {
        List<List<String>> data = table.asLists();
        Assert.assertEquals(responseFromGET.then().extract().response().jsonPath().getString("record.first_name"),data.get(0).get(0));
        Assert.assertEquals(responseFromGET.then().extract().response().jsonPath().getString("record.last_name"),data.get(0).get(1));
        Assert.assertEquals(responseFromGET.then().extract().response().jsonPath().getString("record.contact_type"),data.get(0).get(2));
        Assert.assertEquals(responseFromGET.then().extract().response().jsonPath().getString("record.role"),data.get(0).get(3));
        Assert.assertEquals(responseFromGET.then().extract().response().jsonPath().getString("record.email"),data.get(0).get(4));
        Assert.assertEquals(responseFromGET.then().extract().response().jsonPath().getString("record.fax_number"),data.get(0).get(5));
        Assert.assertEquals(responseFromGET.then().extract().response().jsonPath().getString("record.telephone_number"),data.get(0).get(6));
        Assert.assertEquals(responseFromGET.then().extract().response().jsonPath().getString("record.telephone_extension"),data.get(0).get(7));
        Assert.assertEquals(responseFromGET.then().extract().response().jsonPath().getString("record.address_line_1"),data.get(0).get(8));
        Assert.assertEquals(responseFromGET.then().extract().response().jsonPath().getString("record.address_line_2"),data.get(0).get(9));
        Assert.assertEquals(responseFromGET.then().extract().response().jsonPath().getString("record.address_line_3"),data.get(0).get(10));
        Assert.assertEquals(responseFromGET.then().extract().response().jsonPath().getString("record.address_type"),data.get(0).get(11));
        Assert.assertEquals(responseFromGET.then().extract().response().jsonPath().getString("record.country"),data.get(0).get(12));
        Assert.assertEquals(responseFromGET.then().extract().response().jsonPath().getString("record.state"),data.get(0).get(13));
        Assert.assertEquals(responseFromGET.then().extract().response().jsonPath().getString("record.city"),data.get(0).get(14));
        Assert.assertEquals(responseFromGET.then().extract().response().jsonPath().getString("record.zip"),data.get(0).get(15));
    }
}
