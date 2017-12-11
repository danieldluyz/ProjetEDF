package edf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class EDF {
	
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
