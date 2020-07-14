package util;

public class Bookmark {
	private int associatedTrace;
	private double time;
	private String activeFunction;
	private double[] perfValues;

	public Bookmark (int iTrace, double time, String activeFunction, double[] perfValues) {
		this.associatedTrace = iTrace;
		this.time = time;
		this.activeFunction = activeFunction;
		this.perfValues = perfValues;
	}

	public double getTime () {
		return time;
	}

	public String getActiveFunction () {
		return activeFunction;
	}

	public int getNPerfValues () {
		return perfValues.length;
	}

	public double[] getPerfValues () {
		return perfValues;
	}

	public double getPerfValue (int i) {
		return perfValues[i];
	}

	public int getAssociatedTrace () {
		return associatedTrace;
	}

	public void print () {
		System.out.println ("Time: " + time);
		System.out.println ("Active Function: " + activeFunction);
		if (perfValues.length > 0) {
			System.out.print ("Performance Values: ");
			for (double p : perfValues) {
				System.out.print (p + " " );
			}
			System.out.println ("");
		}
	}
}
