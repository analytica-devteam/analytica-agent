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
package io.analytica.spies.impl.logs;

import io.analytica.KProcessJsonCodec;
import io.analytica.agent.AgentManager;
import io.analytica.api.KProcess;
import io.analytica.api.KProcessBuilder;
import io.analytica.spies.impl.JsonConfReader;
import io.vertigo.core.resource.ResourceManager;
import io.vertigo.lang.Activeable;
import io.vertigo.lang.Assertion;
import io.vertigo.lang.Option;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;

/**
 * Monitoring de facade par Proxy automatique sur les interfaces.
 * @author npiedeloup
 * @version $Id: CounterProxy.java,v 1.5 2010/11/23 09:49:33 npiedeloup Exp $
 */
public final class LogSpyReader implements Activeable {

	private static final String ME_ERROR_PCT = "ERROR_PCT";
	private static final long ONE_DAY_MILLIS = 24 * 60 * 60 * 1000;
	private final Logger logger = Logger.getLogger(getClass());

	private final AgentManager agentManager;
	private final String systemName;
	private final String[] systemLocation;

	private final URL logFileUrl;
	private final List<String> dateFormats;
	private final List<LogPattern> patterns;
	private final Map<LogPattern, Integer> patternStats;

	private final Map<String, List<LogInfo>> logInfoMap = new HashMap<>();
	private Date lastDateRead = new Date(0);

	private static final Comparator<? super LogInfo> LOG_INFO_COMPARATOR = new Comparator<LogInfo>() {
		@Override
		public int compare(final LogInfo o1, final LogInfo o2) {
			if (o1 == null) {
				return o2 != null ? -1 : 0;
			} else if (o2 == null) {
				return 1;
			}
			final int compareDate = o1.getStartDateEvent().compareTo(o2.getStartDateEvent()); //le plus petit en premier
			final int compareTime = Long.valueOf(o1.getTime()).compareTo(o2.getTime()) * -1; //le plus grand en premier
			//sinon requete/job en premier, Service puis Tache => ordre alphabetique du type
			final int compareType = o1.getType().compareTo(o2.getType()); //le plus petit en premier

			//			if (o1.getLogPattern().isProcessRoot() && !o2.getLogPattern().isProcessRoot()) {
			//				return -1;
			//			} else if (o2.getLogPattern().isProcessRoot() && !o1.getLogPattern().isProcessRoot()) {
			//				return 1;
			//			}
			//Systemlogger.infoprintln(o1.getStartDateEvent().getTime() + " compareDate " + o2.getStartDateEvent().getTime() + " = " + compareDate);
			//logger.info(o1.getTime() + " compareTime " + o2.getTime() + " = " + compareTime);
			return compareDate != 0 ? compareDate : compareTime != 0 ? compareTime : compareType;
		}
	};

	/**
	 * Constructeur.
	 * @param agentManager  Agent de r�colte de process
	 * @param resourceManager Resource manager
	 * @param logFileUrl Url du fichier de log � parser
	 * @param confFileUrl Url du fichier de configuration
	 */
	@Inject
	public LogSpyReader(final AgentManager agentManager, final ResourceManager resourceManager, @Named("logFileUrl") final String logFileUrl, @Named("confFileUrl") final String confFileUrl) {
		Assertion.checkNotNull(agentManager);
		Assertion.checkNotNull(resourceManager);
		Assertion.checkArgNotEmpty(logFileUrl);
		//---------------------------------------------------------------------
		this.agentManager = agentManager;
		this.logFileUrl = resourceManager.resolve(logFileUrl);
		final URL confFile = resourceManager.resolve(confFileUrl);
		final LogSpyConf conf = JsonConfReader.loadJsonConf(confFile, LogSpyConf.class);

		systemName = conf.getSystemName();
		systemLocation = conf.getSystemLocation();
		dateFormats = conf.getDateFormats();
		patterns = conf.getLogPatterns();
		patternStats = new HashMap<>();
		for (final LogPattern pattern : patterns) {
			patternStats.put(pattern, 0);
		}
	}

