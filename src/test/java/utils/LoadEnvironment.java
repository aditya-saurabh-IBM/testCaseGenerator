package utils;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

import java.io.File;

public class LoadEnvironment {

    public static String tname="";
    public static String tenant="";
    public static String host="";
    public static String ppUsername="";
    public static String ppPwd="";
    public static String B2BUser="";
    public static String B2BPwd="";
    public static String IDM="";
    public static Document env = null;


    static {

        File envFile = new File(FileReference.propertyFilePath + File.separator + "Environment.xml");
        SAXReader reader = new SAXReader();
        try {
            env = reader.read(envFile);
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        /*-------------------- IP --------------------------*/

        tenant = System.getProperty("tenantURL", ExtractFromEnvXml.getIP());
        host = System.getProperty("host", ExtractFromEnvXml.gethost());
        IDM=System.getProperty("idm", ExtractFromEnvXml.getIDM());
        tname = System.getProperty("tName", ExtractFromEnvXml.getTenantName());
        ppUsername = System.getProperty("ppUsername", ExtractFromEnvXml.getPPUsername());
        ppPwd = System.getProperty("ppPassword", ExtractFromEnvXml.getPPPassword());
        B2BUser = System.getProperty("b2bUser", ExtractFromEnvXml.getB2BUsername());
        B2BPwd = System.getProperty("b2bpwd", ExtractFromEnvXml.getB2BPwd());


    }


}