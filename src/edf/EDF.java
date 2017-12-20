package edf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.IntVar;

public class EDF {
	
	// CONSTANTES :
	
	/** La constante qui fait référence à l'indisponibilité d'une équipe/formateur/trace pendant une trace */
	private static final int NO_DISPONIBLE = -1;
	
	// DONNÉES :
	
	/** Le nombre d'équipes */
	private static final int NB_EQUIPES = 14;
	
	/** Le nombre de formateurs */
	private static final int NB_FORMATEURS = 3;
	
	/** Le nombre de formations données par le centre */
	private static final int NB_FORMATIONS = 7;
	
	/** Le numéro de salles */
	private static final int NB_SALLES = 2;
	
	/** Le nombre de traces disponibles par jour */
	private static final int NB_TRACES_JOUR = 5;
	
	/** Le nombre de jours à planifier */
	private static final int NB_JOURS = 40;
	
	/** Cette matrice comporte la liste de formations
	 * La première colonne est l'id de la formation (un numéro)
	 * La deuxième colonne est la durée en traces de la formation
	 * La troisième colonne est le nombre max. de traces par jour de cette formation
	**/
	private double[][] formations;
	
	/**
	 * 
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
	
	public EDF() {
		model = new Model();
		solver = model.getSolver();
		
		int tracesTot = NB_TRACES_JOUR * NB_JOURS;
		
		equipes = new IntVar[NB_EQUIPES][tracesTot];
		formateurs = new IntVar[NB_FORMATEURS][tracesTot];
		salles = new IntVar[NB_SALLES][tracesTot];
		
		for (int i = 0; i < equipes.length; i++) {
			for (int j = 0; j < tracesTot; j++) {
				equipes[i][j] = model.intVar("EQ"+i+"T"+j, NO_DISPONIBLE, NB_FORMATIONS);
			}
		}
		
		for (int i = 0; i < formateurs.length; i++) {
			for (int j = 0; j < tracesTot; j++) {
				//Si jamais on veut differencier les formateurs (c.a.d. qu'ils font des formations differentes), on change les valeurs du domaine et c'est tout
				formateurs[i][j] = model.intVar("FORM"+i+"T"+j, NO_DISPONIBLE, NB_FORMATIONS);
			}
		}
		
		for (int i = 0; i < salles.length; i++) {
			for (int j = 0; j < tracesTot; j++) {
				//Si jamais on veut differencier les salles (c.a.d. qu'elle n'est pas suffisament equipée pour une formation, on supprime cette formation du domaine
				salles[i][j] = model.intVar("SALLE"+i+"T"+j, NO_DISPONIBLE, NB_FORMATIONS);
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
					if(team[i+1].equals("J")) teamAvailability[i] = 1;
					else {
						teamAvailability[i] = 0;
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
					double b = Integer.parseInt(num[1].trim().substring(0, 1));
					double c = a + b/10;
					
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
			
			// Matrice des besoins de formations par equipes en termes de traces totaux remplie
			for (int i = 0; i < formationsParEquipe.length; i++) {
				for (int j = 0; j < formationsParEquipe[i].length; j++) {
					formationsParEquipe[i][j] = besoinsEquipe[i][j] * formations[j][1];
				}
			}
			
			for (int i = 0; i < formationsParEquipe.length; i++) {
				for (int j = 0; j < formationsParEquipe[i].length; j++) {
					System.out.print(formationsParEquipe[i][j]+" - ");
				}
				System.out.println("");
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
				IntVar countEqFor = model.intVar("count_eq_for_"+i+"_"+j, 0, 100, false);
				IntVar countFormFor = model.intVar("count_form_for_"+i+"_"+j, 0, 100, false);
				IntVar countSalleFor = model.intVar("count_salle_for_"+i+"_"+j, 0, 100, false);
				
				IntVar[] columnEquipe = getColumn(equipes, i);
				IntVar[] columnFormateur = getColumn(formateurs, i);
				IntVar[] columnSalle = getColumn(salles, i);
				
				model.count((int) formations[j][0], columnEquipe, countEqFor).post();
				model.count((int) formations[j][0], columnFormateur, countFormFor).post();
				model.count((int) formations[j][0], columnSalle, countSalleFor).post();

				model.arithm(countEqFor, "=", countFormFor).post();
				model.arithm(countEqFor, "=", countSalleFor).post();
			}
		}
		
		
		// Contrainte # 2 :
		// Contrainte pour assurer que tous les equipes suivent toutes les formations 
		for (int i = 0; i < equipes.length; i++) {
			for (int j = 0; j < formations.length; j++) {
				IntVar cFile=model.intVar("cFile_equipe: "+i, 0, 100, false);
				
				model.count((int) formations[j][0], equipes[i], cFile).post();
				model.arithm(cFile, "=", (int) formationsParEquipe[i][j]).post();
			}
		}
		
		/*
		// Contrainte # 3 :
		// Contrainte pour assurer que le nombre maximum des traces soit respecte
		for (int i = 0; i < equipes.length; i++) {
			for (int j = 0; j < NB_JOURS; j++) {
					for(int k=0;k<formations.length;k++) {
						IntVar cFormJour= model.intVar("CFormJour"+k+"--"+j, 0, 100, false);
						
						model.count((int)formations[k][0], getTracesJour(equipes[i], j), cFormJour).post();
						model.arithm(cFormJour, "<=", (int)formations[k][2]).post();
					}
			}
		}
		*/
		
	}
	
	public IntVar[] getColumn(IntVar[][] matrix, int j) {
		IntVar[] column = model.intVarArray(matrix[0].length, -1, NB_FORMATIONS);
		
		for (int i = 0; i < matrix.length; i++) {
			column[i] = matrix[i][j];
			model.arithm(column[i], "=", matrix[i][j]).post();
		}
		
		return column;
	}
	
	public IntVar[] getTracesJour(IntVar[] matrix, int j) {
		IntVar[] jour = model.intVarArray(NB_TRACES_JOUR, -1, NB_FORMATIONS);
		if(j==0) {
			for (int i = 0; i < jour.length; i++) {
				jour[i] = matrix[i];
				model.arithm(jour[i], "=", matrix[i]).post();
			}
		}
		else {
			for (int i = j*5; i < j*5+jour.length; i++) {
				jour[i-j*5] = matrix[i];
				model.arithm(jour[i-j*5], "=", matrix[i]).post();
			}
		}
		
		return jour;
	}
	
	
	public void go() {
		solver.showSolutions(); 
		solver.findSolution();
		solver.printStatistics();	
	}
	
	public static void main(String[] args) {
		EDF edf = new EDF();
		edf.go();
	}

}
