package org.jenkinsci.plugins.periodicreincarnation;

import java.io.IOException;

import org.jenkinsci.plugins.periodicreincarnation.PeriodicReincarnationGlobalConfiguration.RegEx;

import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.Project;
import hudson.model.listeners.RunListener;
import static hudson.model.Result.SUCCESS;

/**
 * This class triggers a restart automatically after a build has failed.
 * 
 * @author yboev
 * 
 */
@Extension
public class AfterbuildReincarnation extends RunListener<AbstractBuild<?, ?>> {

    /**
     * Maximal times a project can be automatically restarted from this class in
     * a row.
     * 
     */
    private int maxRestartDepth;

    /**
     * Tells if this type of restart is enabled.
     */
    private boolean isEnabled;

    @Override
    public void onCompleted(AbstractBuild<?, ?> build, TaskListener listener) {

        // stop if no build or project can be retrieved
        if (build == null || build.getProject() == null) {
            return;
        }

        // stop if build was a success
        if (build.getResult() == SUCCESS) {
            return;
        }

        final JobLocalConfiguration localConfig = build.getProject()
                .getProperty(JobLocalConfiguration.class);
        final PeriodicReincarnationGlobalConfiguration globalConfig = PeriodicReincarnationGlobalConfiguration
                .get();
        // stop if there is no global configuration
        if (globalConfig == null) {
            return;
        }
        setConfigVariables(localConfig, globalConfig);

        // stop if not enabled
        if (!this.isEnabled) {
            return;
        }

        // check for a regEx hit
        checkForRegExRestart(build, globalConfig);

        checkForNoChangeRestart(build, globalConfig);

    }

    /**
     * Checks if we can restart the project for unchanged project configuration.
     * 
     * @param build
     *            the build
     * @param config
     *            the periodic reincarnation configuration
     */
    private void checkForNoChangeRestart(AbstractBuild<?, ?> build,
            PeriodicReincarnationGlobalConfiguration config) {
        if (build.getProject() instanceof Project<?, ?>
                && Utils.qualifyForUnchangedRestart((Project<?, ?>) build
                        .getProject())
                && config.isRestartUnchangedJobsEnabled()) {
            try {

                Utils.restart((Project<?, ?>) build.getProject(),

                "No difference between last two builds",
                        Constants.AFTERBUILDRESTART, null);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Checks if we can restart the project for a RegEx hit.
     * 
     * @param build
     *            the build
     * @param config
     *            the periodic reincarnation configuration
     */
    private void checkForRegExRestart(AbstractBuild<?, ?> build,
            PeriodicReincarnationGlobalConfiguration config) {
        final RegEx regEx = Utils.checkBuild(build);
        if (regEx != null && checkRestartDepth(build)) {
            try {
                Utils.restart((Project<?, ?>) build.getProject(),
                        "RegEx hit in console output: " + regEx.getValue(),
                        Constants.AFTERBUILDRESTART, regEx);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Retrieves values from global or local config.
     * 
     * @param localconfig
     *            localconfiguration
     * @param config
     *            globalconfiguration
     */
    private void setConfigVariables(JobLocalConfiguration localconfig,
            PeriodicReincarnationGlobalConfiguration config) {
        if (localconfig != null && localconfig.getIsLocallyConfigured()) {
            this.isEnabled = localconfig.getIsEnabled();
            this.maxRestartDepth = localconfig.getMaxDepth();
        } else {
            this.isEnabled = config.isTriggerActive();
            this.maxRestartDepth = config.getMaxDepth();
        }
    }

    /**
     * Checks the restart depth for the current project.
     * 
     * @param build
     *            the current build
     * @return true if check has passed, false otherwise
     */
    private boolean checkRestartDepth(AbstractBuild<?, ?> build) {
        if (this.maxRestartDepth <= 0) {
            return true;
        }
        int count = 0;

        // count the number of restarts for the current project
        while (build != null
                && build.getCause(PeriodicReincarnationBuildCause.class) != null) {
            if (build.getCause(PeriodicReincarnationBuildCause.class)
                    .getShortDescription().contains("Afterbuild")) {
                count++;
            }
            if (count > this.maxRestartDepth) {
                return false;
            }
            build = build.getPreviousBuild();
        }
        return true;
    }
}
