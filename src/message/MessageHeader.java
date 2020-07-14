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

// MPI message Header

package message;

import java.io.RandomAccessFile;
import java.io.IOException;

public class MessageHeader {
    public int    count;   // Nr of messages
    public long   base;    // File offset to messages
    public int    recsize; // Record size
    public RandomAccessFile
                  raf;

    public MessageHeader( RandomAccessFile raFile, long offset, int raRecsize ) {
        raf     = raFile;
        recsize = raRecsize;
	try {
            raf.seek( offset );
            count = raf.readInt();
	    base  = raf.getFilePointer();
	} catch( IOException exception ) {
	    exception.printStackTrace();
	}
    }
    
    public void seek( int record ) {
        try {
            raf.seek( base + record*recsize );
	} catch( IOException exception ) {
	    exception.printStackTrace();
	}
    }
    public void seek( int record, int offset ) {
        try {
            raf.seek( base + record*recsize + offset );
	} catch( IOException exception ) {
	    exception.printStackTrace();
	}
    }
/*
    public void getSortedStartTime( int record, int offset ) {
        try {
            raf.seek( base + record*Constants.MESSAGE_INFO_RECORDSIZE );
	} catch( IOException exception ) {
	    exception.printStackTrace();
	}
    }
*/
}

