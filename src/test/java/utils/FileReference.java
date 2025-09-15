package utils;

import java.io.File;

public class FileReference {

    public static String basePath = System.getProperty("user.dir") + File.separator + "src" + File.separator + "main" + File.separator + "resources";
    public static String propertyFilePath = basePath + File.separator + "properties";
    public static String testdataFiles = basePath + File.separator + "TestData" + File.separator;
}
