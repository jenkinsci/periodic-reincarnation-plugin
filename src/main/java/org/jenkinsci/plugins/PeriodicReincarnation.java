package org.jenkinsci.plugins;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import antlr.ANTLRException;

import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.BuildableItem;
import hudson.model.Result;
import hudson.model.Hudson;
import hudson.model.Project;
import hudson.model.TaskListener;
import hudson.scheduler.CronTab;

@Extension
public class PeriodicReincarnation extends AsyncPeriodicWork {

	private static final Logger LOGGER = Logger
			.getLogger(PeriodicReincarnation.class.getName());

	public PeriodicReincarnation() {
		super("PeriodicReincarnation");
	}

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

					List<Project> projectList = Hudson.getInstance()
							.getProjects();

					for (Iterator<Project> i = projectList.iterator(); i
							.hasNext();) {
						Project project = i.next();
						if (project != null
								&& project instanceof BuildableItem
								&& project.getLastBuild() != null
								&& project.getLastBuild().getResult() != null
								&& project.getLastBuild().getResult()
										.isWorseOrEqualTo(Result.FAILURE)
								&& !project.isBuilding()
								&& !project.isInQueue()) {
							project.scheduleBuild(new ReincarnateFailedBuildsCause());
							LOGGER.info("Reincarnating failed build: "
									+ project.getDisplayName());

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

	@Override
	public long getRecurrencePeriod() {
		// Recurrence will occur every minute, but action will be taken
		// according to the cron time set in the configuration.
		return MIN;
	}

	public static PeriodicReincarnation get() {
		return AsyncPeriodicWork.all().get(PeriodicReincarnation.class);
	}

}
