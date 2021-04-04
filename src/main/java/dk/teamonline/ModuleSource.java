package dk.teamonline;

import java.nio.file.Path;
import java.util.List;

public interface ModuleSource {
    String getModuleName();

    List<String> getPackages();

    List<Path> getSourcePaths();
}
