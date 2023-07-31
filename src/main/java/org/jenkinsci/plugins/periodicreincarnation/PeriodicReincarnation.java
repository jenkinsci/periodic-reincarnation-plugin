package org.jenkinsci.plugins.periodicreincarnation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cloudbees.hudson.plugins.folder.Folder;

import antlr.ANTLRException;
import hudson.AbortException;
import hudson.Extension;
import hudson.maven.MavenModule;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.AsyncPeriodicWork;
import hudson.model.Item;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.scheduler.CronTab;
import jenkins.model.Jenkins;

/**
 * Main class of the periodic reincarnation plug-in. Method execute is called
 * every minute but further functionality and restart of failed jobs happens
 * only when the time specified in the cron tab overlaps with the current
 * minute.
 * 
 * @author yboev
 * 
 */
@Extension
public class PeriodicReincarnation extends AsyncPeriodicWork {

	/**
	 * Logger for PeriodicReincarnation.
	 */
	private static final Logger LOGGER = Logger
			.getLogger(PeriodicReincarnation.class.getName());

	/**
	 * No spam in log file
	 */
	@Override
	protected Level getNormalLoggingLevel() {
		return Level.FINEST;
	}

	/**
	 * For every periodicTrigger holds the projects being restarted because of
	 * it.
	 */
	private HashMap<PeriodicTrigger, ArrayList<AbstractProject<?, ?>>> periodicTriggerRestartList;

	/**
	 * Set with all projects scheduled for restart. Used to determine if a
	 * project is already scheduled for restart.
	 */
	private Set<String> scheduledProjects;

	/**
	 * Contains the projects that will be restarted because there was no change
	 * between the last builds and the last failed, but the second last was a
	 * success.
	 */
	private ArrayList<AbstractProject<?, ?>> unchangedRestartProjects;

	/**
	 * Constructor.
	 */
	public PeriodicReincarnation() {
		super("PeriodicReincarnation");
	}

	/**
	 * This method is called every minute. It contains the workflow of every
	 * restart. If you want to understand what the plugin does, start from here.
	 * 
	 * @param taskListener
	 *            TaskListener
	 */
	@Override
	protected void execute(TaskListener taskListener) {
		final PeriodicReincarnationGlobalConfiguration config = PeriodicReincarnationGlobalConfiguration
				.get();

		// everything(cron regexs, enabled features) is configured in the
		// configuration, so exit if with error message if no configuration can
		// be retrieved. Should not happen!
		if (config == null) {
			LOGGER.warning(
					"No configuration available...returning with nothing being done!");
			return;
		}

		final String cron = config.getCronTime();

		// if cron is not enabled just exit with no calculations.
		if (!config.isCronActive()) {
			return;
		}

		// Initialize the data structures where the to-be-restarted projects are
		// held temporarily.
		// Needed for sorting them by the reason for the restart.
		this.periodicTriggerRestartList = new HashMap<PeriodicTrigger, ArrayList<AbstractProject<?, ?>>>();
		this.scheduledProjects = new HashSet<String>();
		this.unchangedRestartProjects = new ArrayList<AbstractProject<?, ?>>();

		// record current time
		final long currentTime = System.currentTimeMillis();

		// Add projects to be restarted.
		addProjectsFoundByPeriodicTriggerHit(currentTime);
		if (cron != null && config.isRestartUnchangedJobsEnabled()) {
			addUnchangedProjects(cron, currentTime);
		}

		// Do the actual restart.
		if (this.countProjectsForRestart() > 0) {
			restartCronProjects();
		}
	}

	/**
	 * Returns the number of projects scheduled for restart because of a
	 * periodicTrigger hit.
	 * 
	 * @return the number as int.
	 */
	private int getNumberOfProjectsForPeriodicTriggerRestart() {
		int count = 0;
		for (Entry<PeriodicTrigger, ArrayList<AbstractProject<?, ?>>> entry : this.periodicTriggerRestartList
				.entrySet()) {
			count += entry.getValue().size();
		}

		return count;
	}

