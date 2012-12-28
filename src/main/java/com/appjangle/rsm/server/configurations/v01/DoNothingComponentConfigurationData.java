package com.appjangle.rsm.server.configurations.v01;

import com.appjangle.rsm.server.configurations.DoNothingComponentConfiguration;

public class DoNothingComponentConfigurationData implements
		DoNothingComponentConfiguration {

	private static final long serialVersionUID = 1L;

	public String id;

	@Override
	public boolean isBackgroundService() {
		return false;
	}

	@Override
	public String getId() {
		return id;
	}

	public DoNothingComponentConfigurationData() {
		super();

	}

}
