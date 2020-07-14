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

/*******************************************************************************

Vftrace - visualization of ftrace data

*******************************************************************************/

package vftrace;

import java.util.Stack;
import trace.TraceView;

public class Vftrace {

    public static void main(String[] args) {
	boolean   printDisclaimer = false;
        int       maxthreads = Integer.MAX_VALUE;
	int       debug =  0;
	double    tmin  =  0.;
	double    tmax  = -1.;
	Stack<String>     files = new Stack<String>();
		
        for (int i = 0; i < args.length; i++) {
	    if (args[i].equals("-maxthreads")) {
		maxthreads = Integer.parseInt (args[++i]);
	    } else if (args[i].equals("-mintime")) {
		tmin = Double.parseDouble (args[++i]);
	    } else if (args[i].equals("-maxtime")) {
		tmax = Double.parseDouble (args[++i]);
	    } else if (args[i].equals("-debug")) {
		debug = Integer.parseInt (args[++i]);
	    } else if( args[i].equals("-warranty") ||
                       args[i].equals("-w")){
		printDisclaimer = true;
	    } else if (args[i].equals("-help") ||
		       args[i].equals("-h")) {
		printHelp();
		System.exit(0);
	    } else {
	        files.push( args[i] );
	    }
        }

	Stack<String> tmp = new Stack<String>();
	while (!files.empty()) {
		tmp.push (files.pop());
	}
	files = tmp;


        if (debug > 0) {
            String classPath = System.getProperty( "java.class.path" );
	    System.out.println( "CLASSPATH="+classPath );
        }

        TraceView traceView = new TraceView (printDisclaimer, maxthreads,
	                                     files, debug, tmin, tmax);
        traceView.start();

    }

    public static void printHelp () {
	    System.out.println ("Generate a graphical representation of performance profiles.\n"
			        + "Options: \n"
				+ "-w, -warranty: Print build information.\n"
				+ "-maxthreads <n>: ???\n"
				+ "-mintime <n>: Mininum time to display.\n"
				+ "-maxtime <n>: Maximum time to display.\n"
				+ "-debug <level>: Activate debug output.\n");
    }
    
    public String getVersion() {
        return "not set";
    }
}
