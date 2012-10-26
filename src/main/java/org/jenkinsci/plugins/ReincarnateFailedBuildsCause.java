package org.jenkinsci.plugins;

import hudson.model.Cause;

/**
 * This class contains the cause for restarting a single job.
 * @author yboev
 *
 */
public class ReincarnateFailedBuildsCause extends Cause {

    @Override
    public String getShortDescription() {
        return "PeriodicReincarnation";
    }

}
