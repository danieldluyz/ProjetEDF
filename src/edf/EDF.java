package edf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.limits.FailCounter;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.tools.ArrayUtils;

import static org.chocosolver.solver.search.strategy.Search.activityBasedSearch;

public class EDF {
	
	// CONSTANTES :
	
	/** La constante qui fait référence à l'indisponibilité d'une équipe/formateur/trace pendant une trace */
	private static final int NO_DISPONIBLE = -1;
	
	/** La constante qui fait référence à une trace disponible qui n'a pas de cours */
	private static final int PAS_DE_COURS = 0;
	
	// DONNÉES :
	
	/** La date de début du planning en format dd/mm/yyyy */
	private static final String START_DATE = "04/09/2017";
	
	/** Le nombre d'équipes */
	private static final int NB_EQUIPES = 14;
	
	/** Le nombre de formateurs */
	private static final int NB_FORMATEURS = 40;
	
	/** Le nombre de formations données par le centre */
	private static final int NB_FORMATIONS = 7;
	
	/** Le numéro de salles */
	private static final int NB_SALLES = 17;
	
	/** Le nombre de traces disponibles par jour */
	private static final int NB_TRACES_JOUR = 5;
	
	/** Le nombre de jours à planifier */
	private static final int NB_JOURS = 147;
	
	/** Le nombre de jours à planifier */
	private static final int NB_SEMAINES = Math.floorDiv(NB_JOURS, 7);
	
	/** Le nombre de jours maximum qu'un formateur peut travailler par an */
	private static final int NB_MAX_TRAVAIL_FORMATEUR = 100;
	
	/** La différence max. de journées travaillées entre le formateur qui travaille le plus et le moins */
	private static final int NB_MAX_DIF_ENTRE_FORMS = 3;
	
	/** 
	 * Cette matrice comporte la liste de formations
	 * La première colonne est l'id de la formation (un numéro)
	 * La deuxième colonne est la durée en traces de la formation
	 * La troisième colonne est le nombre max. de traces par jour de cette formation
	**/
	private double[][] formations;
	
	/** 
	 * Matrice de taille NB_EQUIPES x NB_FORMATIONS dont la valeur (i,j) 
	 * indique le nombre de fois que l’équipe i a besoin de suivre  la formation j.
	 */
	private double[][] besoinsEquipe;
	
	/** Les besoins de formations par equipes en terme de traces. 
	 * Les lignes sont les equipes, les colonnes les formations et 
	 * la valeur de la case le volume de traces dont l'équipe i a besoin pour la formation j 
	**/
	private double[][] formationsParEquipe;
	
	// VARIABLES DE DÉCISION :
	
	/** Les variables du planning des équipe */
	private IntVar[][] equipes;
	
	/** Les variables du planning des formatteurs */
	private IntVar[][] formateurs;
	
	/** Les variables du planning des salles */
	private IntVar[][] salles;
	
	private ArrayList<BoolVar[][]> formationsSemaines;
	
	private IntVar minJourneesTravaillees;
	
	private IntVar maxJourneesTravaillees;
	
	private ArrayList<BoolVar[]> joursFormateurs;
	
	private IntVar[] journeesTravailleesFormateurs;
	
	//private ArrayList<IntVar[][]> formationsTraces;
	
	// VARIABLES CHOCO
	
	/** Le model Choco */
	private Model model;
	
	/** Le solver Choco */
	private Solver solver;
	
