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

package trace;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.Double;
import java.util.Stack;
import java.util.Collections;
import java.util.Comparator;

import message.MessageInfo;
import util.Constants;

public class OutputTraceFile extends DataOutputStream {

    String    filename;
    int       bufsize;
    long      filePointer;

    double  time;
    private int     id, type;
    private int   peer;
    private byte    dir;
    private int     count, tag;
    private Stack<MessageDetails>
                    messages;
    private int     outputCount;

    OutputTraceFile( String filename, int bufsize ) throws IOException {
        super( new BufferedOutputStream( 
                  new FileOutputStream( filename ), bufsize ) );
        this.filename = filename;
        this.bufsize  = bufsize;
        filePointer   = 0;
	outputCount  = 0;
	messages      = new Stack<MessageDetails>();
    }
    public void storeBlock( byte[] buf, int n ) throws IOException {
        write( buf, 0, n );
        filePointer += n;
    }
    public void storeBlock( byte[] buf, int off, int n ) throws IOException {
        write( buf, off, n );
        filePointer += n;
    }
    public void storeInt( int a ) throws IOException {
        writeInt( a );
        filePointer += 4;
    }
    public void storeLong( long a ) throws IOException {
        writeLong( a );
        filePointer += 8;
    }
    public void storeBoolean( boolean a ) throws IOException {
        writeBoolean( a );
        filePointer += 1;
    }
    public void storeFloat( float a ) throws IOException {
        writeFloat( a );
        filePointer += 4;
    }
    public void storeDouble( double a ) throws IOException {
        writeDouble( a );
        filePointer += 8;
    }
/*
    public void storeSample( StackSample a ) throws IOException {
        writeDouble( a.time    );
        writeFloat ( a.delta   );
        writeInt   ( a.stackID );
        filePointer += 16;
    }
    public void storeSample( double time, float delta, int stackID ) throws IOException {
        writeDouble( time    );
        writeFloat ( delta   );
        writeInt   ( stackID );
        filePointer += 16;
    }
    public void storeSample( long time, long delta, int stackID ) throws IOException {
        writeDouble( (double)time * 1.e-9d  );
        writeFloat ( (float)delta * 1.e-9f  );
        writeInt   ( stackID );
        filePointer += 16;
    }
*/
    public void storeMessageInfo( double timeStart, double timeStop, int id, int type, 
          int size, int count, int self, int peer, int tag, int dir) throws IOException {

        this.time  = time ;
        this.count = count;

	writeDouble( timeStart  );
        writeDouble( timeStop   );
        writeInt   ( count      );
        writeInt   ( id         );
	writeInt   ( self       );
	writeInt   ( peer       );
        writeInt   ( type       );
	writeInt   ( size       );
        writeInt   ( dir        );
        writeInt   ( tag        );
        filePointer += Constants.MESSAGE_INFO_RECORDSIZE;
        outputCount++;
    }

    public void storeMessageInfo( MessageInfo mi  )
                throws IOException {
	writeDouble( mi.timeStart       );
        writeDouble( mi.timeStop        );
        writeInt   ( mi.count           );
        writeInt   ( 0                  );
	writeInt   ( mi.self            );
	writeInt   ( mi.peer            );
        writeInt   ( mi.type            );
	writeInt   ( mi.type_size       );
        writeInt   ( mi.dir             );
        writeInt   ( mi.tag             );
        filePointer += Constants.MESSAGE_INFO_RECORDSIZE;
        outputCount++;
    }

    public void setMessageStartTime (double time, int id)
                throws IOException {
        this.time  = time ;
        this.id    = id;
    }

    public void writePrecise( double startTime, double stopTime, int id )
                throws IOException {
	writeDouble( startTime  );
        writeDouble( stopTime   );
        writeInt   ( id         );
        outputCount++;
    }

    public void storeString( String a ) throws IOException {
        int n = 0;
	int written_before, written_after, written_diff;
        try {
            byte[] b = a.getBytes("UTF8");
            n = b.length + 2;
        } catch( UnsupportedEncodingException e ) {
        }
	written_before = written;
        writeUTF( a );
	written_after = written;
	//written_diff = written_after - written_before;
	//System.out.println ("Written: " + written_diff);
        //filePointer += n;
	filePointer += written_after - written_before;
    }

    public int getOutputCount() {
        return outputCount;
    }

    public long getFilePointer() {
        return filePointer;
    }
    
    private class MessageTime {
        int    record;
        Double time;
        MessageTime( int record, double time ) {
            this.record  = record;
            this.time    = time;
        }
    }

    private class MessageDetails {
        int  id, peer;
        int    type;
	int    count, tag;
	byte   dir;
        double timeStart, timeStop;
	MessageDetails (int id, double timeStart, double timeStop, int count,
                        int type, int peer, int tag, byte dir) {
            this.id        = id   ;
            this.timeStart = timeStart;
            this.timeStop  = timeStop;
            this.count     = count;
            this.type      = type ;
            this.peer      = peer ;
            this.tag       = tag  ;
	    this.dir       = dir  ;
	}
    }
}
