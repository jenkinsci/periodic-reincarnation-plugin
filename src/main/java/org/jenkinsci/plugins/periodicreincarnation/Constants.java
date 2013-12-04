package org.jenkinsci.plugins.periodicreincarnation;

/**
 * Constants.
 * 
 * @author yboev
 * 
 */
public final class Constants {

    /*
     * Text constants.
     */
    public static final String CRONRESTART = "Cron restart";
    public static final String AFTERBUILDRESTART = "Afterbuild restart";
    public static final String NODIFFERENCERESTART = "No SCM difference between the last two builds";
    /**
     * Used when a project should be build as soon as possible.
     */
    public static final int NORMALQUIETPERIOD = 0;
    /**
     * Used as delay for the AfterbuildRestart option.
     */
    public static final int AFTERBUILDQUIETPERIOD = 300;
}
