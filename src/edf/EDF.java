package edf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.tools.ArrayUtils;

import static org.chocosolver.solver.search.strategy.Search.activityBasedSearch;

import jxl.Workbook;
import jxl.read.biff.WorkbookParser;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

public class EDF {

	// CONSTANTES :

	/** La constante qui fait référence à l'indisponibilité d'une équipe/formateur/trace pendant une trace */
	private static final int NO_DISPONIBLE = -1;

	// DONNÉES :

	/** La date de début du planning en format dd/mm/yyyy */
	private static final String START_DATE = "01/09/2017";

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

	private static final int NB_JOURS = 100;


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

	public void lireDisponibilitesEquipes() {

		// Lecture des disponibilités des équipes
		File file = new File("./data/DisposEquipes.csv");
		BufferedReader buf;
		try {
			buf = new BufferedReader(new FileReader(file));
			String line = buf.readLine();
			line = buf.readLine();

			int equipe = 0;

			while(line != null) {
				String[] team = line.split(";");
				Integer[] teamAvailability = new Integer[team.length-1];

				// On trouve les dispos d'une équipe
				for (int i = 0; i < team.length-1; i++) {
					if(team[i+1].equals("J")) teamAvailability[i] = 0;
					else {
						teamAvailability[i] = 1;
					}
				}

				// Contrainte # 4 : Les indisponibilités sont fixées à la valeur de la constante "NO_DISPONIBLE"
				if(equipe < NB_EQUIPES) {
					for (int j = 0; j < NB_JOURS; j++) {
						if(teamAvailability[j] == 0) {
							for (int k = 0; k < NB_TRACES_JOUR; k++) {
								model.arithm(equipes[equipe][j * NB_TRACES_JOUR + k], "=", NO_DISPONIBLE).post();
							}
						} else {
							for (int k = 0; k < NB_TRACES_JOUR; k++) {
								model.arithm(equipes[equipe][j * NB_TRACES_JOUR + k], "!=", NO_DISPONIBLE).post();
							}
						}
					}
				}
				equipe++;
				line = buf.readLine();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		// Lecture des disponibilités des équipes
		File file = new File("./data/Salles-formations.csv");
		BufferedReader buf;
		int[][] sf= new int [NB_FORMATIONS][NB_SALLES];
		try {
			buf = new BufferedReader(new FileReader(file));
			String line = buf.readLine();
			line = buf.readLine();
			int jj=0;
			while(line != null) {
				String[] aux=line.split(",");
				for(int i=1;i<aux.length;i++) {
					sf[jj][i-1]=Integer.parseInt(aux[i])*(jj+1);
				}
				jj++;
				line = buf.readLine();
			}
			ArrayList<ArrayList<Integer>> p= new ArrayList<>();
			for(int i=0;i<sf[0].length;i++) {
				ArrayList<Integer> aux=new ArrayList<Integer>();
				aux.add(-1);
				aux.add(0);
				for(int j=0;j<sf.length;j++) {
					if(!aux.contains(sf[j][i])) {
						aux.add(sf[j][i]);
					}
				}
				p.add(aux);
			}

			int tracesTot = NB_TRACES_JOUR * NB_JOURS;

			//Traitement Date
			Calendar c = Calendar.getInstance();
			SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
			Date date = formatter.parse(START_DATE);

			for (int i = 0; i < salles.length; i++) {

				//Récuperation des formations pour lesquelles la salle est adaptée
				int [] d = new int [p.get(i).size()];
				for (int ii=0; ii < d.length; ii++)
				{
					d[ii] = p.get(i).get(ii).intValue();
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

	public void contraintes() {
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


		// Contrainte # 2 :
		// Contrainte pour assurer que toutes les equipes suivent toutes les formations le bon nombre de fois
		for (int i = 0; i < equipes.length; i++) {
			for (int j = 0; j < formations.length; j++) {
				IntVar cFileIJ=model.intVar("cFile_equipe: "+i+"- formation:"+j, (int) formationsParEquipe[i][j]);
				model.count((int) formations[j][0], equipes[i], cFileIJ).post();
			}
		}


		// Contrainte # 3 :
		// Contrainte pour assurer que le nombre maximum des traces de chaque formation soit respecte
		for (int i = 0; i < equipes.length; i++) {
			for (int j = 0; j < NB_JOURS; j++) {
				for(int k=0;k<formations.length;k++) {
					IntVar cFormJour= model.intVar("CFormJour"+k+"--"+j, 0, NB_TRACES_JOUR);

					model.count((int)formations[k][0], getTracesJour(equipes[i], j), cFormJour).post();
					model.arithm(cFormJour, "<=", (int)formations[k][2]).post();
				}
			}
		}

	}

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


	public void go() throws Exception {
		int tot = NB_TRACES_JOUR * NB_JOURS * NB_EQUIPES + NB_TRACES_JOUR * NB_JOURS * NB_FORMATEURS + NB_TRACES_JOUR * NB_JOURS * NB_SALLES;
		IntVar[] varEquipes = new IntVar[tot];
		int c = 0;
		for (int j = 0; j < equipes[0].length; j++) {
			for (int i = 0; i < equipes.length; i++) {
				varEquipes[c] = equipes[i][j];
				c++;
			}
		}

		for (int j = 0; j < formateurs[0].length; j++) {
			for (int i = 0; i < formateurs.length; i++) {

				varEquipes[c] = formateurs[i][j];
				c++;
			}
		}

		for (int j = 0; j < salles[0].length; j++) {
			for (int i = 0; i < salles.length; i++) {
				varEquipes[c] = salles[i][j];
				c++;
			}
		}

		solver.setSearch(activityBasedSearch(varEquipes));
		solver.showSolutions(); 
		solver.findSolution();
		solver.printStatistics();

		printSolution();
	}

	public void printSolution() throws Exception {
		PrintWriter writer = new PrintWriter("./data/solutionFormateurs.txt", "UTF-8");

		for(int i=0;i < formateurs.length;i++) {
			for (int j = 0; j < formateurs[i].length; j++) {
				writer.println(formateurs[i][j]+"  ");
			}
		}

		writer.close();
		writer = new PrintWriter("./data/solutionEquipes.txt", "UTF-8");

		for(int i=0;i < equipes.length;i++) {
			for (int j = 0; j < equipes[i].length; j++) {
				writer.println(equipes[i][j]+"  ");
			}
		}

		writer.close();
		writer = new PrintWriter("./data/solutionSalles.txt", "UTF-8");	

		for(int i=0;i < salles.length;i++) {
			for (int j = 0; j < salles[i].length; j++) {
				writer.println(salles[i][j]+"  ");
			}
		}

		writer.close();
	}

	public static void main(String[] args) throws Exception {

		EDF edf = new EDF();

		edf.go();

		WritableWorkbook workbook = null;

		try {

			/* On créé un nouveau worbook et on l'ouvre en écriture */

			workbook = Workbook.createWorkbook(new File("/Users/clovismonmousseau/git/ProjetEDF/data/Sortie.xls"));


			/* On créé une nouvelle feuille (test en position 0) et on l'ouvre en écriture */

			WritableSheet sheet = workbook.createSheet("Équipes", 0); 


			/* Creation d'un champ au format texte */

			int posLigne =0;
			for(int i =0; i<edf.NB_EQUIPES;i++){
				for(int j =0; j<edf.NB_JOURS*edf.NB_TRACES_JOUR;j++){
					if(!(edf.equipes[i][j].isInstantiatedTo(-1)) && !(edf.equipes[i][j].isInstantiatedTo(0))){
						Label labelFormation = new Label(i, posLigne, ""+edf.equipes[i][j]);
						sheet.addCell(labelFormation);
						posLigne++;
					}
				}
				posLigne=0;
			}

			WritableSheet sheet1 = workbook.createSheet("Salles", 1);

			posLigne =0;
			for(int i =0; i<edf.NB_SALLES;i++){
				for(int j =0; j<edf.NB_JOURS*edf.NB_TRACES_JOUR;j++){
					if(!(edf.salles[i][j].isInstantiatedTo(-1)) && !(edf.salles[i][j].isInstantiatedTo(0))){
						Label labelFormation = new Label(i, posLigne, ""+edf.salles[i][j]);
						sheet1.addCell(labelFormation);
						posLigne++;
					}
				}
				posLigne=0;
			}

			WritableSheet sheet2 = workbook.createSheet("Formateur", 2);

			posLigne =0;
			for(int i =0; i<edf.NB_FORMATEURS;i++){
				for(int j =0; j<edf.NB_JOURS*edf.NB_TRACES_JOUR;j++){
					if(!(edf.formateurs[i][j].isInstantiatedTo(-1)) && !(edf.formateurs[i][j].isInstantiatedTo(0))){
						Label labelFormation = new Label(i, posLigne, ""+edf.formateurs[i][j]);
						sheet2.addCell(labelFormation);
						posLigne++;
					}
				}
				posLigne=0;
			}




			/* On ecrit le classeur */

			workbook.write(); 


		} 

		catch (IOException e) {

			e.printStackTrace();

		} 

		catch (RowsExceededException e) {

			e.printStackTrace();

		}

		catch (WriteException e) {
			e.printStackTrace();
		}
		finally {
			if(workbook!=null){
				/* On ferme le worbook pour libérer la mémoire */
				/**/
				try {
					workbook.close();
				} 
				catch (WriteException e) {
					e.printStackTrace();
				} 
				catch (IOException e) {
					e.printStackTrace();
				}
				//**/
			}
		}
	}
}
