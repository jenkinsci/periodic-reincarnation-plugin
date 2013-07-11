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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

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

//    private TreeMap<String, ArrayList<Project<?, ?>>> cronRestartProjects;
//    private Set<String> scheduledProjects;

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
            LOGGER.warning("No configuration available...returning with nothing being done!");
            return;
        }

        final String cron = config.getCronTime();
        if (!config.isCronActive()) {
            return;
        }
        
//        this.cronRestartProjects = new TreeMap<String, ArrayList<Project<?, ?>>>();
//        this.scheduledProjects = new HashSet<String>();
        final long currentTime = System.currentTimeMillis();
        

        restartOnRegExHit(currentTime);

        if (cron != null && config.isRestartUnchangedJobsEnabled()) {
            restartUnchangedProjects(cron, currentTime);
        }
    }

    private void restartOnRegExHit(final long currentTime) {
        for (RegEx regEx : PeriodicReincarnationGlobalConfiguration.get()
                .getRegExprs()) {
            if (regEx.isTimeToRestart(currentTime)) {
                for (Project<?, ?> project : Hudson.getInstance().getProjects()) {
                    if (isValidCandidateForRestart(project)
                            && Utils.checkBuild(project.getLastBuild(), regEx)) {
                        try {
                            Utils.restart(
                                    project,
                                    "RegEx hit in console output: "
                                            + regEx.getValue(),
                                    Constants.CRONRESTART, regEx);
                        } catch (IOException e) {
                            LOGGER.warning("IO error restarting project "
                                    + project.getDisplayName());
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            LOGGER.warning("Interrupt error restarting project "
                                    + project.getDisplayName());
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private void restartUnchangedProjects(final String cron,
            final long currentTime) {
        CronTab cronTab;
        try {
            cronTab = new CronTab(cron);
            if ((cronTab.ceil(currentTime).getTimeInMillis() - currentTime) == 0) {
                for (Project<?, ?> project : Hudson.getInstance().getProjects()) {
                    if (isValidCandidateForRestart(project)) {
                        if (Utils.qualifyForUnchangedRestart(project)) {
                            try {
                                Utils.restart(
                                        project,
                                        "No difference between last two builds",
                                        Constants.CRONRESTART, null);
                            } catch (IOException e) {
                                LOGGER.warning("IO error restarting project "
                                        + project.getDisplayName());
                                e.printStackTrace();
                            } catch (InterruptedException e) {
                                LOGGER.warning("Interrupt error restarting project "
                                        + project.getDisplayName());
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        } catch (ANTLRException e1) {
            LOGGER.warning("Global cron time could not be parsed!");
            e1.printStackTrace();
        }
    }

    /**
     * Determines if a project should be tested for RegEx match or no error
     * between the last two builds.
     * 
     * @param project
     *            the current project
     * @return true should be tested, false otherwise
     */
    private boolean isValidCandidateForRestart(final Project<?, ?> project) {
        return project != null
                && project.isBuildable()
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
