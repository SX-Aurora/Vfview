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

package comm;

import java.io.*;

public class SendRecvPair implements Serializable {
    double  timeStart, timeStop, rate;
    int     type, tag, count, source, dest, dir, srcID, dstID;

    SendRecvPair() {
    }

    SendRecvPair( SendRecvPair p ) {
	timeStart = p.timeStart;
	timeStop  = p.timeStop;
	rate      = p.rate;
	type      = p.type;
	tag       = p.tag;
	count     = p.count;
	source    = p.source;
	dest      = p.dest;
	dir       = p.dir;
	srcID     = p.srcID;
	dstID     = p.dstID;
    }

    SendRecvPair( double ts, double te, 
	     int tp, int tg, int n, int src, int dst, int d, int sid, int did ) {
        timeStart = ts;
	timeStop  = te;
	type      = tp;
	tag       = tg;
	count     = n;
	source    = src;
	dest      = dst;
	dir       = d;
	srcID     = sid;
	dstID     = did;
	//rate      = 1.e-6 * (double) count / ( timeStart-timeStop );
    }

    boolean inInterval( double time ) {
	return timeStart <= time && time <= timeStop;
    }

}

