package payloads;

import com.google.gson.Gson;
import io.cucumber.datatable.DataTable;
import resources.AddPartnerIdentity;

import java.util.ArrayList;
import java.util.List;

public class PartnerIdentityPayload {
    private String payload;
    ArrayList<AddPartnerIdentity> partnerIdentityArrayList = new ArrayList<AddPartnerIdentity>();

    public ArrayList addIdentityPayload(DataTable table)
    {
        List<List<String>> data = table.asLists();
        Gson gson = new Gson();

        for (int i = 0; i < data.size(); i++) {

            AddPartnerIdentity addPartnerIdentity = new AddPartnerIdentity();
            addPartnerIdentity.setIdentityType(data.get(i).get(0));
            addPartnerIdentity.setIdentityValue(data.get(i).get(1));
            partnerIdentityArrayList.add(addPartnerIdentity);
            System.out.println("Arraylist  :: "+gson.toJson(partnerIdentityArrayList.get(i)));
        }
        return partnerIdentityArrayList;
    }

    public ArrayList updateIdentityPayload(DataTable table)
    {
        List<List<String>> data = table.asLists();
//        AddPartnerIdentity addPartnerIdentity = new AddPartnerIdentity();
        Gson gson = new Gson();

        for (int i = 0; i < data.size(); i++) {

            AddPartnerIdentity addPartnerIdentity = new AddPartnerIdentity();
            addPartnerIdentity.setIdentityType(data.get(i).get(2));
            addPartnerIdentity.setIdentityValue(data.get(i).get(3));
            partnerIdentityArrayList.add(addPartnerIdentity);
//            System.out.println("Arraylist  :: "+gson.toJson(partnerIdentityArrayList.get(i)));
        }
        return partnerIdentityArrayList;
    }


//    public String addIdentityPayload(DataTable table)
//    {
//        List<List<String>> data = table.asLists();
//        AddPartnerIdentity addPartnerIdentity = new AddPartnerIdentity();
//        Gson gson = new Gson();
//
//        for (int i = 0; i < data.size(); i++) {
//
//            addPartnerIdentity.setIdentityType(data.get(i).get(2));
//            addPartnerIdentity.setIdentityValue(data.get(i).get(3));
//            partnerIdentityArrayList.add(addPartnerIdentity);
//            System.out.println("Arraylist  :: "+gson.toJson(partnerIdentityArrayList.get(i)));
//        }
//
//
//
//
//
//        payload = gson.toJson(partnerIdentityArrayList);
//        return payload;
//    }
}