	/**
	 * Counts the projects that will be restarted in this cycle.
	 * 
	 * @return the number as int.
	 */
	private int countProjectsForRestart() {
		return this.scheduledProjects.size();
	}

	/**
	 * Counts the projects that will be restarted becaue of unchanged restart in
	 * this cycle.
	 * 
	 * @return the number as int.
	 */
	private int getNumberOfProjectsForUnchangedRestart() {
		return this.unchangedRestartProjects.size();
	}

	/**
	 * Prints all projects that are scheduled for restart in this current cron
	 * cycle. Groups them according to the reason they were restarted.
	 */
	private void restartCronProjects() {
		// Initialize summary. This variables contains the whole output that
		// shows which projects have been restarted during this cron-cycle.
		String summary = "Periodic Reincarnation cron restart summary:" + "\n";
		summary += "Number of projects to restart: "
				+ this.countProjectsForRestart() + " (Periodic Trigger hit "
				+ this.getNumberOfProjectsForPeriodicTriggerRestart()
				+ ", Unchanged restart "
				+ this.getNumberOfProjectsForUnchangedRestart() + ")" + "\n";

		// Restarts all projects found by periodicTrigger (also adds them to
		// summary for
		// printing)
		summary += restartPeriodicTriggerProjects();

		// Restarts all projects found by no difference between the last two
		// builds criteria (also adds them to summary for printing)
		if (this.unchangedRestartProjects.size() > 0) {
			summary += restartUnchanged();
		}
		LOGGER.info(summary);
	}

	/**
	 * Restarts all projects listed for unchanged restart.
	 * 
	 * @return the output that should be added to summary String.
	 */
	private String restartUnchanged() {
		String summary = Constants.NODIFFERENCERESTART + ": "
				+ this.unchangedRestartProjects.size()
				+ " projects scheduled for restart" + "\n";
		final StringBuilder sb = new StringBuilder();
		for (AbstractProject<?, ?> proj : this.unchangedRestartProjects) {
			Utils.restart(proj,
					"(Cron restart) " + Constants.NODIFFERENCERESTART, null,
					Constants.NORMALQUIETPERIOD);
			sb.append("\t" + proj.getDisplayName() + "\n");
		}
		summary += sb.toString();
		return summary;
	}

	/**
	 * Restarts all projects found via PeriodicTrigger Hit. Also produces log
	 * information and returns it upon exit.
	 * 
	 * @return the output that should be added to summary String.
	 */
	private String restartPeriodicTriggerProjects() {
		final StringBuilder summary = new StringBuilder();
		for (Entry<PeriodicTrigger, ArrayList<AbstractProject<?, ?>>> entry : this.periodicTriggerRestartList
				.entrySet()) {
			PeriodicTrigger perTri = entry.getKey();
			ArrayList<AbstractProject<?, ?>> projects = entry.getValue();
			summary.append(getRestartCause(perTri) + ": " + projects.size()
					+ " projects scheduled for restart" + "\n");
			final StringBuilder sb = new StringBuilder();
			for (AbstractProject<?, ?> proj : projects) {
				Utils.restart(proj, getRestartCause(perTri), perTri,
						Constants.NORMALQUIETPERIOD);
				sb.append("\t" + proj.getDisplayName() + "\n");
			}

			summary.append(sb.toString());
		}

		return summary.toString();
	}

