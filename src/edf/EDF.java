package edf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
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
	private static final int NB_FORMATIONS = 3;
	
	/** Le numéro de salles */
	private static final int NB_SALLES = 2;
	
	/** Le nombre de traces disponibles par jour */
	private static final int NB_TRACES_JOUR = 5;
	
	/** Le nombre de jours à planifier */
	private static final int NB_JOURS = 3;
	
	/** La liste de formations */
	private int[] formations;
	
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
				//TO DO : Établir comme dommaine de chaque équipe seulement les formations dont chaque equipe a besoin
				equipes[i][j] = model.intVar(-1, NB_FORMATIONS);
			}
		}
		
		for (int i = 0; i < formateurs.length; i++) {
			for (int j = 0; j < tracesTot; j++) {
				//Si jamais on veut differencier les formateurs (c.a.d. qu'ils font des formations differentes), on change les valeurs du domaine et c'est tout
				formateurs[i][j] = model.intVar(-1, NB_FORMATIONS);
			}
		}
		
		for (int i = 0; i < salles.length; i++) {
			for (int j = 0; j < tracesTot; j++) {
				//Si jamais on veut differencier les salles (c.a.d. qu'elle n'est pas suffisament equipée pour une formation, on supprime cette formation du domaine
				salles[i][j] = model.intVar(-1, NB_FORMATIONS);
			}
		}
		
		formations = new int[NB_FORMATIONS];
		for (int i = 0; i < NB_FORMATIONS; i++) {
			formations[i] = i;
		}
		
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
				
				// Contrainte # 3 : Les indisponibilités sont fixées à la valeur de la constante "NO_DISPONIBLE"
				if(equipe < NB_EQUIPES) {
					for (int j = 0; j < NB_JOURS; j++) {
						if(teamAvailability[j] == 0) {
							for (int k = 0; k < NB_TRACES_JOUR; k++) {
								model.arithm(equipes[equipe][j * NB_TRACES_JOUR + k], "=", NO_DISPONIBLE);
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
	
	public void constraintes() {
		// 1ère contrainte :
		// Contrainte pour assurer que quand il y a une formation il y a bien une
		// equipe, une salle et un formatteur
		for (int i = 0; i < equipes[i].length; i++) {
			for (int j = 0; j < formations.length; j++) {
				IntVar countEqFor = model.intVar("count_eq_for_"+i+"_"+j, 0, 100, false);
				IntVar countFormFor = model.intVar("count_form_for_"+i+"_"+j, 0, 100, false);
				IntVar countSalleFor = model.intVar("count_salle_for_"+i+"_"+j, 0, 100, false);
				
				IntVar[] columnEquipe = getColumn(equipes, j);
				IntVar[] columnFormateur = getColumn(formateurs, j);
				IntVar[] columnSalle = getColumn(salles, j);
				
				model.count(formations[j], columnEquipe, countEqFor).post();
				model.count(formations[j], columnFormateur, countFormFor).post();
				model.count(formations[j], columnSalle, countSalleFor).post();

				model.arithm(countEqFor, "=", countFormFor).post();
				model.arithm(countEqFor, "=", countSalleFor).post();
			}
		}
	}
	
	public IntVar[] getColumn(IntVar[][] matrix, int j) {
		IntVar[] column = new IntVar[matrix[0].length];
		
		for (int i = 0; i < column.length; i++) {
			column[i] = matrix[i][j];
		}
		
		return column;
	}
	
	public void go() {
		solver.showSolutions(); 
//		solver.findOptimalSolution(, true);
		solver.printStatistics();	
	}
	
	public static void main(String[] args) {
		EDF edf = new EDF();
		edf.lireDisponibilitesEquipes();
	}

}
