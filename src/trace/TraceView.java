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

import java.util.Stack;

public class TraceView extends Thread {

    public TraceFrame traceFrame;

    TraceInfo  traceInfo;
    boolean    printDisclaimer;
    int        maxthreads;
    int        debug;
    double     tmin;
    double     tmax;
    Stack      files;

    public TraceView(boolean printDisclaimer, int maxthreads,
	       Stack<String> files, int debug, double tmin, double tmax ) {
        super();
	this.printDisclaimer	= printDisclaimer;
	this.maxthreads		= maxthreads;
	this.debug		= debug;
	this.tmin		= tmin;
	this.tmax		= tmax;
	this.files		= files;

	traceInfo = new TraceInfo (files, debug, tmin, tmax );
	traceInfo.traceView = this;
    }
    
    public void run() {
        traceFrame = new TraceFrame(printDisclaimer, maxthreads,
	                            files, debug, tmin, tmax, traceInfo);
	if (!printDisclaimer) traceInfo.start();
    }

    private synchronized void kickReader() {
	notifyAll();
    }

}
