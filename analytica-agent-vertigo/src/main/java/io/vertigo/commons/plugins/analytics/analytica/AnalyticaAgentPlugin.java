/**
 * Analytica - beta version - Systems Monitoring Tool
 *
 * Copyright (C) 2013, KleeGroup, direction.technique@kleegroup.com (http://www.kleegroup.com)
 * KleeGroup, Centre d'affaire la Boursidière - BP 159 - 92357 Le Plessis Robinson Cedex - France
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation;
 * either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, see <http://www.gnu.org/licenses>
 */
package io.vertigo.commons.plugins.analytics.analytica;

import io.analytica.agent.AgentManager;
import io.vertigo.commons.impl.analytics.AnalyticsAgentPlugin;
import io.vertigo.lang.Assertion;

import javax.inject.Inject;

/**
 * Implémentation de l'agent de collecte avec redirection vers Analytica.
 * @author pchretien, npiedeloup
 * @version $Id: AnalyticaAgentPlugin.java,v 1.6 2012/05/10 09:38:14 npiedeloup Exp $
 */
public final class AnalyticaAgentPlugin implements AnalyticsAgentPlugin {
	private final AgentManager analyticaManager;

	/**
	 * Constructeur.
	 * @param analyticaManager Manager Analytica
	 */
	@Inject
	public AnalyticaAgentPlugin(final AgentManager analyticaManager) {
		Assertion.checkNotNull(analyticaManager);
		//---------------------------------------------------------------------
		this.analyticaManager = analyticaManager;
	}

	/** {@inheritDoc} */
	@Override
	public void startProcess(final String processType, final String processName) {
		analyticaManager.startProcess(processType, processName);
	}

	/** {@inheritDoc} */
	@Override
	public void incMeasure(final String measureType, final double value) {
		analyticaManager.incMeasure(measureType, value);
	}

	/** {@inheritDoc} */
	@Override
	public void setMeasure(final String measureType, final double value) {
		analyticaManager.setMeasure(measureType, value);
	}

	/** {@inheritDoc} */
	@Override
	public void addMetaData(final String metaDataName, final String value) {
		analyticaManager.addMetaData(metaDataName, value);
	}

	/** {@inheritDoc} */
	@Override
	public void stopProcess() {
		analyticaManager.stopProcess();
	}
}
