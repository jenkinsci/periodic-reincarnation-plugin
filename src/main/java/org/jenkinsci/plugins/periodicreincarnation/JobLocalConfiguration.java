package org.jenkinsci.plugins.periodicreincarnation;

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

    // private static final Logger LOGGER = Logger
    // .getLogger(JobLocalConfiguration.class.getName());

    /**
     * Tells if this project is locally configured(true means we override global
     * values).
     */
    private boolean isLocallyConfigured;
    
    
    /**
     * Tells if this project is locally deactivated.
     */
    private boolean isLocallyDeactivated = false;
    

    /**
     * Instance of the localValues nested class.
     */
    private LocalValues localValues;

    /**
     * Contructor for data binding of form data.
     * 
     * @param optionalBlock
     *            instance of LocalValues.
     */
    @DataBoundConstructor
    public JobLocalConfiguration(LocalValues optionalBlock) {
        if (optionalBlock != null) {
            this.isLocallyConfigured = true;
            this.localValues = optionalBlock;
            this.isLocallyDeactivated = optionalBlock.isLocallyDeactivated;
            
        } else {
            this.isLocallyConfigured = false;
            this.isLocallyDeactivated = false;
            
        }
    }

    /**
     * Returns isEnabled.
     * 
     * @return isEnabled value.
     */
    public boolean getIsEnabled() {
        if (this.localValues == null) {
            return false;
        }
        return this.localValues.isEnabled;
    }

    /**
     * Returns maxDepth.
     * 
     * @return maxDepth value.
     */
    public int getMaxDepth() {
        if (this.localValues == null) {
            return 0;
        }
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
    
    /**
     * Returns isLocallyDeactivated.
     * 
     * @return isLocallyDeactivated value.
     */
    public boolean getIsLocallyDeactivated() {
        return isLocallyDeactivated;
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
        /**
         * Tells if this job is deactivated for global reincarnations.
         */
        private boolean isLocallyDeactivated;

        /**
         * Constructor.
         * 
         * @param isEnabled
         *            is afterbuild restart enabled.
         * @param maxDepth
         *            what is the maximal restart depth for this particular job.
         */
        @DataBoundConstructor
        public LocalValues(boolean isEnabled, int maxDepth, boolean isLocallyDeactivated) {
            this.isEnabled = isEnabled;
            this.maxDepth = maxDepth;
            this.isLocallyDeactivated = isLocallyDeactivated;
        }
    }
}
