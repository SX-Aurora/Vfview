package util;

import java.io.*;
import java.util.*;

public class PerformanceSample {

	private class PerformanceElement {
		public double[] integratedValue;
		public double[] differentialValue;
		public String name;
		public int traceCount;

		public PerformanceElement (int count) {
			this.traceCount = count;
			integratedValue = new double[count];
			differentialValue = new double[count];
			for (int i = 0; i < count; i++) {
				integratedValue[i] = 0.;
				differentialValue[i] = 0.;
			}
		}

		private void read (DataInputStream in, int count) throws IOException {
			for (int i = 0; i < count; i++) {
				integratedValue[i] = in.readDouble();
			}
		}

		public void setDifference (double[] valuePrevious) {
			for (int i = 0; i < traceCount; i++) {
				differentialValue[i] = integratedValue[i] - valuePrevious[i];
			}
		}

		public void setDifferenceZero () {
			differentialValue = integratedValue.clone();
		}

		public double[] getIntegratedValues () {
			return integratedValue;
		}

		public double getIntegratedValue (int i) {
			return integratedValue[i];
		}


		public double getDifferentialValue (int i) {
			return differentialValue[i];
		}

		public int getTraceCount () {
			return traceCount;
		}

	}

	private Vector<PerformanceElement> perfTypes = new Vector<PerformanceElement>(4,2);
	private int nPerfTypes;
	public double mintime, maxtime, sampleTime;
	private double deltaT;
	private boolean intervalBegin;	

	public PerformanceSample (DataInputStream in, double mintime,
			double maxtime, double sampleTime,
			int count, int nPerfTypes,
			boolean[] showIntegrated) throws IOException {

		for (int iPerf = 0; iPerf < nPerfTypes; iPerf++) {
			PerformanceElement tmp = new PerformanceElement(count);
			tmp.read (in, count);
			perfTypes.add (tmp);
		}
		this.nPerfTypes = nPerfTypes;
		this.mintime = mintime;
		this.maxtime = maxtime;
		this.sampleTime = sampleTime;
		this.deltaT = -1.0;
		this.intervalBegin = false;
	}

	public void setSampleTime (double sampleTime) {
		this.sampleTime = sampleTime;
	}

	public void setTimeDelta (double deltaT) {
		this.deltaT = deltaT;
	}

	public double computeTime (int index) {
		return mintime + (double)index * sampleTime;
	}

	public int getNPerfValues () {
		return nPerfTypes;
	}

	public void setDifference (PerformanceSample pOther) {
		for (int iPerf = 0; iPerf < nPerfTypes; iPerf++) {
			perfTypes.elementAt(iPerf).setDifference (pOther.getIntegratedValues(iPerf));
		}
	}

	public void setDifference (double[] valuePrevious, int iPerf) {
		perfTypes.elementAt(iPerf).setDifference(valuePrevious);
	}

	public void setDifferenceZero () {
		for (int iPerf = 0; iPerf < nPerfTypes; iPerf++) {
			perfTypes.elementAt(iPerf).setDifferenceZero();
		}
	}

	public double getIntegratedValue (int perf_type, int trace) {
		PerformanceElement p = perfTypes.elementAt (perf_type);
		return p.integratedValue[trace];
	}

	public double[] getIntegratedValues (int perf_type) {
		PerformanceElement p = perfTypes.elementAt (perf_type);
		return p.integratedValue;
	}

	public double getDifferentialValue (int perf_type, int trace) {
		PerformanceElement p = perfTypes.elementAt (perf_type);
		return deltaT > 0 ? p.differentialValue[trace] / deltaT : -1.;
	}

	public double[] getDifferentialValuesForPerfType (int perf_type) {
		PerformanceElement p = perfTypes.elementAt (perf_type);
		double[] tmp = new double [p.getTraceCount()];
		for (int i = 0; i < p.getTraceCount(); i++) {
			tmp[i] = p.getDifferentialValue(i) / deltaT;
		}
		return tmp;
	}

	public double[] getDifferentialValuesForTrace (int iTrace) {
		double[] values = new double [nPerfTypes];
		for (int i = 0; i < nPerfTypes; i++) {
			values[i] = perfTypes.elementAt(i).getDifferentialValue(iTrace) / deltaT;
		}
		return values;
	}

	public double getValueDecide (int iPerf, int iTrace, boolean isIntegrated) {
		return isIntegrated ? getIntegratedValue (iPerf, iTrace) : getDifferentialValue (iPerf, iTrace);
	}

	public double[] getValuesDecide (int iTrace, boolean[] isIntegrated) {
		double[] values = new double [nPerfTypes];
		for (int i = 0; i < nPerfTypes; i++) {
			if (isIntegrated[i]) {
				values[i] = perfTypes.elementAt(i).getIntegratedValue(iTrace);
			} else {
				values[i] = perfTypes.elementAt(i).getDifferentialValue(iTrace) / deltaT;
			}
		}
		return values;
	}


	public void printDifferentialValues (int trace) {
		System.out.print ("(");
		for (int i = 0; i < nPerfTypes; i++) {
			System.out.print (perfTypes.elementAt(i).getDifferentialValue(trace) + " ");
		}
		System.out.println (")");
	}

	public double timeLeft (int index) {
		return mintime + (double)index * sampleTime;
	}

	public double timeRight (int index) {
		return mintime + (double)(index + 1) * sampleTime;
	}

	public int getNPerfTypes () {
		return perfTypes.size();
	}

	public int getNValues (int i) {
		PerformanceElement p = perfTypes.elementAt(i);
		return p.integratedValue.length;
	}

	public String getName (int perf_type) {
		PerformanceElement p = perfTypes.elementAt(perf_type);
		return p.name;
	}

	public void setIntervalBegin(boolean value) {
		intervalBegin = value;
	}
	
	public boolean isIntervalBegin () {
		return intervalBegin;
	}

}
