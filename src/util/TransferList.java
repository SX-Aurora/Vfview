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

// Hash of active transfers

package util;

import java.util.Hashtable;
import util.Transfer;
import util.TransferInc;

public class TransferList extends Hashtable<Integer,Transfer> {
	
    final double tolerance = 1e-8;

    public TransferList(int length)
    {
        super(length);
    }
    
    public TransferList(int length, int inc)
    {
        super(length,inc);
    }
    
    // put() maintains the active transfer list.
    //
    // Argument rate is positive at the tranfer start, negative at the end.
    // If no entry for the src,dst pair the rate is added as new entry and
    // we are done.
    // Else, the rate is added to the value present.
    // If the sum is zero, all transfers between src and dst have ended
    // and the entry is removed.
    // Else, the rate is replaced by the sum.
    // put() returns the current rate.

    public double put (TransferInc t) {
        return put (t.rate, t.src, t.dst, t.dir);
    }

    public double put (Transfer t) {
        return put (t.rate, t.src, t.dst, t.dir);
    }

    public double put (double rate, int src, int dst, int dir) {
        Integer key = encodeKey (src, dst, dir);
        Transfer tr = (Transfer)super.get(key);
        if (tr != null) {
            rate += tr.rate;
            if (Math.abs(rate) < tolerance) {
		    super.remove (key);
            }
        }

        if(rate > tolerance) {
		super.put( key, new Transfer(rate, src, dst, dir) );
	} else if (rate < -tolerance){
		System.out.println( 
                           "TransferList.put() ERROR Negative rate: "+rate);
        }
        return rate;
    }

    private Integer encodeKey (int src, int dst, int dir) {
        int intKey = (((src<<15) | dst ) << 2 ) | (dir + 1);
        return intKey;
    }

    // get() retrieves tranfer rate for a key and returns
    // rate, source and destination a Transfer object.

    public Transfer get (Integer key) {
        return (Transfer)super.get(key);
    }

    public Transfer get (int src, int dst, int dir ) {
        Integer key = encodeKey (src, dst, dir);
        return (Transfer)super.get(key);
    }

}

