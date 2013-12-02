//package org.jenkinsci.plugins.periodicreincarnationtest;
//
//import static org.junit.Assert.*;
//
//import org.junit.Test;
//import org.jvnet.hudson.test.JenkinsRule;
//import org.jvnet.hudson.test.recipes.LocalData;
//
//import com.gargoylesoftware.htmlunit.html.HtmlPage;
//
//public class TestLocalConfiguration extends JenkinsRule {
//    @LocalData
//    @Test
//    public void test2() throws Exception {
//        JenkinsRule.SLAVE_DEBUG_PORT = 8080;
//        final HtmlPage page = this.createWebClient().goTo("job/afterbuild_test/config");
//        final String allElements = page.asText();
//        assertNotNull(allElements);
//
//    }
//}