	public EDF() throws Exception {
		model = new Model();
		solver = model.getSolver();
		
		//Traitement des dates
		Calendar c = Calendar.getInstance();
		SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
		Date date = formatter.parse(START_DATE);
		
		//Initialization des données
		int tracesTot = NB_TRACES_JOUR * NB_JOURS;
		
		equipes = new IntVar[NB_EQUIPES][tracesTot];
		formateurs = new IntVar[NB_FORMATEURS][tracesTot];
		salles = new IntVar[NB_SALLES][tracesTot];
		formationsSemaines = new ArrayList<BoolVar[][]>();
		//formationsTraces = new ArrayList<IntVar[][]>();
		
		for (int i = 0; i < NB_EQUIPES; i++) {
			BoolVar[][] formationsSemainesEquipe = new BoolVar[NB_FORMATIONS][NB_SEMAINES];
			formationsSemaines.add(formationsSemainesEquipe);
		}
		
		/*
		for (int i = 0; i < 1; i++) {
			IntVar[][] formationsTraceEquipe = new IntVar[NB_FORMATIONS][NB_TRACES_JOUR];
			formationsTraces.add(formationsTraceEquipe);
		}
		*/
		
		for (int i = 0; i < equipes.length; i++) {
			for (int j = 0; j < tracesTot; j++) {
				c.setTime(date);
				c.add(Calendar.DATE, j/NB_TRACES_JOUR);
				String dateString = formatter.format(c.getTime());
				int trace = (j  % NB_TRACES_JOUR) + 1;
				equipes[i][j] = model.intVar("EQ"+i+" "+dateString+" "+"T"+trace, NO_DISPONIBLE, NB_FORMATIONS);
			}
		}
		
		for (int i = 0; i < formateurs.length; i++) {
			for (int j = 0; j < tracesTot; j++) {
				c.setTime(date);
				c.add(Calendar.DATE, j/NB_TRACES_JOUR);
				String dateString = formatter.format(c.getTime());
				int trace = (j  % NB_TRACES_JOUR) + 1;
				//Si jamais on veut differencier les formateurs (c.a.d. qu'ils font des formations differentes), on change les valeurs du domaine et c'est tout
				formateurs[i][j] = model.intVar("FORM"+i+" "+dateString+" "+"T"+trace, NO_DISPONIBLE, NB_FORMATIONS);
			}
		}
		
		formations = new double[NB_FORMATIONS][3];
		for (int i = 0; i < NB_FORMATIONS; i++) {
			formations[i][0] = i+1;
		}
		
		formationsParEquipe = new double[NB_EQUIPES][NB_FORMATIONS];
		besoinsEquipe = new double[NB_EQUIPES][NB_FORMATIONS];
		
		lireDisponibilitesEquipes();
		lireBesoinsEquipes();
		lireContraintesSalles();
		lireDsiponibilitesFormateurs();
		
		contraintes();
	}
	
	/** 
	 *  Contraintes
	**/
	
	public void contraintes() {
		contrainteLiaisonEquipeFormSalle();
		contrainteRespectBesoinsEquipes();
		maxTraceParJourDuneFormation();
		contraintesRHFormateurs();
		contrainteFormationsContigues();
	}
	
	public void contrainteLiaisonEquipeFormSalle() {
		// Contrainte # 1 :
		// Contrainte pour assurer que quand il y a une formation il y a bien une
		// equipe, une salle et un formatteur
		for (int i = 0; i < equipes[0].length; i++) {
			for (int j = 0; j < formations.length; j++) {
				int max = (int) Math.ceil(formations[j][2]);
				IntVar count = model.intVar("count_eq_for_"+i+"_"+j,0, max);
				
				IntVar[] columnEquipe = getColumn(equipes, i);
				IntVar[] columnFormateur = getColumn(formateurs, i);
				IntVar[] columnSalle = getColumn(salles, i);
				
				// On peut aussi faire des objets qu lieu des matrices pour les formations
				model.count((int) formations[j][0], columnEquipe, count).post();
				model.count((int) formations[j][0], columnFormateur, count).post();
				model.count((int) formations[j][0], columnSalle, count).post();
			}
		}
	}
	
	public void contrainteRespectBesoinsEquipes() {
		// Contrainte # 2 :
		// Contrainte pour assurer que toutes les equipes suivent toutes les formations le bon nombre de fois
		for (int i = 0; i < equipes.length; i++) {
			for (int j = 0; j < formations.length; j++) {
				IntVar cFileIJ=model.intVar("cFile_equipe: "+i+"- formation:"+j, (int) formationsParEquipe[i][j]);
				model.count((int) formations[j][0], equipes[i], cFileIJ).post();
			}
		}	
	}
	
