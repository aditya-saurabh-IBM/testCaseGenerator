package utils;

public class PPRRESTEndpoints {

    LoadEnvironment loadEnvironment = new LoadEnvironment();


    /////// /* generic endpoints  */
    public String GET_Partner_ContextRecordID = loadEnvironment.tenant + "/wmapps/core/startpage?legacy-error-format=true";

    /////////////////////     partner portal identity     //////////////////
    public String GET_Partner_Identities_MyAccount = loadEnvironment.tenant + "/wmapps/core/svc-external/rest?context=/portal/partners/partner_id/users";
    public String GET_Specific_Partner_Identity = loadEnvironment.tenant + "/wmapps/core/objects/pp_identity_object/records?lookup-page=true&fields=record_locator,id,identity_value&filter=search_string:contains=%27%20{identity_ID}%27%20AND%20partner:eq=%27{partner_id}%27";
    public String POST_Partner_Identities = loadEnvironment.tenant + "/wmapps/core/objects/pp_identity_object/records";
    public String PUT_Partner_Identity = loadEnvironment.tenant + "/wmapps/core/objects/pp_identity_object/records/{Identity_ID}";
    public String DELETE_Partner_Identity = loadEnvironment.tenant + "/wmapps/core/objects/pp_identity_object/records/{Identity_ID}";

    /////////////////////     partner portal Contacts     //////////////////
    public String POST_Partner_Contacts = loadEnvironment.tenant + "/wmapps/core/objects/pp_contact_object/records";
    public String GET_Partner_Contacts_From_ContactID = loadEnvironment.tenant + "/wmapps/core/objects/pp_contact_object/records/{contactID}?components=formInfo,record&layout-id=pp_cont_default_form_layout";
    public String GET_Specific_Partner_Contact = loadEnvironment.tenant + "/wmapps/core/objects/pp_contact_object/records?lookup-page=true&fields=record_locator,id,first_name,last_name,email,role,address_type,city,state,id&filter=search_string:contains='{email}'&partner:eq='{partner_id}'";
    public String PUT_Partner_Contact = loadEnvironment.tenant + "/wmapps/core/objects/pp_contact_object/records/{contactID}";
    public String DELETE_Partner_Contact = loadEnvironment.tenant + "/wmapps/core/objects/pp_contact_object/records/{contactID}";

    /////////////////////     partner portal Endpoints     //////////////////
    public String POST_Partner_Endpoint = loadEnvironment.tenant + "/wmapps/core/objects/pp_endpoint_object/records";
    public String GET_Partner_Endpoint_ID = loadEnvironment.tenant + "/wmapps/core/objects/pp_endpoint_object/records?lookup-page=true&fields=record_locator,id&filter=search_string:contains='{email}'&partner:eq='{partner_id}'&legacy-error-format=true";
    public String GET_Partner_Endpoint = loadEnvironment.tenant + "/wmapps/core/objects/pp_endpoint_object/records/{endpoint_ID}";
    public String PUT_Partner_Endpoint = loadEnvironment.tenant + "/wmapps/core/objects/pp_endpoint_object/records/{endpoint_ID}";
    public String DELETE_Partner_Endpoint = loadEnvironment.tenant + "/wmapps/core/objects/pp_endpoint_object/records/{endpoint_ID}";

    /////////////////////     partner portal Endpoints     //////////////////

    public String POST_Certificate_Validate = loadEnvironment.tenant + "/wmapps/core/svc-external/rest?context=/portal/partners/partner_id/certificates/validateChain";
    public String POST_Certificate_RecordID = loadEnvironment.tenant + "/wmapps/core/objects/pp_certificate_object/records";
    public String POST_Certificate_Data = loadEnvironment.tenant + "/wmapps/core/objects/pp_certificate_data_object/records";
    public String PUT_Certificate = loadEnvironment.tenant + "/wmapps/core/objects/pp_certificate_object/records/{cert_ID}";
    public String DELETE_Certificate = loadEnvironment.tenant + "/wmapps/core/objects/pp_certificate_object/records/{cert_ID}";
    public String GET_Certificates = loadEnvironment.tenant + "/wmapps/core/objects/pp_certificate_object/records?lookup-page=true&fields=id&filter=search_string:contains='{cert_type}'&partner:eq='{partner_id}'&legacy-error-format=true";

    ////b2b endPoints
    public String b2bconnections = loadEnvironment.tenant + "/b2b/portal/partners/connections";
    public String b2bReview = loadEnvironment.tenant + "/b2b/portal/partners/{contextrecordID}/review";
    public String b2bActivities = loadEnvironment.tenant + "/b2b/portal/partners/{contextrecordID}/activities";
}