	/**
	 * Returns the restart cause for a PeriodicTrigger. The cause is built from
	 * the description if there is one, if not then the value of the
	 * PeriodicTrigger itself becomes the cause.
	 * 
	 * @param perTri
	 *            the periodic trigger.
	 * @return the restart cause as String.
	 */
	private String getRestartCause(PeriodicTrigger perTri) {
		String restartCause;
		if (perTri.getDescription() != null
				&& perTri.getDescription().length() > 1) {
			if (Utils.isBfaAvailable()) {
				if (perTri.getClass() == BuildFailureObject.class) {
					String val = perTri.getValue();
					try {
						val = ((BuildFailureObject) perTri)
								.getFailureCauseName();
					} catch (AbortException e) {
						LOGGER.warning("The FailureCause ID "
								+ perTri.getValue()
								+ " doesn't seem to exist. Might have been deleted");
					}
					restartCause = perTri.getDescription() + "(" + val + ")";
				} else {
					restartCause = perTri.getDescription() + "("
							+ perTri.getValue() + ")";
				}
			} else {
				restartCause = perTri.getDescription() + "(" + perTri.getValue()
						+ ")";
			}
		} else {
			if (Utils.isBfaAvailable()) {
				if (perTri.getClass() == BuildFailureObject.class) {
					String val = perTri.getValue();
					try {
						val = ((BuildFailureObject) perTri)
								.getFailureCauseName();
					} catch (AbortException e) {
						LOGGER.warning("The FailureCause ID "
								+ perTri.getValue()
								+ " doesn't seem to exist. Might have been deleted");
					}
					restartCause = "RegEx hit: " + val;
				} else {
					restartCause = "RegEx hit: " + perTri.getValue();
				}
			} else {
				restartCause = "RegEx hit: " + perTri.getValue();
			}
		}
		return restartCause;
	}

	/**
	 * Adds all projects found because of a periodicTrigger hit to the
	 * periodicTriggerRestartList Map.
	 * 
	 * @param currentTime
	 *            current time, recorded previously.
	 */
	private void addProjectsFoundByPeriodicTriggerHit(final long currentTime) {
		// IMPORTANT: Here you have to catch every PeriodicTrigger Class
		// existing!
		if (PeriodicReincarnationGlobalConfiguration.get()
				.getPeriodicTriggers() == null
				|| PeriodicReincarnationGlobalConfiguration.get()
						.getPeriodicTriggers().isEmpty()) {
			return;
		}
		for (PeriodicTrigger perTri : PeriodicReincarnationGlobalConfiguration
				.get().getPeriodicTriggers()) {
			if (perTri.isTimeToRestart(currentTime)) {
				Jenkins jenkins = Jenkins.getInstance();
				if (jenkins == null)
					continue;
				for (Item item : jenkins.getAllItems()) {
					if(item instanceof Folder)
						checkFolder((Folder)item, perTri);
					if (item instanceof AbstractProject<?, ?> && !(isMavenModule((AbstractProject<?, ?>)item)))
						checkProject((AbstractProject<?, ?>)item, perTri);
				}
			}
		}
	}

	private void checkProject(AbstractProject<?, ?> project, PeriodicTrigger perTri) {
		if (isValidCandidateForRestart(project)
				&& !scheduledProjects.contains(project.getFullDisplayName())) {
			if (matchesFailure(project, perTri) || matchesRegex(project, perTri)) {
				this.scheduledProjects.add(project.getFullDisplayName());
				if (this.periodicTriggerRestartList.containsKey(perTri)) {
					this.periodicTriggerRestartList.get(perTri).add(project);
				} else {
					final ArrayList<AbstractProject<?, ?>> newList = new ArrayList<AbstractProject<?, ?>>();
					newList.add(project);
					this.periodicTriggerRestartList.put(perTri, newList);
				}
			}
		}
	}

	private void checkFolder(Folder folder, PeriodicTrigger perTri) {
		for (Item itemInFolder : folder.getItems()) {
			if (itemInFolder instanceof Folder) {
				checkFolder((Folder)itemInFolder, perTri);
			} else if (itemInFolder instanceof AbstractProject<?, ?> && !(isMavenModule((AbstractProject<?, ?>)itemInFolder))){
				checkProject((AbstractProject<?, ?>)itemInFolder, perTri);
			}
		}
	}

