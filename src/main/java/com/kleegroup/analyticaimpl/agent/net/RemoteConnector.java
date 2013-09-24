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
package com.kleegroup.analyticaimpl.agent.net;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status.Family;

import kasper.kernel.exception.KRuntimeException;
import net.sf.ehcache.Element;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.kleegroup.analytica.core.KProcess;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.WebResource;

/**
 * TODO voir http://ghads.wordpress.com/2008/09/24/calling-a-rest-webservice-from-java-without-libs/
 * @author npiedeloup
 * @version $Id: RemoteNetPlugin.java,v 1.4 2012/06/14 13:49:17 npiedeloup Exp $
 */
public final class RemoteConnector implements KProcessConnector {
	private static final String SPOOL_CONTEXT = "Analytica_Spool";
	private static final String VERSION_MAJOR = "1.0"; //definit la compatibilit�
	private static final String VERSION_MINOR = "0";
	private static final String VERSION = VERSION_MAJOR + "." + VERSION_MINOR;

	private final Logger logger = Logger.getLogger(RemoteConnector.class);

	private Thread processSenderThread = null;
	private final ConcurrentLinkedQueue<KProcess> processQueue = new ConcurrentLinkedQueue<KProcess>();
	private net.sf.ehcache.CacheManager manager;
	private final String serverUrl;
	private final int sendPaquetSize;
	private final int sizeCheckFrequencyMs;
	private final int sendPaquetFrequencySeconds;
	private Client locatorClient;
	private WebResource remoteWebResource;
	private final int maxResendJson = 5; //On limite a 5 car ce sont d�j� des paquets de sendPaquetSize Processes

	/**
	 * @param serverUrl Url du serveur Analytica
	 * @param sendPaquetSize Taille des paquets d�clenchant l'envoi anticip�
	 * @param sendPaquetFrequencySeconds Frequence normal d'envoi des paquets (en seconde)
	 */
	@Inject
	public RemoteConnector(@Named("serverUrl") final String serverUrl, @Named("sendPaquetSize") final int sendPaquetSize, @Named("sendPaquetFrequencySeconds") final int sendPaquetFrequencySeconds) {
		this.serverUrl = serverUrl;
		this.sendPaquetSize = sendPaquetSize;
		this.sendPaquetFrequencySeconds = sendPaquetFrequencySeconds;
		sizeCheckFrequencyMs = 250;
	}

	/** {@inheritDoc} */
	public void add(final KProcess process) {
		processQueue.add(process);
		//		if (processQueue.size() >= sendPaquetSize) {
		//			synchronized (processQueue) {
		//				processQueue.notify();
		//				logger.trace("processQueue.size() >= sendPaquetSize notify : " + processQueue.size() + " >= " + sendPaquetSize);
		//			}
		//		}
	}

	/** {@inheritDoc} */
	public void start() {
		locatorClient = Client.create();
		locatorClient.addFilter(new com.sun.jersey.api.client.filter.GZIPContentEncodingFilter());
		remoteWebResource = locatorClient.resource(serverUrl);

		manager = net.sf.ehcache.CacheManager.create();
		if (!manager.cacheExists(SPOOL_CONTEXT)) {
			final boolean overflowToDisk = true;
			final boolean eternal = false;
			final int timeToLiveSeconds = 8 * 60 * 60; //on accept 8h d'indisponibilit� max
			final int timeToIdleSeconds = timeToLiveSeconds;
			final int maxElementsInMemory = 1;//0 = illimit�, 1 car on souhaite le minimum d'empreinte m�moire
			final net.sf.ehcache.Cache cache = new net.sf.ehcache.Cache(SPOOL_CONTEXT, maxElementsInMemory, overflowToDisk, eternal, timeToLiveSeconds, timeToIdleSeconds);
			manager.addCache(cache);
		}

		processSenderThread = new SendProcessThread(this);
		processSenderThread.start();

		logger.info("Start Analytica RemoteNetPlugin : connect to " + serverUrl);
		//checkServerVersion();
	}

	/** {@inheritDoc} */
	public void stop() {
		processSenderThread.interrupt();
		try {
			processSenderThread.join(10000);//on attend 10s max
		} catch (final InterruptedException e) {
			//rien, si interrupt on continu l'arret
		}
		processSenderThread = null;
		flushProcessQueue();

		locatorClient = null;
		remoteWebResource = null;
		manager.shutdown();

		logger.info("Stop Analytica RemoteNetPlugin");
	}

	private static class SendProcessThread extends Thread {
		private final RemoteConnector remoteConnector;

		//private final Logger logger = Logger.getLogger(SendProcessThread.class);

		SendProcessThread(final RemoteConnector remoteConnector) {
			super("AnalyticaSendProcessThread");
			setDaemon(false); //ce n'est pas un d�mon car on veux envoyer les derniers process
			if (remoteConnector == null) {
				throw new NullPointerException("remoteConnector is required");
			}
			//-----------------------------------------------------------------
			this.remoteConnector = remoteConnector;
		}

		/** {@inheritDoc} */
		@Override
		public void run() {
			while (!isInterrupted()) {
				try {
					remoteConnector.waitToSendPacket();
				} catch (final InterruptedException e) {
					interrupt();//On remet le flag qui a �t� reset lors du throw InterruptedException (pour le test isInterrupted())
					//logger.trace("interrupt()");
					//on envoi avant l'arret du serveur
				}
				//On flush la queue sur :
				// - le timeout 
				// - un processQueue.notify (taille max de la queue atteinte)
				// - un interrupt (arret du serveur)
				remoteConnector.retrySendProcesses();
				remoteConnector.flushProcessQueue();
			}
		}
	}

