package utils;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class ERProfile {

    private static ERProfile erProfile;
    public static ERProfile build() {
        if (erProfile == null)
            erProfile = new ERProfile();
        return erProfile;
    }

    private Model model;
    public ERProfile() {
        try {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            model = reader.read(new FileReader("pom.xml"));
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getVersion() {
        assert model != null;
        return model.getVersion();
    }

    public String getName() {
        assert model != null;
        return model.getName();
    }

    public String getArtifactId() {
        assert model != null;
        return model.getArtifactId();
    }
}
