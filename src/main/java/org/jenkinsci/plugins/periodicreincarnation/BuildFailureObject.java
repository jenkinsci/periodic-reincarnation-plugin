package org.jenkinsci.plugins.periodicreincarnation;

import java.util.Collection;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.kohsuke.stapler.DataBoundConstructor;

import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;

import hudson.AbortException;

/**
 * Class for handling regular expressions.
 * 
 * @author yboev
 * 
 */
public class BuildFailureObject extends PeriodicTrigger {

    /**
     * Logger for PeriodicReincarnation.
     */
    private static final Logger LOGGER = Logger.getLogger(BuildFailureObject.class.getName());

    /**
     * Constructor. Creates a reg ex.
     * 
     * @param value
     *            the reg ex.
     * @param description
     *            regex description
     * @param cronTime
     *            cron time format.
     * @param nodeAction
     *            node script.
     * @param masterAction
     *            master script
     */
    @DataBoundConstructor
    public BuildFailureObject(String value, String description, String cronTime, String nodeAction, String masterAction) {
        super(value, description, cronTime, nodeAction, masterAction);
    }

    
    /**
     * Returns the pattern corresponding to this reg ex.
     * 
     * @return the pattern.
     * @throws AbortException
     *             if the pattern could not be compiled.
     */
    public String getFailureCause() throws AbortException {
        Collection<String> causes = Utils.getAvailableFailureCausesIds();
        FailureCause fc = Utils.getFailureCauseById(this.value);
        if(fc == null) throw new AbortException("Failure Cause with id " + this.value + " does not exist!");
        return fc.getId();
    }
    

    /**
     * Returns the pattern corresponding to this reg ex.
     * 
     * @return the pattern.
     * @throws AbortException
     *             if the pattern could not be compiled.
     */
    public String getFailureCauseName() throws AbortException {
    	Collection<String> causes = Utils.getAvailableFailureCausesIds();
        FailureCause fc = Utils.getFailureCauseById(this.value);
        if(fc == null) throw new AbortException("Failure Cause with id " + this.value + " does not exist!");
        return fc.getName();
    }
}