	/**
	 * Populates the array unchangedRestartProjects, that contains unchanged
	 * projects failing for the first time.
	 * 
	 * @param cron
	 *            cron parameter as String
	 * @param currentTime
	 *            current time recorded previously
	 */
	private void addUnchangedProjects(final String cron,
			final long currentTime) {
		CronTab cronTab;
		try {
			cronTab = new CronTab(cron);
			if ((cronTab.ceil(currentTime).getTimeInMillis()
					- currentTime) == 0) {
				Jenkins jenkins = Jenkins.getInstance();
				if (jenkins == null)
					return;
				for (Item item : jenkins.getAllItems()) {
					checkFolder(item);
					if (item instanceof AbstractProject<?, ?> && !(isMavenModule((AbstractProject<?, ?>)item)))
						checkProject((AbstractProject<?, ?>)item);
				}
			}
		} catch (ANTLRException e1) {
			LOGGER.fine("Global cron time could not be parsed!");
			e1.printStackTrace();
		}
	}

	private void checkFolder(Item item) {
		if (item instanceof Folder) {
			Folder folder = (Folder) item;
			for (Item itemInFolder : folder.getItems()) {
				if (itemInFolder instanceof Folder) {
					checkFolder(itemInFolder);
				} else if (itemInFolder instanceof AbstractProject && !(isMavenModule((AbstractProject<?, ?>)itemInFolder))){
					checkProject((AbstractProject<?, ?>)itemInFolder);
				}
			}
		}
	}

	private void checkProject(AbstractProject<?, ?> project) {
		if (isValidCandidateForRestart(project)
				&& Utils.qualifyForUnchangedRestart(project)
				&& !scheduledProjects.contains(project.getFullDisplayName())) {
			this.scheduledProjects.add(project.getFullDisplayName());
			this.unchangedRestartProjects.add(project);
		}
	}

	/**
	 * Determines if a project should be tested for periodicTrigger match or no
	 * error between the last two builds.
	 * 
	 * @param project
	 *            the current project
	 * @return true should be tested, false otherwise
	 */
	private boolean isValidCandidateForRestart(
			final AbstractProject<?, ?> project) {
		if (project == null)
			return false;
		boolean isLocallyDeactivated = false;
		JobLocalConfiguration property = (JobLocalConfiguration) project
				.getProperty(JobLocalConfiguration.class);
		if (property != null) {
			isLocallyDeactivated = property.getIsLocallyDeactivated();
		}
		return !isLocallyDeactivated && !project.isDisabled()
				&& project.isBuildable()
				&& failedOrWorse(project.getLastBuild())
				&& !project.isBuilding() && !project.isInQueue();
	}

	/**
	 * Recurrence will occur every minute, but action will be taken according to
	 * the cron time set in the configuration. Only when this minute and the
	 * cron tab time overlap.
	 * 
	 * @return The recurrence period in ms.
	 */
	@Override
	public long getRecurrencePeriod() {
		return MIN;
	}

	/**
	 * Returns this AsyncTask.
	 * 
	 * @return the instance of PeriodicReincarnation which is currently running
	 */
	public static PeriodicReincarnation get() {
		return AsyncPeriodicWork.all().get(PeriodicReincarnation.class);
	}
	
	private boolean isMavenModule(AbstractProject<?, ?> project) {
		return (Utils.isMavenPluginAvailable() && project instanceof MavenModule);
	}

	private static boolean failedOrWorse(AbstractBuild<?, ?> build) {
		if (build == null) {
			return false;
		}
		Result result = build.getResult();
		if (result == null) {
			return false;
		}
		return result.isWorseOrEqualTo(Result.FAILURE);
	}

	private static boolean matchesRegex(AbstractProject<?, ?> project, PeriodicTrigger perTri) {
		AbstractBuild<?, ?> lastBuild = project.getLastBuild();
		return perTri.getClass() == RegEx.class
				&& lastBuild != null
				&& Utils.checkBuild(lastBuild, (RegEx) perTri);
	}

	private static boolean matchesFailure(AbstractProject<?, ?> project, PeriodicTrigger perTri) {
		return Utils.isBfaAvailable()
				&& perTri.getClass() == BuildFailureObject.class
				&& Utils.checkBuild(project.getLastBuild(), (BuildFailureObject) perTri);
	}
}
