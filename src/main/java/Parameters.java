import com.beust.jcommander.Parameter;

import java.util.ArrayList;
import java.util.List;

public class Parameters {
    @Parameter(names = {"--rest", "-r"}, description = "To start rest server mode")
    public boolean rest = false;

    @Parameter(names = {"--cli", "-c"}, description = "To run in cmd mode, use -c dataset/conf")
    public String cli;

    @Parameter(names = {"--help", "-h"}, help = true)
    public boolean help;

    @Parameter(names = {"--client", "-cl"}, description = "A rest client sends a rest request, use -c [url]." +
            " It must combine with option --json")
    public String url;

    @Parameter(names = {"--json", "-j"}, description = "To load a json request in client mode, use -j [config.json].")
    public String jsonDir;
}