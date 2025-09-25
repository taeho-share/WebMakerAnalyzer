package com.bizflow.bas.webmaker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class XmlFileFinder {

    /**
     * Finds XML files in a directory that match a specific pattern.
     *
     * @param directoryPath The directory to search in
     * @param filePattern The file pattern to match (e.g., "*_rules.xml")
     * @return A list of matching file paths
     */
    public List<Path> findXmlFiles(String directoryPath, String filePattern) throws IOException {
        Path dir = Paths.get(directoryPath);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            throw new IllegalArgumentException("Invalid directory path: " + directoryPath);
        }

        // Convert wildcard pattern to regex pattern
        String regex = filePattern.replace(".", "\\.").replace("*", ".*");

        try (Stream<Path> paths = Files.walk(dir)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().matches(regex))
                    .collect(Collectors.toList());
        }
    }
}