package org.jenkinsci.plugins.periodicreincarnation;

import java.io.IOException;
import java.util.logging.Logger;

import hudson.model.AsyncPeriodicWork;
import hudson.model.BuildableItem;
import hudson.model.Result;
import hudson.model.Hudson;
import hudson.model.Project;
import hudson.model.TaskListener;
import hudson.scheduler.CronTab;

import org.jenkinsci.plugins.periodicreincarnation.PeriodicReincarnationGlobalConfiguration.RegEx;

import antlr.ANTLRException;

import hudson.Extension;

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
     * Constructor.
     */
    public PeriodicReincarnation() {
        super("PeriodicReincarnation");
    }

    /**
     * This method is called every minute.
     * 
     * @param taskListener
     *            TaskListener
     */
    @Override
    protected void execute(TaskListener taskListener) {
        final PeriodicReincarnationGlobalConfiguration config = PeriodicReincarnationGlobalConfiguration
                .get();
        if (config == null) {
            LOGGER.warning("No configuration available. Returning with nothing being done!");
            return;
        }

        final String cron = config.getCronTime();
        if (!config.isActiveCron()) {
            return;
        }
        if (cron != null) {
            try {
                final CronTab cronTab = new CronTab(cron);
                final long currentTime = System.currentTimeMillis();
                RegEx regEx;

                if ((cronTab.ceil(currentTime).getTimeInMillis() - currentTime) == 0) {
                    for (Project<?, ?> project : Hudson.getInstance()
                            .getProjects()) {
                        if (isValidCandidateForRestart(project)) {
                            regEx = Utils.checkBuild(project.getLastBuild());
                            if (regEx != null) {
                                Utils.restart(project, config,
                                        "(Cron restart) RegEx hit in console output: "
                                                + regEx.getValue(), regEx);

                            } else if (config.isRestartUnchangedJobsEnabled()
                                    && Utils.qualifyForUnchangedRestart(project)) {
                                Utils.restart(
                                        project,
                                        config,
                                        "(Cron restart) No difference between last two builds",
                                        null);
                            }
                        }
                    }
                }

            } catch (ANTLRException e) {
                LOGGER.warning("Could not parse the given cron tab. Check for type errors: "
                        + e.getMessage());
            } catch (InterruptedException e) {
                LOGGER.warning("Could not parse the given cron tab. Check for type errors: "
                        + e.getMessage());
            } catch (IOException e) {
                LOGGER.warning("Could not parse the given cron tab. Check for type errors: "
                        + e.getMessage());
            }
        } else {
            LOGGER.warning("Cron time is not configured.");
        }
    }

    /**
     * Determines if a project should be tested for RegEx match or no error
     * between the last two builds.
     * 
     * @param project
     * @return true should be tested, false otherwise
     */
    private boolean isValidCandidateForRestart(final Project<?, ?> project) {
        // TODO: possible race condition with last build variable
        return project != null
                && project instanceof BuildableItem
                && project.getLastBuild() != null
                && project.getLastBuild().getResult() != null
                && project.getLastBuild().getResult()
                        .isWorseOrEqualTo(Result.FAILURE)
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
}
