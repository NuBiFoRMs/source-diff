package com.nubiform.sourcediff.svn;

public class SvnException extends RuntimeException {
    
    public SvnException(String message) {
        super(message);
    }

    public SvnException(String message, Throwable cause) {
        super(message, cause);
    }
}
