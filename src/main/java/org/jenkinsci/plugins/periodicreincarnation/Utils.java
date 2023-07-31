package org.jenkinsci.plugins.periodicreincarnation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.FoundFailureCause;

import hudson.AbortException;
import hudson.PluginWrapper;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildBadgeAction;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.Run;
import hudson.plugins.jobConfigHistory.JobConfigBadgeAction;
import hudson.remoting.VirtualChannel;
import hudson.util.RemotingDiagnostics;
import jenkins.model.Jenkins;

/**
 * Utility class. Functions for determining if there should be a restart and
 * functions actually performing the restart.
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
	protected static boolean qualifyForUnchangedRestart(
			AbstractProject<?, ?> project) {
		// proves if a build WAS stable and if there are changes between the
		// last two builds.
		// WAS stable means: last build was worse or equal to FAILURE, second
		// last was better than FAILURE
		final Run<?, ?> lastBuild = project.getLastBuild();
		if (lastBuild == null) {
			return false;
		}
		final Run<?, ?> secondLastBuild = lastBuild.getPreviousBuild();
		Result lastBuildResult = lastBuild.getResult();
		if (lastBuildResult != null
				&& lastBuildResult.isWorseOrEqualTo(Result.FAILURE)
				&& secondLastBuild != null
				&& secondLastBuild.getResult() != null
				&& secondLastBuild.getResult().isBetterThan(Result.FAILURE)
				&& !areThereSCMChanges(lastBuild)
				&& !isThereConfigChange(lastBuild)) {
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
			Jenkins jenkins = Jenkins.getInstance();
			if (jenkins == null)
				return false;
			if (jenkins.getPlugin("jobConfigHistory") != null) {
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
	 * @param perTri
	 *            periodic trigger.
	 * @param quietPeriod
	 *            amount of time a job will wait in the queue(in seconds).
	 * @throws IOException
	 * @throws InterruptedException
	 * 
	 */
	protected static void restart(AbstractProject<?, ?> project, String cause,
			PeriodicTrigger perTri, int quietPeriod) {
		if (perTri != null) {
			try {
				Utils.execAction(project, perTri.getNodeAction(),
						perTri.getMasterAction());
			} catch (IOException e) {
				LOGGER.warning("I/O Problem executing groovy script.");
				e.printStackTrace();
			} catch (InterruptedException e) {
				LOGGER.warning("Interrupt while executing groovy script.");
				e.printStackTrace();
			}
		}
		project.scheduleBuild(quietPeriod,
				new PeriodicReincarnationBuildCause(cause));
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
	 * Checks if a certain build matches any of the given Failure Cause.
	 * 
	 * @param build
	 *            the build.
	 * @return BuildFailureObject if at least one match, null otherwise.
	 */
	protected static BuildFailureObject checkBuildForBuildFailure(
			Run<?, ?> build) {
		final PeriodicReincarnationGlobalConfiguration config = new PeriodicReincarnationGlobalConfiguration();
		final List<BuildFailureObject> bfas = config.getBfas();
		if (bfas == null || bfas.size() == 0) {
			return null;
		}
		for (final Iterator<BuildFailureObject> i = bfas.iterator(); i
				.hasNext();) {
			final BuildFailureObject currentBFA = i.next();
			if (checkBuild(build, currentBFA)) {
				return currentBFA;
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
			LOGGER.finest("Start check log file for project: "
					+ build.getParent().getDisplayName());
			return checkFile(build.getLogFile(), regEx.getPattern(), true);
		} catch (AbortException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Checks if a certain build matches the given Build Failure Cause.
	 * 
	 * @param build
	 *            the build.
	 * @param regEx
	 *            the regular expression.
	 * @return true means a match, false otherwise.
	 */
	protected static boolean checkBuild(Run<?, ?> build,
			BuildFailureObject bfa) {
		if (!Utils.isBfaAvailable())
			return false;
		List<FoundFailureCause> failureCauses;
		FailureCauseBuildAction subAction = build
				.getAction(FailureCauseBuildAction.class);
		if (subAction != null) {
			failureCauses = subAction.getFailureCauseDisplayData() != null
					? subAction.getFailureCauseDisplayData()
							.getFoundFailureCauses()
					: null;
			if (failureCauses == null)
				return false;
			for (FoundFailureCause ffc : failureCauses) {
				try {
					if (((BuildFailureObject) bfa).getFailureCause()
							.equals(ffc.getId()))
						return true;
				} catch (AbortException e) {
					LOGGER.warning(
							"Failure cause doesn't seem to exist (may have been deleted): "
									+ e.getMessage());
				}
			}
		}
		return false;
	}

	/**
	 * Determine if the plugin build-failure-analyzer is available
	 * 
	 * @return true iff pluginManager contains "build-failure-analyzer"
	 */
	public static boolean isBfaAvailable() {
		Jenkins jenkins = Jenkins.getInstance();
		if (jenkins == null)
			return false;
		return jenkins.pluginManager
				.getPlugin("build-failure-analyzer") != null;
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
		// pattern to filter out our own messages in the logs so we don't create
		// a respawn loop
		Pattern prPattern = Pattern.compile(".*Periodic Reincarnation.*");
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
			String line;
			while ((line = reader.readLine()) != null) {
				final Matcher matcher = pattern.matcher(line);
				final Matcher prMatcher = prPattern.matcher(line);
				if (matcher.find() && !prMatcher.find()) {
					// we have a hit
					rslt = true;
					if (abortAfterFirstHit) {
						return true;
					}
				}
			}
		} catch (IOException e) {
			LOGGER.warning("No such file: " + file.getPath());
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
	protected static void execAction(AbstractProject<?, ?> project,
			String nodeAction, String masterAction)
			throws IOException, InterruptedException {
		if (project == null) {
			return;
		}
		AbstractBuild<?, ?> lastBuild = project.getLastBuild();
		if (lastBuild == null) {
			return;
		}
		final Node node = lastBuild.getBuiltOn();
		if (node == null) {
			return;
		}
		final Computer slave = node.toComputer();
		if (nodeAction != null && nodeAction.length() > 1) {
			LOGGER.fine("Executing node script");
			executeGroovyScript(nodeAction, slave.getChannel());
		}
		if (masterAction != null && masterAction.length() > 1) {
			LOGGER.fine("Executing master script");
			executeGroovyScript(nodeAction,
					Jenkins.MasterComputer.localChannel);
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

	/**
	 * Searches for a FailureCause by its ID
	 * 
	 * @return the FailureCause object, if one with the given id exists, null
	 *         otherwise
	 */
	public static FailureCause getFailureCauseById(String id) {
		try {
			Jenkins jenkins = Jenkins.getInstance();
			if (jenkins == null)
				return null;
			return jenkins.getPlugin(PluginImpl.class).getKnowledgeBase()
					.getCause(id);
		} catch (Exception e) {
			LOGGER.warning(e.getMessage());
		}
		return null;
	}

	/**
	 * Returns all available failure cause ids as string from Build Failure
	 * Analyzer Plugin.
	 * 
	 * @return Returns the list of all available failure cause ids.
	 */
	public static List<String> getAvailableFailureCausesIds() {
		return getAvailableFailureCausesIds(false);
	}

	/**
	 * Reads all available failure causes from Build Failure Analyzer Plugin.
	 * 
	 * @param true
	 *            if string should contain name, false if it should contain id
	 * 
	 * @return Returns list of ids(false) or the names(true)
	 */
	public static List<String> getAvailableFailureCausesIds(boolean names) {
		Collection<FailureCause> failureCausesColl;
		List<FailureCause> failureCauseNames = null;
		List<String> ret = new ArrayList<String>();
		try {
			Jenkins jenkins = Jenkins.getInstance();
			if (jenkins == null)
				return null;
			failureCausesColl = jenkins.getPlugin(PluginImpl.class)
					.getKnowledgeBase().getCauseNames();

			if (jenkins.getPlugin(PluginImpl.class).getKnowledgeBase()
					.getCauseNames() instanceof List) {
				failureCauseNames = (List<FailureCause>) failureCausesColl;
			} else {
				failureCauseNames = new ArrayList<FailureCause>(
						failureCausesColl);
			}
			for (FailureCause fc : failureCauseNames) {
				if (names) {
					ret.add(fc.getName());
				} else {
					ret.add(fc.getId());
				}
			}
		} catch (Exception e) {
			LOGGER.info("Failed to load failure causes. " + e);
		}
		return ret;
	}

	/**
	 * Reads all available failure causes from Build Failure Analyzer Plugin.
	 * 
	 * @return Returns the list of all available failure causes (names and ids).
	 */
	public static List<FailureCause> getAvailableFailureCauses() {
		Collection<FailureCause> failureCausesColl;
		List<FailureCause> failureCauseNames = null;
		try {
			Jenkins jenkins = Jenkins.getInstance();
			if (jenkins == null)
				return null;
			failureCausesColl = jenkins.getPlugin(PluginImpl.class)
					.getKnowledgeBase().getCauseNames();
			if (jenkins.getPlugin(PluginImpl.class).getKnowledgeBase()
					.getCauseNames() instanceof List) {
				failureCauseNames = (List<FailureCause>) failureCausesColl;
			} else {
				failureCauseNames = new ArrayList<FailureCause>(
						failureCausesColl);
			}
		} catch (Exception e) {
			LOGGER.info("Failed to load failure causes. " + e);
		}
		return failureCauseNames;
	}
	
	protected static boolean isMavenPluginAvailable() {
		Jenkins jenkins = Jenkins.getInstanceOrNull();
		if (jenkins == null) {
			return false;
		}
		PluginWrapper wrapper = jenkins.getPluginManager().getPlugin("maven-plugin");
		return (wrapper != null && wrapper.isEnabled());
	}

}
