package com.bizflow.bas.webmaker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * FileScanManager
 *
 * This class is responsible for managing the scanning of WebMaker application files, including
 * JavaScript, HTML, thumbnails, and XML binding files. It filters out unnecessary files, copies
 * relevant files to a result directory, and generates a structured log report.
 *
 * The scanning process includes:
 * - Filtering files based on predefined exclusion lists
 * - Copying files to a structured result directory
 * - Logging details of the scanned files
 *
 * Usage:
 * 1. Create an instance with a log file name and optional output subdirectory.
 * 2. Call `scanAll()` with the base path to the WebMaker application.
 * 3. Call `close()` when finished to release resources.
 *
 * Example:
 *   FileScanManager scanner = new FileScanManager("scan_log.txt", "app1_export");
 *   scanner.scanAll("/path/to/webmaker", "app1_export");
 *   scanner.close();
 *
 * Author: Taeho Lee (taeho.share@gmail.com, thlee@bizflow.com)
 * Version: 1.0
 * Since: 2025-09-15
 */
public class FileScanManager {
    @FunctionalInterface
    private interface DirectoryScanner {
        void scan(String dirPath, String outputSubdir) throws IOException;
    }

    // A set of JavaScript library files to exclude from scanning.
    // These files are common utility scripts or libraries that are not part of the application-specific logic.
    private static final Set<String> JS_EXCLUSIONS = new HashSet<>(Arrays.asList(
            "basicwihactionclient.js"
            ,"bizflowFunctions.js"
            ,"BooleanValidator.js"
            ,"CalendarPopup.js"
            ,"combobox.js"
            ,"date.js"
            ,"DateValidator.js"
            ,"DisplayMessages.js"
            ,"DisplayUtils.js"
            ,"engineer_review.js"
            ,"ErrorDisplay.js"
            ,"FMActions.js"
            ,"FormValidator.js"
            ,"jquery-ui.min.js"
            ,"jquery.min.js"
            ,"NumberValidator.js"
            ,"PIE_uncompressed.js"
            ,"PIE.js"
            ,"StringValidator.js"
            ,"ValidationError.js"
            ,"ValueConverter.js"
            ,"editabletable.js"
            ,"checkbox_switch.js"
    ));

    private static final Set<String> HTML_EXCLUSIONS = new HashSet<>(
            Arrays.asList("Page_preview_BizFlowEntry.html"));

    private LogWriter logWriter;
    private XmlElementFinder xmlFinder;
    private XmlBindingFinder bindingFinder;
    private String resultDirPath;

    /**
     * Creates a new FileScanManager with the specified log file name.
     * This is kept for backward compatibility.
     */
    public FileScanManager(String logFileName) throws IOException {
        this(logFileName, "");
    }

    /**
    * Initializes a new instance of FileScanManager with the specified log file name
    * and an optional output subdirectory for organizing results.
    *
    * @param logFileName The name of the log file where scan details will be recorded.
    * @param outputSubdir The subdirectory for storing output files. If left empty, files will be stored in the root result directory.
    */
    public FileScanManager(String logFileName, String outputSubdir) throws IOException {
        // Create result directory
        this.resultDirPath = createResultDirectory();

        // Set log file to be created in the root result directory
        String logFilePath = resultDirPath + File.separator + logFileName;

        this.logWriter = new LogWriter(logFilePath);
        this.xmlFinder = new XmlElementFinder();
        this.bindingFinder = new XmlBindingFinder();

        logWriter.writeMessage("Results will be stored in: " + resultDirPath);
    }

    /**
     * Creates the result directory where output files will be stored.
     * If the directory does not exist, it attempts to create it.
     *
     * @return The absolute path of the result directory.
     * @throws IOException If the directory creation fails.
     */
    private String createResultDirectory() throws IOException {
        // Define the name of the result directory
        String resultDirName = "WMReportResult";
        File resultDir = new File(resultDirName);

        // Check if the directory already exists
        if (!resultDir.exists()) {
            // Attempt to create the directory
            if (!resultDir.mkdir()) {
                // Throw an exception if directory creation fails
                throw new IOException("Failed to create result directory: " + resultDirName);
            }
        }

        // Return the absolute path of the result directory
        return resultDir.getAbsolutePath();
    }

