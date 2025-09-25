package com.bizflow.bas.webmaker;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HtmlReportGenerator {
    private final String resultDirPath;
    private final String timestamp;
    private final List<SqlQueryInfo> allSqlQueries = new ArrayList<>();

    private static class SqlQueryInfo {
        String fileName;
        String ruleId;
        String sqlQuery;

        public SqlQueryInfo(String fileName, String ruleId, String sqlQuery) {
            this.fileName = fileName;
            this.ruleId = ruleId;
            this.sqlQuery = sqlQuery;
        }
    }

    public HtmlReportGenerator(String resultDirPath) {
        this.resultDirPath = resultDirPath;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }

    public String generateReport() throws IOException {
        String reportFileName = "REPORT_" + timestamp + ".html";
        String reportPath = resultDirPath + File.separator + reportFileName;

        // Get all subdirectories sorted alphabetically
        List<File> subDirs = getSortedSubdirectories();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(reportPath))) {
            // Write HTML header
            writer.write("<!DOCTYPE html>\n");
            writer.write("<html lang=\"en\">\n");
            writer.write("<head>\n");
            writer.write("  <meta charset=\"UTF-8\">\n");
            writer.write("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            writer.write("  <title>WebMaker Analyzer Report</title>\n");
            writer.write("  <style>\n");
            writer.write("    :root {\n");
            writer.write("      --primary-color: #2c3e50;\n");
            writer.write("      --secondary-color: #3498db;\n");
            writer.write("      --accent-color: #e74c3c;\n");
            writer.write("      --light-bg: #f8f9fa;\n");
            writer.write("      --border-color: #e9ecef;\n");
            writer.write("      --text-color: #343a40;\n");
            writer.write("    }\n");
            writer.write("    body {\n");
            writer.write("      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', sans-serif;\n");
            writer.write("      color: var(--text-color);\n");
            writer.write("      line-height: 1.6;\n");
            writer.write("      margin: 0;\n");
            writer.write("      padding: 20px;\n");
            writer.write("      max-width: 1400px;\n");
            writer.write("      margin: 0 auto;\n");
            writer.write("      background-color: #ffffff;\n");
            writer.write("    }\n");
            writer.write("    h1 {\n");
            writer.write("      color: var(--primary-color);\n");
            writer.write("      border-bottom: 2px solid var(--secondary-color);\n");
            writer.write("      padding-bottom: 15px;\n");
            writer.write("      margin-top: 30px;\n");
            writer.write("      font-weight: 600;\n");
            writer.write("      letter-spacing: -0.5px;\n");
            writer.write("    }\n");
            writer.write("    h2 {\n");
            writer.write("      color: var(--secondary-color);\n");
            writer.write("      margin-top: 40px;\n");
            writer.write("      border-bottom: 1px solid var(--border-color);\n");
            writer.write("      padding-bottom: 8px;\n");
            writer.write("      font-weight: 500;\n");
            writer.write("      letter-spacing: -0.3px;\n");
            writer.write("    }\n");
            writer.write("    h3 {\n");
            writer.write("      margin-top: 25px;\n");
            writer.write("      color: var(--primary-color);\n");
            writer.write("      font-weight: 500;\n");
            writer.write("      letter-spacing: -0.2px;\n");
            writer.write("    }\n");
            writer.write("    .folder-content {\n");
            writer.write("      margin-left: 20px;\n");
            writer.write("      padding: 10px 0;\n");
            writer.write("    }\n");
            writer.write("    .thumbnails {\n");
            writer.write("      display: flex;\n");
            writer.write("      flex-wrap: wrap;\n");
            writer.write("      gap: 24px;\n");
            writer.write("      margin: 20px 0;\n");
            writer.write("    }\n");
            writer.write("    .thumbnail {\n");
            writer.write("      display: flex;\n");
            writer.write("      flex-direction: column;\n");
            writer.write("      align-items: center;\n");
            writer.write("      margin-bottom: 20px;\n");
            writer.write("      border-radius: 10px;\n");
            writer.write("      overflow: hidden;\n");
            writer.write("      box-shadow: 0 4px 12px rgba(0,0,0,0.08);\n");
            writer.write("      transition: all 0.3s ease;\n");
            writer.write("      background-color: white;\n");
            writer.write("    }\n");
            writer.write("    .thumbnail:hover {\n");
            writer.write("      transform: translateY(-6px);\n");
            writer.write("      box-shadow: 0 8px 20px rgba(0,0,0,0.12);\n");
            writer.write("    }\n");
            writer.write("    .thumbnail img {\n");
            writer.write("      max-width: 350px;\n");
            writer.write("      border-bottom: 1px solid var(--border-color);\n");
            writer.write("      cursor: pointer;\n");
            writer.write("    }\n");
            writer.write("    .xml-content {\n");
            writer.write("      background-color: var(--light-bg);\n");
            writer.write("      padding: 18px;\n");
            writer.write("      border-radius: 8px;\n");
            writer.write("      border: 1px solid var(--border-color);\n");
            writer.write("      overflow: auto;\n");
            writer.write("      margin-bottom: 24px;\n");
            writer.write("      box-shadow: inset 0 1px 3px rgba(0,0,0,0.05);\n");
            writer.write("    }\n");
            writer.write("    .xml-content a {\n");
            writer.write("      display: inline-block;\n");
            writer.write("      margin-bottom: 12px;\n");
            writer.write("      color: var(--secondary-color);\n");
            writer.write("      text-decoration: none;\n");
            writer.write("      font-weight: 500;\n");
            writer.write("      padding: 8px 14px;\n");
            writer.write("      border-radius: 5px;\n");
            writer.write("      background-color: rgba(52, 152, 219, 0.1);\n");
            writer.write("      transition: all 0.2s ease;\n");
            writer.write("    }\n");
            writer.write("    .xml-content a:hover {\n");
            writer.write("      background-color: rgba(52, 152, 219, 0.2);\n");
            writer.write("      transform: translateY(-2px);\n");
            writer.write("      box-shadow: 0 3px 8px rgba(0,0,0,0.1);\n");
            writer.write("    }\n");
            writer.write("    pre {\n");
            writer.write("      white-space: pre-wrap;\n");
            writer.write("      font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;\n");
            writer.write("      margin: 0;\n");
            writer.write("      font-size: 14px;\n");
            writer.write("      line-height: 1.6;\n");
            writer.write("      padding: 5px;\n");
            writer.write("    }\n");
            writer.write("    .file-link {\n");
            writer.write("      display: block;\n");
            writer.write("      margin-top: 10px;\n");
            writer.write("      padding: 10px 15px;\n");
            writer.write("      background-color: var(--light-bg);\n");
            writer.write("      border-radius: 0 0 8px 8px;\n");
            writer.write("      width: 100%;\n");
            writer.write("      text-align: center;\n");
            writer.write("      transition: background-color 0.2s ease;\n");
            writer.write("    }\n");
            writer.write("    .file-link a {\n");
            writer.write("      color: var(--secondary-color);\n");
            writer.write("      text-decoration: none;\n");
            writer.write("      font-weight: 500;\n");
            writer.write("      transition: color 0.2s ease;\n");
            writer.write("    }\n");
            writer.write("    .file-link:hover {\n");
            writer.write("      background-color: rgba(52, 152, 219, 0.1);\n");
            writer.write("    }\n");
            writer.write("    .file-link a:hover {\n");
            writer.write("      color: var(--primary-color);\n");
            writer.write("    }\n");
            writer.write("    .toc {\n");
            writer.write("      background-color: var(--light-bg);\n");
            writer.write("      padding: 25px;\n");
            writer.write("      margin: 35px 0;\n");
            writer.write("      border-radius: 10px;\n");
            writer.write("      border: 1px solid var(--border-color);\n");
            writer.write("      box-shadow: 0 3px 10px rgba(0,0,0,0.05);\n");
            writer.write("    }\n");
            writer.write("    .toc h2 {\n");
            writer.write("      margin-top: 0;\n");
            writer.write("      padding-bottom: 12px;\n");
            writer.write("      border-bottom: 1px solid var(--border-color);\n");
            writer.write("    }\n");
            writer.write("    .toc ol {\n");
            writer.write("      padding-left: 28px;\n");
            writer.write("      margin-bottom: 0;\n");
            writer.write("    }\n");
            writer.write("    .toc li {\n");
            writer.write("      margin-bottom: 10px;\n");
            writer.write("    }\n");
            writer.write("    .toc a {\n");
            writer.write("      color: var(--secondary-color);\n");
            writer.write("      text-decoration: none;\n");
            writer.write("      transition: all 0.2s ease;\n");
            writer.write("      padding: 3px 6px;\n");
            writer.write("      border-radius: 4px;\n");
            writer.write("    }\n");
            writer.write("    .toc a:hover {\n");
            writer.write("      color: var(--primary-color);\n");
            writer.write("      background-color: rgba(52, 152, 219, 0.1);\n");
            writer.write("    }\n");
            writer.write("    .sql-highlight {\n");
            writer.write("      font-weight: bold;\n");
            writer.write("      color: var(--accent-color);\n");
            writer.write("      background-color: rgba(231, 76, 60, 0.1);\n");
            writer.write("      padding: 3px 5px;\n");
            writer.write("      border-radius: 3px;\n");
            writer.write("    }\n");
            writer.write("    .collapsible {\n");
            writer.write("      background-color: var(--light-bg);\n");
            writer.write("      color: var(--primary-color);\n");
            writer.write("      cursor: pointer;\n");
            writer.write("      padding: 14px 18px;\n");
            writer.write("      width: 100%;\n");
            writer.write("      text-align: left;\n");
            writer.write("      font-size: 15px;\n");
            writer.write("      border: 1px solid var(--border-color);\n");
            writer.write("      border-radius: 8px;\n");
            writer.write("      margin-top: 12px;\n");
            writer.write("      font-weight: 500;\n");
            writer.write("      transition: all 0.2s ease;\n");
            writer.write("      box-shadow: 0 1px 3px rgba(0,0,0,0.05);\n");
            writer.write("    }\n");
            writer.write("    .active, .collapsible:hover {\n");
            writer.write("      background-color: #e9ecef;\n");
            writer.write("      box-shadow: 0 2px 5px rgba(0,0,0,0.08);\n");
            writer.write("    }\n");
            writer.write("    .content {\n");
            writer.write("      padding: 0;\n");
            writer.write("      margin-top: -6px;\n");
            writer.write("      margin-bottom: 15px;\n");
            writer.write("      display: none;\n");
            writer.write("      overflow: hidden;\n");
            writer.write("      border-radius: 0 0 8px 8px;\n");
            writer.write("      transition: all 0.3s ease-in-out;\n");
            writer.write("    }\n");
            writer.write("    table {\n");
            writer.write("      border-collapse: collapse;\n");
            writer.write("      width: 100%;\n");
            writer.write("      margin: 20px 0;\n");
            writer.write("      border-radius: 8px;\n");
            writer.write("      overflow: hidden;\n");
            writer.write("      box-shadow: 0 3px 8px rgba(0,0,0,0.06);\n");
            writer.write("    }\n");
            writer.write("    th, td {\n");
            writer.write("      border: 1px solid var(--border-color);\n");
            writer.write("      padding: 14px 18px;\n");
            writer.write("      text-align: left;\n");
            writer.write("    }\n");
            writer.write("    th {\n");
            writer.write("      background-color: var(--secondary-color);\n");
            writer.write("      color: white;\n");
            writer.write("      font-weight: 500;\n");
            writer.write("      letter-spacing: 0.3px;\n");
            writer.write("    }\n");
            writer.write("    tr:nth-child(even) {\n");
            writer.write("      background-color: rgba(0,0,0,0.02);\n");
            writer.write("    }\n");
            writer.write("    tr:hover {\n");
            writer.write("      background-color: rgba(0,0,0,0.05);\n");
            writer.write("    }\n");
            writer.write("    @media (max-width: 768px) {\n");
            writer.write("      .thumbnails { justify-content: center; }\n");
            writer.write("      .thumbnail img { max-width: 100%; }\n");
            writer.write("      body { padding: 15px; }\n");
            writer.write("      th, td { padding: 10px 12px; }\n");
            writer.write("    }\n");
            writer.write("    @media print {\n");
            writer.write("      body { font-size: 12pt; }\n");
            writer.write("      .collapsible { display: none; }\n");
            writer.write("      .content { display: block !important; }\n");
            writer.write("      .thumbnails { gap: 10px; }\n");
            writer.write("      a { text-decoration: none !important; color: black; }\n");
            writer.write("    }\n");

            writer.write("    /* Export section styling */\n");
            writer.write("    .export-section {\n");
            writer.write("      background-color: #f0f8ff; /* Light blue background */\n");
            writer.write("      border-left: 4px solid #3498db;\n");
            writer.write("      border-radius: 10px;\n");
            writer.write("      padding: 20px;\n");
            writer.write("      margin-bottom: 30px;\n");
            writer.write("      box-shadow: 0 3px 10px rgba(52, 152, 219, 0.1);\n");
            writer.write("    }\n");
            writer.write("    .export-section h2 {\n");
            writer.write("      color: #2980b9;\n");
            writer.write("      display: flex;\n");
            writer.write("      align-items: center;\n");
            writer.write("      gap: 10px;\n");
            writer.write("    }\n");
            writer.write("    .export-badge {\n");
            writer.write("      background-color: #3498db;\n");
            writer.write("      color: white;\n");
            writer.write("      padding: 4px 8px;\n");
            writer.write("      border-radius: 6px;\n");
            writer.write("      font-size: 14px;\n");
            writer.write("      font-weight: 500;\n");
            writer.write("      display: inline-block;\n");
            writer.write("      margin-left: 10px;\n");
            writer.write("    }\n");
            writer.write("    .export-section .folder-content {\n");
            writer.write("      border-top: 1px dashed rgba(52, 152, 219, 0.3);\n");
            writer.write("      margin-top: 15px;\n");
            writer.write("      padding-top: 15px;\n");
            writer.write("    }\n");
            writer.write("    .toc li.export-item a {\n");
            writer.write("      color: #2980b9;\n");
            writer.write("      font-weight: 500;\n");
            writer.write("    }\n");
        //    writer.write("    .toc li.export-item:before {\n");
        //    writer.write("      content: '📤 ';\n");
        //    writer.write("    }\n");
            writer.write("  </style>\n");
            writer.write("</head>\n");
            writer.write("<body>\n");

            // Main title with user-friendly timestamp
            String formattedDate = LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            writer.write("  <h1>WebMaker Analyzer Report - " + formattedDate + "</h1>\n");

            // Table of contents
            writer.write("  <div class=\"toc\">\n");
            writer.write("    <h2>Contents</h2>\n");
            writer.write("    <ul>\n");
            int tocIndex = 1;
            for (File subDir : subDirs) {
                String displayIndex = String.valueOf(tocIndex);
                String exportClass = "";
                if (subDir.getName().endsWith("_export")) {
                    displayIndex = "E" + tocIndex;
                    exportClass = " class=\"export-item\"";
                }
                writer.write("      <li" + exportClass + "><a href=\"#" + subDir.getName() + "\">" +
                        displayIndex + ". " + subDir.getName() + "</a></li>\n");
                tocIndex++;
            }
            writer.write("      <li><a href=\"#appendix\">Appendix - SQL Queries</a></li>\n");
            writer.write("    </ul>\n");
            writer.write("  </div>\n");

            // Generate content for each subdirectory
            int folderIndex = 1;
            for (File subDir : subDirs) {
                generateFolderContent(writer, subDir, folderIndex);
                folderIndex++;
            }

            // Generate Appendix with all SQL queries
            generateSqlAppendix(writer);

            // Add JavaScript for collapsible sections
            writer.write("<script>\n");
            writer.write("var coll = document.getElementsByClassName('collapsible');\n");
            writer.write("for (var i = 0; i < coll.length; i++) {\n");
            writer.write("  coll[i].addEventListener('click', function() {\n");
            writer.write("    this.classList.toggle('active');\n");
            writer.write("    var content = this.nextElementSibling;\n");
            writer.write("    if (content.style.display === 'block') {\n");
            writer.write("      content.style.display = 'none';\n");
            writer.write("      this.textContent = this.textContent.replace('[-]', '[+]');\n");
            writer.write("    } else {\n");
            writer.write("      content.style.display = 'block';\n");
            writer.write("      this.textContent = this.textContent.replace('[+]', '[-]');\n");
            writer.write("    }\n");
            writer.write("  });\n");
            writer.write("}\n");
            writer.write("</script>\n");

            // Close HTML
            writer.write("</body>\n");
            writer.write("</html>\n");
        }

        return reportPath;
    }

    private void generateSqlAppendix(BufferedWriter writer) throws IOException {
        writer.write("  <h2 id=\"appendix\">Appendix - SQL Queries</h2>\n");
        writer.write("  <div class=\"folder-content\">\n");

        if (allSqlQueries.isEmpty()) {
            writer.write("    <p>No SQL queries found in the analyzed files.</p>\n");
        } else {
            writer.write("    <table>\n");
            writer.write("      <thead>\n");
            writer.write("        <tr>\n");
            writer.write("          <th>Filename</th>\n");
            writer.write("          <th>SQL Query</th>\n");
            writer.write("        </tr>\n");
            writer.write("      </thead>\n");
            writer.write("      <tbody>\n");

            for (SqlQueryInfo sql : allSqlQueries) {
                writer.write("        <tr>\n");
                writer.write("          <td>" + sql.fileName + "</td>\n");
                writer.write("          <td><pre class=\"sql-highlight\">" + sql.sqlQuery + "</pre></td>\n");
                writer.write("        </tr>\n");
            }

            writer.write("      </tbody>\n");
            writer.write("    </table>\n");
        }

        writer.write("  </div>\n");
    }

    private List<File> getSortedSubdirectories() {
        File resultDir = new File(resultDirPath);
        File[] dirs = resultDir.listFiles(File::isDirectory);
        List<File> sortedDirs = dirs != null ? Arrays.asList(dirs) : new ArrayList<>();
        sortedDirs.sort(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        return sortedDirs;
    }

    private void generateFolderContent(BufferedWriter writer, File folder, int index) throws IOException {
        String folderName = folder.getName();
        boolean isExportSection = folderName.endsWith("_export");

        // Special handling for folders ending with _export
        String displayIndex = String.valueOf(index);
        if (isExportSection) {
            displayIndex = "E" + index;
        }

        // Add special styling for export sections
        String sectionClass = isExportSection ? " class=\"export-section\"" : "";
        writer.write("  <div" + sectionClass + ">\n");

        // Add the heading with special badge for export sections
        writer.write("    <h2 id=\"" + folderName + "\">" + displayIndex + ". " + folderName);
        if (isExportSection) {
            writer.write(" <span class=\"export-badge\">Analysis</span>");
        }
        writer.write("</h2>\n");

        writer.write("    <div class=\"folder-content\">\n");

        // Process PNG files and link to related HTML files
        Map<String, File> pngFiles = new HashMap<>();
        Map<String, File> htmlFiles = new HashMap<>();
        Map<String, File> xmlFiles = new HashMap<>();
        Map<String, File> bindingFiles = new HashMap<>();
        Map<String, File> jsFiles = new HashMap<>();

        // Collect files by type
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName().toLowerCase();
                if (fileName.endsWith(".png")) {
                    // Extract base name for matching
                    String baseName = extractBaseName(fileName);
                    pngFiles.put(baseName, file);
                } else if (fileName.endsWith(".html")) {
                    String baseName = extractBaseName(fileName);
                    htmlFiles.put(baseName, file);
                } else if (fileName.endsWith("controller_rules.xml")) {
                    xmlFiles.put(file.getName(), file);
                } else if (fileName.endsWith("_bindings.xml")) {
                    bindingFiles.put(file.getName(), file);
                } else if (fileName.endsWith(".js")) {
                    jsFiles.put(file.getName(), file);
                }
            }
        }

        // Display PNG thumbnails with HTML links
        if (!pngFiles.isEmpty()) {
            writer.write("    <h3>Forms</h3>\n");
            writer.write("    <div class=\"thumbnails\">\n");

            for (Map.Entry<String, File> entry : pngFiles.entrySet()) {
                String baseName = entry.getKey();
                File pngFile = entry.getValue();
                File matchingHtml = findMatchingHtmlFile(baseName, htmlFiles);

                if (matchingHtml != null) {
                    writer.write("      <div class=\"thumbnail\">\n");

                    // Make the image clickable to open the HTML file
                    String relativeHtmlPath = folderName + "/" + matchingHtml.getName();
                    String relativeImgPath = folderName + "/" + pngFile.getName();

                    writer.write("        <a href=\"" + relativeImgPath + "\" target=\"_blank\">\n");
                    writer.write("          <img src=\"" + relativeImgPath + "\" alt=\"" + pngFile.getName() + "\">\n");
                    writer.write("        </a>\n");
                    writer.write("        <div class=\"file-link\">\n");
                    writer.write("          <a href=\"" + relativeHtmlPath + "\" target=\"_blank\">" + matchingHtml.getName() + "</a>\n");
                    writer.write("        </div>\n");
                    writer.write("      </div>\n");
                }
            }
            writer.write("    </div>\n");
        }

        // Display XML files with formatted content
        if (!xmlFiles.isEmpty()) {
            writer.write("    <h3>WebMaker Rules</h3>\n");

            for (Map.Entry<String, File> entry : xmlFiles.entrySet()) {
                File xmlFile = entry.getValue();
                writer.write("    <button class=\"collapsible\">[+] " + xmlFile.getName() + "</button>\n");
                writer.write("    <div class=\"content\">\n");

                try {
                    processXmlFile(writer, xmlFile, folderName);
                } catch (Exception e) {
                    writer.write("      <div class=\"xml-content\">\n");
                    writer.write("        <pre>Error formatting XML: " + e.getMessage() + "</pre>\n");
                    // Write raw content as fallback
                    writer.write("        <pre>" + Files.readString(xmlFile.toPath())
                            .replace("&", "&amp;")
                            .replace("<", "&lt;")
                            .replace(">", "&gt;") + "</pre>\n");
                    writer.write("      </div>\n");
                }

                writer.write("    </div>\n");
            }
        }

        // Display Binding files
        if (!bindingFiles.isEmpty()) {
            writer.write("    <h3>Binding Information</h3>\n");

            for (Map.Entry<String, File> entry : bindingFiles.entrySet()) {
                File bindingFile = entry.getValue();
                String relativePath = folderName + "/" + bindingFile.getName();

                writer.write("    <button class=\"collapsible\">[+] " + bindingFile.getName() + "</button>\n");
                writer.write("    <div class=\"content\">\n");

                try {
                    processBindingFile(writer, bindingFile, relativePath);
                } catch (Exception e) {
                    writer.write("      <div class=\"xml-content\">\n");
                    writer.write("        <pre>Error processing binding file: " + e.getMessage() + "</pre>\n");
                    writer.write("      </div>\n");
                }

                writer.write("    </div>\n");
            }
        }

        // Display JavaScript files
        if (!jsFiles.isEmpty()) {
            writer.write("    <h3>&lt;-- JavaScript Files --&gt;</h3> \n");

            for (Map.Entry<String, File> entry : jsFiles.entrySet()) {
                File jsFile = entry.getValue();
                String relativePath = folderName + "/" + jsFile.getName();

                writer.write("    <button class=\"collapsible\">[+] " + jsFile.getName() + "</button>\n");
                writer.write("    <div class=\"content\">\n");

                try {
                    processJsFile(writer, jsFile, relativePath);
                } catch (Exception e) {
                    writer.write("      <div class=\"xml-content\">\n");
                    writer.write("        <pre>Error processing JavaScript file: " + e.getMessage() + "</pre>\n");
                    writer.write("      </div>\n");
                }

                writer.write("    </div>\n");
            }
        }

        // Close both divs properly
        writer.write("    </div>\n");  // Close folder-content div
        writer.write("  </div>\n");    // Close section div
    }

    private void processBindingFile(BufferedWriter writer, File bindingFile, String relativePath)
            throws IOException, ParserConfigurationException, SAXException {

        XmlBindingFinder bindingFinder = new XmlBindingFinder();
        List<String> mappings = bindingFinder.findMappings(bindingFile);

        writer.write("      <div class=\"xml-content\">\n");
        writer.write("        <a href=\"" + relativePath + "\" target=\"_blank\">Open binding file</a>\n");

        if (mappings.isEmpty()) {
            writer.write("        <pre>No mappings found in this binding file.</pre>\n");
        } else {
            writer.write("        <pre>");
            for (String mapping : mappings) {
                // HTML escape the content
                String escapedMapping = mapping
                        .replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;");

                // Highlight ProcessVariables entries
                if (escapedMapping.contains("ProcessVariables")) {
                    escapedMapping = escapedMapping.replaceAll(
                            "(.*ProcessVariables.*)",
                            "<span class=\"sql-highlight\">$1</span>");
                }

                writer.write(escapedMapping + "\n");
            }
            writer.write("</pre>\n");
        }

        writer.write("      </div>\n");
    }

    private void processXmlFile(BufferedWriter writer, File xmlFile, String folderName)
            throws IOException {
        // Create relative path for the file link
        String relativePath = folderName + "/" + xmlFile.getName();

        // Read the XML file as text
        String xmlContent = Files.readString(xmlFile.toPath());

        // First mark SQL statements with unique tokens (before HTML escaping)
        String START_TOKEN = "##SQL_HIGHLIGHT_START##";
        String END_TOKEN = "##SQL_HIGHLIGHT_END##";

        Pattern originalPattern = Pattern.compile(
                "<hy:param\\s+name=\"sql_statement\"\\s+type=\"java\\.lang\\.String\">(.*?)</hy:param>",
                Pattern.DOTALL);

        StringBuffer tokenizedXml = new StringBuffer();
        Matcher originalMatcher = originalPattern.matcher(xmlContent);

        // Find all SQL statements and mark them with tokens
        while (originalMatcher.find()) {
            String sqlContent = originalMatcher.group(1);
            String ruleId = "Unknown";

            // Try to find rule ID
            int paramPos = xmlContent.indexOf(originalMatcher.group(0));
            int ruleStart = xmlContent.lastIndexOf("<rule ", paramPos);
            if (ruleStart >= 0) {
                int idStart = xmlContent.indexOf("id=\"", ruleStart);
                if (idStart >= 0) {
                    idStart += 4; // length of 'id="'
                    int idEnd = xmlContent.indexOf("\"", idStart);
                    if (idEnd > idStart) {
                        ruleId = xmlContent.substring(idStart, idEnd);
                    }
                }
            }

            // Add to SQL list
            allSqlQueries.add(new SqlQueryInfo(xmlFile.getName(), ruleId, sqlContent));

            // Replace with tokens
            String replacement = "<hy:param name=\"sql_statement\" type=\"java.lang.String\">" +
                    START_TOKEN + sqlContent + END_TOKEN +
                    "</hy:param>";

            originalMatcher.appendReplacement(tokenizedXml, Matcher.quoteReplacement(replacement));
        }
        originalMatcher.appendTail(tokenizedXml);

        // HTML escape the content with tokens
        String escapedXml = tokenizedXml.toString()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");

        // Replace tokens with span elements
        String highlightedXml = escapedXml
                .replace(START_TOKEN, "<span class=\"sql-highlight\">")
                .replace(END_TOKEN, "</span>");

        writer.write("      <div class=\"xml-content\">\n");
        writer.write("        <a href=\"" + relativePath + "\" target=\"_blank\">Open rules file</a>\n");
        writer.write("        <pre>" + highlightedXml + "</pre>\n");
        writer.write("      </div>\n");
    }

    private void processJsFile(BufferedWriter writer, File jsFile, String relativePath)
            throws IOException {
        // Read the JavaScript file content
        String jsContent = Files.readString(jsFile.toPath());

        // HTML escape the content
        String escapedJs = jsContent
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");

        writer.write("      <div class=\"xml-content\">\n");
        writer.write("        <a href=\"" + relativePath + "\" target=\"_blank\">Open JavaScript file</a>\n");
        writer.write("        <pre>" + escapedJs + "</pre>\n");
        writer.write("      </div>\n");
    }

    private String findRuleIdForSql(Document document, String sqlQuery) {
        NodeList ruleNodes = document.getElementsByTagName("rule");
        for (int i = 0; i < ruleNodes.getLength(); i++) {
            Element rule = (Element) ruleNodes.item(i);
            NodeList paramNodes = rule.getElementsByTagNameNS("*", "param");

            for (int j = 0; j < paramNodes.getLength(); j++) {
                Element param = (Element) paramNodes.item(j);
                if ("sql_statement".equals(param.getAttribute("name")) &&
                        param.getTextContent().trim().equals(sqlQuery.trim())) {
                    return rule.getAttribute("id");
                }
            }
        }
        return "Unknown";
    }

    private String extractBaseName(String fileName) {
        // Extract base name from file names for matching
        // Example: convert "AssignEngineerForm_124.png" to "assignengineerform"
        String name = fileName.toLowerCase();
        if (name.contains(".")) {
            name = name.substring(0, name.lastIndexOf("."));
        }
        // Remove numeric suffixes
        name = name.replaceAll("_\\d+$", "");
        // Remove common prefixes/suffixes
        name = name.replace("page_preview_", "");
        return name;
    }

    private File findMatchingHtmlFile(String baseName, Map<String, File> htmlFiles) {
        // Try direct match first
        if (htmlFiles.containsKey(baseName)) {
            return htmlFiles.get(baseName);
        }

        // Try to find closest match
        for (String htmlKey : htmlFiles.keySet()) {
            if (htmlKey.contains(baseName) || baseName.contains(htmlKey)) {
                return htmlFiles.get(htmlKey);
            }
        }

        return null;
    }

    private String formatXml(Document document) throws TransformerException {
        // Format the XML
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));

        // Escape HTML characters for display in pre tag
        return writer.toString()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}