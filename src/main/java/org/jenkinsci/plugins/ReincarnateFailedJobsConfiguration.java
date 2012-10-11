package org.jenkinsci.plugins;

import java.util.List;
import java.util.logging.Logger;

import hudson.Extension;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import jenkins.model.GlobalConfiguration;

@Extension
public class ReincarnateFailedJobsConfiguration extends GlobalConfiguration {

	private String active;
	private String cronTime;
	private List<RegEx> regExprs;

	private static final Logger LOGGER = Logger
			.getLogger(ReincarnateFailedJobsConfiguration.class.getName());

	public ReincarnateFailedJobsConfiguration() {
		load();
	}

	@DataBoundConstructor
	public ReincarnateFailedJobsConfiguration(String active, String cronTime,
			List<RegEx> regExprs) {
		this.active = active;
		this.cronTime = cronTime;
		this.regExprs = regExprs;
	}

	@Override
	public boolean configure(StaplerRequest req, JSONObject json)
			throws FormException {
		this.regExprs = req.bindJSON(ReincarnateFailedJobsConfiguration.class,
				json).regExprs;
		this.active = json.getString("active").trim();
		this.cronTime = json.getString("cronTime");
		
		save();
		return true;
	}

	public static ReincarnateFailedJobsConfiguration get() {
		return GlobalConfiguration.all().get(
				ReincarnateFailedJobsConfiguration.class);
	}

	public List<RegEx> getRegExprs() {
		return this.regExprs;
	}

	public String getCronTime() {
		return this.cronTime;
	}

	public String getActive() {
		return this.active;
	}

	public boolean isActive() {
		return (this.active == "true");
	}
	
	public static class RegEx {
        public String value;
        
        @DataBoundConstructor
        public RegEx(String value) {
            this.value = value;
        }
        
        public String getValue() {
        	return this.value;
        }
    }

}
