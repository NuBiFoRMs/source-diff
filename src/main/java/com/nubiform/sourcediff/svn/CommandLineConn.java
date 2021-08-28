package com.nubiform.sourcediff.svn;

import com.nubiform.sourcediff.util.SvnUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class CommandLineConn implements SvnConnector {

    private static final String LOGENTRY = "logentry";
    private static final String REVISION = "revision";
    private static final String AUTHOR = "author";
    private static final String DATE = "date";
    private static final String MSG = "msg";
    private static final String PATH = "path";

    @Override
    public void checkout(String url, String revision, File location, String username, String password) {
        SvnUtils.checkout(url, revision, location, username, password);
    }

    @Override
    public long getHeadRevision(String url, String username, String password) {
        try {
            return extractLog(SvnUtils.log(url, "HEAD", 1, username, password)).get(0).getRevision();
        } catch (Exception e) {
            log.info("ignore exception: {}", e.getLocalizedMessage());
        }
        return 0;
    }

    @Override
    public long getBaseRevision(File location, String username, String password) {
        try {
            return extractLog(SvnUtils.log(location, "BASE", 1, username, password)).get(0).getRevision();
        } catch (Exception e) {
            log.info("ignore exception: {}", e.getLocalizedMessage());
        }
        return 0;
    }

    @Override
    public SvnLog log(File location, String username, String password) {
        try {
            return extractLog(SvnUtils.log(location, 1, username, password)).get(0);
        } catch (Exception e) {
            log.info("ignore exception: {}", e.getLocalizedMessage());
        }
        return null;
    }

    @Override
    public List<SvnLog> log(File location, long limit, String username, String password) {
        try {
            return extractLog(SvnUtils.log(location, limit, username, password));
        } catch (Exception e) {
            log.info("ignore exception: {}", e.getLocalizedMessage());
        }
        return new ArrayList<>();
    }

    @Override
    public void export(String url, String revision, File location, String username, String password) {
        SvnUtils.export(url, revision, location, username, password);
    }

    private Document createDocument(String xmlString) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        return documentBuilder.parse(IOUtils.toInputStream(xmlString, StandardCharsets.UTF_8));
    }

    private List<SvnLog> extractLog(String result) throws ParserConfigurationException, IOException, SAXException {
        Document document = createDocument(result);
        NodeList logEntryList = document.getElementsByTagName(LOGENTRY);
        List<SvnLog> svnLogList = new ArrayList<>();
        for (int i = 0; i < logEntryList.getLength(); i++) {
            Element logEntryElement = (Element) logEntryList.item(i);
            String revision = logEntryElement.getAttribute(REVISION);
            String author = logEntryElement.getElementsByTagName(AUTHOR).item(0).getTextContent();
            String date = logEntryElement.getElementsByTagName(DATE).item(0).getTextContent();
            String msg = logEntryElement.getElementsByTagName(MSG).item(0).getTextContent();

            NodeList pathList = logEntryElement.getElementsByTagName(PATH);
            List<String> path = new ArrayList<>();
            for (int j = 0; j < pathList.getLength(); j++) {
                Element pathElement = (Element) pathList.item(j);
                String textContent = pathElement.getTextContent();
                path.add(textContent);
            }

            svnLogList.add(SvnLog.builder()
                    .revision(Long.parseLong(revision))
                    .author(author)
                    .date(ZonedDateTime
                            .parse(date, DateTimeFormatter.ISO_DATE_TIME)
                            .withZoneSameInstant(ZoneId.systemDefault())
                            .toLocalDateTime())
                    .message(msg)
                    .build());
        }

        return svnLogList;
    }
}
