package utils;

import org.dom4j.Node;

public class ExtractFromEnvXml extends LoadEnvironment {

    public static String getIP() {
        Node node = env.selectSingleNode("Environments/Environment/Setup/tenant");
        System.out.println("getIP : "+node.getText());
        return node.getText();
    }

    public static String getTenantName() {
        Node node = env.selectSingleNode("Environments/Environment/Setup/tname");
        System.out.println("getTenantName : "+node.getText());
        return node.getText();
    }

    public static String getPPUsername() {
        Node node = env.selectSingleNode("Environments/Environment/Users/PartnerPortalUser/username");
        System.out.println("getPPUsername : "+node.getText());
        return node.getText();
    }
    public static String getB2BUsername() {
        Node node = env.selectSingleNode("Environments/Environment/Users/AdminUser/username");
        System.out.println("getB2BUsername : "+node.getText());
        return node.getText();
    }
    public static String getB2BPwd() {
        Node node = env.selectSingleNode("Environments/Environment/Users/AdminUser/pwd");
        System.out.println("getB2BPwd : "+node.getText());
        return node.getText();
    }

//    public static String getPPUserName() {
//        String user_name = null;
//        Node node = env.selectSingleNode("Environments/Environment/Users/AdminUser");
//        user_name = node.selectSingleNode("username").getText();
//        return user_name;
//    }

    public static String getPPPassword() {
        Node node = env.selectSingleNode("Environments/Environment/Users/PartnerPortalUser/pwd");
        System.out.println("getPPPassword : "+node.getText());
        return node.getText();
    }

    public static String getIDM() {
        Node node = env.selectSingleNode("Environments/Environment/Setup/idm");
        System.out.println("getIDM : "+node.getText());
        return node.getText();
    }

    public static String gethost() {
        Node node = env.selectSingleNode("Environments/Environment/Setup/host");
        System.out.println("gethost : "+node.getText());
        return node.getText();
    }
}
