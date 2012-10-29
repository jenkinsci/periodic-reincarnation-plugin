package org.jenkinsci.plugins;

import hudson.model.Cause;

/**
 * This class contains the cause for restarting a single job.
 * 
 * @author yboev
 * 
 */
public class ReincarnateFailedBuildsCause extends Cause {
    /**
     * Cause of restart for a certain job.
     */
    private String restartCause;

    /**
     * Constructor. Also adds the reason why this particular job has bee
     * restarted.
     * 
     * @param s
     *            the reason/cause of restart.
     */
    public ReincarnateFailedBuildsCause(String s) {
        super();
        this.restartCause = s;
    }

    @Override
    public String getShortDescription() {
        return "PeriodicReincarnation - " + this.restartCause;
    }
}
