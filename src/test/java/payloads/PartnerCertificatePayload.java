package payloads;

import com.google.gson.Gson;
import features.PartnerCertificate.resources.CertificateCreation;
import features.PartnerCertificate.resources.CertificateRecordID;
import steps.PartnerCertificateSteps;

import java.util.ArrayList;
import java.util.Map;

public class PartnerCertificatePayload {
    ArrayList<CertificateRecordID> partnerCert_recordID_list = new ArrayList<CertificateRecordID>();
    String payload;

    public ArrayList getCertificateRecordID(String usage, String primary) {
        CertificateRecordID certificateRecordID = new CertificateRecordID();
        certificateRecordID.setIsPrimary(Boolean.parseBoolean(primary));
        certificateRecordID.setUsageType(usage);
        partnerCert_recordID_list.add(certificateRecordID);
        return partnerCert_recordID_list;

    }

    public String createPartnerCertificatePayload(Map<String, String> rowData) {
        CertificateCreation certificateCreation = new CertificateCreation();
        certificateCreation.setCertificate((int) PartnerCertificateSteps.certRecordID);
        certificateCreation.setData(rowData.get("data"));
        certificateCreation.setExpiryDate(Long.valueOf(rowData.get("expiry_date")));
        certificateCreation.setExtension(rowData.get("extension"));
        certificateCreation.setIssuedBy(rowData.get("issued_by"));
        certificateCreation.setIssuedTo(rowData.get("issued_to"));
        certificateCreation.setSerialNumber(rowData.get("serial_number"));

        Gson gson = new Gson();
        payload = gson.toJson(certificateCreation);
        return payload;
    }
}
