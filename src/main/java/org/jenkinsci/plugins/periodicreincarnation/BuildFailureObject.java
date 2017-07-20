package org.jenkinsci.plugins.periodicreincarnation;

import org.kohsuke.stapler.DataBoundConstructor;

import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;

import hudson.AbortException;

/**
 * Class for handling Failure Causes of Build-Failure-Plugin.
 * 
 * @author Jochen Gietzen
 * 
 */
public class BuildFailureObject extends PeriodicTrigger {

	/**
	 * Constructor. Creates a failure cause.
	 * 
	 * @param value
	 *            the failure cause id.
	 * @param description
	 *            failure cause description
	 * @param cronTime
	 *            cron time format.
	 * @param nodeAction
	 *            node script.
	 * @param masterAction
	 *            master script
	 */
	@DataBoundConstructor
	public BuildFailureObject(String value, String description, String cronTime,
			String nodeAction, String masterAction) {
		super(value, description, cronTime, nodeAction, masterAction);
	}

	/**
	 * Returns the Id of this Failure Cause.
	 * 
	 * @return the failure cause id.
	 * @throws AbortException
	 *             if the FailureCause ID doesn't exist in the BuildFailure
	 *             Database.
	 */
	public String getFailureCause() throws AbortException {
		FailureCause fc = Utils.getFailureCauseById(this.value);
		if (fc == null)
			throw new AbortException(
					"Failure Cause with id " + this.value + " does not exist!");
		return fc.getId();
	}

	/**
	 * Returns the Name of this Failure Cause.
	 * 
	 * @return the failure cause name.
	 * @throws AbortException
	 *             if the FailureCause ID doesn't exist in the BuildFailure
	 *             Database.
	 */
	public String getFailureCauseName() throws AbortException {
		FailureCause fc = Utils.getFailureCauseById(this.value);
		if (fc == null)
			throw new AbortException(
					"Failure Cause with id " + this.value + " does not exist!");
		return fc.getName();
	}
}
