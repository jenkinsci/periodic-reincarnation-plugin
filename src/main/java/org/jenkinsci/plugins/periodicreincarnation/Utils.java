package org.jenkinsci.plugins.periodicreincarnation;

import hudson.AbortException;
import hudson.model.BuildBadgeAction;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Project;
import hudson.model.Run;
import hudson.plugins.jobConfigHistory.JobConfigBadgeAction;
import hudson.remoting.VirtualChannel;
import hudson.util.IOUtils;
import hudson.util.RemotingDiagnostics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jenkins.model.Jenkins;

/**
 * Utility class.
 * 
 * @author yboev
 * 
 */
public class Utils {

    /**
     * Logger for PeriodicReincarnation.
     */
    private static final Logger LOGGER = Logger
            .getLogger(Utils.class.getName());

    /**
     * If there were no changes between the last 2 builds of a project and the
     * last build failed but the previous didn't, then this project is being
     * restarted if this unchanged restart option is enabled.
     * 
     * @param project
     *            the project.
     * @return true if it qualifies, false otherwise.
     */
    protected static boolean qualifyForUnchangedRestart(Project<?, ?> project) {
        // proves if a build WAS stable and if there are changes between the
        // last two builds.
        // WAS stable means: last build was worse or equal to FAILURE, second
        // last was better than FAILURE
        final Run<?, ?> lastBuild = project.getLastBuild();
        if (lastBuild == null) {
            return false;
        }
        final Run<?, ?> secondLastBuild = lastBuild.getPreviousBuild();
        final boolean configChange = isThereConfigChange(lastBuild);
        LOGGER.info("configChange is....." + configChange);
        if (lastBuild.getResult() != null
                && lastBuild.getResult().isWorseOrEqualTo(Result.FAILURE)
                && secondLastBuild != null
                && secondLastBuild.getResult() != null
                && secondLastBuild.getResult().isBetterThan(Result.FAILURE)
                && !areThereSCMChanges(lastBuild) && !configChange) {
            // last build failed, but second one didn't and there were no
            // changes between the two builds
            // in this case we restart the build
            return true;
        }
        // last build was not a failure or 2nd last was.
        // no restart
        return false;
    }

    /**
     * Returns true if this build has a BadgeAction from Job Config History
     * plugin. This is a way of checking if there was a change in the
     * configuration before the build.
     * 
     * @param lastBuild
     *            the last build being checked.
     * @return false means no icon or no jobConfigHistory, returns true
     *         otherwise.
     */
    private static boolean isThereConfigChange(Run<?, ?> lastBuild) {
        // use the String method here, because we check if optional dependency
        // JobConfigHistory is listed.
        try {
            if (Jenkins.getInstance().getPlugin("jobConfigHistory") != null) {
                for (BuildBadgeAction ba : lastBuild.getBadgeActions()) {
                    if (ba instanceof JobConfigBadgeAction) {
                        return true;
                    }
                }
            }
            return false;
        } catch (java.lang.NoClassDefFoundError e) {
            return false;
        }
    }

    /**
     * Helper method for restarting a project.
     * 
     * @param project
     *            the project.
     * @param cause
     *            the cause for the restart.
     * @param regEx
     *            regual expression
     * @throws IOException
     * @throws InterruptedException
     * 
     */
    protected static void restart(Project<?, ?> project, String cause,
            RegEx regEx, int quietPeriod) {
        if (regEx != null) {
            try {
                Utils.execAction(project, regEx.getNodeAction(),
                        regEx.getMasterAction());
            } catch (IOException e) {
                LOGGER.warning("I/O Problem executing groovy script.");
                e.printStackTrace();
            } catch (InterruptedException e) {
                LOGGER.warning("Interrupt while executing groovy script.");
                e.printStackTrace();
            }
        }
        project.scheduleBuild(quietPeriod, new PeriodicReincarnationBuildCause(cause));
    }

