package org.jenkinsci.plugins;

import java.io.IOException;
import java.util.logging.Logger;

import org.jenkinsci.plugins.PeriodicReincarnationGlobalConfiguration.RegEx;

import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.Project;
import hudson.model.listeners.RunListener;
import static hudson.model.Result.SUCCESS;

@Extension
public class AfterbuildReincarnation extends RunListener<AbstractBuild<?, ?>> {

    private static final Logger LOGGER = Logger
            .getLogger(AfterbuildReincarnation.class.getName());

    @Override
    public void onCompleted(AbstractBuild<?, ?> build, TaskListener listener) {
        LOGGER.info("los geht's");
        if (build.getResult() == SUCCESS) {
            return;
        }

        if (PeriodicReincarnation.get() == null) {
            return;
        }

        PeriodicReincarnationGlobalConfiguration config = PeriodicReincarnationGlobalConfiguration
                .get();
        if (config == null || config.isTriggerActive() == false) {
            return;
        }

        // check for a regEx hit
        RegEx regEx = Utils.checkBuild(build);
        if (regEx != null && checkRestartDepth(build, config)) {
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

    private boolean checkRestartDepth(AbstractBuild<?, ?> build,
            PeriodicReincarnationGlobalConfiguration config) {
        int maxDepth = Integer.parseInt(config.getMaxDepth());
        if (maxDepth <= 0) {
            return true;
        }
        int count = 0;
        while (build != null
                && build.getCause(PeriodicReincarnationBuildCause.class) != null) {
            if (build.getCause(PeriodicReincarnationBuildCause.class)
                    .getShortDescription().contains("Afterbuild")) {
                count++;
            }
            if (count > maxDepth) {
                return false;
            }
            build = build.getPreviousBuild();
        }
        return true;
    }
}