	public void maxTraceParJourDuneFormation() {
		// Contrainte # 3 :
		// Contrainte pour assurer que le nombre maximum des traces par jour de chaque formation soit respecté
		int[] valeurs = new int[NB_FORMATIONS+2];
		for (int l = PAS_DE_COURS; l < NB_FORMATIONS+2; l++) {
			valeurs[l] = l-1;
		}
		
		for (int i = 0; i < equipes.length; i++) {
			for (int j = 0; j < NB_JOURS; j++) {
				IntVar[] maxPerFormation = new IntVar[NB_FORMATIONS+2];
				maxPerFormation[0] = model.intVar("Domaine no dispo :", 0, 5);
				maxPerFormation[1] = model.intVar("Domaine pas de cours :", 0, 5);
				
				for (int l = 2; l < NB_FORMATIONS + 2; l++) {
					maxPerFormation[l] = model.intVar("Domaine formation :"+(l-2), 0, (int)formations[l-2][2]);
				}
				
				model.globalCardinality(getTracesJour(equipes[i], j), valeurs, maxPerFormation, true).post();
			}
		}
	}
	
	public void contraintesRHFormateurs() {
		//Contrainte # 4:
		//Contrainte pour assurer la limite de 100 journees travaillees par formateur
		
		joursFormateurs = new ArrayList<BoolVar[]>();
		journeesTravailleesFormateurs = new IntVar[NB_FORMATEURS];
		for (int i = 0; i < journeesTravailleesFormateurs.length; i++) {
			journeesTravailleesFormateurs[i] = model.intVar("Journées travaillées par la formateur "+i+1, 0, NB_MAX_TRAVAIL_FORMATEUR);
		}
		
		minJourneesTravaillees = model.intVar("Min de journees travaillees par un formateur", 0, NB_MAX_TRAVAIL_FORMATEUR);
		maxJourneesTravaillees = model.intVar("Min de journees travaillees par un formateur", 0, NB_MAX_TRAVAIL_FORMATEUR);
		
		for(int i=0; i < formateurs.length; i++) {
			BoolVar[] joursFormateur = model.boolVarArray(NB_JOURS);
			joursFormateurs.add(joursFormateur);
			for(int j=0; j < NB_JOURS; j++) {
				IntVar[] tracesFormateur = getTracesJour(formateurs[i], j);
				IntVar sum = model.intVar(-5, NB_FORMATIONS * NB_TRACES_JOUR);
				model.sum(tracesFormateur, "=", sum).post();
				
				model.arithm(sum, ">", 0).reifyWith(joursFormateur[j]);
			}
			model.sum(joursFormateur,"<=", NB_MAX_TRAVAIL_FORMATEUR).post();
			model.sum(joursFormateur, "=", journeesTravailleesFormateurs[i]).post();
		}
		model.min(minJourneesTravaillees, journeesTravailleesFormateurs).post();
		model.max(maxJourneesTravaillees, journeesTravailleesFormateurs).post();
		
		//Contrainte # 5:
		//Contrainte pour assurer que la charge est assez repartie entre les formateurs
		model.distance(minJourneesTravaillees, maxJourneesTravaillees, "<", NB_MAX_DIF_ENTRE_FORMS).post();
	}
	
