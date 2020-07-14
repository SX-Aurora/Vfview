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

// MPI message info

package message;

public class MessageInfo {
    public int    dir, count, type, self, peer, tag, index, id,
                  type_size, msgCount, indexSorted;
    public long   base;
    public double timeStart, timeStop, timeStartSorted;

    public MessageInfo() {
    }

    public MessageInfo( MessageInfo mi ) {
        timeStart       = mi.timeStart;
        timeStop        = mi.timeStop;
        count           = mi.count;
        type            = mi.type;
	type_size       = mi.type_size;
        self            = mi.self;
        peer            = mi.peer;
        tag             = mi.tag;
        dir             = mi.dir;
        index           = mi.index;
        indexSorted     = mi.indexSorted;
        timeStartSorted = mi.timeStartSorted;
        msgCount        = mi.msgCount;
        base            = mi.base;
    }

    public MessageInfo( double timeStart, double timeStop, int count, int type,
                        int type_size, int self, int peer, int tag, int dir ) {
        this.timeStart = timeStart;
        this.timeStop  = timeStop;
        this.count     = count;
        this.type      = type;
	this.type_size = type_size;
        this.self      = self;
        this.peer      = peer;
        this.tag       = tag;
        this.dir       = dir;
    }

    public boolean eof() {
	return index >= msgCount || msgCount == 0;
    }
}

