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


// Function information.

package util;

import java.util.*;
import java.util.regex.*;

public class FunctionList {

    private class FunctionAttibutes {
        boolean mpi, openmp, precise;
	FunctionAttibutes( boolean mpi, boolean openmp, boolean precise ) {
	    this.mpi     = mpi;
	    this.openmp  = openmp;
	    this.precise = precise;
	}
    }

    Hashtable<String,Integer> functionsByName;
    Vector<String> functionsByNumber;
    Vector<FunctionAttibutes> attributes;
    int count;
    
    public FunctionList() {
        functionsByName   = new Hashtable<String,Integer>( 1009, 0.5f );
	functionsByNumber = new Vector<String>( 1009, 500 );
	attributes        = new Vector<FunctionAttibutes>( 1009, 500 );
	count = 0;
    }
    
    public int index (String funcName) {

        int functionID;
	Integer entry = (Integer)functionsByName.get( funcName );
	if( entry == null) {
	    functionID = count;
	    functionsByName.put (funcName, count++);
	    functionsByNumber.addElement( funcName );
	    attributes.addElement( null );
	} else {
            functionID = entry.intValue();
	}
        return( functionID );
    }    

    public void setAttributes (int id, boolean mpi, boolean openmp, boolean precise) {
	if (attributes.size () <= id)
	       attributes.setSize (id + 1);	
	attributes.setElementAt( new FunctionAttibutes( mpi, openmp, precise ), id );
    }

    public boolean isParallel (String name) {
	return (isParallel( index(name)));
    }

    public boolean isParallel (int i) {
	FunctionAttibutes fa = (FunctionAttibutes) attributes.elementAt( i );
	return ( fa.mpi || fa.openmp );
    }

    public boolean isPrecise (int i) {
	FunctionAttibutes fa = (FunctionAttibutes) attributes.elementAt( i );
	return fa.precise;
    }

    public String name (int index) {
        return (String)functionsByNumber.elementAt(index);
    }

    // Used for function color codes
    public int hashcode (int index) {
        String s = name(index);
	return (s.hashCode());
    }

    public int size() {
        return( count );
    }
}

	

