package utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

public class ERProfile {

    private static ERProfile erProfile;
    public static ERProfile build() {
        if (erProfile == null)
            erProfile = new ERProfile();
        return erProfile;
    }

    private Properties properties;
    public ERProfile() {
        try {
            properties = new Properties();
            properties.load(this.getClass().getClassLoader().getResourceAsStream("pom.properties"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getVersion() {
        assert properties != null;
        return properties.getProperty("version");
    }

    public String getName() {
        assert properties != null;
        return properties.getProperty("name");
    }

    public String getArtifactId() {
        assert properties != null;
        return properties.getProperty("artifactId");
    }
}
