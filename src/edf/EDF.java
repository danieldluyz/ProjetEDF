package edf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.IntVar;

public class EDF {
	
	private Model model;
	private Solver solver;
	
	private static final int NB_EQUIPES = 2;
	private static final int NB_FORMATEURS = 3;
	private static final int NB_FORMATIONS = 3;
	private static final int NB_SALLES = 2;
	private static final int NB_TRACES_JOUR = 5;
	private static final int NB_JOURS = 1;
	
	private IntVar[][] equipes;
	
	private IntVar[][] formateurs;
	
	private IntVar[][] salles;
	
	private int[] formations;
	
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
	
	public void constraintes() {
		// Contrainte pour assurer que quand il y a une formation il y a bien une
		// equipe, une salle et un formatteur
		for (int i = 0; i < equipes.length; i++) {
			
			for (int j = 0; j < formations.length; j++) {
				IntVar countEqFor = model.intVar("count_eq_for_"+i+"_"+j, 0, 5, false);
				IntVar countFormFor = model.intVar("count_form_for_"+i+"_"+j, 0, 5, false);
				IntVar countSalleFor = model.intVar("count_salle_for_"+i+"_"+j, 0, 5, false);
				
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
		File file = new File("./data/DisposEquipes.csv");
		BufferedReader buf;
		try {
			
			buf = new BufferedReader(new FileReader(file));
			String line = buf.readLine();
			line = buf.readLine();
			while(line != null) {
				String[] team = line.split(";");
				Integer[] teamAvailability = new Integer[team.length-1];
				for (int i = 0; i < team.length-1; i++) {
					if(team[i+1].equals("J")) teamAvailability[i] = 1;
					else teamAvailability[i] = 0;
				}
				for (int i = 0; i < teamAvailability.length; i++) {
					System.out.print(teamAvailability[i]+";");
				}
				System.out.println("");
				line = buf.readLine();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
