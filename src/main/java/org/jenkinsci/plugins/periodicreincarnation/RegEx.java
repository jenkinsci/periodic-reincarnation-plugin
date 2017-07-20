package org.jenkinsci.plugins.periodicreincarnation;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.AbortException;

/**
 * Class for handling regular expressions.
 * 
 * @author yboev
 * 
 */
public class RegEx extends PeriodicTrigger {

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
		super(value, description, cronTime, nodeAction, masterAction);
	}

	/**
	 * Returns the pattern corresponding to this reg ex.
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
}
