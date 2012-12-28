package com.appjangle.rsm.server;

import com.appjangle.rsm.server.configurations.v01.DoNothingComponentConfigurationData;

import de.mxro.server.ComponentConfiguration;

public class RsmConfigurations {

	public static ComponentConfiguration createDoNothingComponentConfiguration(
			final String id) {
		final DoNothingComponentConfigurationData data = new DoNothingComponentConfigurationData();
		data.id = id;
		return data;
	}

}
