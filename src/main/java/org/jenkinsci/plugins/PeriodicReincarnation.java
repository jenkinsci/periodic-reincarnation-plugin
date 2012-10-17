package org.jenkinsci.plugins;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jenkinsci.plugins.ReincarnateFailedJobsConfiguration.RegEx;

import antlr.ANTLRException;

import hudson.AbortException;
import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.BuildableItem;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.Hudson;
import hudson.model.Project;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scheduler.CronTab;
import hudson.util.IOUtils;
import hudson.util.RunList;

/**
 * Main class of the periodic reincarnation plug-in. Method execute is called
 * every minute but further functionality and restart of failed jobs happens
 * only when the time specified in the cron tab overlaps with the current minute.
 *   
 */
@Extension
public class PeriodicReincarnation extends AsyncPeriodicWork {

	private static final Logger LOGGER = Logger
			.getLogger(PeriodicReincarnation.class.getName());

	/**
	 * Constructor.
	 */
	public PeriodicReincarnation() {
		super("PeriodicReincarnation");
	}

	/**
	 * This method is called every minute.
	 */
	@Override
	protected void execute(TaskListener taskListener) {
		ReincarnateFailedJobsConfiguration config = ReincarnateFailedJobsConfiguration
				.get();
		if (config == null) {
			LOGGER.info("No configuration available.");
			return;
		}

		String cron = config.getCronTime();
		boolean isActive = config.isActive();
		if (!isActive)
			return;
		if (cron != null) {
			try {

				CronTab cronTab = new CronTab(cron);
				long currentTime = System.currentTimeMillis();

				if ((cronTab.ceil(currentTime).getTimeInMillis() - currentTime) == 0
						&& isActive) {

					List<?> projectList = Hudson.getInstance()
							.getProjects();

					for (Iterator<?> i = projectList.iterator(); i
							.hasNext();) {
						Project<?, ?> project = (Project<?, ?>) i.next();
						if (project != null
								&& project instanceof BuildableItem
								&& project.getLastBuild() != null
								&& project.getLastBuild().getResult() != null
								&& project.getLastBuild().getResult()
										.isWorseOrEqualTo(Result.FAILURE)
								&& !project.isBuilding()
								&& !project.isInQueue()) {
							if (checkRegExprs(project.getLastBuild())) {
								this.restart(project, config);
							} else if (config.isRestartUnchangedJobsEnabled()
									&& qualifyForUnchangedRestart(project)) {
								this.restart(project, config);
							}
						}

					}
				}

			} catch (ANTLRException e) {
				LOGGER.warning("Could not parse the given cron tab. Check for type errors: "
						+ e.getMessage());
			}
		} else {
			LOGGER.warning("Cron time is not configured.");
		}
	}

	/**
	 * Recurrence will occur every minute, but action will be taken
	 * according to the cron time set in the configuration. Only
	 * when this minute and the cron tab time overlap.
	 */
	@Override
	public long getRecurrencePeriod() {
		return MIN;
	}
	
	
	/**
	 * Returns this AsyncTask.
	 * @return
	 */
	public static PeriodicReincarnation get() {
		return AsyncPeriodicWork.all().get(PeriodicReincarnation.class);
	}
	
	/**
	 * Determines whether or not there were changes between two builds.
	 * @param build1 First build.
	 * @param build2 Second build.
	 * @return true if there is at least one chage, false otherwise.
	 */
	private boolean changesBetweenTwoBuilds(Run<?, ?> build1, Run<?, ?> build2) {
		return ((AbstractBuild<?, ?>) build1).getChangeSet().equals(((AbstractBuild<?, ?>) build2).getChangeSet());
	}

	/**
	 * If there were no changes between the last 2 builds of a project and
	 * the last build failed but the previous didn't, then this project is
	 * being restarted if this unchanged restart option is enabled.
	 * @param project the project.
	 * @return true if it qualifies, false otherwise.
	 */
	private boolean qualifyForUnchangedRestart(Project<?, ?> project) {
		// proves if a build WAS stable and if there are changes between the last two builds.
		// WAS stable means: last build was worse or equal to FAILURE, second last was better than FAILURE 
		if (project.getBuilds() != null && project.getBuilds().size() >= 2) {
			RunList<?> builds =  project.getBuilds();
			Run<?, ?> lastBuild = builds.getLastBuild();
			Run<?, ?> secondLastBuild = builds.get(builds.size() - 2);
			if (lastBuild != null && lastBuild.getResult() != null
					&& lastBuild.getResult().isWorseOrEqualTo(Result.FAILURE)
					&& secondLastBuild != null
					&& secondLastBuild.getResult() != null
					&& secondLastBuild.getResult().isBetterThan(Result.FAILURE)
					&& !changesBetweenTwoBuilds(lastBuild, secondLastBuild)) {
				// last build failed, but second one didn't and there were no changes between the two builds
				// in this case we restart the build
				return true;
			}
			// last build was not a failure or 2nd last was.
			// no restart
			return false;
		}
		// less than 2 builds
		// no restart
		return false;
	}

	/**
	 * Helper method for restarting a project.
	 * @param project the project.
	 * @param config instance of periodic reincarnation configuration.
	 */
	private void restart(Project<?, ?> project,
			ReincarnateFailedJobsConfiguration config) {
		project.scheduleBuild(new ReincarnateFailedBuildsCause());
		if (config.isLogInfoEnabled()) {
			LOGGER.info("Reincarnating failed build: "
					+ project.getDisplayName());
		}
	}

	/**
	 * Checks if a certain build matches any of the given regular expressions.
	 * @param build the build.
	 * @return true if at least one match, false otherwise.
	 */
	private boolean checkRegExprs(Run<?, ?> build) {
		ReincarnateFailedJobsConfiguration config = new ReincarnateFailedJobsConfiguration();
		List<RegEx> regExprs = config.getRegExprs();
		if (regExprs == null || regExprs.size() == 0)
			return true;
		for (Iterator<RegEx> i = regExprs.iterator(); i.hasNext();) {
			RegEx currentRegEx = i.next();
			try {
				if (checkFile(build.getLogFile(), currentRegEx.getPattern(),
						true)) {
					return true;
				}
			} catch (AbortException e) {
				// e.printStackTrace();
			}
		}
		return false;
	}

	/**
	 * Searches for a given pattern in a given file.
	 * 
	 * @param abortAfterFirstHit
	 *            normally true, can be set to false in order to continue
	 *            searching.
	 */
	private boolean checkFile(File file, Pattern pattern,
			boolean abortAfterFirstHit) {
		if (pattern == null)
			return false;
		boolean rslt = false;
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			String line;
			while ((line = reader.readLine()) != null) {
				Matcher matcher = pattern.matcher(line);
				if (matcher.find()) {
					// we have a hit
					rslt = true;
					if (abortAfterFirstHit)
						return true;
				}
			}
		} catch (IOException e) {
			LOGGER.info("File could not be read!");
		} finally {
			IOUtils.closeQuietly(reader);
		}
		return rslt;
	}

}