    private void copyFileToResultDir(File sourceFile, String subdir) throws IOException {
        // Create subdirectory if needed
        String targetDir = resultDirPath;
        if (subdir != null && !subdir.isEmpty()) {
            targetDir = resultDirPath + File.separator + subdir;
            File subDirFile = new File(targetDir);
            if (!subDirFile.exists()) {
                if (!subDirFile.mkdir()) {
                    throw new IOException("Failed to create subdirectory: " + subdir);
                }
            }
        }

        String targetFileName = sourceFile.getName();
        Path targetPath = Paths.get(targetDir, targetFileName);

        // If file with same name exists, add a suffix
        int count = 1;
        while (Files.exists(targetPath)) {
            String nameWithoutExt = targetFileName;
            String extension = "";
            int dotIndex = targetFileName.lastIndexOf('.');
            if (dotIndex > 0) {
                nameWithoutExt = targetFileName.substring(0, dotIndex);
                extension = targetFileName.substring(dotIndex);
            }
            targetPath = Paths.get(targetDir, nameWithoutExt + "_" + count + extension);
            count++;
        }

        Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        logWriter.writeMessage("Copied to: " + targetPath);
    }

    // For backward compatibility
    private void copyFileToResultDir(File sourceFile) throws IOException {
        copyFileToResultDir(sourceFile, "");
    }

    public String getLogFilePath() {
        return logWriter.getFilePath();
    }

    // For backward compatibility
    public void scanAll(String basePath) throws IOException {
        scanAll(basePath, "");
    }

    public void close() {
        if (logWriter != null) {
            try {
            logWriter.close();
            } catch (IOException e) {
                // Log the exception or handle it appropriately
                System.err.println("Error closing log writer: " + e.getMessage());
            }
        }
    }

