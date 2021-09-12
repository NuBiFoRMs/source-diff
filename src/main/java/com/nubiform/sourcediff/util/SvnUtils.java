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

    private static String getUrlCommand(String url) {
        return " '" + url + "'";
    }

    private static <T> String getUrlCommand(String url, T revision) {
        return " '" + url + "@" + revision + "'";
    }

    private static String getAuthenticationCommand(String username, String password) {
        return " --username '" + username + "' --password '" + password + "'";
    }

    private static String getLocationCommand(File location) {
        return " '" + location.getAbsolutePath() + "'";
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

    public static void checkout(String url, String revision, File location, String username, String password) {
        log.debug("checkout: {}@{} -> {}", url, revision, location.getAbsolutePath());

        // svn checkout
        String checkoutCommand = "svn checkout --depth infinity" +
                getUrlCommand(url, revision) +
                getLocationCommand(location) +
                getAuthenticationCommand(username, password);
        try {
            // svn revert
            String revertCommand = "svn revert -R" +
                    getLocationCommand(location) +
                    getAuthenticationCommand(username, password);
            try {
                executeCommand(revertCommand);
            } catch (Exception e) {
                log.debug("ignore exception: {}, revertCommand: {}", e.getLocalizedMessage(), revertCommand);
            }

            executeCommand(checkoutCommand);
        } catch (Exception e) {
            log.error("exception: {}", e.getLocalizedMessage());
            log.error("checkoutCommand: {}", checkoutCommand);
            throw new RuntimeException("failed to svn checkout", e);
        }
    }

    public static void export(String url, String revision, File location, String username, String password) {
        log.debug("export: {}@{} -> {}", url, revision, location.getAbsolutePath());

        // svn checkout
        String exportCommand = "svn export --force" +
                getUrlCommand(url, revision) +
                getLocationCommand(location) +
                getAuthenticationCommand(username, password);
        try {
            executeCommand(exportCommand);
        } catch (Exception e) {
            log.error("exception: {}", e.getLocalizedMessage());
            log.error("checkoutCommand: {}", exportCommand);
            throw new RuntimeException("failed to svn export", e);
        }
    }

    public static String svnInfo(File location, String username, String password) {
        log.debug("svnInfo: file: {}", location);
        String svnInfoCommand = "svn info --xml" +
                (Objects.nonNull(location) ? getLocationCommand(location) : "") +
                getAuthenticationCommand(username, password);
        try {
            return executeCommand(svnInfoCommand);
        } catch (Exception e) {
            log.error("exception: {}", e.getLocalizedMessage());
            log.error("checkoutCommand: {}", svnInfoCommand);
        }
        return null;
    }

    public static String log(String url, long limit, String username, String password) {
        return svnLog(url, null, null, limit, username, password);
    }

    public static String log(String url, String revision, long limit, String username, String password) {
        return svnLog(url, null, getRevisionCommand(revision), limit, username, password);
    }

    public static String log(String url, String startRevision, String endRevision, long limit, String username, String password) {
        return svnLog(url, null, getRevisionCommand(startRevision, endRevision), limit, username, password);
    }

    public static String log(File location, long limit, String username, String password) {
        return svnLog(null, location, null, limit, username, password);
    }

    public static String log(File location, String revision, long limit, String username, String password) {
        return svnLog(null, location, getRevisionCommand(revision), limit, username, password);
    }

    public static String log(File location, String startRevision, String endRevision, long limit, String username, String password) {
        return svnLog(null, location, getRevisionCommand(startRevision, endRevision), limit, username, password);
    }

    private static String svnLog(String url, File location, String revisionCommand, long limit, String username, String password) {
        log.debug("svnLog: url: {}, file: {}", url, location);
        String logCommand = "svn log -v --with-all-revprops --xml" +
                (Objects.nonNull(revisionCommand) ? revisionCommand : "") +
                (limit > 0 ? getLimitCommand(limit) : "") +
                (Objects.nonNull(url) ? getUrlCommand(url) : "") +
                (Objects.nonNull(location) ? getLocationCommand(location) : "") +
                getAuthenticationCommand(username, password);
        try {
            return executeCommand(logCommand);
        } catch (Exception e) {
            log.error("exception: {}", e.getLocalizedMessage());
            log.error("checkoutCommand: {}", logCommand);
        }
        return null;
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
