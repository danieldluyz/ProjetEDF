package edf;

public class Conge {
	
	private String dateDebut;
	private String dateFin;
	private int formateur;
	
	public Conge(int f, String dD, String dF) {
		dateDebut = dD;
		dateFin = dF;
		formateur = f;
	}

	public String getDateDebut() {
		return dateDebut;
	}

	public String getDateFin() {
		return dateFin;
	}

	public int getFormateur() {
		return formateur;
	}
	
	public String toString() {
		return "Formateur "+formateur+" - Date début : "+dateDebut+" - Date fin : "+dateFin;
	}

}
