package org.jenkinsci.plugins.periodicreincarnation;

import hudson.PluginManager;
import hudson.PluginWrapper;
import jenkins.model.Jenkins;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UtilsTest {

	@Mock
	private Jenkins jenkins;

	@Mock
	private PluginManager pluginManager;

	@Mock
	private PluginWrapper wrapper;

	@Test
	public void testIsMavenPluginAvailable() {
		try (MockedStatic<Jenkins> jenkinsStatic = Mockito.mockStatic(Jenkins.class)) {
			jenkinsStatic.when(Jenkins::getInstanceOrNull).thenReturn(jenkins);
			when(jenkins.getPluginManager()).thenReturn(pluginManager);
			when(pluginManager.getPlugin("maven-plugin")).thenReturn(wrapper);
			when(wrapper.isEnabled()).thenReturn(true);
			assertTrue(Utils.isMavenPluginAvailable());
		}
	}
}
