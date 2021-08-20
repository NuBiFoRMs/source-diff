package com.nubiform.sourcediff.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@UtilityClass
public class SvnUtils {

    public static final String REVISION = "revision";
    public static final String AUTHOR = "author";
    public static final String DATE = "date";
    public static final String MSG = "msg";

    public static void checkout(String url, String revision, File location, String svnUser, String svnPassword) {
        log.info("checkout: {}@{} -> {}", url, revision, location.getAbsolutePath());
        try {
            Map<String, Object> param = new HashMap<>();

            // svn revert
            String command = "svn revert -R '" + location.getAbsolutePath() + "' --username '" + svnUser + "' --password '" + svnPassword + "'";
            try {
                executeCommand(command);
            } catch (Exception e) {
                log.info("ignore exception: {}", e.getMessage());
            }

            // svn checkout
            command = "svn co --depth infinity '" + url + "@" + revision + "' '" + location.getAbsolutePath() + "' --username '" + svnUser + "' --password '" + svnPassword + "'";
            executeCommand(command);

        } catch (Exception e) {
            throw new RuntimeException("failed to svn checkout", e);
        }
    }

    public static Map<String, Object> log(String url, String svnUser, String svnPassword) {
        try {
            return log(url, svnUser, svnPassword, 1).get(0);
        } catch (Exception e) {
            log.info("ignore exception: {}", e.getMessage());
        }
        return null;
    }

    public static List<Map<String, Object>> log(String url, String svnUser, String svnPassword, int limit) {
        log.info("log: {}", url);
        try {
            String command = "svn log --with-all-revprops --xml -l " + limit + " '" + url + "' --username '" + svnUser + "' --password '" + svnPassword + "'";
            String result = executeCommand(command);
            return extractLog(result);
        } catch (Exception e) {
            log.info("ignore exception: {}", e.getMessage());
        }
        return null;
    }

    public static Map<String, Object> log(File location) {
        try {
            return log(location, 1).get(0);
        } catch (Exception e) {
            log.info("ignore exception: {}", e.getMessage());
        }
        return null;
    }

    public static List<Map<String, Object>> log(File location, int limit) {
        log.info("log: {}", location.getAbsolutePath());
        try {
            String command = "svn log --with-all-revprops --xml -l " + limit + " '" + location.getAbsolutePath() + "'";
            String result = executeCommand(command);
            return extractLog(result);
        } catch (Exception e) {
            log.info("ignore exception: {}", e.getMessage());
        }
        return null;
    }

    private static List<Map<String, Object>> extractLog(String result) throws ParserConfigurationException, IOException, SAXException {
        Document document = createDocument(result);
        NodeList logEntry = document.getElementsByTagName("logentry");

        List<Map<String, Object>> svnLogs = new ArrayList<>();
        for (int i = 0; i < logEntry.getLength(); i++) {
            Element element = (Element) logEntry.item(i);
            String revision = element.getAttribute(REVISION);
            String author = element.getElementsByTagName(AUTHOR).item(0).getTextContent();
            String date = element.getElementsByTagName(DATE).item(0).getTextContent();
            String msg = element.getElementsByTagName(MSG).item(0).getTextContent();

            Map<String, Object> svnLog = new HashMap<>();
            svnLog.put(REVISION, revision);
            svnLog.put(AUTHOR, author);
            svnLog.put(DATE, ZonedDateTime
                    .parse(date, DateTimeFormatter.ISO_DATE_TIME)
                    .withZoneSameInstant(ZoneId.systemDefault())
                    .toLocalDateTime());
            svnLog.put(MSG, StringUtils.left(msg, 500));
            svnLogs.add(svnLog);
        }
        return svnLogs;
    }

    private static Document createDocument(String xmlString) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        return documentBuilder.parse(IOUtils.toInputStream(xmlString, StandardCharsets.UTF_8));
    }

    public static void update(File location, String revision, String svnUser, String svnPassword) {
        log.info("update: {}", location.getAbsolutePath());
        try {
            String command = "svn up '" + location.getAbsolutePath() + "' -r '" + revision + "' --username '" + svnUser + "' --password '" + svnPassword + "'";
            executeCommand(command);
        } catch (Exception e) {
            throw new RuntimeException("failed to svn update", e);
        }
    }

    private static String executeCommand(CommandLine commandLine) throws IOException {
        log.debug("executing command: {}", commandLine);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DefaultExecutor exec = new DefaultExecutor();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
        exec.setStreamHandler(streamHandler);
        exec.execute(commandLine);

        String output = outputStream.toString();
        log.debug("executed command: {}, result: {}", commandLine, output);
        return output;
    }

    private static CommandLine getLinuxCommandLine(String command) {
        CommandLine cmdLine = new CommandLine("/bin/bash");
        cmdLine.addArgument("-c");
        cmdLine.addArgument(command, false);
        return cmdLine;
    }

    private static CommandLine getWindowCommandLine(String command) {
        return CommandLine.parse("cmd /c " + command);
    }

    private static String executeCommand(String command) throws IOException {
        log.debug("executeCommand: {}", command);
        if (isWindows()) {
            return executeCommand(getWindowCommandLine(command));
        } else {
            return executeCommand(getLinuxCommandLine(command));
        }
    }

    private static boolean isWindows() {
        String osName = System.getProperty("os.name");
        return StringUtils.containsIgnoreCase(osName, "win");
    }
}
