/**
 * Analytica - beta version - Systems Monitoring Tool
 *
 * Copyright (C) 2013, KleeGroup, direction.technique@kleegroup.com (http://www.kleegroup.com)
 * KleeGroup, Centre d'affaire la Boursidi�re - BP 159 - 92357 Le Plessis Robinson Cedex - France
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
package com.kleegroup.analyticaimpl.agent;

import javax.inject.Inject;

import vertigo.kernel.lang.Assertion;

import com.kleegroup.analytica.agent.AgentManager;
import com.kleegroup.analytica.core.KProcess;
import com.kleegroup.analyticaimpl.agent.plugins.net.NetPlugin;

/**
 * Agent de collecte des donn�es.
 * Collecte automatique des Process (les infos sont port�es par le thread courant).
 * 
 * @author pchretien, npiedeloup
 * @version $Id: AgentManagerImpl.java,v 1.7 2012/03/29 08:48:19 npiedeloup Exp $
 */
public final class AgentManagerImpl implements AgentManager {
	private final KProcessCollector processCollector;

	/**
	 * Constructeur.
	 * @param netPlugin Plugin de communication
	 */
	@Inject
	public AgentManagerImpl(final NetPlugin netPlugin) {
		super();
		Assertion.checkNotNull(netPlugin);
		//-----------------------------------------------------------------
		processCollector = new KProcessCollector(netPlugin);
	}

	/** {@inheritDoc} */
	public void startProcess(final String type, final String... names) {
		processCollector.startProcess(type, names);
	}

	/** {@inheritDoc} */
	public void incMeasure(final String measureType, final double value) {
		processCollector.incMeasure(measureType, value);
	}

	/** {@inheritDoc} */
	public void setMeasure(final String measureType, final double value) {
		processCollector.setMeasure(measureType, value);
	}

	/** {@inheritDoc} */
	public void addMetaData(final String metaDataName, final String value) {
		processCollector.addMetaData(metaDataName, value);
	}

	/** {@inheritDoc} */
	public void stopProcess() {
		processCollector.stopProcess();
	}

	/** {@inheritDoc} */
	public void add(final KProcess process) {
		processCollector.add(process);
	}
}
