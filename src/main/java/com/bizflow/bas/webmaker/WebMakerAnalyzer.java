package com.bizflow.bas.webmaker;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * WebMakerAnalyzer
 *
 * This class is responsible for analyzing WebMaker application files. It processes both zip files
 * and unzipped directories, scans for specific file types, and generates a structured report.
 * The analysis includes copying relevant files to a result directory and creating an HTML report.
 *
 * The class supports:
 * - Processing directories and zip files recursively
 * - Extracting zip files to temporary directories
 * - Scanning for JavaScript, HTML, thumbnail, and XML binding files
 * - Generating a log file and an HTML report
 *
 * Usage:
 * 1. Run the application with paths to directories or zip files as arguments.
 * 2. The results will be saved in the `WMReportResult` directory.
 *
 * Example:
 *   java -jar WebMakerAnalyzer.jar C:/webmaker_files C:/another_file.zip
 *
 * Author: Taeho Lee (taeho.share@gmail.com, thlee@bizflow.com)
 * Version: 1.0
 * Since: 2025-09-15
 */
public class WebMakerAnalyzer {

    private static final List<Path> tempDirectories = new ArrayList<>();

    /**
     * The main entry point of the application.
     *
     * @param args Command-line arguments specifying paths to directories or zip files
     */
    public static void main(String[] args) {
        FileScanManager scanner = null;

        if (args.length < 1) {
            System.out.println("Usage: java -jar WebMakerAnalyzer.jar <path1> [path2] [path3] ...");
            System.out.println("Paths can be directories or zip files");
            System.out.println("Example: java -jar WebMakerAnalyzer.jar C:/webmaker_files C:/another_file.zip");
            return;
        }

        try {
            clearResultDirectory();

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String logFileName = "REPORT_" + timestamp + ".LOG";

            scanner = new FileScanManager(logFileName, "");

            for (String inputPath : args) {
                File input = new File(inputPath);

                if (!input.exists()) {
                    System.out.println("Error: The path does not exist: " + inputPath);
                    continue;
                }

                if (input.isDirectory()) {
                    String subDirName = getSubdirectoryName(input);
                    System.out.println("Processing directory: " + inputPath);
                    scanner.scanAll(inputPath, subDirName);

                    List<File> zipFiles = findZipFilesInDirectory(inputPath);
                    if (!zipFiles.isEmpty()) {
                        System.out.println("Found " + zipFiles.size() + " zip files in directory: " + inputPath);
                        for (File zipFile : zipFiles) {
                            String zipSubDirName = getSubdirectoryName(zipFile);
                            System.out.println("Processing zip file: " + zipFile.getAbsolutePath());
                            Path extractedPath = extractZipFile(zipFile.getAbsolutePath());
                            if (extractedPath != null) {
                                tempDirectories.add(extractedPath);
                                scanner.scanAll(extractedPath.toString(), zipSubDirName);
                            }
                        }
                    }
                } else if (inputPath.toLowerCase().endsWith(".zip")) {
                    String subDirName = getSubdirectoryName(input);
                    System.out.println("Processing zip file: " + inputPath);
                    Path extractedPath = extractZipFile(inputPath);
                    if (extractedPath != null) {
                        tempDirectories.add(extractedPath);
                        scanner.scanAll(extractedPath.toString(), subDirName);
                    }
                } else {
                    System.out.println("Skipping unsupported file: " + inputPath);
                }
            }

            HtmlReportGenerator htmlGenerator = new HtmlReportGenerator("WMReportResult");
            String htmlReportPath = htmlGenerator.generateReport();

            System.out.println("Analysis complete. Results written to: " + scanner.getLogFilePath());
            System.out.println("HTML report generated at: " + htmlReportPath);
            System.out.println("Found files copied to: " + new File("WMReportResult").getAbsolutePath());

        } catch (Exception e) {
            System.err.println("Error during analysis: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (scanner != null) {
                scanner.close();
            }
            cleanupTempDirectories();
        }
    }

    /**
     * Finds all zip files in the specified directory.
     *
     * @param dirPath The path to the directory to search
     * @return A list of zip files found in the directory
     */
    private static List<File> findZipFilesInDirectory(String dirPath) {
        List<File> zipFiles = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(dirPath))) {
            zipFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".zip"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("Error scanning for zip files in " + dirPath + ": " + e.getMessage());
        }
        return zipFiles;
    }

    /**
     * Generates a sanitized subdirectory name based on the input file or directory name.
     *
     * @param input The input file or directory
     * @return A sanitized subdirectory name
     */
    private static String getSubdirectoryName(File input) {
        String name = input.getName();
        if (name.toLowerCase().endsWith(".zip")) {
            name = name.substring(0, name.lastIndexOf('.'));
        }
        return name.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    /**
     * Extracts the contents of a zip file to a temporary directory.
     *
     * @param zipFilePath The path to the zip file
     * @return The path to the temporary directory where the zip file was extracted
     * @throws IOException If an error occurs during extraction
     */
    private static Path extractZipFile(String zipFilePath) throws IOException {
        Path tempDir = Files.createTempDirectory("webmaker_analyzer_");
        System.out.println("Extracting zip to: " + tempDir);

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];

            while ((entry = zis.getNextEntry()) != null) {
                Path filePath = tempDir.resolve(entry.getName());
                Files.createDirectories(filePath.getParent());

                if (!entry.isDirectory()) {
                    try (OutputStream os = Files.newOutputStream(filePath)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            os.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            System.err.println("Error extracting zip file: " + e.getMessage());
            cleanupDirectory(tempDir);
            throw e;
        }

        return tempDir;
    }

    /**
     * Cleans up temporary directories created during the analysis.
     */
    private static void cleanupTempDirectories() {
        for (Path tempDir : tempDirectories) {
            cleanupDirectory(tempDir);
        }
    }

    /**
     * Deletes all files and subdirectories in the specified directory.
     *
     * @param dir The directory to clean up
     */
    private static void cleanupDirectory(Path dir) {
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.err.println("Error cleaning up directory " + dir + ": " + e.getMessage());
        }
    }

    /**
     * Clears the `WMReportResult` directory by deleting all files and subdirectories.
     *
     * @throws IOException If an error occurs during the cleanup
     */
    private static void clearResultDirectory() throws IOException {
        Path resultDir = Paths.get("WMReportResult");

        if (!Files.exists(resultDir)) {
            Files.createDirectory(resultDir);
            return;
        }

        Files.walkFileTree(resultDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (!dir.equals(resultDir)) {
                    Files.delete(dir);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        System.out.println("Cleared all files and subdirectories in WMReportResult directory");
    }
}