	/**
	 * On attend la constitution d'un paquet.
	 * Rend la main apr�s : 
	 * - le timeout 
	 * - un processQueue.notify (taille max de la queue atteinte)
	 * - un interrupt (arret du serveur) 
	 * @throws InterruptedException Si interrupt
	 */
	void waitToSendPacket() throws InterruptedException {
		final long start = System.currentTimeMillis();
		while (processQueue.size() < sendPaquetSize // 
				&& System.currentTimeMillis() - start < sendPaquetFrequencySeconds * 1000) {
			Thread.sleep(sizeCheckFrequencyMs);
		}
		//		synchronized (processQueue) { //synchronized pour recevoir le notifiy
		//			logger.trace("processQueue.wait");
		//			processQueue.wait(sendPaquetFrequencySeconds * 1000);
		//			logger.trace("processQueue.wait continue");
		//		}
	}

	/**
	 * Effectue le flush de la queue des processes � envoyer.
	 */
	void flushProcessQueue() {
		final Collection<KProcess> processes = new ArrayList<KProcess>();
		KProcess head;
		do {
			head = processQueue.poll();
			if (head != null) {
				processes.add(head);
			}
			if (processes.size() >= sendPaquetSize * 2) { //si besoin on va jusqu'a un sur-booking x2 des paquets
				doSendProcesses(processes);
				processes.clear();
			}
		} while (head != null); //On depile tout : car lors de l'arret du serveur on aura pas d'autre flush
		doSendProcesses(processes);

		//On n'utilise pas le MediaType.APPLICATION_JSON, car jackson a besoin de modifications sur KProcess
		//final ClientResponse response = remoteWeResource.accept(MediaType.APPLICATION_JSON).put(ClientResponse.class, processes);
	}

	private void doSendProcesses(final Collection<KProcess> processes) {
		if (!processes.isEmpty()) {
			final String json = new Gson().toJson(processes);
			try {
				doSendJson(remoteWebResource, json);
				logger.info("Send " + processes.size() + " processes to " + serverUrl);
			} catch (final Exception e) {
				logSendError(false, e);
				doStoreJson(json);
			}
		}
	}

	/**
	 * Tente de renvoyer les paquets qui ont �chou�s.
	 */
	void retrySendProcesses() {
		try {
			final List<UUID> keys = manager.getCache(SPOOL_CONTEXT).getKeys();
			for (int i = 0; i < maxResendJson && i < keys.size(); i++) { //on limite a 5 car ce sont d�j� des paquets constitu�s 
				final UUID key = keys.get(i);
				doSendJson(remoteWebResource, (String) manager.getCache(SPOOL_CONTEXT).get(key).getValue());
				manager.getCache(SPOOL_CONTEXT).remove(key); //si l'envoi est pass�, on retire du cache
			}
		} catch (final Exception e) {
			//serveur indisponible ou en erreur
			logSendError(true, e);
		}
	}

	private void logSendError(final boolean isResend, final Exception e) {
		//serveur indisponible ou en erreur
		final String action = isResend ? "Resend" : "Send";
		if (logger.isDebugEnabled()) {
			logger.debug(action + " : Serveur Analytica indisponible : " + e.getMessage(), e);
		} else {
			final String message = action + " : Serveur Analytica indisponible : " + e.getMessage();
			//volontairement on ne passe pas l'exception pour ne pas saturer le log
			if (isResend) {
				logger.info(message);
			} else {
				logger.warn(message);
			}
		}
	}

	private void doStoreJson(final String json) {
		final Element element = new Element(UUID.randomUUID(), json);
		manager.getCache(SPOOL_CONTEXT).put(element);
	}

	private void doSendJson(final WebResource webResource, final String json) {
		final ClientResponse response = webResource.accept(MediaType.TEXT_PLAIN).post(ClientResponse.class, json);
		checkResponseStatus(response);
	}

	private String doGet(final WebResource webResource) {
		final ClientResponse response = webResource.get(ClientResponse.class);
		checkResponseStatus(response);
		return response.getEntity(String.class);
	}

	private void checkResponseStatus(final ClientResponse response) {
		final Status status = response.getClientResponseStatus();
		if (status.getFamily() == Family.SUCCESSFUL) {
			return;
		}
		throw new KRuntimeException("Une erreur est survenue : " + status.getStatusCode() + " " + status.getReasonPhrase());
	}

	private void checkServerVersion() {
		//On check la version
		final WebResource remoteVersionWebResource = locatorClient.resource(serverUrl + "/version");
		try {
			final String serverVersion = doGet(remoteVersionWebResource);
			if (!serverVersion.startsWith(VERSION_MAJOR)) {
				logger.warn("Cette version du client Analytica (" + VERSION + ") n''est pas compatible avec la version du serveur (" + serverVersion + ")");
			} else {
				logger.info("Connexion OK avec le serveur Analytica (" + serverUrl + ")");
			}
		} catch (final Exception e) {
			//serveur indisponible ou en erreur
			logger.warn("Serveur Analytica indisponible (" + serverUrl + ") : " + e.getMessage());
		}
	}
}