	public void contrainteFormationsContigues() {
		//Contrainte # 6:
		//Contrainte pour assurer qu'une formation est faite pendant une même semaine
		for (int i = 0; i < equipes.length; i++) {
			BoolVar[][] formationsEquipeSemaine = formationsSemaines.get(i);
			for (int j = 0; j < formationsEquipeSemaine.length; j++) {
				for (int k = 0; k < formationsEquipeSemaine[0].length; k++) {
					int f = j+1;
					int s = k+1;
					formationsEquipeSemaine[j][k] = model.boolVar("Equipe : "+i+"- Formation : "+f+" - Semaine : "+s);
				}
			}
			
			
			for (int s = 0; s < NB_SEMAINES; s++) {
				IntVar[] tracesSemaine = getTracesSemaine(equipes[i], s);
				
				for (int j = 0; j < NB_FORMATIONS; j++) {
					int maxBesoin = 5;
					IntVar count = model.intVar(0, maxBesoin);
					model.count(j+1, tracesSemaine, count).post();
					
					model.arithm(count, ">", PAS_DE_COURS).reifyWith(formationsEquipeSemaine[j][s]);
				}
			}
			
			for (int j = 0; j < formationsEquipeSemaine.length; j++) {
				int nombreDeSemainesNecessaires = Math.floorDiv((int) besoinsEquipe[i][j], 5) + 1;
				model.sum(formationsEquipeSemaine[j], "=", nombreDeSemainesNecessaires).post();
			}
		}
		
		//Contrainte # 7:
		//Contrainte pour assurer qu'une formation est faite toujours sur une même trace
		/*
		for (int i = 0; i < formationsTraces.size(); i++) {
			IntVar[][] formationsTracesEquipe = formationsTraces.get(i);
			for (int j = 0; j < formationsTracesEquipe.length; j++) {
				for (int k = 0; k < formationsTracesEquipe[0].length; k++) {
					int e = i+1;
					int f = j+1;
					int t = k+1;
					formationsTracesEquipe[j][k] = model.intVar("Equipe : "+e+" - Formation : "+f+" - Trace : "+t,0, 1);
				}
			}
			
			for (int t = 0; t < NB_TRACES_JOUR; t++) {
				IntVar[] tracesHoraireEquipe = new IntVar[NB_SEMAINES * 5];
				int count = 0;
				for (int s = 0; s < NB_SEMAINES; s++) {
					// Mejora -> coger solo las disponibles
					IntVar[] tracesSemaineHoraire = getTracesSemainePourUneTrace(equipes[i], s, t);
					for (int j = 0; j < tracesSemaineHoraire.length; j++) {
						tracesHoraireEquipe[count] = tracesSemaineHoraire[j];
						count++;
					}
				}
				
				for (int f = 0; f < NB_FORMATIONS; f++) {
					int formation = f+1;
					// Mejora -> dominio según el besoin
					IntVar compteur = model.intVar(0, 10);
					model.count(formation, tracesHoraireEquipe, compteur).post();
					
					Constraint oui = model.arithm(compteur, ">", PAS_DE_COURS);
					Constraint non = model.arithm(compteur, "=", PAS_DE_COURS);
					
					Constraint formationCetteTrace = model.arithm(formationsTracesEquipe[f][t], "=", 1);
					Constraint pasDeFormationCetteTrace = model.arithm(formationsTracesEquipe[f][t], "=", 0);
					
					model.or(model.and(oui, formationCetteTrace), model.and(non, pasDeFormationCetteTrace)).post();
				}
			}
			
			for (int j = 0; j < formationsTracesEquipe.length; j++) {
				model.sum(formationsTracesEquipe[j], "<=", 3).post();
			}
		}
		*/
		
	}
	
	/** 
	 *  Méthodes de lecture des fichiers
	**/
	
	public void lireDisponibilitesEquipes() throws Exception {
		
		// Lecture des disponibilités des équipes
		File file = new File("./data/DisposEquipes.csv");
		BufferedReader buf;
		buf = new BufferedReader(new FileReader(file));
		String line = buf.readLine();
		line = buf.readLine();
		
		int equipe = 0;
		
		while(line != null) {
			String[] team = line.split(";");
			Integer[] teamAvailability = new Integer[NB_JOURS];
			
			if(team.length < NB_JOURS) {
				buf.close();
				throw new Exception("Les disponibilités des équipes ne sont pas complètes pour la période à planifier : "+NB_JOURS+" jours à partir du "+START_DATE);
			} else {
				// On trouve les dispos d'une équipe
				for (int i = 0; i < teamAvailability.length; i++) {
					if(team[i+1].equals("J")) {
						teamAvailability[i] = 1;
					}
					else {
						teamAvailability[i] = 0;
					}
				}
				
				// Contrainte # 8 : Les indisponibilités sont fixées à la valeur de la constante "NO_DISPONIBLE"
				if(equipe < NB_EQUIPES) {
					for (int j = 0; j < NB_JOURS; j++) {
						if(teamAvailability[j] == 0) {
							for (int k = 0; k < NB_TRACES_JOUR; k++) {
								model.arithm(equipes[equipe][j * NB_TRACES_JOUR + k], "=", NO_DISPONIBLE).post();
							}
						} else {
							for (int k = 0; k < NB_TRACES_JOUR; k++) {
								model.arithm(equipes[equipe][j * NB_TRACES_JOUR + k], ">=", PAS_DE_COURS).post();
							}
						}
					}
				}
				equipe++;
				line = buf.readLine();
			}
		}
		buf.close();
	}
	