    /**
     * Checks if a certain build matches any of the given regular expressions.
     * 
     * @param build
     *            the build.
     * @return RegEx object if at least one match, null otherwise.
     */
    protected static RegEx checkBuild(Run<?, ?> build) {
        final PeriodicReincarnationGlobalConfiguration config = new PeriodicReincarnationGlobalConfiguration();
        final List<RegEx> regExprs = config.getRegExprs();
        if (regExprs == null || regExprs.size() == 0) {
            return null;
        }
        for (final Iterator<RegEx> i = regExprs.iterator(); i.hasNext();) {
            final RegEx currentRegEx = i.next();
            try {
                if (checkFile(build.getLogFile(), currentRegEx.getPattern(),
                        true)) {
                    return currentRegEx;
                }
            } catch (AbortException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Checks if a certain build matches the given regular expression.
     * 
     * @param build
     *            the build.
     * @param regEx
     *            the regular expression.
     * @return true means a match, false otherwise.
     */
    protected static boolean checkBuild(Run<?, ?> build, RegEx regEx) {
        try {
            if (build.getLogFile() == null) {
                LOGGER.warning("Log file cound not be retrieved for project: "
                        + build.getParent().getDisplayName());
                return false;
            }
            return checkFile(build.getLogFile(), regEx.getPattern(), true);
        } catch (AbortException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Searches for a given pattern in a given file.
     * 
     * @param file
     *            the current file being checked.
     * @param pattern
     *            the reg ex we are checking with.
     * @param abortAfterFirstHit
     *            normally true, can be set to false in order to continue
     *            searching.
     * 
     * @return True if reg ex was found in the file, false otherwise.
     */
    private static boolean checkFile(File file, Pattern pattern,
            boolean abortAfterFirstHit) {
        if (pattern == null) {
            return false;
        }
        boolean rslt = false;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                final Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    // we have a hit
                    rslt = true;
                    if (abortAfterFirstHit) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.warning("No such file: " + file.getPath());
        } finally {
            IOUtils.closeQuietly(reader);
        }
        return rslt;
    }

    /**
     * Determines whether or not there were changes between the last build that
     * failed and the second last that was a success.
     * 
     * @param build1
     *            the last build that failed.
     * @return true if there is at least one change, false otherwise.
     */
    private static boolean areThereSCMChanges(Run<?, ?> build1) {
        if (build1 instanceof AbstractBuild) {
            return !((AbstractBuild<?, ?>) build1).getChangeSet().isEmptySet();
        }
        return true;
    }

    /**
     * Executes script actions for a given project.
     * 
     * @param project
     *            the project
     * @param nodeAction
     *            the nodeAction
     * @param masterAction
     *            the masterAction
     * @throws IOException
     *             input/output problem
     * @throws InterruptedException
     *             interrupt exception
     */
    protected static void execAction(Project<?, ?> project, String nodeAction,
            String masterAction) throws IOException, InterruptedException {
        final Node node = project.getLastBuild().getBuiltOn();
        final Computer slave = node.toComputer();

        if (nodeAction != null && nodeAction.length() > 1) {
            LOGGER.fine("Executing node script");
            executeGroovyScript(nodeAction, slave.getChannel());
        }
        if (masterAction != null && masterAction.length() > 1) {
            LOGGER.fine("Executing master script");
            executeGroovyScript(nodeAction, Jenkins.MasterComputer.localChannel);
        }
    }

    /**
     * Executes groovy script.
     * 
     * @param script
     *            the script as String.
     * @param channel
     *            virtual channel of the node.
     */
    private static void executeGroovyScript(String script,
            VirtualChannel channel) {
        try {
            RemotingDiagnostics.executeGroovy(script, channel);
        } catch (IOException e) {
            LOGGER.warning("I/O Problem while executing the following script: "
                    + script);
        } catch (InterruptedException e) {
            LOGGER.warning("Interrupted while executing the following script: "
                    + script);
        }
    }

}
