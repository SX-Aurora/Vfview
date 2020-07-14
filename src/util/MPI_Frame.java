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

import javax.swing.JFrame;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Vector;
import java.util.Iterator;

import trace.TraceGraphics;
import message.MessageInfo;

public class MPI_Frame {
    private MPI_Table table;
    private JFrame frame;
    private TraceGraphics tg;
    public boolean isOpen;

    public MPI_Frame( TraceGraphics tg ) {
        this.tg = tg;
	frame = new JFrame( "MPI Panel" );
	table = new MPI_Table();
        table.setOpaque(true);
        frame.setContentPane(table);
	frame.addWindowListener( new WindowListener() {
	    public void windowOpened     ( WindowEvent e ) {
		System.out.println( "MPI frame Opened" );
	        isOpen = true;
		updateScreen();
	    }
	    public void windowClosing    ( WindowEvent e ) {
		System.out.println( "MPI frame closing" );
	        isOpen = false;
		updateScreen();
	    }
	    public void windowActivated  ( WindowEvent e ) {
	        if( !isOpen ) {
		    System.out.println( "MPI frame activated" );
	            isOpen = true;
		    updateScreen();
		}
	    }
	    public void windowDeactivated( WindowEvent e ) {}
	    public void windowIconified  ( WindowEvent e ) {}
	    public void windowDeiconified( WindowEvent e ) {}
	    public void windowClosed     ( WindowEvent e ) {}
	} );
	frame.pack();
	frame.setLocation (1120, 415);
	frame.setVisible (false);
    }
    
	
    public void update( Vector<MessageInfo> messages ) {
        Iterator it = messages.iterator();
        table.clear();
        while( it.hasNext() ) {
            MessageInfo mi = (MessageInfo)it.next();
            String peer   = new String( (mi.dir == 1 ? "<": ">") + mi.peer );
            double mbytes = (double)mi.type_size * (double)mi.count / (1024.*1024.);
            table.update( mi.timeStart, mi.timeStop, mi.self, mi.dir, mi.peer,
                          mi.count, mbytes, dataType(mi.type), mi.tag, mi.index );
        }
    }

    private void updateScreen() {
	//tg.traceNeedsMoreSamples ();
    }

    public void clear() {
        table.clear();
    }

    public boolean isVisible() {
	return isOpen;
    }
    
    public void setVisible(boolean state) {
	frame.setVisible(state);
    }
    
    public String dataType(int number) {
       String[] MPI_Type_Names = 
          new String[] {"MPI_CHAR",
                        "MPI_SHORT",
                        "MPI_INT",
                        "MPI_LONG",
                        "MPI_LONG_LONG_INT",
                        "MPI_LONG_LONG",
                        "MPI_SIGNED_CHAR",
                        "MPI_UNSIGNED_CHAR",
                        "MPI_UNSIGNED_SHORT",
                        "MPI_UNSIGNED",
                        "MPI_UNSIGNED_LONG",
                        "MPI_UNSIGNED_LONG_LONG",
                        "MPI_FLOAT",
                        "MPI_DOUBLE",
                        "MPI_LONG_DOUBLE",
                        "MPI_WCHAR",
                        "MPI_C_BOOL",
                        "MPI_INT8_T",
                        "MPI_INT16_T",
                        "MPI_INT32_T",
                        "MPI_INT64_T",
                        "MPI_UINT8_T",
                        "MPI_UINT16_T",
                        "MPI_UINT32_T",
                        "MPI_UINT64_T",
                        "MPI_C_COMPLEX",
                        "MPI_C_FLOAT_COMPLEX",
                        "MPI_C_DOUBLE_COMPLEX",
                        "MPI_C_LONG_DOUBLE_COMPLEX",
                        "MPI_INTEGER",
                        "MPI_LOGICAL",
                        "MPI_REAL",
                        "MPI_DOUBLE_PRECISION",
                        "MPI_COMPLEX",
                        "MPI_CHARACTER",
                        "MPI_BYTE",
                        "MPI_PACKED"};
       if (number == -1) {
          return "MPI_DERIVED_TYPE";
       }
       if (number < 0 || number >= MPI_Type_Names.length) {
          System.out.println("Invalid MPI_Type index!");
          System.out.println("Falling back on NULL type!");
          return "MPI_UNDEFINED_TYPE";
       }
       return MPI_Type_Names[number];
        //return tg.getTraceDataTypeName(number);
    }
    // public int dataSize( int number ) {
    //     return tg.trace.traceAdmin[0].mpiTypes[number].size;
    // }
}