	public void lireBesoinsEquipes() {
		
		// Lecture des disponibilités des équipes
		File file = new File("./data/BesoinsFormations.csv");
		BufferedReader buf;
		try {
			buf = new BufferedReader(new FileReader(file));
			String line = buf.readLine();
			line = buf.readLine();
			
			int equipe = 0;
			
			while(line != null) {
				String[] besoin = line.split(";");
				
				for (int i = 1; i < besoin.length; i++) {
					String[] num = besoin[i].split(",");
					
					int a = Integer.parseInt(num[0].trim());
					double c = a;
					
					if(num.length>1) {
						double b = Integer.parseInt(num[1].trim().substring(0, 1));
						c += b/10;
					} 
					
					besoinsEquipe[equipe][i-1] = c;
				}
				equipe++;
				line = buf.readLine();
			}
			
			// Lecture des informations des formations
			file = new File("./data/FormationsInfos.csv");
			buf = new BufferedReader(new FileReader(file));
			line = buf.readLine();
			line = buf.readLine();
			line = buf.readLine();
			
			int formation = 0;

			while(line != null) {
				String[] besoin = line.split(";");
				for (int i = 6; i <= 7 && formation < 7; i++) {
					formations[formation][i-5] = Integer.parseInt(besoin[i]);
				}
				formation++;
				line = buf.readLine();
			}
			
			// Matrice des besoins de formations par equipes en traces totales remplie
			for (int i = 0; i < formationsParEquipe.length; i++) {
				for (int j = 0; j < formationsParEquipe[i].length; j++) {
					formationsParEquipe[i][j] = besoinsEquipe[i][j] * formations[j][1];
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void lireContraintesSalles() {
		// Contrainte # 9 : Toutes les salles ne possèdent pas l'équipement nécessaire pour toutes les formations
		// Lecture des disponibilités des équipes
		File file = new File("./data/Salles-formations.csv");
		BufferedReader buf;
		int[][] formationsSalles = new int [NB_FORMATIONS][NB_SALLES];
		try {
			buf = new BufferedReader(new FileReader(file));
			String line = buf.readLine();
			line = buf.readLine();
			int formation = 0;
			while(line != null) {
				String[] ligne = line.split(";");
				for(int i = 1;i < ligne.length; i++) {
					formationsSalles[formation][i-1] = Integer.parseInt(ligne[i])*(formation+1);
				}
				formation++;
				line = buf.readLine();
			}
			
			//Définition des domaines pour les salles
			ArrayList<ArrayList<Integer>> domaines = new ArrayList<>();
			for(int i = 0; i < formationsSalles[0].length; i++) {
				ArrayList<Integer> formationSalle = new ArrayList<Integer>();
				formationSalle.add(NO_DISPONIBLE);
				formationSalle.add(PAS_DE_COURS);
				for(int j = 0; j < formationsSalles.length; j++) {
					if(!formationSalle.contains(formationsSalles[j][i])) {
						formationSalle.add(formationsSalles[j][i]);
					}
				}
				domaines.add(formationSalle);
			}
			
			int tracesTot = NB_TRACES_JOUR * NB_JOURS;
			
			//Traitement Date
			Calendar c = Calendar.getInstance();
			SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
			Date date = formatter.parse(START_DATE);
			
			for (int i = 0; i < salles.length; i++) {
	
				//Récuperation des formations pour lesquelles la salle est adaptée
				int [] d = new int [domaines.get(i).size()];
			    for (int ii=0; ii < d.length; ii++)
			    {
			        d[ii] = domaines.get(i).get(ii).intValue();
			    }
			    
				for (int j = 0; j < tracesTot; j++) {
					c.setTime(date);
					c.add(Calendar.DATE, j/NB_TRACES_JOUR);
					String dateString = formatter.format(c.getTime());
					int trace = (j  % NB_TRACES_JOUR) + 1;
					//Si jamais on veut differencier les salles (c.a.d. qu'elle n'est pas suffisament equipée pour une formation, on supprime cette formation du domaine
					salles[i][j] = model.intVar("SALLE"+i+" "+dateString+" "+"T"+trace, d);
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void lireDsiponibilitesFormateurs() {
		// Contrainte # 10 : Les formateurs ne sont pas disponibles pendant les périodes de congés
		// Lecture des disponibilit�s des �quipes
				File file = new File("./data/DisposFormateurs.csv");
				BufferedReader buf;
				try {
					buf = new BufferedReader(new FileReader(file));
					String line = buf.readLine();
					line = buf.readLine();
					
					int id = 0;
					ArrayList<Conge> conges = new ArrayList<Conge>();
					
					//R�cuperation des donn�es de cong�s
					while(line != null) {
						String[] formateur = line.split(";");
						for (int i = 1; i < formateur.length; i = i+2) {
							if(formateur[i].length() > 0) {
								Conge conge = new Conge(id, formateur[i], formateur[i+1]);
								conges.add(conge);
							}
						}
						id++;
						line = buf.readLine();
					}
					
					//Traitement des dates
					DateTimeFormatter formatterDateTime = DateTimeFormatter.ofPattern("dd/MM/yy");
					DateTimeFormatter formatterStartDate = DateTimeFormatter.ofPattern("dd/MM/yyyy");
					
					//Dates du planning
					LocalDate startDate = LocalDate.parse(START_DATE, formatterStartDate);
					LocalDate endDate = startDate.plusDays(NB_JOURS);
					
					for (int i = 0; i < conges.size(); i++) {
						Conge conge = conges.get(i);
						int idFormateur = conge.getFormateur();
						
						LocalDate dateDebutConges = LocalDate.parse(conge.getDateDebut(), formatterDateTime);
						LocalDate dateFinConges = LocalDate.parse(conge.getDateFin(), formatterDateTime);
						
						if(dateFinConges.isAfter(dateDebutConges) && dateDebutConges.isAfter(startDate) && dateFinConges.isBefore(endDate)) {
							int joursConge = (int) ChronoUnit.DAYS.between(dateDebutConges, dateFinConges);
							int joursDepuisDebut = (int) ChronoUnit.DAYS.between(startDate, dateDebutConges);
							
							for (int j = joursDepuisDebut; j < (joursConge * NB_TRACES_JOUR); j++) {
								model.arithm(formateurs[idFormateur][j], "=", NO_DISPONIBLE).post();
							}
							
						}
					}
					
				} catch (Exception e) {
					e.printStackTrace();
				}
	}
	
	/** 
	 *  Méthodes de support
	**/
	
	public IntVar[] getColumn(IntVar[][] matrix, int j) {		
		return ArrayUtils.getColumn(matrix, j);
	}
	
	public IntVar[] getTracesJour(IntVar[] matrix, int j) {
		IntVar[] tracesJour = new IntVar[NB_TRACES_JOUR];
		if(j==0) {
			for (int i = 0; i < NB_TRACES_JOUR; i++) {
				tracesJour[i] = matrix[i];
			}
		}
		else {
			for (int i = j*5; i < j*5 + NB_TRACES_JOUR; i++) {
				tracesJour[i-j*5] = matrix[i];
			}
		}
		return tracesJour;
	}
	
	public IntVar[] getTracesSemainePourUneTrace(IntVar[] planning, int semaine, int trace) {
		IntVar[] traces = new IntVar[5];
		
		for (int i = 0; i < 5; i++) {
			traces[i] = planning[semaine * 35 + trace + 5 * i];
		}
		
		return traces;
	}
	
	public IntVar[] getTracesSemaine(IntVar[] planning, int semaine) {
		IntVar[] tracesSemaine = new IntVar[25];
		
		int k = 0;
		for (int i = 0; i < NB_TRACES_JOUR; i++) {
			IntVar[] traces = getTracesSemainePourUneTrace(planning, semaine, i);
			for (int j = 0; j < traces.length; j++) {
				tracesSemaine[k] = traces[j];
				k++;
			}
		}
		
		return tracesSemaine;
	}
	
	/** 
	 *  Méthodes de résolution
	**/
	
	public void go() throws Exception {
		int tot = NB_TRACES_JOUR * NB_JOURS * NB_EQUIPES + NB_TRACES_JOUR * NB_JOURS * NB_FORMATEURS + NB_TRACES_JOUR * NB_JOURS * NB_SALLES;
		IntVar[] vars = new IntVar[tot];
		int c = 0;
		
		/*
		for (int i = 0; i < formationsTraces.size(); i++) {
			IntVar[][] f = formationsTraces.get(i);
			for (int j = 0; j < f.length; j++) {
				for (int k = 0; k < f[0].length; k++) {
					vars[c] = f[j][k];
					c++;
				}
			}
		}
		*/
		
		for (int j = 0; j < equipes[0].length; j++) {
			for (int i = 0; i < equipes.length; i++) {
				vars[c] = equipes[i][j];
				c++;
			}
		}
		
		for (int j = 0; j < formateurs[0].length; j++) {
			for (int i = 0; i < formateurs.length; i++) {

				vars[c] = formateurs[i][j];
				c++;
			}
		}
		
		for (int j = 0; j < salles[0].length; j++) {
			for (int i = 0; i < salles.length; i++) {
				vars[c] = salles[i][j];
				c++;
			}
		}
		
		solver.setSearch(Search.domOverWDegSearch(vars));
		solver.setLubyRestart(500, new FailCounter(model, 100), 5000);
		solver.showSolutions(); 
		solver.showShortStatistics();
		System.out.println("go");
		solver.showStatisticsDuringResolution(2000);
		solver.findSolution();
		solver.printStatistics();
		
		printSolution();
	}
	
	public void go2() throws Exception {
		int jours = NB_JOURS;
		int tot = NB_TRACES_JOUR * jours * NB_EQUIPES + NB_TRACES_JOUR * jours * NB_FORMATEURS + NB_TRACES_JOUR * jours * NB_SALLES;
		IntVar[] vars = new IntVar[tot];
		int c = 0;
		int jour = 0;
		while(jour < jours) {
			for (int j = 0; j < equipes.length; j++) {
				IntVar[] equipe = getTracesJour(equipes[j], jour);
				for (int i = 0; i < equipe.length; i++) {
					vars[c] = equipe[i];
					c++;
				}
			}
			
			for (int j = 0; j < formateurs.length; j++) {
				IntVar[] formateur = getTracesJour(formateurs[j], jour);
				for (int i = 0; i < formateur.length; i++) {
					vars[c] = formateur[i];
					c++;
				}
			}
			
			for (int j = 0; j < salles.length; j++) {
				IntVar[] salle = getTracesJour(salles[j], jour);
				for (int i = 0; i < salle.length; i++) {
					vars[c] = salle[i];
					c++;
				}
			}
			
			jour++;
		}
		
		solver.setSearch(activityBasedSearch(vars));
		solver.showSolutions(); 
		solver.showShortStatistics();
		System.out.println("go");
		solver.showStatisticsDuringResolution(5000);
		solver.findSolution();
		solver.printStatistics();
		
		printSolution();
	}
	
	public void printSolution() throws Exception {
		PrintWriter writer = new PrintWriter("./data/solutionFormateursCycle3.txt", "UTF-8");
		
		for(int i=0;i < formateurs.length;i++) {
			for (int j = 0; j < formateurs[i].length; j++) {
				if(formateurs[i][j].getValue() != NO_DISPONIBLE && formateurs[i][j].getValue() != 0) writer.println(formateurs[i][j]+"  ");
			}
		}
		
		writer.close();
		writer = new PrintWriter("./data/solutionEquipesCycle3.txt", "UTF-8");
		
		for(int i=0;i < equipes.length;i++) {
			IntVar[][] org = formationsSemaines.get(i);
			for (int j = 0; j < org.length; j++) {
				for (int k = 0; k < org[0].length; k++) {
					if(org[j][k].getValue() > 0) writer.println(org[j][k]+"  ");
				}
			}
			for (int j = 0; j < equipes[i].length; j++) {
				if(equipes[i][j].getValue() != NO_DISPONIBLE && equipes[i][j].getValue() != 0) writer.println(equipes[i][j]+"  ");
			}
		}
		
		writer.close();
		writer = new PrintWriter("./data/solutionSallesCycle3.txt", "UTF-8");	
		
		for(int i=0;i < salles.length;i++) {
			for (int j = 0; j < salles[i].length; j++) {
				if(salles[i][j].getValue() != NO_DISPONIBLE && salles[i][j].getValue() != 0) writer.println(salles[i][j]+"  ");
			}
		}
		
		writer.close();
	}
	
	/** 
	 *  Main
	**/
	
	public static void main(String[] args) {
		try {
			EDF edf = new EDF();
			edf.go();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
