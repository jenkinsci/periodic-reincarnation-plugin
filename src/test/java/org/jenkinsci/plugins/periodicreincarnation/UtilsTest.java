package org.jenkinsci.plugins.periodicreincarnation;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import hudson.PluginManager;
import hudson.PluginWrapper;
import jenkins.model.Jenkins;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Jenkins.class, PluginManager.class, PluginWrapper.class})
public class UtilsTest {

	@Mock
	private Jenkins jenkins;

	@Mock
	private PluginManager pluginManager;

	@Mock
	private PluginWrapper wrapper;

	@Before
	public void setUp() throws Exception {
		PowerMockito.mockStatic(Jenkins.class);
		PowerMockito.when(Jenkins.getInstance()).thenReturn(jenkins);
		PowerMockito.when(jenkins.getPluginManager()).thenReturn(pluginManager);
		PowerMockito.when(pluginManager.getPlugin("maven-plugin"))
				.thenReturn(wrapper);
	}

	@Test
	public void testIsMavenPluginAvailable() {
		PowerMockito.when(wrapper.isEnabled()).thenReturn(true);
		assertTrue(Utils.isMavenPluginAvailable());
	}

}
