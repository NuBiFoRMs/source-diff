package com.nubiform.sourcediff.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

@Slf4j
@UtilityClass
public class SvnUtils {

    public static final String REVISION = "revision";
    public static final String AUTHOR = "author";
    public static final String DATE = "date";
    public static final String MSG = "msg";
    public static final String PATH = "path";

    private static String getUrlCommand(String url) {
        return " '" + url + "'";
    }

    private static <T> String getUrlCommand(String url, T revision) {
        return " '" + url + "@" + revision + "'";
    }

    private static String getAuthenticationCommand(String username, String password) {
        return " --username '" + username + "' --password '" + password + "'";
    }

    private static String getLocalLocationCommand(File localLocation) {
        return " '" + localLocation.getAbsolutePath() + "'";
    }

    private static String getRevisionCommand(String revision) {
        return " -r '" + revision + "'";
    }

    private static String getRevisionCommand(String startRevision, String endRevision) {
        return " -r '" + startRevision + ":" + endRevision + "'";
    }

    private static String getLimitCommand(long limit) {
        return " -l '" + limit + "'";
    }

    public static void checkout(String url, String revision, File localLocation, String username, String password) {
        log.info("checkout: {}@{} -> {}", url, revision, localLocation.getAbsolutePath());

        // svn checkout
        String checkoutCommand = "svn checkout --depth infinity" +
                getUrlCommand(url, revision) +
                getLocalLocationCommand(localLocation) +
                getAuthenticationCommand(username, password);
        try {
            // svn revert
            String revertCommand = "svn revert -R" +
                    getLocalLocationCommand(localLocation) +
                    getAuthenticationCommand(username, password);
            try {
                executeCommand(revertCommand);
            } catch (Exception e) {
                log.info("ignore exception: {}, revertCommand: {}", e.getLocalizedMessage(), revertCommand);
            }

            executeCommand(checkoutCommand);
        } catch (Exception e) {
            log.error("exception: {}", e.getLocalizedMessage());
            log.error("checkoutCommand: {}", checkoutCommand);
            throw new RuntimeException("failed to svn checkout", e);
        }
    }

    public String log(String url, long limit, String username, String password) {
        return svnLog(url, null, null, limit, username, password);
    }

    public String log(String url, String revision, long limit, String username, String password) {
        return svnLog(url, null, getRevisionCommand(revision), limit, username, password);
    }

    public String log(String url, String startRevision, String endRevision, long limit, String username, String password) {
        return svnLog(url, null, getRevisionCommand(startRevision, endRevision), limit, username, password);
    }

    public String log(File localLocation, long limit, String username, String password) {
        return svnLog(null, localLocation, null, limit, username, password);
    }

    public String log(File localLocation, String revision, long limit, String username, String password) {
        return svnLog(null, localLocation, getRevisionCommand(revision), limit, username, password);
    }

    public String log(File localLocation, String startRevision, String endRevision, long limit, String username, String password) {
        return svnLog(null, localLocation, getRevisionCommand(startRevision, endRevision), limit, username, password);
    }

    private String svnLog(String url, File localLocation, String revisionCommand, long limit, String username, String password) {
        log.info("svnLog: url: {}, file: {}", url, localLocation);
        String logCommand = "svn log -v --with-all-revprops --xml" +
                (Objects.nonNull(revisionCommand) ? revisionCommand : "") +
                (limit > 0 ? getLimitCommand(limit) : "") +
                (Objects.nonNull(url) ? getUrlCommand(url) : "") +
                (Objects.nonNull(localLocation) ? getLocalLocationCommand(localLocation) : "") +
                getAuthenticationCommand(username, password);
        try {
            return executeCommand(logCommand);
        } catch (Exception e) {
            log.error("exception: {}", e.getLocalizedMessage());
            log.error("checkoutCommand: {}", logCommand);
        }
        return null;
    }

    public static void export(String url, String revision, File localLocation, String username, String password) {
        log.info("export: {}@{} -> {}", url, revision, localLocation.getAbsolutePath());

        // svn checkout
        String exportCommand = "svn export" +
                getUrlCommand(url, revision) +
                getLocalLocationCommand(localLocation) +
                getAuthenticationCommand(username, password);
        try {
            executeCommand(exportCommand);
        } catch (Exception e) {
            log.error("exception: {}", e.getLocalizedMessage());
            log.error("checkoutCommand: {}", exportCommand);
            throw new RuntimeException("failed to svn export", e);
        }
    }

    private static String executeCommand(CommandLine commandLine) throws RuntimeException {
        log.debug("executing command: {}", commandLine);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DefaultExecutor exec = new DefaultExecutor();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
        exec.setStreamHandler(streamHandler);

        try {
            exec.execute(commandLine);
        } catch (IOException e) {
            throw new RuntimeException(outputStream.toString(), e);
        }

        String output = outputStream.toString();
        log.debug("executed Command: {}\n{}", commandLine, output);

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
