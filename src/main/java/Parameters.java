import com.beust.jcommander.Parameter;

import java.util.ArrayList;
import java.util.List;

public class Parameters {
    @Parameter(names = {"--rest", "-r"}, description = "To start rest server mode")
    public boolean rest = false;

    @Parameter(names = {"--cli", "-c"}, description = "To run in cmd mode, use -c [dataset]")
    public String cli;

    @Parameter(names = {"--help", "-h"}, help = true)
    public boolean help;

    @Parameter(names = {"--client", "-cl"}, description = "A rest client sends a rest request to the rest server, use -c [dataset]")
    public String url;
}