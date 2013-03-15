package org.jenkinsci.plugins.periodicreincarnation.test;


import java.io.IOException;

import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.Result;

import org.jenkinsci.plugins.periodicreincarnation.PeriodicReincarnation;
import org.jenkinsci.plugins.periodicreincarnation.PeriodicReincarnationBuildCause;
import org.jenkinsci.plugins.periodicreincarnation.PeriodicReincarnationGlobalConfiguration;
import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.recipes.LocalData;
import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

/**
 * Test class.
 * @author yboev
 *
 */
public class TestConfiguration extends HudsonTestCase {
    
    /**
     * The global configuration.
     */
    private PeriodicReincarnationGlobalConfiguration config;
    /**
     * The HTML form.
     */
    private HtmlForm form;

    /**
     * TestCase.
     * @throws Exception exception.
     */
    @LocalData
    @Test
    public void test1() throws Exception {
        final long reccurancePeriod = 60000;
        assertNotNull(PeriodicReincarnation.get());
        final String s = "reg ex hit";
        assertEquals("PeriodicReincarnation - " + s,
                new PeriodicReincarnationBuildCause(s).getShortDescription());
        assertEquals(reccurancePeriod, PeriodicReincarnation.get().getRecurrencePeriod());

        final Job<?, ?> job1 = (Job<?, ?>) Hudson.getInstance().getItem("test_job");
        assertNotNull("job missing.. @LocalData problem?", job1);
        assertEquals(Result.FAILURE, job1.getLastBuild().getResult());
        System.out.println("JOB1 LOG:"
                + job1.getLastBuild().getLogFile().toString());

        final Job<?, ?> job2 = (Job<?, ?>) Hudson.getInstance().getItem("no_change");
        assertNotNull("job missing.. @LocalData problem?", job2);
        assertEquals(Result.FAILURE, job2.getLastBuild().getResult());
        assertNotNull(job2.getLastSuccessfulBuild());

        this.getGlobalForm();

        final HtmlTextInput cronTime = form.getInputByName("_.cronTime");
        assertNotNull("Cron Time is null!", cronTime);
        cronTime.setValueAttribute("* * * * *");

        final HtmlInput activeCron = form.getInputByName("_.activeCron");
        assertNotNull("EnableDisable cron field is null!", activeCron);
        activeCron.setChecked(true);
        
        final HtmlInput activeTrigger = form.getInputByName("_.activeTrigger");
        assertNotNull("EnableDisable trigger field is null!", activeTrigger);
        activeTrigger.setChecked(true);
        
        final HtmlTextInput maxDepth = form.getInputByName("_.maxDepth");
        assertNotNull("EnableDisable trigger field is null!", activeTrigger);
        maxDepth.setValueAttribute("2");


        final HtmlInput noChange = form.getInputByName("_.noChange");
        assertNotNull("NoChange checkbox was null!", noChange);
        noChange.setChecked(true);

        final HtmlElement regExprs = (HtmlElement) form.getByXPath(
                "//tr[td='Regular Expressions']").get(0);
        assertNotNull("RegExprs is null!", regExprs);
        assertNotNull("Add button not found",
                regExprs.getFirstByXPath(".//button"));
        ((HtmlButton) regExprs.getFirstByXPath(".//button")).click();
        final HtmlTextInput regEx1 = (HtmlTextInput) regExprs
                .getFirstByXPath("//input[@name='" + "regExprs.value" + "']");
        assertNotNull("regEx1 is null!", regEx1);
        regEx1.setValueAttribute("reg ex one");

        submit(form);
        config = new PeriodicReincarnationGlobalConfiguration();
        assertNotNull(config);
        assertEquals("* * * * *", config.getCronTime());
        assertEquals("true", config.getActiveCron());
        assertTrue(config.isCronActive());
        assertTrue(config.isTriggerActive());
        assertEquals(1, config.getRegExprs().size());
        assertEquals("reg ex one", config.getRegExprs().get(0).getValue());
        assertEquals("true", config.getNoChange());
        assertTrue(config.isRestartUnchangedJobsEnabled());
        assertEquals(2, config.getMaxDepth());
    }

    /**
     * Finds and sets the global form. Also makes a couple of test to see if
     * everything is correct.
     * 
     * @throws IOException
     *             IO error
     * @throws SAXException
     *             SAX error
     */
    private void getGlobalForm() throws IOException, SAXException {
        final HtmlPage page = new WebClient().goTo("configure");
        final String allElements = page.asText();

        assertTrue(allElements.contains("Cron Time"));
        assertTrue(allElements.contains("Periodic Reincarnation"));
        assertTrue(allElements.contains("Max restart depth"));
        assertTrue(allElements.contains("Enable/Disable afterbuild job reincarnation"));
        assertTrue(allElements.contains("Regular Expressions"));
        assertTrue(allElements.contains("Enable/Disable cron job reincarnation"));
        assertTrue(allElements
                .contains("Restart unchanged builds failing for the first time"));

        this.form = page.getFormByName("config");
        assertNotNull("Form is null!", this.form);
    }

}
