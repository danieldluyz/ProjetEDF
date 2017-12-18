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
	
	private static final int NB_EQUIPES = 2;
	private static final int NB_FORMATEURS = 3;
	private static final int NB_FORMATIONS = 3;
	private static final int NB_SALLES = 2;
	private static final int NB_TRACES_JOUR = 5;
	private static final int NB_JOURS = 1;
	
	private ArrayList<IntVar[]> equipes;
	
	private ArrayList<IntVar[]> formateurs;
	
	private ArrayList<IntVar[]> salles;
	
	public EDF() {
		Model model = new Model();
		Solver solver = model.getSolver();
		
		equipes = new ArrayList<IntVar[]>();
		formateurs = new ArrayList<IntVar[]>();
		salles = new ArrayList<IntVar[]>();
		
		for (int i = 0; i < NB_EQUIPES; i++) {
			IntVar[] equipe = model.intVarArray("E"+i, NB_TRACES_JOUR * NB_JOURS, 1, NB_FORMATIONS);
		}
		
		for (int i = 0; i < salles.size(); i++) {
			IntVar var2 = model.intVar(2);
		}
		
	}
	
	public void contraintes() {
		
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
