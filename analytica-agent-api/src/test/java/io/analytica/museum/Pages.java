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
package io.analytica.museum;

import io.analytica.api.KProcess;
import io.analytica.api.KProcessBuilder;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

final class Pages {
	public final static PageBuilder HOME = new HomePage();
	public final static PageBuilder ARTIST_SEARCH = new SearchArtistPage();
	public final static PageBuilder OEUVRE_SEARCH = new SearchOeuvrePage();
	public final static PageBuilder ARTIST = new ArtistPage();
	public final static PageBuilder OEUVRE = new OeuvrePage();
	public final static PageBuilder EXPOSITION = new ExpositionPage();
	public final static PageBuilder IMAGE_ARTIST = new ImageArtistPage();
	public final static PageBuilder IMAGE_OEUVRE = new ImageOeuvrePage();

	private static final String ERROR_MEASURE = "error";
	private static final String PAGE_PROCESS = "page";
	private static final String SERVICE_PROCESS = "service";
	private static final String SQL_PROCESS = "sql";
	private static final String SEARCH_PROCESS = "search";

	private static class HomePage implements PageBuilder {
		@Override
		public KProcess createPage(final Date dateVisite) {
			final int[] randomDurations = StatsUtil.randoms(getCoef(dateVisite), 100, 5, 40, 5, 40, 5, 40, 5, 40);
			//@formatter:off
			return new KProcessBuilder(Museum.APP_NAME, PAGE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 0, 1, 2, 3, 4, 5, 6, 7, 8)).withCategory("home" )
					.withCategory("home")
					.withLocation("mexico")
					.setMeasure(ERROR_MEASURE, StatsUtil.randomValue(1, 0.01, 100, 0))// 1% d'erreur
					.beginSubProcess(SERVICE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 1, 2)).withCategory("CommunicationServices/loadNews" )
						.beginSubProcess(SQL_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 2)).withCategory("select * from news" )
					.endSubProcess()
					.endSubProcess()
					.beginSubProcess(SERVICE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 3, 4)).withCategory("ExpositionServices/loadPushExpositions" )
					.beginSubProcess(SQL_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 4)).withCategory("select * from expositions" ).endSubProcess()
					.endSubProcess()
					.beginSubProcess(SERVICE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 5, 6)).withCategory( "OeuvreServices/loadPushOeuvres" )
					.beginSubProcess(SQL_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 6)).withCategory( "select * from oeuvres" ).endSubProcess()
					.endSubProcess()
					.beginSubProcess(SERVICE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 7, 8)).withCategory( "OeuvreServices/loadFavoriesOeuvres" )
						.beginSubProcess(SQL_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 8)).withCategory("select * from oeuvres" ).endSubProcess()
					.endSubProcess()
					.build();
			//@formatter:on
		}
	}

	private static class SearchArtistPage implements PageBuilder {
		@Override
		public KProcess createPage(final Date dateVisite) {
			final int[] randomDurations = StatsUtil.randoms(getCoef(dateVisite), 100, 50, 150, 5, 10, 5, 10, 5, 10, 5, 10, 5, 10, 5, 10, 5, 10, 5, 10, 5, 10);
			//@formatter:off
			return new KProcessBuilder(Museum.APP_NAME, "search", dateVisite, StatsUtil.sum(randomDurations, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20)).withCategory("artists")//
					.withLocation("mexico")
					.setMeasure(ERROR_MEASURE, StatsUtil.randomValue(1, 0.01, 100, 0))// 1% d'erreur
					.beginSubProcess(SERVICE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 1, 2)).withCategory("ArtistServices/search" )
						.beginSubProcess(SEARCH_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 2)).withCategory( "find artists" ).endSubProcess()
					.endSubProcess()
					.beginSubProcess(PAGE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 3, 4)).withCategory( "images/artists/"+ String.valueOf(StatsUtil.random(100, 1)))
							.beginSubProcess(SQL_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 4)).withCategory( "select data from blob" ).endSubProcess()
					.endSubProcess()
					.beginSubProcess(PAGE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 5, 6)).withCategory("images/artists"+String.valueOf(StatsUtil.random(101, 1)))
						.beginSubProcess(SQL_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 6)).withCategory( "select data from blob" ).endSubProcess()
					.endSubProcess()
					.beginSubProcess(PAGE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 7, 8)).withCategory("images/artists"+ String.valueOf(StatsUtil.random(102, 1)))
						.beginSubProcess(SQL_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 8)).withCategory("select data from blob" ).endSubProcess()
					.endSubProcess()
					.beginSubProcess(PAGE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 9, 10)).withCategory( "images/artists"+String.valueOf(StatsUtil.random(103, 1)))
						.beginSubProcess(SQL_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 10)).withCategory( "select data from blob" ).endSubProcess()
					.endSubProcess()
					.beginSubProcess(PAGE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 11, 12)).withCategory( "images/artists"+String.valueOf(StatsUtil.random(104, 1)))
						.beginSubProcess(SQL_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 12)).withCategory( "select data from blob").endSubProcess()
					.endSubProcess()
					.beginSubProcess(PAGE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 13, 14)).withCategory("images/artists/"+ String.valueOf(StatsUtil.random(105, 1)))
						.beginSubProcess(SQL_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 14)).withCategory("select data from blob" ).endSubProcess()
					.endSubProcess()
					.beginSubProcess(PAGE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 15, 16)).withCategory("images/artists"+ String.valueOf(StatsUtil.random(106, 1)))
						.beginSubProcess(SQL_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 16)).withCategory( "select data from blob" ).endSubProcess()
					.endSubProcess()
					.beginSubProcess(PAGE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 17, 18)).withCategory("images/artists"+ String.valueOf(StatsUtil.random(107, 1)))
						.beginSubProcess(SQL_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 18)).withCategory("select data from blob" ).endSubProcess()
					.endSubProcess()
					.beginSubProcess(PAGE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 19, 20)).withCategory("images/artists"+String.valueOf(StatsUtil.random(108, 1)) )
						.beginSubProcess(SQL_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 20)).withCategory( "select data from blob").endSubProcess()
					.endSubProcess()
					.build();
			//@formatter:on			
		}
	}

	private static class SearchOeuvrePage implements PageBuilder {
		@Override
		public KProcess createPage(final Date dateVisite) {
			final int[] randomDurations = StatsUtil.randoms(getCoef(dateVisite), 100, 50, 250, 5, 10, 5, 10, 5, 10, 5, 10, 5, 10, 5, 10, 5, 10, 5, 10, 5, 10);

			return new KProcessBuilder(Museum.APP_NAME, "search", dateVisite, StatsUtil.sum(randomDurations, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20)).withCategory("oeuvres")
					.withLocation("mexico")
					.setMeasure(ERROR_MEASURE, StatsUtil.randomValue(1, 0.01, 100, 0))// 1% d'erreur
					.beginSubProcess(SERVICE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 1, 2)).withCategory("OeuvreServices/search")
					.beginSubProcess(SEARCH_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 2)).withCategory("find oeuvres").endSubProcess()
					.endSubProcess()
					.beginSubProcess(PAGE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 3, 4)).withCategory("images/oeuvres" + String.valueOf(StatsUtil.random(200, 1)))
					.beginSubProcess(SQL_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 4)).withCategory("select data from blob").endSubProcess()
					.endSubProcess()
					.beginSubProcess(PAGE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 5, 6)).withCategory("images/oeuvres" + String.valueOf(StatsUtil.random(201, 1)))
					.beginSubProcess(SQL_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 6)).withCategory("select data from blob").endSubProcess()
					.endSubProcess()
					.beginSubProcess(PAGE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 7, 8)).withCategory("images/oeuvres" + String.valueOf(StatsUtil.random(202, 1)))
					.beginSubProcess(SQL_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 8)).withCategory("select data from blob").endSubProcess()
					.endSubProcess()
					.beginSubProcess(PAGE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 9, 10)).withCategory("images/oeuvres" + String.valueOf(StatsUtil.random(203, 1)))
					.beginSubProcess(SQL_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 10)).withCategory("select data from blob").endSubProcess()
					.endSubProcess()
					.beginSubProcess(PAGE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 11, 12)).withCategory("images/oeuvres" + String.valueOf(StatsUtil.random(204, 1)))
					.beginSubProcess(SQL_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 12)).withCategory("select data from blob").endSubProcess()
					.endSubProcess()
					.beginSubProcess(PAGE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 13, 14)).withCategory("images/oeuvres" + String.valueOf(StatsUtil.random(205, 1)))
					.beginSubProcess(SQL_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 14)).withCategory("select data from blob").endSubProcess()
					.endSubProcess()
					.beginSubProcess(PAGE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 15, 16)).withCategory("images/oeuvres" + String.valueOf(StatsUtil.random(206, 1)))
					.beginSubProcess(SQL_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 16)).withCategory("select data from blob").endSubProcess()
					.endSubProcess()
					.beginSubProcess(PAGE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 17, 18)).withCategory("images/oeuvres" + String.valueOf(StatsUtil.random(207, 1)))
					.beginSubProcess(SQL_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 18)).withCategory("select data from blob").endSubProcess()
					.endSubProcess()
					.beginSubProcess(PAGE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 19, 20)).withCategory("images/oeuvres" + String.valueOf(StatsUtil.random(208, 1)))
					.beginSubProcess(SQL_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 20)).withCategory("select data from blob").endSubProcess()
					.endSubProcess()
					.build();
		}
	}

	private static class ImageOeuvrePage implements PageBuilder {
		@Override
		public KProcess createPage(final Date dateVisite) {
			final int[] randomDurations = StatsUtil.randoms(getCoef(dateVisite), 5, 15);

			return new KProcessBuilder(Museum.APP_NAME, PAGE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 0, 1)).withCategory("images/oeuvres" + String.valueOf(StatsUtil.random(200, 1)))
					.withLocation("mexico")
					.setMeasure(ERROR_MEASURE, StatsUtil.randomValue(1, 0.01, 100, 0))// 1% d'erreur
					.beginSubProcess(SQL_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 1)).withCategory("select data from blob").endSubProcess()
					.build();
		}
	}

	private static class ImageArtistPage implements PageBuilder {
		@Override
		public KProcess createPage(final Date dateVisite) {
			final int[] randomDurations = StatsUtil.randoms(getCoef(dateVisite), 5, 10);

			return new KProcessBuilder(Museum.APP_NAME, PAGE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 0, 1)).withCategory("images/artists" + String.valueOf(StatsUtil.random(100, 1)))
					.withLocation("mexico")
					.setMeasure(ERROR_MEASURE, StatsUtil.randomValue(1, 0.01, 100, 0))// 1% d'erreur
					.beginSubProcess(SQL_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 1)).withCategory("select data from blob").endSubProcess()
					.build();
		}
	}

	private static class ArtistPage implements PageBuilder {
		//On joue sur plusieurs listes de fa�on � ne pas avoir une �quir�partition des donn�es.
		private static final String[] artistsA = "vinci;monet;picasso;renoir;rubens".split(";");
		private static final String[] artistsB = "bazille;bonnard;munch;signac;hopper;c�zanne;bacon;johnes;rothko;warhol".split(";");

		@Override
		public KProcess createPage(final Date dateVisite) {
			final int[] randomDurations = StatsUtil.randoms(getCoef(dateVisite), 100, 5, 20, 5, 20);
			final String artist = getArtist();
			return new KProcessBuilder(Museum.APP_NAME, PAGE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 0, 1, 2, 3, 4)).withCategory("artists/" + artist)
					.withLocation("mexico")
					.setMeasure(ERROR_MEASURE, StatsUtil.randomValue(1, 0.01, 100, 0))// 1% d'erreur
					.beginSubProcess(SERVICE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 1, 2)).withCategory("ArtistServices/loadArtist")
					.beginSubProcess(SQL_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 2)).withCategory("select * from artists").endSubProcess()
					.endSubProcess()
					.beginSubProcess(SERVICE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 3, 4)).withCategory("OeuvreServices/loadOeuvreByArtId")
					.beginSubProcess(SQL_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 4)).withCategory("select * from oeuvres").endSubProcess()
					.endSubProcess().build();
		}

		private static String getArtist() {
			final String[] artists = Math.random() > 0.3 ? artistsA : artistsB;
			final int r = Double.valueOf(Math.random() * artists.length).intValue();
			return artists[r];
		}
	}

	private static class OeuvrePage implements PageBuilder {
		//On joue sur plusieurs listes de fa�on � ne pas avoir une �quir�partition des donn�es.
		private static final String[] oeuvresA = "Le Bapt�me du Christ;L'Annonciation;Ginevra de'Benci;La Madone � l'�illet;Madonna Benois;Saint J�r�me;L'Adoration des mages;La Vierge aux rochers;La Dame � l'hermine;Madonna Litta;Portrait de musicien;La Belle Ferronni�re;La C�ne;La Vierge aux rochers;Sala delle Asse;La Vierge, l'Enfant J�sus avec sainte Anne et saint Jean Baptiste;La Madone aux fuseaux;La Joconde�ou�Mona Lisa;Jeune fille d�coiff�e;La Vierge, l'Enfant J�sus et sainte Anne;Bacchus;Saint Jean Baptiste".split(";");
		private static final String[] oeuvresB = "Autoportrait 1901;La C�lestine;Les Demoiselles d'Avignon;Dora Maar au chat;Gar�on � la pipe;Guernica;Massacre en Cor�e;Les Noces de Pierrette;Maya � la poup�e;Nu au plateau de sculpteur;Le R�ve;Le Vieux Guitariste aveugle".split(";");
		private static final String[] oeuvresC = "Achille Emperaire;Nature morte � la bouilloire;La Pendule noire;Pastorale ou l'Idylle;La Maison du pendu;Autoportrait;Madame C�zanne dans un fauteuil rouge;Pont de Maincy;Cour de ferme � Auvers;Pommes et biscuits;Plateau de la montagne Sainte Victoire;L'Estaque, vue du golfe de Marseille;Vase de fleurs et pommes;Les Collines de Meyreuil;Gardanne le soir, Vue de la colline des fr�res;Gardanne, Vue de Saint Andr�;Les rideaux;Payannet et la Sainte-Victoire. Environs de Gardanne;L'aqueduc;Marronniers et ferme du Jas de Bouffon;Pont sur la Marne � Cr�teil;La table de cuisine (Nature morte au panier);Mardi-gras;Madame C�zanne sur une chaise jaune;Les Joueurs de cartes;Les baigneurs;Baigneurs;Femme � la cafeti�re;Le Gar�on au gilet rouge;Les Grandes Baigneuses;Oignons et bouteille;Joachim Gasquet;Paysan � la blouse bleue;Pommes et oranges;Nature morte aux oignons;Fumeur accoud�;Oignons et bouteille;Le fumeur;Le rocher rouge;Le ch�teau noir;Montagne Sainte Victoire;Rocher de Bibemus;Vieille Femme au rosaire;La Montagne Sainte-Victoire et le Ch�teau Noir"
				.split(";");

		@Override
		public KProcess createPage(final Date dateVisite) {
			final int[] randomDurations = StatsUtil.randoms(getCoef(dateVisite), 100, 5, 50, 5, 20);
			final String oeuvre = getOeuvre();
			return new KProcessBuilder(Museum.APP_NAME, PAGE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 0, 1, 2, 3, 4)).withCategory("oeuvres/" + oeuvre)
					.withLocation("mexico")
					.setMeasure(ERROR_MEASURE, StatsUtil.randomValue(1, 0.01, 100, 0))// 1% d'erreur
					.beginSubProcess(SERVICE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 1, 2)).withCategory("OeuvreServices/loadOeuvre")
					.beginSubProcess(SQL_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 2)).withCategory("select * from oeuvres").endSubProcess()
					.endSubProcess()
					.beginSubProcess(SERVICE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 3, 4)).withCategory("ExpositionServices/loadExpositionByOeuId")
					.beginSubProcess(SQL_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 4)).withCategory("select * from expositions").endSubProcess()
					.endSubProcess().build();
		}

		private static String getOeuvre() {
			final String[] oeuvres = Math.random() < 0.5 ? oeuvresA : Math.random() < 0.7 ? oeuvresB : oeuvresC;
			final int r = Double.valueOf(Math.random() * oeuvres.length).intValue();
			return oeuvres[r];
		}
	}

	private static class ExpositionPage implements PageBuilder {
		private static final String[] museums = "Mus�e du Louvre,Paris;Mus�e d'Orsay,Paris;The Metropolitan Museum of Art,New York;Pushkin Museum,Moscow;Courtauld Institute Galleries,London".split(";");
		private static final String[] annees = "1954;1966;1970;1982;1991;2002;2005;2007;2008;2009;2010;2011;2011;2012;2012;2012;2013;2013;2013;2013".split(";");

		@Override
		public KProcess createPage(final Date dateVisite) {
			final int[] randomDurations = StatsUtil.randoms(getCoef(dateVisite), 100, 5, 20, 5, 50, 5, 40);
			final String[] expositionInfos = getExposition().split(",");// [Mus�e,Ville,Ann�e]

			return new KProcessBuilder(Museum.APP_NAME, PAGE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 0, 1, 2, 3, 4, 5, 6)).withCategory("exposition/" + expositionInfos[0] + "/" + expositionInfos[1] + "/" + expositionInfos[2])
					.withLocation("mexico")
					.setMeasure(ERROR_MEASURE, StatsUtil.randomValue(1, 0.01, 100, 0))// 1% d'erreur
					.beginSubProcess(SERVICE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 1, 2)).withCategory("ExpositionServices/loadExposition")
					.beginSubProcess(SQL_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 2)).withCategory("select * from expositions").endSubProcess()
					.endSubProcess()
					.beginSubProcess(SERVICE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 3, 4)).withCategory("OeuvreServices/loadOeuvreByExpId")
					.beginSubProcess(SQL_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 4)).withCategory("select * from oeuvres").endSubProcess()
					.endSubProcess()
					.beginSubProcess(SERVICE_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 5, 6)).withCategory("ArtistServices/loadArtistByExpId")
					.beginSubProcess(SQL_PROCESS, dateVisite, StatsUtil.sum(randomDurations, 6)).withCategory("select from artists").endSubProcess()
					.endSubProcess()
					.build();
		}

		private static String getExposition() {
			final int m = Double.valueOf(Math.random() * museums.length).intValue();
			final String museum = museums[m];
			final int a = Double.valueOf(Math.random() * annees.length).intValue();
			final String annee = annees[a];
			return museum + "," + annee;
		}
	}

	/**
	 * Fournit le coef de charge 
	 * @param date Date
	 * @return charge coef at this hour
	 */
	private static double getCoef(final Date date) {
		final Calendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		final int h = calendar.get(Calendar.HOUR_OF_DAY);
		if (h <= 5 || h >= 21) {
			return 0.6;
		}
		return 1 + 0.5 * (0.35 * Math.sin((h - 7) * Math.PI / 4d) + 0.15 * Math.sin((h - 7 - 2 / 3d) * Math.PI / 8d)); //varie de 0.6 � 1.5 (entre 6h et 20h)
	}
}
