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


// Multiplexed Stack Sample

package util;

import java.io.*;

public class MultiStackSample {
    private int[]   stackID;
    private int     index;
    //private int     xmin, xmax;
    private double  mintime, maxtime, sampletime;
    
    public MultiStackSample() {
	this.stackID = null;
	this.index   = 0;
    }
    
    public MultiStackSample (DataInputStream in, 
                             double mintime, double maxtime, double sampletime,
			     int count, int index)
      throws IOException {
        this.mintime    = mintime;
	this.maxtime    = maxtime;
	this.sampletime = sampletime;
	this.index      = index;
	read (in, count);
    }

    public MultiStackSample( DataInputStream in, int count )
      throws IOException {
        read (in, count);
    }

    private void read (DataInputStream in, int count)
      throws IOException {
	stackID = new int[count];
	for (int i = 0; i < count; i++) {
	    stackID[i] = in.readInt();
	}
    }

    public double computeTime () {
	    return mintime + (double)index * sampletime;
    }
    
    public double endTime() {
        return (computeTime () + sampletime);
    }

    public double endTime (int iInc) {
	    return (computeTime () + sampletime * iInc);
    }

    public boolean isVisible (double tmin, double tmax) {
        return (computeTime () + sampletime > tmin && computeTime() < tmax);
    }

    public String toString() {
        return( "time="+computeTime()+" delta="+sampletime+" id="+stackID );
    }

    public int getIndex () {
	    return index;
    }

    public int getStackID (int i) {
	    return stackID[i];
    }

    public void setStackID (int i, int id) {
	    stackID[i] = id;
    }

    public void nullifyStackID () {
	    stackID = null;
    }

    public boolean stackIdIsNull () {
	    return stackID == null;
    }
    
    public double getSampleTime () {
	    return this.sampletime;
    }

    public void setSampleTime (double delta) {
	    this.sampletime = delta;
    }

    public StackSample extractStackSample (int iTrace) {
	if (stackID == null) {
		return new StackSample (this.sampletime, 0., 0);
	} else {
		return new StackSample (this.sampletime, 0., stackID[iTrace]);
	}
    }
}