    public void scanAll(String basePath, String outputSubdir) throws IOException {
        // Regular scans for fixed directories
        scanJsFiles(basePath + "/webapps/js", outputSubdir);
        scanHtmlFiles(basePath + "/webapps", outputSubdir);
        scanThumbnailFiles(basePath + "/webapps/thumbnails", outputSubdir);

        // Scan for directories dynamically
        scanDirectoriesWithPattern(basePath, "logicsheet_pool",
                (path, subdir) -> scanLogicsheetRules(path, subdir), outputSubdir);
        scanDirectoriesWithPattern(basePath, "hyfinityBindings",
                (path, subdir) -> scanBindingsFiles(path, subdir), outputSubdir);
        // No logWriter.close() call here to allow for multiple uses
    }

private void scanJsFiles(String dirPath, String outputSubdir) throws IOException {
    logWriter.writeMessage("JavaScript Files Report");

    // Define additional exclusion criteria
    List<String> excludeStartsWith = Arrays.asList("test", "sample", "angular");
    List<String> excludeEndsWith = Arrays.asList("min.js", "debug.js");

    try (Stream<Path> paths = Files.walk(Paths.get(dirPath))) {
        List<File> jsFiles = paths
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().toLowerCase().endsWith(".js"))
                .map(Path::toFile)
                .filter(file -> !JS_EXCLUSIONS.contains(file.getName()))
                .filter(file -> !file.getPath().contains("PIE"))
                .filter(file -> excludeStartsWith.stream().noneMatch(prefix -> file.getName().toLowerCase().startsWith(prefix)))
                .filter(file -> excludeEndsWith.stream().noneMatch(suffix -> file.getName().toLowerCase().endsWith(suffix)))
                .filter(file -> Arrays.stream(file.getPath().split(File.separator))
                        .noneMatch(part -> part.toLowerCase().startsWith("angular"))) // Exclude folders starting with "angular"
                .collect(Collectors.toList());

        for (File file : jsFiles) {
            logWriter.writeMessage("Found JS file: " + file.getName());
            logWriter.writeElement(file.getAbsolutePath());
            copyFileToResultDir(file, outputSubdir);
        }
    } catch (Exception e) {
        logWriter.writeMessage("Error scanning JS files: " + e.getMessage());
    }
}
    private void scanHtmlFiles(String dirPath, String outputSubdir) throws IOException {
        logWriter.writeMessage("HTML Files Report");

        try (Stream<Path> paths = Files.walk(Paths.get(dirPath))) {
            List<File> htmlFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".html"))
                    .filter(p -> !p.getFileName().toString().endsWith("_BizFlowEntry.html"))
                    .filter(p -> !p.toString().contains(File.separator + "theme" + File.separator))
                    .map(Path::toFile)
                    .filter(file -> !HTML_EXCLUSIONS.contains(file.getName()))
                    .collect(Collectors.toList());

            for (File file : htmlFiles) {
                logWriter.writeMessage("Found HTML file: " + file.getName());
                logWriter.writeElement(file.getAbsolutePath());
                copyFileToResultDir(file, outputSubdir);
            }
        } catch (Exception e) {
            logWriter.writeMessage("Error scanning HTML files: " + e.getMessage());
        }
    }

    private void scanThumbnailFiles(String dirPath, String outputSubdir) throws IOException {
        logWriter.writeMessage("Thumbnail Files Report");

        try (Stream<Path> paths = Files.walk(Paths.get(dirPath))) {
            List<File> pngFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith("1024.png"))
                    .map(Path::toFile)
                    .filter(file -> !file.getName().startsWith("BizFlowEntry"))
                    .collect(Collectors.toList());

            for (File file : pngFiles) {
                logWriter.writeMessage("Found thumbnail: " + file.getName());
                logWriter.writeElement(file.getAbsolutePath());
                copyFileToResultDir(file, outputSubdir);
            }
        } catch (Exception e) {
            logWriter.writeMessage("Error scanning thumbnail files: " + e.getMessage());
        }
    }

    private void scanLogicsheetRules(String dirPath, String outputSubdir) throws IOException {
        logWriter.writeMessage("Logicsheet Rules with Database Actions Report");

        try (Stream<Path> paths = Files.walk(Paths.get(dirPath))) {
            List<File> xmlFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith("controller_rules.xml"))
                    .map(Path::toFile)
                    .filter(file -> !file.getName().equals("GetWICDetails_Controller_rules.xml"))
                    .collect(Collectors.toList());

            for (File file : xmlFiles) {
                logWriter.writeMessage("Processing rules file: " + file.getName());
                try {
                    List<String> dbActions = xmlFinder.findMatchingElements(file);
                    if (!dbActions.isEmpty()) {
                        logWriter.writeMessage("Found " + dbActions.size() + " database actions in " + file.getName());
                        for (String action : dbActions) {
                            logWriter.writeElement(action);
                        }
                        copyFileToResultDir(file, outputSubdir);
                    } else {
                        logWriter.writeMessage("No database actions found in " + file.getName());
                    }
                } catch (Exception e) {
                    logWriter.writeMessage("Error processing " + file.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    private void scanBindingsFiles(String dirPath, String outputSubdir) throws IOException {
        logWriter.writeMessage("Hyfinity Bindings Mapping Report");

        try (Stream<Path> paths = Files.walk(Paths.get(dirPath))) {
            List<File> bindingFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith("_bindings.xml"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());

            for (File file : bindingFiles) {
                logWriter.writeMessage("Processing bindings file: " + file.getName());
                try {
                    List<String> mappings = bindingFinder.findMappings(file);
                    if (!mappings.isEmpty()) {
                        logWriter.writeMessage("Found " + mappings.size() + " mappings in " + file.getName());
                        for (String mapping : mappings) {
                            logWriter.writeElement(mapping);
                        }
                        copyFileToResultDir(file, outputSubdir);
                    } else {
                        logWriter.writeMessage("No mappings found in " + file.getName());
                    }
                } catch (Exception e) {
                    logWriter.writeMessage("Error processing " + file.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    private void scanDirectoriesWithPattern(String basePath,
                                            String targetSubdir, DirectoryScanner scanner,
                                            String outputSubdir) throws IOException {
        File baseDir = new File(basePath);
        File[] directories = baseDir.listFiles(File::isDirectory);

        if (directories != null) {
            for (File dir : directories) {
                //if (dir.getName().endsWith(dirSuffix)) {
                    File targetDir = new File(dir, targetSubdir);
                    if (targetDir.exists() && targetDir.isDirectory()) {
                        logWriter.writeMessage("Found " + targetSubdir + " in " + dir.getName());
                        scanner.scan(targetDir.getAbsolutePath(), outputSubdir);
                    }
                //}
            }
        }
    }
}