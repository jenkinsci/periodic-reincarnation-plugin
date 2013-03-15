package org.jenkinsci.plugins.periodicreincarnation;

import java.io.IOException;

import org.jenkinsci.plugins.periodicreincarnation.PeriodicReincarnationGlobalConfiguration.RegEx;

import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.Project;
import hudson.model.listeners.RunListener;
import static hudson.model.Result.SUCCESS;

@Extension
public class AfterbuildReincarnation extends RunListener<AbstractBuild<?, ?>> {

    int maxRestartDepth;
    boolean isEnabled;

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

        JobLocalConfiguration localconfig = build.getProject().getProperty(
                JobLocalConfiguration.class);
        PeriodicReincarnationGlobalConfiguration config = PeriodicReincarnationGlobalConfiguration
                .get();
        // stop if there is no global configuration
        if (config == null) {
            return;
        }
        setConfigVariables(localconfig, config);

        // stop if not enabled
        if (this.isEnabled == false) {
            return;
        }

        // check for a regEx hit
        checkForRegExRestart(build, config);
        
        
        checkForNoChangeRestart(build, config);

    }

    private void checkForNoChangeRestart(AbstractBuild<?, ?> build,
            PeriodicReincarnationGlobalConfiguration config) {
        if (Utils
                .qualifyForUnchangedRestart((Project<?, ?>) build.getProject())
                && config.isRestartUnchangedJobsEnabled()) {
            try {

                Utils.restart(
                        (Project<?, ?>) build.getProject(),
                        config,
                        "(Afterbuild restart) No difference between last two builds",
                        null);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void checkForRegExRestart(AbstractBuild<?, ?> build,
            PeriodicReincarnationGlobalConfiguration config) {
        RegEx regEx = Utils.checkBuild(build);
        if (regEx != null && checkRestartDepth(build)) {
            try {
                Utils.restart((Project<?, ?>) build.getProject(), config,
                        "(Afterbuild restart) RegEx hit in console output: "
                                + regEx.getValue(), regEx);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

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
