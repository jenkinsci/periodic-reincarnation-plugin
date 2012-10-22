package org.jenkinsci.plugins;

import java.util.Iterator;

import org.jenkinsci.plugins.ReincarnateFailedJobsConfiguration;
import org.junit.Before;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.recipes.LocalData;
import org.jvnet.hudson.test.recipes.Recipe;
import org.jvnet.hudson.test.recipes.Recipe.Runner;

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

import hudson.model.*;
import hudson.tasks.Shell;

public class TestConfiguration extends HudsonTestCase {

	ReincarnateFailedJobsConfiguration config;

	@LocalData
	public void test1() throws Exception {
		/*
		FreeStyleProject project = createFreeStyleProject();
		project.getBuildersList().add(new Shell("echo hello"));

		FreeStyleBuild build = project.scheduleBuild2(0).get();
		System.out.println(build.getDisplayName() + " completed");

		Job job1 = (Job) Hudson.getInstance().getItem("block_job");
		assertNotNull("job missing.. @LocalData problem?", job1);
		 */

		HtmlPage page = new WebClient().goTo("configure");
		final String allElements = page.asText();

		assertTrue(allElements.contains("Cron Time"));
		assertTrue(allElements.contains("Periodic Reincarnation"));
		assertTrue(allElements.contains("Regular Expressions"));
		assertTrue(allElements.contains("Show info in log"));
		assertTrue(allElements.contains("Enable/Disable"));
		assertTrue(allElements
				.contains("Restart unchanged builds failing for the first time"));

		HtmlForm form = page.getFormByName("config");
		assertNotNull("Form is null!", form);
		
		final HtmlTextInput cronTime = form.getInputByName("_.cronTime");
		assertNotNull("Cron Time is null!", cronTime);
		cronTime.setValueAttribute("* * * * *");
		
		final HtmlInput active = form.getInputByName("_.active");
		assertNotNull("EnableDisable field is null!", active);
		active.setChecked(true);
		
		final HtmlInput logInfo = form.getInputByName("_.logInfo");
		assertNotNull("Log info checkbox is null!", logInfo);
		logInfo.setChecked(true);
		
		final HtmlInput noChange = form.getInputByName("_.noChange");
		assertNotNull("NoChange checkbox was null!", noChange);
		noChange.setChecked(true);
		
		final HtmlElement regExprs = (HtmlElement) form.getByXPath("//tr[td='Regular Expressions']").get(0);
		assertNotNull("RegExprs is null!", regExprs);
		assertNotNull("Add button not found", regExprs.getFirstByXPath(".//button"));
		((HtmlButton)regExprs.getFirstByXPath(".//button")).click();
		final HtmlTextInput regEx1 = (HtmlTextInput)regExprs.getFirstByXPath("//input[@name='" + "regExprs.value" + "']");
		assertNotNull("regEx1 is null!", regEx1);
		regEx1.setValueAttribute("reg ex one");
		
		submit(form);
		config = new ReincarnateFailedJobsConfiguration();
		assertEquals("* * * * *", config.getCronTime());
		assertEquals("true", config.getActive());
		assertTrue(config.isActive());
		assertEquals("reg ex one", config.getRegExprs().get(0).getValue());
		assertEquals("true", config.getNoChange());
		assertTrue(config.isRestartUnchangedJobsEnabled());
		assertEquals("true", config.getLogInfo());
		assertTrue(config.isLogInfoEnabled());
	}
	
	/*
	@LocalData
	public void test2() throws Exception {
		FreeStyleProject project = createFreeStyleProject();
		project.getBuildersList().add(new Shell("echo hello"));

		FreeStyleBuild build = project.scheduleBuild2(0).get();
		System.out.println(build.getDisplayName() + " completed");

		Job job1 = (Job) Hudson.getInstance().getItem("block_job");
		assertNotNull("job missing.. @LocalData problem?", job1);
		new PeriodicReincarnation();
	}
	 */
}
