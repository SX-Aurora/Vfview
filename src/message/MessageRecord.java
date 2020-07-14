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

// MPI message info - not used (yet)

package message;

import java.io.Serializable;

public class MessageRecord implements Serializable {
    public double timeStart, timeStop;
    public double timeStartSorted, timeStopSorted;
    public int    timeStartRecord, timeStopRecord;
    public int    count, id, source, dest, tag;
    public int    type, dir;

    public MessageRecord() {
        /* Empty record, all data zero */
    }

    public MessageRecord( MessageRecord mr ) {
        timeStart       = mr.timeStart;
        timeStop        = mr.timeStop;
        timeStartSorted = mr.timeStartSorted;
        timeStopSorted  = mr.timeStopSorted;
        timeStartRecord = mr.timeStartRecord;
        timeStopRecord  = mr.timeStopRecord;
        count           = mr.count;
        id              = mr.id;
        source          = mr.source;
        dest            = mr.dest;
        tag             = mr.tag;
        type            = mr.type;
        dir             = mr.dir;
    }
}

