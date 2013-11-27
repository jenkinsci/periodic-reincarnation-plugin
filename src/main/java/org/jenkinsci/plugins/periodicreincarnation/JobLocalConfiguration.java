package org.jenkinsci.plugins.periodicreincarnation;

import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;
import hudson.Extension;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.AbstractProject;
import hudson.model.Job;

/**
 * Local configuration class.
 * 
 * @author yboev
 * 
 */
public class JobLocalConfiguration extends JobProperty<Job<?, ?>> {

    private static final Logger LOGGER = Logger
            .getLogger(JobLocalConfiguration.class.getName());

    /**
     * Tells if this project is locally configured(true means we override global
     * values).
     */
    private boolean isLocallyConfigured;

    /**
     * Instance of the localValues nested class.
     */
    private LocalValues localValues;

    /**
     * Contructor for data binding of form data.
     * 
     * @param isEnabled
     *            true if activated.
     * @param maxDepth
     *            max restart depth
     * @param isLocallyConfigured
     *            tells if local config is enabled.
     */
    @DataBoundConstructor
    public JobLocalConfiguration(LocalValues optionalBlock) {
        LOGGER.info("CONSTRUCTOR called...");
        if (optionalBlock != null) {
            this.isLocallyConfigured = true;
            this.localValues = optionalBlock;
        } else {
            LOGGER.info("FALSE...");
            this.isLocallyConfigured = false;
        }
    }

    /**
     * Returns isEnabled.
     * 
     * @return isEnabled value.
     */
    public boolean getIsEnabled() {
        return this.localValues.isEnabled;
    }

    /**
     * Returns maxDepth.
     * 
     * @return maxDepth value.
     */
    public int getMaxDepth() {
        return this.localValues.maxDepth;
    }

    /**
     * Returns isLocallyConfigured.
     * 
     * @return isLocallyConfigured value.
     */
    public boolean getIsLocallyConfigured() {
        return isLocallyConfigured;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return DESCRIPTOR;
    }

    /**
     * Descriptor object.
     */
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    /**
     * Implementation of the descriptor object.
     * 
     * @author yboev
     * 
     */
    public static final class DescriptorImpl extends JobPropertyDescriptor {

        /** Constructor loads previously saved form data. */
        DescriptorImpl() {
            super(JobLocalConfiguration.class);
            load();
        }

        /**
         * Returns caption for our part of the config page.
         * 
         * @return caption
         */
        public String getDisplayName() {
            return "PeriodicReincarnation";
        }

        /**
         * Certainly does something.
         * 
         * @param item
         *            Some item, I guess
         * @return true
         */
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }
    }

    /**
     * The options that unfold after enabling the local configuration for
     * periodic reincarnation in the job config page.
     * 
     * @author yboev
     * 
     */
    public static class LocalValues {
        /**
         * Tells if restart is enabled for this project.
         */
        private boolean isEnabled;
        /**
         * Max restart depth.
         */
        private int maxDepth;

        @DataBoundConstructor
        public LocalValues(boolean isEnabled, int maxDepth) {
            LOGGER.info("CONSTRUCTOR2 called...");
            this.isEnabled = isEnabled;
            this.maxDepth = maxDepth;
            LOGGER.info("values2: [" + isEnabled + "][" + maxDepth + "]");
        }
    }
}
