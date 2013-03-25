package org.jenkinsci.plugins.periodicreincarnation;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.AbstractProject;
import hudson.model.Job;

/**
 * Local configuration class.
 * @author yboev
 *
 */
public class JobLocalConfiguration extends JobProperty<Job<?, ?>> {
    
    /**
     * Tells if restart is enabled for this project.
     */
    private boolean isEnabled;
    /**
     * Max restart depth.
     */
    private int maxDepth;
    
    /**
     * Tells if this project is locally configured(true means we override global values).
     */
    private boolean isLocallyConfigured;

    /**
     * Contructor for data binding of form data.
     * @param isEnabled true if activated.
     * @param maxDepth max restart depth
     * @param isLocallyConfigured tells if local config is enabled.
     */
    @DataBoundConstructor
    public JobLocalConfiguration(boolean isEnabled, int maxDepth,
            boolean isLocallyConfigured) {
        this.isEnabled = isEnabled;
        this.maxDepth = maxDepth;
        this.isLocallyConfigured = isLocallyConfigured;
    }
    
    /**
     * Returns isEnabled.
     * @return isEnabled value.
     */
    public boolean getIsEnabled() {
        return isEnabled;
    }

    /**
     * Returns maxDepth.
     * @return maxDepth value.
     */
    public int getMaxDepth() {
        return maxDepth;
    }
    
    /**
     * Returns isLocallyConfigured.
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

        @Override
        public JobProperty<?> newInstance(StaplerRequest req,
                JSONObject formData) throws FormException {

            final JSONObject newData = new JSONObject();
            if (formData.containsKey("isLocallyConfigured")) {
                newData.put("isEnabled",
                        formData.getJSONObject("isLocallyConfigured")
                                .getString("isEnabled"));
                newData.put("maxDepth",
                        formData.getJSONObject("isLocallyConfigured")
                                .getString("maxDepth"));
                newData.put("isLocallyConfigured", "true");
            } else {
                newData.put("isEnabled", "false");
                newData.put("maxDepth", "");
                newData.put("isLocallyConfigured", "false");
            }
            return super.newInstance(req, newData);
        }
    }
}
