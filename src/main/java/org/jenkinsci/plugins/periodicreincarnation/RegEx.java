package org.jenkinsci.plugins.periodicreincarnation;

import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import antlr.ANTLRException;
import hudson.AbortException;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.scheduler.CronTab;
import hudson.util.FormValidation;

/**
 * Class for handling regular expressions.
 * 
 * @author yboev
 * 
 */
public class RegEx extends AbstractDescribableImpl<RegEx> {
    
    /**
     * Logger for PeriodicReincarnation.
     */
    private static final Logger LOGGER = Logger
            .getLogger(RegEx.class.getName());
    
    /**
     * Value of the reg ex as String.
     */
    public String value;
    /**
     * Description for this regex.
     */
    public String description;
    /**
     * Cron time format for this regex. Overrides the globally configured cron
     * time if set.
     */
    public String cronTime;
    /**
     * Script for node.
     */
    public String nodeAction;
    /**
     * Script for master.
     */
    public String masterAction;

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
    public RegEx(String value, String description, String cronTime,
            String nodeAction, String masterAction) {
        this.value = value;
        this.description = description;
        this.cronTime = cronTime;
        this.nodeAction = nodeAction;
        this.masterAction = masterAction;
    }

    /**
     * Returns this reg ex.
     * 
     * @return the reg ex value.
     */
    public String getValue() {
        return this.value;
    }

    /**
     * Returns a description for this regex.
     * 
     * @return Description as String.
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Returns the cron time for this particular regex.
     * 
     * @return crontime as String.
     */
    public String getCronTime() {
        return this.cronTime;
    }

    /**
     * Returns the pattern correspondin to this reg ex.
     * 
     * @return the pattern.
     * @throws AbortException
     *             if the pattern could not be compiled.
     */
    public Pattern getPattern() throws AbortException {
        Pattern pattern;
        try {
            pattern = Pattern.compile(this.value);
        } catch (PatternSyntaxException e) {
            throw new AbortException("RegEx cannot be compiled!");
        }
        return pattern;
    }

    /**
     * Returns the node script.
     * 
     * @return the script as String.
     */
    public String getNodeAction() {
        return this.nodeAction;
    }

    /**
     * Returns the master script.
     * 
     * @return the script as String.
     */
    public String getMasterAction() {
        return this.masterAction;
    }

    /**
     * Checks if the current time corresponds to the cron tab configured for
     * this RegEx. If such cron tab is missing or could not be parsed then the
     * global cron tab is used.
     * 
     * @param currentTime
     *            current time from System
     * @return true if global cron time covers the current time, false
     *         otherwise.
     */
    public boolean isTimeToRestart(long currentTime) {
        CronTab regExCronTab = null;
        CronTab globalExCronTab = null;
        try {
            if (this.getCronTime() != null) {
                regExCronTab = new CronTab(this.getCronTime());
            }
        } catch (ANTLRException e) {
            LOGGER.warning("RegEx cron tab could not be parsed!");
        }
        try {
            if (PeriodicReincarnationGlobalConfiguration.get().getCronTime() != null) {
                globalExCronTab = new CronTab(
                        PeriodicReincarnationGlobalConfiguration.get()
                                .getCronTime());
            }
        } catch (ANTLRException e) {
            LOGGER.warning("Global cron tab could not be parsed!");
        }
        // if the regExCronTab is available use it, if not go with the
        // global cron time.
        if (regExCronTab != null) {
            return (regExCronTab.ceil(currentTime).getTimeInMillis()
                    - currentTime == 0);
        }
        if (globalExCronTab != null) {
            return (globalExCronTab.ceil(currentTime).getTimeInMillis()
                    - currentTime == 0);
        }
        return false;

    }

    @Extension
    public static class DescriptorImpl extends Descriptor<RegEx> {

        /**
         * Performs on-the-fly validation on the name of the parser that needs
         * to be unique.
         * 
         * @param name
         *            the name of the parser
         * @return the validation result
         */
        public FormValidation doCheckValue(@QueryParameter final String value) {
            return FormValidation.error("checked!");
        }
        
        public FormValidation doCheckCronTime(@QueryParameter final String cronTime) {
            return FormValidation.error("checked!");
        }

        @Override
        public String getDisplayName() {
            return "";
        }
    }

}