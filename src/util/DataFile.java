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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

public class DataFile
{
    public  DataInputStream indata;
    private BufferedInputStream instream;
    private boolean littleEndian;
    public int filepos;

    FileInputStream infile = null;

    public DataFile( String datafilename, int inbufsize, boolean littleEndian ) 
           throws IOException
    {

        this.littleEndian = littleEndian;
        findFile( datafilename );
	if( infile == null ) return;
	if( datafilename.endsWith( ".gz" ) ) {
	    instream = new BufferedInputStream( 
		         new GZIPInputStream( infile ), inbufsize );
        } else {
	    instream = new BufferedInputStream( infile, inbufsize );
	}
	indata = new DataInputStream( instream );
	filepos = 0;
    }

    private void findFile( String datafilename ) {
	// Find data file, original or compressed
	for( ;; ) {
	    try {
		infile = new FileInputStream( datafilename );
		break;
	    } catch( FileNotFoundException f ) {
		System.out.println( "not found: " + datafilename );
		// Fixme: must throw exception here
		if( datafilename.endsWith( ".gz" ) ) return;
		datafilename += ".gz";
	    }
	}
    }
    
    private int    byteswap( int x ) {
      return x << 24 |
             x <<  8 & 0x00ff0000 |
             x >>  8 & 0x0000ff00 |
             x >>>24 ;
    }

    private long   byteswap( long x ) {
      return x << 56 |
             x << 40 & 0x00ff000000000000L |
             x << 24 & 0x0000ff0000000000L |
             x <<  8 & 0x000000ff00000000L |
             x >>  8 & 0x00000000ff000000L |
             x >> 24 & 0x0000000000ff0000L |
             x >> 40 & 0x000000000000ff00L |
             x >>>56 ;
    }
    
    public void readFully( byte[] buf, int offset, int length ) 
    //public void readFully( byte[] buf ) 
           throws IOException
    {
        //indata.readFully( buf, offset, length );
        indata.readFully( buf );
	filepos += length;
	//if (indata == null) {
	//		System.out.println ("INDATA DOES NOT EXIST");
	//} else {
	//		System.out.println ("INDATA EXISTS!");
	//}
	//int length = indata.available ();
	//System.out.printf ("SIZE OF INDATA: %d\n", length);
	//byte[] buf2 = new byte[length];
        //indata.readFully( buf );
        //indata.readFully( buf2 );
    }
    public int read( byte[] buf, int offset, int length ) 
           throws IOException
    {
	filepos += length;
        return indata.read( buf, offset, length );
    }
    public byte readByte() 
           throws IOException
    {
	filepos += 1;
        return indata.readByte();
    }
    public int readInt() 
           throws IOException
    {
	filepos += 4;
        if( littleEndian ) {
		//int foo = indata.readInt();
		//System.out.println ("Before byteswap: " + foo);
		//return byteswap(foo);
		return byteswap(indata.readInt());
	}else {
 		return          indata.readInt() ;
	}
    }
    public long readLong() 
           throws IOException
    {
	filepos += 8;
        if( littleEndian ) return byteswap(indata.readLong());
        else               return          indata.readLong() ;
    }
    public double readDouble() 
           throws IOException
    {
	filepos += 8;
        if( littleEndian ) return Double.longBitsToDouble( byteswap(indata.readLong()));
        else               return Double.longBitsToDouble(          indata.readLong()) ;
    }
    public float readFloat() 
           throws IOException
    {
	filepos += 4;
        if( littleEndian ) return Float.intBitsToFloat( byteswap(indata.readInt()));
        else               return Float.intBitsToFloat(          indata.readInt()) ;
    }
    public void skipBytes( long count ) 
           throws IOException
    {
	filepos += (int)count;
        indata.skipBytes( (int)count );
    }
    public void close() 
           throws IOException
    {
        indata.close();
    }

}
