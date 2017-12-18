package edf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.IntVar;

public class EDF {
	
	private static final int NB_EQUIPES = 2;
	private static final int NB_FORMATEURS = 3;
	private static final int NB_SALLES = 2;
	
	public EDF() {
		Model model = new Model();
		Solver solver = model.getSolver();
		
		IntVar equipe1 =  model.intVar(1,NB_FORMATEURS);
		IntVar equipe2 = model.intVar(1,NB_FORMATEURS);
		
		IntVar formateur1 = model.intVar(1,NB_EQUIPES);
		IntVar formateur2 = model.intVar(1,NB_EQUIPES);
		IntVar formateur3 = model.intVar(1,NB_EQUIPES);
		
		IntVar salle1 = model.intVar(1,NB_SALLES);
		IntVar salle2 = model.intVar(1,NB_SALLES);
		
		
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
