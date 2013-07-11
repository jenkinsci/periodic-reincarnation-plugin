//package org.jenkinsci.plugins.periodicreincarnationtest;
//
//import org.junit.Test;
//import org.jvnet.hudson.test.HudsonTestCase;
//import org.jvnet.hudson.test.HudsonTestCase.WebClient;
//import org.jvnet.hudson.test.recipes.LocalData;
//
//import com.gargoylesoftware.htmlunit.html.HtmlPage;
//
//public class TestLocalConfiguration extends HudsonTestCase {
//    @LocalData
//    @Test
//    public void testLocalConfig() throws Exception {
//        final HtmlPage page = new WebClient()
//                .goTo("job/afterbuild_test/configure");
//        final String allElements = page.asText();
//        assertNotNull(allElements);
//
//    }
//}