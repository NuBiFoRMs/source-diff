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
import java.util.HashMap;
import java.util.Map;

@Slf4j
@UtilityClass
public class SvnUtils {

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

    public static Map<String, Object> log(File location) {
        log.info("log: {}", location.getAbsolutePath());
        try {
            String command = "svn log --with-all-revprops --xml -l 1 '" + location.getAbsolutePath() + "'";
            String result = executeCommand(command);

            Document document = createDocument(result);
            NodeList logEntry = document.getElementsByTagName("logentry");
            Element element = (Element) logEntry.item(0);
            String revision = element.getAttribute("revision");
            String author = element.getElementsByTagName("author").item(0).getTextContent();
            String msg = element.getElementsByTagName("msg").item(0).getTextContent();
            String date = element.getElementsByTagName("date").item(0).getTextContent();

            Map<String, Object> svnInfo = new HashMap<>();
            svnInfo.put("revision", revision);
            svnInfo.put("author", author);
            svnInfo.put("date", ZonedDateTime
                    .parse(date, DateTimeFormatter.ISO_DATE_TIME)
                    .withZoneSameInstant(ZoneId.systemDefault())
                    .toLocalDateTime());
            svnInfo.put("msg", StringUtils.left(msg, 500));

            return svnInfo;
        } catch (Exception e) {
            throw new RuntimeException("failed to svn update", e);
        }
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
