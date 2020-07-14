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

// Profile entry

package profile;

import java.util.*;
import java.text.*;
import util.*;

public class Profile {
    public ProfileEntry[] entries;
    public int            size;
    public double         totalTime, timeStart, timeStop;
    public boolean        active;

    public Profile( int size, double timeStart, double timeStop ) {
        this.size      = size;
	this.timeStart = timeStart;
	this.timeStop  = timeStop;
	totalTime = 0.;
	active    = true;
	entries   = new ProfileEntry[ size ];
	for( int i=0; i<size; i++ ) {
	    entries[i] = new ProfileEntry( i );
	}
    }
    public void sort() {
	Arrays.sort( entries );
    }

    public void setActive( boolean active ) {
	this.active = active;
    }

    public void add( int i, double delta ) {
	if( i < 0 || i > entries.length ) return;
        entries[i].time += delta;
	totalTime += delta;
    }
    public void setMax() {
        for( int i=0; i<size; i++ ) {
	    entries[i].time = Double.MAX_VALUE;
	}
    }
    public void scaleTime( double totalTime ) {
        double scale = 1. / totalTime;
        for( int i=0; i<size; i++ ) {
	    entries[i].time *= scale;
	}
    }
    public void findMax( int i, double time ) {
	if( entries[i].time < time ) entries[i].time = time;
    }
    public void findMin( int i, double time ) {
	if( entries[i].time > time ) entries[i].time = time;
    }
/*
    public void print() {
	TraceAdmin t = trace;
	StackInfo[] stackArray = t.stackArray;
	double timeScale = 1. / totalTime;
	DecimalFormat dec = new DecimalFormat( "#0.00%" );
	for( int j=0; j<size; j++ ) {
	    int id = entries[j].id;
	    if( entries[j].time > 0.) {
		double p = entries[j].time * timeScale;
		System.out.print( id + ": " + dec.format(p) + " " );
		for( int i=stackArray[id].depth-1; i>=0; i-- ) {
		    int number  = stackArray[id].functionID;
		    String name    = t.functionList.name( number );
		    System.out.print( name + "<" );
		    id      = stackArray[id].caller;
		}
		System.out.print( "\n" );
	    }
	}
    }
*/
}

