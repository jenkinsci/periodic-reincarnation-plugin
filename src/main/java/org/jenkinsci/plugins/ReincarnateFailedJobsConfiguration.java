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
		LOGGER.info("LOADING REINCARNATION CONFIGURATION...................................");
		load();
		if (this.regExprs != null)
			LOGGER.info("In CONFIG: " + regExprs.get(0).value);
		else LOGGER.info("RegExprs is NULL");
	}

	@DataBoundConstructor
	public ReincarnateFailedJobsConfiguration(String active, String cronTime,
			List<RegEx> regExprs) {
		LOGGER.info("2nd configuration constructor.........................................");
		this.active = active;
		this.cronTime = cronTime;
		if (regExprs == null) LOGGER.info("regExprs NULL!!");
		if (regExprs != null) LOGGER.info(regExprs.get(0).value);
		this.regExprs = regExprs;
	}

	@Override
	public boolean configure(StaplerRequest req, JSONObject json)
			throws FormException {
		this.regExprs = req.bindJSON(ReincarnateFailedJobsConfiguration.class,
				json).regExprs;
		this.active = json.getString("active").trim();
		this.cronTime = json.getString("cronTime");
		
		LOGGER.info("RegEx in SAVING: " + regExprs.get(0).value);
		save();
		return true;
	}

	public static ReincarnateFailedJobsConfiguration get() {
		return GlobalConfiguration.all().get(
				ReincarnateFailedJobsConfiguration.class);
	}

	public List<RegEx> getRegExprs() {
		LOGGER.info("regEx size: " + this.regExprs.size());
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