	private List<LogInfo> getLogInfos(final String threadName) {
		List<LogInfo> logInfos = logInfoMap.get(threadName);
		if (logInfos == null) {
			logInfos = new ArrayList<>();
			logInfoMap.put(threadName, logInfos);
		}
		return logInfos;
	}

	private void appendLogInfo(final LogInfo logInfo) {
		final List<LogInfo> logInfos = getLogInfos(logInfo.getThreadName());
		if (!logInfo.getLogPattern().isStartLog()) {
			for (final LogInfo oldLogInfo : logInfos) {
				if (oldLogInfo.getLogPattern().isStartLog()) {
					if (oldLogInfo.getType().equals(logInfo.getType()) && oldLogInfo.getCategoryTerms().equals(logInfo.getCategoryTerms())) {
						logInfo.linkStartLogInfo(oldLogInfo);
						logInfos.remove(oldLogInfo);
						break;
					}
				}
			}
		}
		logInfos.add(logInfo);
	}

	private KProcess extractProcess(final String threadName) {
		final List<LogInfo> logInfos = getLogInfos(threadName);
		//1 - on tri par date de d�but
		//logger.info("extract >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		//for (final LogInfo logInfo : logInfos) {
		//	logger.info("    " + logInfo.toString());
		//}
		//logger.info("extract <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
		Collections.sort(logInfos, LOG_INFO_COMPARATOR);

		logger.info("extract sort >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		for (final LogInfo logInfo : logInfos) {
			logger.info("    " + logInfo.toString());
		}
		logger.info("extract sort <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");

		final Stack<KProcessBuilder> stackProcessBuilder = new Stack<>();
		final Stack<LogInfo> stackLogInfo = new Stack<>();
		KProcessBuilder processBuilder;
		//2 - on d�pile les lignes de log
		for (final LogInfo logInfo : logInfos) {
			processBuilder = new KProcessBuilder(systemName, logInfo.getType(), logInfo.getStartDateEvent(), logInfo.getTime())
					.withLocation(systemLocation).withCategory(logInfo.getCategoryTerms());
			processBuilder.setMeasure(ME_ERROR_PCT, logInfo.getLogPattern().isError() ? 100 : 0);
			if (stackLogInfo.isEmpty()) {
				//2a - la premiere ligne cr�e la racine
				stackLogInfo.push(logInfo);
				stackProcessBuilder.push(processBuilder);
			} else {
				//2b - Ajoute les suivantes dans la pile
				push(logInfo, processBuilder, stackLogInfo, stackProcessBuilder);
			}
		}
		//3 - A la fin on depile tout
		KProcess process;
		do {
			final KProcessBuilder processBuilderPrevious = stackProcessBuilder.pop();
			stackLogInfo.pop();
			process = processBuilderPrevious.build();
			//3a - Le process est ajout� au parent s'il existe
			if (!stackLogInfo.isEmpty()) {
				final KProcessBuilder processBuilderParent = stackProcessBuilder.peek();
				processBuilderParent.addSubProcess(process);
			}
		} while (!stackLogInfo.isEmpty());

		//4 - On purge les logs lus 
		getLogInfos(threadName).clear();

		//5 - On retourne le process r�sultat
		logger.info("process<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
		logger.info("process:" + fullToString(process, new StringBuilder(), "").toString());

		return process;
	}

	private StringBuilder fullToString(final KProcess process, final StringBuilder sb, final String linePrefix) {
		final SimpleDateFormat sdfHour = new SimpleDateFormat("HH:mm:ss.SSS ");

		sb.append(linePrefix);
		sb.append("{").append("").append(process.getType()).append(":").append(Arrays.asList(process.getCategoryTerms())).append("; startDate:").append(sdfHour.format(process.getStartDate())).append("; endDate:").append(sdfHour.format(new Date(process.getStartDate().getTime() + (long) process.getDuration()))).append("; duration:").append(process.getDuration());
		if (!process.getSubProcesses().isEmpty()) {
			sb.append("\n").append(linePrefix).append("subprocess:{");
			for (final KProcess subProcess : process.getSubProcesses()) {
				sb.append("\n");
				fullToString(subProcess, sb, linePrefix + "  ");
				sb.append(";");
			}

			sb.append("\n").append(linePrefix).append("}");
		}
		sb.append("}");
		return sb;
	}

	private void push(final LogInfo logInfo, final KProcessBuilder processBuilder, final Stack<LogInfo> stackLogInfo, final Stack<KProcessBuilder> stackProcessBuilder) {
		//final SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");

		final LogInfo logInfoPrevious = stackLogInfo.peek();
		final boolean sameType = logInfo.getType().equals(logInfoPrevious.getType());
		//final Date dateEvent = logInfo.getDateEvent();
		final boolean dateIncluded;
		//final long timeD1 = logInfoPrevious.getStartDateEvent().getTime();
		final long timeF1 = logInfoPrevious.getDateEvent().getTime();
		final long timeD2 = logInfo.getStartDateEvent().getTime();
		final long timeF2 = logInfo.getDateEvent().getTime();
		final long timeInclude = Math.min(timeF1, timeF2) - timeD2;
		final long timeExclude = timeF2 - timeF1;
		dateIncluded = !(timeInclude == 0 && sameType) && timeInclude >= timeExclude;
		if (dateIncluded) {
			logger.info(logInfo.getCategoryTerms() + " in " + logInfoPrevious.getCategoryTerms() + " " + dateIncluded + "=" + timeInclude + ">=" + timeExclude);
			//
			//		if (sameType) {
			//			dateIncluded = logInfo.getStartDateEvent().before(logInfoPrevious.getDateEvent()) && logInfo.getDateEvent().after(logInfoPrevious.getStartDateEvent());
			//			System.out.print(dateIncluded + "(sametype) = ");
			//			System.out.print(sdfDate.format(logInfo.getStartDateEvent()) + "<" + sdfDate.format(logInfoPrevious.getDateEvent()) + " && ");
			//			logger.info(sdfDate.format(logInfo.getDateEvent()) + ">" + sdfDate.format(logInfoPrevious.getStartDateEvent()));
			//
			//		} else {
			//			dateIncluded = !logInfo.getStartDateEvent().after(logInfoPrevious.getDateEvent()) && !logInfo.getDateEvent().before(logInfoPrevious.getStartDateEvent());
			//			System.out.print(dateIncluded + " = ");
			//			System.out.print(sdfDate.format(logInfo.getStartDateEvent()) + "<=" + sdfDate.format(logInfoPrevious.getDateEvent()) + " && ");
			//			logger.info(sdfDate.format(logInfo.getDateEvent()) + ">=" + sdfDate.format(logInfoPrevious.getStartDateEvent()));
			//		}
		}

		if (dateIncluded) {
			//Si l'event est inclus dans les dates du pr�c�dent on l'ajout dedans
			stackLogInfo.push(logInfo);
			stackProcessBuilder.push(processBuilder);
		} else {
			//Si l'event n'est pas inclus dans les dates du pr�c�dent, 
			//on d�pile le pr�c�dent qu'on ajoute au parent, 
			//puis on empile le nouveau suivant la m�me regle
			final KProcessBuilder processBuilderPrevious = stackProcessBuilder.pop();
			stackLogInfo.pop();

			Assertion.checkState(!stackProcessBuilder.isEmpty(), "La stackProcessBuilder est vide : \n\tcurrent:{0}\n\tprevious:{1}", processBuilder.build(), processBuilderPrevious.build());
			if (!stackProcessBuilder.isEmpty()) {
				final KProcessBuilder processBuilderParent = stackProcessBuilder.peek();
				processBuilderParent.addSubProcess(processBuilderPrevious.build());
				push(logInfo, processBuilder, stackLogInfo, stackProcessBuilder);
			} else {
				logger.info("La stackProcessBuilder est vide : \n\tcurrent:" + processBuilder.build() + "\n\tprevious:" + processBuilderPrevious.build());
			}
		}
	}

	/** {@inheritDoc} */
	@Override
	public void start() {
		try {
			//on lit le fichier
			final InputStream in = logFileUrl.openStream();
			try {
				final Reader isr = new InputStreamReader(in);
				try {
					final BufferedReader br = new BufferedReader(isr);
					try {
						readLogFile(br);
					} finally {
						br.close();
					}
				} finally {
					isr.close();
				}
			} finally {
				in.close();
			}
		} catch (final IOException e) {
			throw new RuntimeException("Erreur de lecture du log", e);
		}
	}

	private void readLogFile(final BufferedReader br) throws IOException {
		long lineCount = 0;
		long parsedLineCount = 0;
		String currentLine;
		Option<LogInfo> logInfoOption;
		while ((currentLine = br.readLine()) != null) {
			lineCount++;
			logInfoOption = parseLine(currentLine);
			if (logInfoOption.isDefined()) {
				final LogInfo logInfo = logInfoOption.get();
				parsedLineCount++;
				patternHit(logInfo.getLogPattern());
				if (logInfo.getLogPattern().isProcessesJson()) {
					final List<KProcess> processes = KProcessJsonCodec.fromJson(logInfo.getJson());
					for (final KProcess process : processes) {
						agentManager.add(process);
					}
				} else {
					appendLogInfo(logInfo);
					if (logInfo.getLogPattern().isProcessRoot()) {
						agentManager.add(extractProcess(logInfo.getThreadName()));
					} else if (logInfo.getLogPattern().isCleanStack()) {
						logInfoMap.clear();
					}
				}
			}
			if (lineCount % 250 == 0) {
				logPatternMatchingSummary(lineCount, parsedLineCount);
			}
		}
		logPatternMatchingSummary(lineCount, parsedLineCount);
	}

	private void logPatternMatchingSummary(final long lineCount, final long parsedLineCount) {
		final StringBuilder sb = new StringBuilder();
		sb.append("read ").append(lineCount).append(" lines, parsed: ").append(parsedLineCount).append(" (").append(parsedLineCount * 100 / lineCount).append("%), detail:");
		String sep = "";
		sb.append("{");
		for (final Map.Entry<LogPattern, Integer> entry : patternStats.entrySet()) {
			sb.append(sep);
			sb.append(entry.getKey().getCode());
			sb.append("=");
			sb.append(entry.getValue());
			sep = ", ";
		}
		sb.append("}");

		logger.info(sb.toString());
	}

	private void patternHit(final LogPattern logPattern) {
		patternStats.put(logPattern, patternStats.get(logPattern) + 1);
	}

	private Option<LogInfo> parseLine(final String currentLine) {
		//(date, nom thread, type, sous type, si erreur, dur�e, si log de fin
		for (final LogPattern logPattern : patterns) {
			final Matcher startMatch = logPattern.getPattern().matcher(currentLine);
			if (startMatch.find()) {
				if (logPattern.isProcessesJson()) {
					final String json = startMatch.group(logPattern.getIndexProcessesJson());
					final String threadName = logPattern.getIndexThreadName() > 0 ? startMatch.group(logPattern.getIndexThreadName()) : "none";
					return Option.some(new LogInfo(threadName, json, logPattern));
				}
				final String date = startMatch.group(logPattern.getIndexDate());
				final String threadName = logPattern.getIndexThreadName() > 0 ? startMatch.group(logPattern.getIndexThreadName()) : "none";
				final String type = logPattern.getIndexType() > 0 ? startMatch.group(logPattern.getIndexType()) : logPattern.getCode();
				// TODO reimplement for an array of categories
				final String[] categoryTerms = { logPattern.getIndexCategoryTerms() > 0 ? startMatch.group(logPattern.getIndexCategoryTerms()) : "" };
				final String time = logPattern.getIndexTime() > 0 ? startMatch.group(logPattern.getIndexTime()) : null;
				return Option.some(new LogInfo(readDate(date), threadName, type, categoryTerms, readTime(time), logPattern));
			}
		}
		return Option.none();
	}

	private long readTime(final String time) {
		return time != null ? Long.valueOf(time) : -1;
	}

	private Date readDate(final String date) {
		Date dateTime = null;
		for (final String dateFormat : dateFormats) {
			final SimpleDateFormat sdf = new SimpleDateFormat(dateFormat, Locale.ENGLISH);
			try {
				dateTime = sdf.parse(date);
				if (dateTime.getTime() <= ONE_DAY_MILLIS) {
					dateTime = adjustDate(dateTime, lastDateRead);
				}
				break;
			} catch (final ParseException e) {
				//rien on tente les autres formats
			}
		}
		if (dateTime == null) {
			throw new RuntimeException("Date non reconnue : " + date);
		}
		lastDateRead = dateTime;
		return dateTime;
	}

	private static Date adjustDate(final Date dateToAdjust, final Date lastDateRead) {
		//		final long dateToAdjustTime = dateToAdjust.getTime();
		//		Assertion.invariant(dateToAdjustTime < ONE_DAY_MILLIS, "La dateTime � ajuster contenait d�j� la date");
		//		final long lastDateReadTime = lastDateRead.getTime();
		//		final long dateAdjustedTime = dateToAdjustTime + lastDateReadTime / ONE_DAY_MILLIS * ONE_DAY_MILLIS;
		//		if (dateAdjustedTime >= lastDateReadTime - ONE_DAY_MILLIS / 24) { //au moins 1h d'�cart (ce point est util pour les changements de jour donc on passe de 22h � 7h par exemple)
		//			return new Date(dateAdjustedTime);
		//		} else {
		//			return new Date(dateAdjustedTime + ONE_DAY_MILLIS);
		//		}
		final Calendar lastDateReadCal = new GregorianCalendar();
		final Calendar dateToAdjustCal = new GregorianCalendar();
		lastDateReadCal.setTime(lastDateRead);
		dateToAdjustCal.setTime(dateToAdjust);
		dateToAdjustCal.set(Calendar.YEAR, lastDateReadCal.get(Calendar.YEAR));
		dateToAdjustCal.set(Calendar.MONTH, lastDateReadCal.get(Calendar.MONTH));
		dateToAdjustCal.set(Calendar.DAY_OF_MONTH, lastDateReadCal.get(Calendar.DAY_OF_MONTH));
		//		lastDateReadCal.set(Calendar.HOUR_OF_DAY, dateToAdjustCal.get(Calendar.HOUR_OF_DAY));
		//		lastDateReadCal.set(Calendar.MINUTE, dateToAdjustCal.get(Calendar.MINUTE));
		//		lastDateReadCal.set(Calendar.SECOND, dateToAdjustCal.get(Calendar.SECOND));
		//		lastDateReadCal.set(Calendar.MILLISECOND, dateToAdjustCal.get(Calendar.MILLISECOND));
		if (dateToAdjustCal.getTimeInMillis() >= lastDateReadCal.getTimeInMillis() - ONE_DAY_MILLIS / 24) { //au moins 1h d'�cart (ce point est util pour les changements de jour donc on passe de 22h � 7h par exemple)
			return dateToAdjustCal.getTime();
		}
		dateToAdjustCal.add(Calendar.DAY_OF_MONTH, 1);
		return dateToAdjustCal.getTime();
	}

	/** {@inheritDoc} */
	@Override
	public void stop() {
		//rien
	}
}
