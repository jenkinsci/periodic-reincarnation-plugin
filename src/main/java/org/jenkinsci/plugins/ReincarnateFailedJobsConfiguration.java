package org.jenkinsci.plugins;

import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import hudson.AbortException;
import hudson.Extension;
import hudson.scheduler.CronTab;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import antlr.ANTLRException;

import jenkins.model.GlobalConfiguration;

/**
 * Implements the interface GlobalConfiguration from Jenkins.
 * Gives the option to configure how this plugin works, what
 * should be restarted, when it should be restarted and what
 * should appear in the log file.
 * With the help of the load() and save() method the configuration
 * is loaded when instancing this class and saved when changing it.
 * 
 * @author yboev
 *
 */
@Extension
public class ReincarnateFailedJobsConfiguration extends GlobalConfiguration {

	private String active;
	private String cronTime;
	private List<RegEx> regExprs;
	private String logInfo;
	private String noChange;

	private static final Logger LOGGER = Logger
			.getLogger(ReincarnateFailedJobsConfiguration.class.getName());

	
	public ReincarnateFailedJobsConfiguration() {
		load();
	}

	@DataBoundConstructor
	public ReincarnateFailedJobsConfiguration(String active, String cronTime,
			List<RegEx> regExprs, String logInfo) {
		this.active = active;
		this.cronTime = cronTime;
		this.regExprs = regExprs;
		this.logInfo = logInfo;
	}

	@Override
	public boolean configure(StaplerRequest req, JSONObject json)
			throws FormException {
		this.regExprs = req.bindJSON(ReincarnateFailedJobsConfiguration.class,
				json).regExprs;
		this.active = json.getString("active").trim();
		this.cronTime = json.getString("cronTime");
		this.logInfo = json.getString("logInfo");
		this.noChange = json.getString("noChange");
		save();
		return true;
	}
	
	public FormValidation doCheckCronTime() {
		try {
			new CronTab(cronTime);
			return FormValidation.ok();
		} catch (ANTLRException e) {
			return FormValidation.error("Cron time could not be parsed. Please check for type errors!");
		} catch (NullPointerException e) {
			return FormValidation.error("Cron time was null");
		}
	}

	public static ReincarnateFailedJobsConfiguration get() {
		return GlobalConfiguration.all().get(
				ReincarnateFailedJobsConfiguration.class);
	}
	
	public String getNoChange() {
		return this.noChange;
	}
	
	public boolean isRestartUnchangedJobsEnabled() {
		return (this.noChange != null && this.noChange.equals("true"));
	}
	
	public String getLogInfo() {
		return this.logInfo;
	}
	
	public boolean isLogInfoEnabled() {
		return (this.logInfo != null && this.logInfo.equals("true"));
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
		return (this.active != null && this.active.equals("true"));
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
        
        public Pattern getPattern() throws AbortException {
            Pattern pattern;
            try {
                pattern = Pattern.compile(this.value);
            } catch (PatternSyntaxException e) {
                throw new AbortException();
            }
            return pattern;
        }
    }

}
