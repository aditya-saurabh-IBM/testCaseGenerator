package features.PartnerCertificate.resources;

import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("jsonschema2pojo")
public class CertificateCreation {

    @SerializedName("expiry_date")
    @Expose
    private Long expiryDate;
    @SerializedName("issued_to")
    @Expose
    private String issuedTo;
    @SerializedName("issued_by")
    @Expose
    private String issuedBy;
    @SerializedName("serial_number")
    @Expose
    private String serialNumber;
    @SerializedName("extension")
    @Expose
    private String extension;
    @SerializedName("data")
    @Expose
    private String data;
    @SerializedName("certificate")
    @Expose
    private Integer certificate;

    public Long getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Long expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getIssuedTo() {
        return issuedTo;
    }

    public void setIssuedTo(String issuedTo) {
        this.issuedTo = issuedTo;
    }

    public String getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(String issuedBy) {
        this.issuedBy = issuedBy;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Integer getCertificate() {
        return certificate;
    }

    public void setCertificate(Integer certificate) {
        this.certificate = certificate;
    }

}

