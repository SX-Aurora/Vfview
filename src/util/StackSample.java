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

public class StackSample {
    public double time;
    public float  delta;
    public int    stackID;
    int    index;
    
    public StackSample() {
        time	= 0.;
	delta	= 0.f;
	stackID = 0;
	index   = 0;
    }
    
    /*
    public StackSample( long time, long delta, int stackID ) {
        this.time    = 1.e-9d * (double)time;
	this.delta   = 1.e-9f * (float)delta;
	this.stackID = stackID;
	this.index   = 0;
    }
    */
    
    public StackSample( double time, double delta, int stackID ) {
        this.time    = time;
	this.delta   = (float)delta;
	this.stackID = stackID;
	this.index   = 0;
    }
    
    public StackSample( double time, float delta, int stackID ) {
        this.time    = time;
	this.delta   = delta;
	this.stackID = stackID;
	this.index   = 0;
    }
    
    public StackSample( double time, float delta, int stackID, int index ) {
        this.time    = time;
	this.delta   = delta;
	this.stackID = stackID;
	this.index   = index;
    }
    public StackSample( double time, double delta, int stackID, int index ) {
        this.time    = time;
	this.delta   = (float)delta;
	this.stackID = stackID;
	this.index   = index;
    }
    public StackSample( StackSample samp ) {
        time	= samp.time;
	delta	= samp.delta;
	stackID = samp.stackID;
	this.index   = index;
    }
    
    public StackSample( StackSample samp, int i ) {
        time    = samp.time;
	delta   = samp.delta;
	stackID = samp.stackID;
	index   = i;
    }

    public StackSample( RandomAccessFile raf, int i ) {
        read( raf );
	index   = i;
    }

    public StackSample( RandomAccessFile raf ) {
        read( raf );
    }
    
    public void read( RandomAccessFile raf ) {
	try {
	    time    = raf.readDouble();
	    delta   = raf.readFloat ();
	    stackID = raf.readInt   ();
	} catch( IOException e ) {
	    System.out.println( "Cannot read RandomAccessFile:"+e );
	    e.printStackTrace();
	    return;
	}
    }
    
    public void write( RandomAccessFile raf ) {
	try {
            raf.writeDouble( time    );
            raf.writeFloat ( delta   );
            raf.writeInt   ( stackID );
	} catch( IOException e ) {
	    System.out.println( "Cannot write RandomAccessFile:"+e );
	    e.printStackTrace();
	    return;
	}
    }
    
    public double endTime() {
        return( time+delta );
    }

    public boolean isVisible( double tmin, double tmax ) {
        return( time+delta > tmin && time < tmax );
    }

    public String toString() {
        return( "time="+time+" delta="+delta+" id="+stackID );
    }

}
