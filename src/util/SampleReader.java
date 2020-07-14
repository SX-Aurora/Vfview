/*******************************************************************************
*                                                                              *
*   This file is part of Vfview.                                              *
*                                                                              *
*                                                                              *
*   Vftrace is free software; you can redistribute it and/or modify            *
*   it under the terms of the GNU General Public License as published by       *
*   the Free Software Foundation; either version 2 of the License, or          *
*   (at your option) any later version.                                        *
*                                                                              *
*   Vftrace is distributed in the hope that it will be useful,                 *
*   but WITHOUT ANY WARRANTY; without even the implied warranty of             *
*   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
*   GNU General Public License for more details.                               *
*                                                                              *
*   You should have received a copy of the GNU General Public License along    *
*   with this program; if not, write to the Free Software Foundation, Inc.,    *
*   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.                *
*                                                                              *
*******************************************************************************/

package util;

import java.io.*;

public class SampleReader {

    RandomAccessFile raf;
    private long     stackBase;
    private long     perfBase;
    long 	     recordSize;
    byte[]           inputBuffer;
    DataInputStream  inputStream;
    double           mintime, maxtime, sampletime;
    private int tracecount, samplecount;
    private int nPerfValues;
    private boolean[] showIntegrated;

    public SampleReader( RandomAccessFile raf, 
                         double mintime, double maxtime,
			 double sampletime, int samplecount,
                         long stackBase, long perfBase,
			 int tracecount, int nPerfValues) {
	this.raf        = raf;
	this.mintime    = mintime;
	this.maxtime    = maxtime;
	this.sampletime = sampletime;
	this.samplecount = samplecount;
	this.tracecount = tracecount;
	this.stackBase = stackBase;
	this.perfBase  = perfBase;
	this.nPerfValues      = nPerfValues;
	showIntegrated = new boolean [nPerfValues];
	for (int i = 0; i < nPerfValues; i++) {
		showIntegrated[i] = false;
	}
	recordSize   	= tracecount * (4 + 8 * nPerfValues); // <count> (Integers + Doubles)
	inputBuffer  	= new byte[(int)recordSize];
	inputStream  	= new DataInputStream (new ByteArrayInputStream (inputBuffer));
    }

    public MultiStackSample getMss (int index) throws IOException {
	long pos = stackBase + (long)index * recordSize;
	raf.seek (pos);
	int n = raf.read( inputBuffer );
	inputStream.reset();
	return new MultiStackSample( inputStream, mintime, maxtime, sampletime, tracecount, index );
    }

    public PerformanceSample getPerformanceSample (int index) throws IOException {
	    long pos = perfBase + (long)index * recordSize;
	    raf.seek (pos);
	    raf.read (inputBuffer);
            inputStream.reset ();
	    PerformanceSample p = new PerformanceSample (inputStream, mintime, maxtime, sampletime, 
			    tracecount, nPerfValues, showIntegrated);
	    return p;
    }

    public double getTime (int index) throws IOException {
	return mintime + index * sampletime;
    }
    
}
