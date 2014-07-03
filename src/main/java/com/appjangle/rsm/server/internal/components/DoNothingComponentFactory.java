package com.appjangle.rsm.server.internal.components;

import com.appjangle.rsm.server.configurations.DoNothingComponentConfiguration;

import de.mxro.factories.Configuration;
import de.mxro.factories.Factory;
import de.mxro.server.ComponentDependencies;

public class DoNothingComponentFactory implements Factory<DoNothingComponent, DoNothingComponentConfiguration, ComponentDependencies> {

	@Override
	public boolean canInstantiate(Configuration conf) {
		return conf instanceof DoNothingComponentConfiguration;
	}

	@Override
	public DoNothingComponent create(DoNothingComponentConfiguration conf,
			ComponentDependencies dependencies) {
		
		return new DoNothingComponent();
	}

}
