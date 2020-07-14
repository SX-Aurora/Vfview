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

// Table of unique stacks

package util;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class TraceAdmin {

    public int          task, thread, traceID, sampleCount,
                        tasks, threads, stacksCount,
                        preciseCount, messageCount;
    private int         maxDepth;
    private int         position, posIncrement;
    public double       runTime, cycleTime, sampleTime;
    public long         initTime;
    public StackInfo[]  stackArray;
    public String       filename, pathname;
    public FunctionList functionList;
    public long         offsetPrecise, offsetMessages;

    int          mpiInfoDim, debug;
    String       versionID, date;

    private int nPerfValues;
    private String[] perfNames;
    private boolean[] perfIntegrated;

    RandomAccessFile raf;

    Pattern pattern = Pattern.compile( "([^/]+)$" );
    Matcher matcher;

    // Constructor as used by readTraceFile_std() in TraceInfo class
    // Read trace info from std file (used by TraceInfo class)
    public TraceAdmin(
                RandomAccessFile raf, int trace, int traceCount,
		FunctionList functionList, int debug )
	   throws IOException {

        this.raf          = raf;
        this.functionList = functionList;
	this.debug        = debug;
	traceID           = trace;

        pathname       = raf.readUTF();
        versionID      = raf.readUTF();
        date           = raf.readUTF();
        threads        = raf.readInt();
        thread         = raf.readInt();
        tasks          = raf.readInt();
        task           = raf.readInt();
        cycleTime      = raf.readDouble();
        runTime        = raf.readDouble();
        sampleTime     = raf.readDouble();
        sampleCount    = raf.readInt();    // Note: original sample count
        preciseCount   = raf.readInt();
        offsetPrecise  = raf.readLong();
        messageCount   = raf.readInt();
        offsetMessages = raf.readLong();

        matcher = pattern.matcher( pathname );
	if( matcher.find() ) filename = matcher.group( 1 );


	nPerfValues = raf.readInt();
	if (nPerfValues > 0) {
		perfNames = new String[nPerfValues];
		perfIntegrated = new boolean[nPerfValues];
		for (int i = 0; i < nPerfValues; i++) {
			perfNames[i] = raf.readUTF();
			perfIntegrated[i] = raf.readInt() != 0;
		}
	}


        if( debug>1 )
            System.out.println( 
              "-----------------------------------------------------"+
	      "\nTrace pathname:    "+pathname+
	      "\nTrace filename:    "+filename+
              "\nVersionID:         "+versionID+
              "\nDate:              "+date+
              "\nThreads:           "+threads+
              "\nThread:            "+thread+
              "\nTasks:             "+tasks+
              "\nTask:              "+task+
              "\nCycleTime:         "+cycleTime+
              "\nRunTime:           "+runTime+
              "\nSampleTime:        "+sampleTime+
              "\nSampleCount:       "+sampleCount+
              "\nMPI types dim:     "+mpiInfoDim );

        // if( debug>2 ) {
        //     System.out.println( "MPI types info:"   );
        //     for( int i=0; i<mpiInfoDim; i++ )
        //         if( mpiTypes[i].size > 0 )
        //             System.out.println( 
	// 		"                   "+
        //                 mpiTypes[i].name+": size="+
        //                 mpiTypes[i].size );
        // }
    }
    
    public void getStacksTable() 
           throws IOException     {

        stacksCount = raf.readInt();
	stackArray  = new StackInfo[stacksCount];

        if( debug>1 )
            System.out.println( 
              "-----------------------------------------------------"+
              "\nThread:            "+thread+
              "\nTask:              "+task+
              "\nStacksCount:       "+stacksCount+
	      "\nStacks:" );

	maxDepth = 0;
	for( int i=0; i<stacksCount; i++ ) {
	    int    stackID  = raf.readInt();
	    int     levels  = raf.readInt();
	    int     caller  = raf.readInt();
	    int     funcID  = raf.readInt();
	    boolean    mpi  = raf.readBoolean();
	    boolean openmp  = raf.readBoolean();
	    boolean precise = raf.readBoolean();
	    if( maxDepth < levels ) maxDepth = levels;

	    stackArray[stackID] = new StackInfo( stackID, levels,
	                                caller, funcID, mpi, openmp, precise );

            functionList.setAttributes( funcID, mpi, openmp, precise );

            if( debug>2 ) {
		System.out.println( 
                    "                   "+
		    stackID+","+levels+","+caller+","+
		    functionList.name( funcID ) );
            }
	}

	posIncrement = Math.max (maxDepth + 1, 5);
    }
    
    // Constructor as used by Multiplexer class
    public TraceAdmin( 
                String pathname, String versionID, String date,
                int trace, int task, int tasks, int thread, int threads, 
                double cycleTime, double runTime, double sampleTime,
		long initTime,
                int stacksCount, int sampleCount) {
	this.pathname     = pathname;
	this.versionID    = versionID;
	this.date         = date;
	this.traceID      = trace;
        this.task         = task;
	this.thread       = thread;
        this.tasks        = tasks;
	this.threads      = threads;
	this.cycleTime    = cycleTime;
	this.runTime      = runTime;
	this.initTime     = initTime;
	this.sampleTime   = sampleTime;
	this.stacksCount  = stacksCount;
	this.sampleCount  = sampleCount;
	this.functionList = functionList;
	stackArray        = new StackInfo[stacksCount];
	maxDepth          = 0;
	position          = 0;

        matcher = pattern.matcher( pathname );
	if( matcher.find() ) filename = matcher.group( 1 );
    }

    public void setPosition (int position) {
	    this.position = position;
    }

    public int getPosition () {
	    return position;
    }

    public void setMaxDepth (int maxDepth) {
	    this.maxDepth = maxDepth;
    }

    public int getMaxDepth () {
	    return maxDepth;
    }

    public int getPosInc () {
	    return posIncrement;
    }

    public int getNPerfValues () {
	    return nPerfValues;
    }

    public String[] getPerfNames () {
	    return perfNames;
    }

    public boolean perfIsIntegrated (int iPerf) {
	    return perfIntegrated[iPerf];
    }

    public boolean[] perfIsIntegrated () {
	    return perfIntegrated;
    }

}

	

