package org.jenkinsci.plugins;

import hudson.model.Cause;

public class ReincarnateFailedBuildsCause extends Cause {

	@Override
	public String getShortDescription() {
		return "ReincarnateFailedBuilds";
		//return Messages.ReincarnateFailedBuildsCause_Description();
	}

}
