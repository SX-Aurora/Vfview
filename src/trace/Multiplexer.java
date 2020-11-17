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

// Read the raw experiment data files, resample and multiplex
// the samples.

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.util.EmptyStackException;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.Iterator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import message.MessageInfo;
import util.Constants;
import util.DataFile;
import util.FunctionList;
import util.MPI_Types;
import util.StackInfo;
import util.TraceAdmin;
import util.Transfer;
import util.TransferInc;
import util.TransferList;
import util.TransferSnapshot;

public class Multiplexer {

    private class Trace {
        DataFile         indata;
        OutputTraceFile  tmpMessage, tmpPrecise;
	String           traceFilename, tmpMsgFilename, tmpPreFilename;
        TraceAdmin       admin;
	private int      count;
        int              samplesRead, stackID, MPImsgID, stacksCount,
	                 task, thread, preciseCount,
                         messageCount, tracenum;
        long skip, sampleOffset;
        double           time, prevTime, offsetTime, preciseTime, sampleTime,
                         firstTime;
	int              nPerfValues;
	double[]         perfValues;
        boolean          precise, moreSamples;
	long             offsetPrecise, offsetMessages, offsetMessageIndex;
	Trace            prev, next;
	int[]            commonStackID;
        Stack<Double>    startTimes;
    }
    
    Trace             firstTrace;
    Trace[]           traceList;
    TraceAdmin[]      traceAdmin;
    FunctionList      functionList;
    OutputTraceFile   outdata;
    String            stdFilename;
    int               traceCount, muxSampleCount, debug;
    int               perfSampleCount;
    long              offsetTraceDetails=0,
                      offsetFunctionTable=0,
		      offsetStackSamples=0,
		      offsetMessages=0,
		      offsetPrecise=0,
                      offsetTransfers=0,
                      offsetProfile=0;
    double            time, prevTime, timeInterval, mintime, maxtime,
                      nextSampleTime, runTime = 0.;
//     private final int SID_IMASK     = 0xf0000000, /* Sample ID mask (short) */
//                       SID_ENTRY     = 0x10000000, /* Function entry */
//                       SID_EXIT      = 0x20000000, /* Function exit  */
//                       SID_LONGTIME  = 0x80000000, /* Long delta time flag bit */
//                       SID_SHORTMSG  = 0x30000000, /* Short message info OBSOLETE  */
//                       SID_LONGMSG   = 0x40000000, /* Long message info OBSOLETE */
//                       SID_MESSAGE   = 0x40000000, /* Message info   */
// 		      MASK_DIR      = 0x08000000, /* Direction mask (0-1) */
//                       MASK_TYPE     = 0x07f00000, /* Type index mask (0-127) */
//                       MASK_PEER     = 0x000fffff, /* Peer mask (0-1048575) */
//                       SHIFT_DIR     = 27,         /* Direction right shift */
//                       SHIFT_TYPE    = 20,         /* Type index right shift */
    private final int INBUFSIZE     = 64*1024,
		      OUTPUTBUFSIZE = 64*1024,
		      SID_ENTRY = 0,
		      SID_EXIT = 1,
	 	      SID_MESSAGE = 2;


    private boolean endian = true;

    private int VFD_VERSION = 1;
    int this_vfd_version;

    private int    byteswap( int x ) {
      return x << 24 |
             x <<  8 & 0x00ff0000 |
             x >>  8 & 0x0000ff00 |
             x >>>24 ;
    }


    String call = new String( "call" );
    String exit = new String( "exit" );
    
    //Key: function.caller
    //Content: common stack IDs
    Hashtable<String,Integer>
                      commonStackIDs = new Hashtable<String,Integer>( 1000 );
    int               commonStackIDcounter = 0;
    Stack<StackInfo>  stackInfo = new Stack<StackInfo>();
    RandomAccessFile  raf;

    int               messageCount = 0;
    ArrayList<TransferInc>  transfers = new ArrayList<TransferInc>();
    
    private class CompareByTime implements Comparator<TransferInc> 
    { 
        public int compare(TransferInc a, TransferInc b) 
        { 
            double diff = a.time - b.time;
            return diff < 0. ? -1 : diff == 0. ?  0 : 1; 
        } 
    } 

    public int filepos;

    Multiplexer( Stack traceFiles, double tmin, double tmax,
                 int deb )
    throws IOException
    {
        byte[]  buf = new byte[40];
        String  fileName, versionID, date, root=null;
        int     tasks, threads, stacksCount;
        int     function_sampleCount, message_sampleCount, sampleCount;
        long    stacksOffset, sampleOffset, profileOffset;
        double  cycleTime, runTime;
	long    initTime;
        long    interval, longDummy=0;
        String  firstName    = (String) traceFiles.elementAt(0);
        Pattern patRoot      = Pattern.compile( "(.*/)*([^/]*)_\\d+\\.vfd$" );
        Pattern patTemp      = Pattern.compile( "^[^.]*" );
        Pattern patMPI       = Pattern.compile( "(^mpi_|^MPI_)" );
        Pattern patOpenMP    = Pattern.compile( "\\$\\d+$" );
        Matcher matRoot      = patRoot.matcher( firstName );

        mintime = tmin;
	maxtime = tmax;
	debug	= deb;

	traceCount     = traceFiles.size();
        muxSampleCount = 0;
	perfSampleCount = 0;

        functionList = new FunctionList();
        traceAdmin   = new TraceAdmin[traceCount];

        // Output file for multiplexed data
	if (matRoot.find() ) root = matRoot.group (2);
	if (matRoot.group(1) != null) {
		stdFilename = matRoot.group (1) + root + ".std";
	} else {
		stdFilename = root + ".std";
	}
	outdata = new OutputTraceFile (stdFilename, OUTPUTBUFSIZE);
	outdata.storeInt ( traceCount           );
	outdata.storeInt ( messageCount         ); // Dummy: updated later
	outdata.storeLong( offsetTraceDetails   ); // Dummy: updated later
	outdata.storeLong( offsetFunctionTable  ); // Dummy: updated later
	outdata.storeLong( offsetStackSamples   ); // Dummy: updated later
	outdata.storeLong( offsetPrecise        ); // Dummy: updated later
	outdata.storeLong( offsetMessages       ); // Dummy: updated later
	outdata.storeLong( offsetTransfers      ); // Dummy: updated later
	outdata.storeLong( offsetProfile        ); // Dummy: updated later

	offsetTraceDetails    = outdata.getFilePointer();

        time              = 0.;
        prevTime          = 0.;
        traceList         = new Trace[traceCount];
	firstTrace        = null;

	filepos = 0;
        for (int trace = 0; trace < traceCount; trace++) {
            Trace t = new Trace();

            t.traceFilename = (String) traceFiles.elementAt(trace);
            
	    // Scratch file for each trace
	    if (matRoot.group(1) != null) {
	    	t.tmpMsgFilename = matRoot.group (1) + root + "." + trace + ".msg";
	    	t.tmpPreFilename = matRoot.group (1) + root + "." + trace + ".pre";
	    } else {
	        t.tmpMsgFilename = root + "." + trace + ".msg";
	        t.tmpPreFilename = root + "." + trace + ".pre";
	    }
	    t.tmpMessage = new OutputTraceFile (t.tmpMsgFilename, OUTPUTBUFSIZE);
	    t.tmpPrecise = new OutputTraceFile (t.tmpPreFilename, OUTPUTBUFSIZE);

            traceList[trace] = t;
            t.tracenum       = trace;
            t.samplesRead    = 0;
            t.count          = 0;
            t.offsetTime     = 0.;
            t.prevTime       = 0.;
            t.time           = 0.;
	    t.MPImsgID       = 0;
	    t.precise        = false;
	    t.prev           = null;
	    t.next           = null;
            t.indata         = new DataFile( 
                                    t.traceFilename, INBUFSIZE, endian );
            t.startTimes     = new Stack<Double>();
	    
	    this_vfd_version = t.indata.readInt ();
	    if (this_vfd_version != VFD_VERSION) {
		System.out.println ("Error: Input vfd file "+
				    t.traceFilename +
				    " (version "+ this_vfd_version +
				    ") does not have the most recent version "+
				    VFD_VERSION);
	        System.exit(-1);
            }
            t.indata.readFully( buf, 0, 40 );

            versionID     = new String( buf,  0, 16 );
            date          = new String( buf, 16, 24 );
            interval      = t.indata.readLong();
            threads       = t.indata.readInt();
	    if( threads > 0xffff ) {
		System.out.println ("New input data");
	        t.indata.close();
		endian = !endian;
		t.indata = new DataFile( t.traceFilename, INBUFSIZE, endian );
                t.indata.readFully( buf, 0, 40 );
                interval = t.indata.readLong();
                threads  = t.indata.readInt();
	    }
            t.thread      = t.indata.readInt();
            tasks         = t.indata.readInt();
            t.task        = t.indata.readInt();
            cycleTime     = t.indata.readDouble();
	    initTime      = t.indata.readLong();
            runTime       = t.indata.readDouble();
            function_sampleCount  = t.indata.readInt();
            message_sampleCount   = t.indata.readInt();
            sampleCount   = function_sampleCount + message_sampleCount;
            stacksCount   = t.indata.readInt();
	    System.out.println ("stacksCount: " + stacksCount);
            stacksOffset  = t.indata.readLong();
            sampleOffset  = t.indata.readLong();
            profileOffset = t.indata.readLong();
	    
	    // Default maxtime: full execution time
	    if( maxtime == -1. ) maxtime = runTime;

            t.sampleTime   = interval * 1.0e-6;
	    t.stacksCount  = stacksCount;
	    t.sampleOffset = sampleOffset;
	    t.skip         = stacksOffset - sampleOffset;

            t.preciseCount   = 0; /* updated later */
            t.messageCount   = 0; /* updated later */
            t.offsetPrecise  = 0; /* updated later */
            t.offsetMessages = 0; /* updated later */
	    
	    this.runTime = Math.max( this.runTime, runTime );
	    
	    outdata.storeString( t.traceFilename );
	    outdata.storeString( versionID    );
	    outdata.storeString( date         );
	    outdata.storeInt   ( threads      );
	    outdata.storeInt   ( t.thread     );
	    outdata.storeInt   ( tasks        );
	    outdata.storeInt   ( t.task	      );
	    outdata.storeDouble( cycleTime    );
	    outdata.storeDouble( runTime      );
	    outdata.storeDouble( t.sampleTime );
	    outdata.storeInt   ( sampleCount  );

            t.offsetMessageIndex = outdata.getFilePointer();
	    outdata.storeInt   ( t.preciseCount   ); // Dummy: updated later
	    outdata.storeLong  ( t.offsetPrecise  ); // Dummy: updated later
	    outdata.storeInt   ( t.messageCount   ); // Dummy: updated later
	    outdata.storeLong  ( t.offsetMessages ); // Dummy: updated later



            int n;

	    byte[] perf_buf = new byte[32];
	    t.nPerfValues = t.indata.readInt();
	    t.perfValues = new double[t.nPerfValues];
	    for (int i = 0; i < t.nPerfValues; i++) {
	            t.perfValues[i] = 0.;
	    }
	    outdata.storeInt (t.nPerfValues);
	    for (int i = 0; i < t.nPerfValues; i++) {
	            t.indata.readFully (perf_buf, 0, 32);
	            for (n = 0; n < 32 && perf_buf[n] != 0; n++);
	            String s = new String (perf_buf, 0, n);
	            outdata.storeString (s);
	            // Integer indicating integrated or differential performance counters
	            outdata.storeInt (t.indata.readInt());
	    }


            traceAdmin[trace] = new TraceAdmin(
                t.traceFilename, versionID, date, 
                trace, t.task, tasks, t.thread, threads,
                cycleTime, runTime, t.sampleTime,
		initTime, stacksCount, sampleCount);

            t.admin = traceAdmin[trace];

            if( debug>1 ) {
                System.out.println( 
                  "-----------------------------------------------------"+
		  "\nTrace filename:    "+t.traceFilename+
                  "\nVersionID:         "+versionID+
                  "\nDate:              "+date+
                  "\nThreads:           "+threads+
                  "\nThread:            "+t.thread+
                  "\nTasks:             "+tasks+
                  "\nTask:              "+t.task+
                  "\nCycleTime:         "+cycleTime+
                  "\nRunTime:           "+runTime+
                  "\nSampleTime:        "+t.sampleTime+
                  "\nSampleCount:       "+sampleCount+
                  "\nStacksCount:       "+stacksCount+
                  "\nStacksOffset:      "+stacksOffset+
                  "\nSampleOffset:      "+sampleOffset+
                  "\nProfileOffset      "+profileOffset);
		// Temporarily unavailable and probably deacitvated in future.
                // if( debug>1 ) {
                //     System.out.println( "MPI types info:"   );
                //     for( int i=0; i<mpi_info_dim; i++ )
                // 	if( mpiTypes[i].size > 0 )
                //             System.out.println( 
		// 	        "                   "+
                //                 mpiTypes[i].name+": size="+
                //                 mpiTypes[i].size );
		// }
            }
        }

	for( int trace=0; trace<traceCount; trace++ ) {
	    Trace t = traceList[trace];

	    t.commonStackID = new int[t.stacksCount];

            if( debug>2 )
                System.out.println( 
                  "-----------------------------------------------------"+
		  "\nTrace filename:    "+t.traceFilename+
                  "\nThread:            "+t.thread+
                  "\nTask:              "+t.task+
                  "\nStacks:"                                              );

            // Skip to stack info
            t.indata.skipBytes( t.skip );
            for( int i=0; i<t.stacksCount; i++ ) {
                int stackID = t.indata.readInt();
                int levels  = t.indata.readInt();
                int caller  = t.indata.readInt();
                int len     = t.indata.readInt();
		String functionName;
		boolean precise;
        	if( debug>5 )
		    System.out.println( "stackID="+stackID+
		                	" levels="+levels +
					" caller="+caller +
					" len   ="+len      );

                byte namebuf[] = new byte[len];
                t.indata.readFully( namebuf, 0, len );
                functionName  = new String( namebuf );

		if( functionName.endsWith( "*" ) ) {
                    functionName  = new String( namebuf, 0, len-1 );
		    precise = true;
		} else {
		    precise = false;
                }
		int     funcID = functionList.index( functionName );
		boolean mpi    = patMPI.matcher( functionName ).find();
		boolean openmp = patOpenMP.matcher( functionName ).find();
		int     newStackID, newCallerID;
		
		// Convert trace-specific stackID to common stackID
		// Special case: first stack, no caller
		newCallerID = stackID == 0 ? 0 : t.commonStackID[caller];
		String key = new String( functionName + "<" + newCallerID );
		Integer id = (Integer)commonStackIDs.get( key );
		if( id == null ) {
		    newStackID = commonStackIDcounter++;
		    commonStackIDs.put (key, newStackID);
		    stackInfo.push( new StackInfo( newStackID, levels, 
			newCallerID, funcID, mpi, openmp, precise ) );
		} else {
		    newStackID = id.intValue();
		}
		t.commonStackID[stackID] = newStackID;

                if (debug > 2) { 
		    System.out.println( 
                        "                   " +
			stackID + "," + levels + "," + caller + "," +
			functionName + (precise ? " precise" : ""));
		    System.out.println( 
                        "                           " +
			newStackID + "," + levels + "," +
			newCallerID + " key=" + key);
		}
            }
        }

        if( debug>2 )
	    System.out.println( "New stacksCount="+commonStackIDcounter );
	outdata.storeInt( commonStackIDcounter );
        for( int i=0; i<commonStackIDcounter; i++ ) {
            StackInfo si = (StackInfo)stackInfo.elementAt( i );
	    outdata.storeInt    ( si.stackID    );
	    outdata.storeInt    ( si.depth      );
	    outdata.storeInt    ( si.caller     );
	    outdata.storeInt    ( si.functionID );
	    outdata.storeBoolean( si.mpi        );
	    outdata.storeBoolean( si.openmp     );
	    outdata.storeBoolean( si.precise    );
        }

	for( int trace=0; trace<traceCount; trace++ ) {
	    Trace t = traceList[trace];
            
            // Close, reopen and skip to samples info
            t.indata.close();
            t.indata = new DataFile( t.traceFilename, INBUFSIZE, endian );
            t.indata.skipBytes( t.sampleOffset );
	    
	    if( trace == 0 ) {
	        firstTrace   = t;
		// Resample time interval defined by sample time interval
		// of the first trace specified
		timeInterval = t.sampleTime;
	    }
        }

        // Get first samples
	firstTrace = null;
        for (int trace = 0; trace < traceCount; trace++) {
	    Trace t = traceList[trace];
	    t.moreSamples = getSample (t);
            t.firstTime = t.time;
	    // insertSampleInList( t );		
	}
	nextSampleTime = 0.;

        offsetFunctionTable = outdata.getFilePointer();
	int size = functionList.size();
	if( debug>1 ) System.out.println( 
	    "Function table file pointer: "+offsetFunctionTable+
	    "\nFunction count: "+size );

	outdata.storeInt( size );
	for( int i=0; i<size; i++ )
	    outdata.storeString( functionList.name(i) );

	offsetStackSamples = outdata.getFilePointer();
	if( debug>1 ) System.out.println( 
	    "Stack samples file pointer: "+offsetStackSamples );
	outdata.storeInt( muxSampleCount ); // Dummy: updated later
	outdata.storeDouble( mintime 	  );
	outdata.storeDouble( maxtime 	  );
	outdata.storeDouble( timeInterval );
    }
    
    public void closeVFDFiles() 
                throws IOException {

        // Add last sample again for easier binary search
	storeMuxSample();
	storePerfSample();

        // Close VFD file; append precise stacks and delete temp file
	for (int trace = 0; trace < traceCount; trace++){
            Trace t = traceList[trace];
            t.offsetPrecise = outdata.getFilePointer();
	    t.preciseCount  = t.tmpPrecise.getOutputCount();
	    t.indata.close();
	    t.tmpPrecise.close();
	    if (debug > 1) System.out.println( 
                            "Closed trace file " + t.traceFilename);
            t.indata = new DataFile( t.tmpPreFilename, INBUFSIZE, endian );
	    try {
		int   n;
        	byte  buf[] = new byte[INBUFSIZE];
		while( (n = t.indata.read( buf,0,INBUFSIZE )) > 0 )
	            outdata.storeBlock( buf, n );
            } catch( EOFException e ) {
	    }
	    t.indata.close();
	    File f = new File( t.tmpPreFilename );
	    f.delete();
	    if( debug>1 ) System.out.println( 
                              "Closed temp file " + t.tmpPreFilename);
        }
	if( debug>0 ) System.out.println( "Precise stack temp files closed");

        // Append messages and delete temp file

        int[] msgOffsets = new int[traceCount];
        messageCount  = 0;

	for( int trace=0; trace<traceCount; trace++ ){
            Trace t = traceList[trace];
            t.offsetMessages = outdata.getFilePointer();
	    t.messageCount   = t.tmpMessage.getOutputCount();
	    t.tmpMessage.close();
            t.indata = new DataFile( t.tmpMsgFilename, INBUFSIZE, endian );
	    try {
		int   n;
        	byte  buf[] = new byte[INBUFSIZE];
		while( (n = t.indata.read( buf,0,INBUFSIZE )) > 0 )
	            outdata.storeBlock( buf, n );
            } catch( EOFException e ) {
	    }
	    t.indata.close();
	    File f = new File( t.tmpMsgFilename );
	    f.delete();
            msgOffsets[trace] = messageCount;
            messageCount += t.messageCount;
	    if( debug>1 ) System.out.println( 
                              "Closed temp file " + t.tmpMsgFilename);
        }
	if( debug>0 ) System.out.println( "Message temp files closed");

        // Sort the message transfers

        long offsetAbs = 0;
        long offsetInc = outdata.getFilePointer();  // First offset

        Collections.sort( transfers, new CompareByTime() );

        // Compute and store the message transfer snapshots and
        // the increments in between

        Vector<TransferSnapshot> transferSnapshots = new Vector<TransferSnapshot>(100,100);
        int maxIncrements = Constants.XFER_BLOCKSIZE;
        int increments = 0;
        int lengthAbs  = 0;
        int lengthInc  = 0;
        int snapshotCount = 0;
        TransferList snapshot = new TransferList(maxIncrements);
        TransferInc ti;

        transferSnapshots.add (new TransferSnapshot(0, offsetAbs, offsetInc, lengthAbs, maxIncrements));
        snapshotCount++;

        Iterator<TransferInc> iterTransfers = transfers.iterator();
        while (iterTransfers.hasNext()) {
            ti = iterTransfers.next();
            // Write increments
            outdata.storeDouble(ti.time);
            outdata.storeDouble(ti.rate);
            outdata.storeInt (ti.src);
            outdata.storeInt (ti.dst);
            outdata.storeInt (ti.dir);

            // Update snapshot
            snapshot.put (ti);
            lengthInc++;

            if( (lengthInc >= maxIncrements) ) {
                offsetAbs = outdata.getFilePointer();
                lengthAbs = snapshot.size();
                // Save snapshot's absolute rates
                Set<Integer> keys = snapshot.keySet();
                for( Integer key: keys ) {
                    Transfer t = snapshot.get(key);
                    outdata.storeDouble(t.rate);
                    outdata.storeInt (t.src);
                    outdata.storeInt (t.dst);
                    outdata.storeInt (t.dir);
                }
                offsetInc = outdata.getFilePointer();
                if(false)System.out.println("DEBUG closeVFDFiles"+
                      " index="+snapshotCount+
                      " snapTime="+ti.time+
                      " offsetAbs="+offsetAbs+
                      " offsetInc="+offsetInc+
                      " lengthAbs="+lengthAbs+
                      " lengthInc="+lengthInc
                      );
                transferSnapshots.lastElement().lengthInc = lengthInc;
                transferSnapshots.add(
                  new TransferSnapshot( ti.time, offsetAbs, offsetInc, lengthAbs, maxIncrements ));
                lengthInc = 0;
                snapshotCount++;
            }
        }
        transferSnapshots.lastElement().lengthInc = lengthInc;

	offsetTransfers = outdata.getFilePointer();
        outdata.storeInt(transferSnapshots.size());
        // Store the snapshots administration
        Iterator<TransferSnapshot> iterSnapshot = transferSnapshots.iterator();
        while( iterSnapshot.hasNext() ) {
            TransferSnapshot ts = iterSnapshot.next();
            outdata.storeDouble(ts.time);
            outdata.storeLong(ts.offsetAbs);
            outdata.storeLong(ts.offsetInc);
            outdata.storeInt(ts.lengthAbs);
            outdata.storeInt(ts.lengthInc);
        }
	if( debug>0 ) System.out.println( "Message transfers written");

	offsetProfile = outdata.getFilePointer();
	outdata.close();

	// Reopen to update the index pointers
	raf = new RandomAccessFile( stdFilename, "rw" );
	raf.seek( 4 ); // Skip traceCount
	raf.writeInt ( messageCount         );        // Total nr of messages
	raf.writeLong( offsetTraceDetails   );
	raf.writeLong( offsetFunctionTable  );
	raf.writeLong( offsetStackSamples   );
        raf.writeLong( traceList[0].offsetPrecise  ); /* First precise offset */
        raf.writeLong( traceList[0].offsetMessages ); /* First message offset */
        raf.writeLong( offsetTransfers      );
        raf.writeLong( offsetProfile        );

	for( int trace=0; trace<traceCount; trace++ ) {
	    Trace t = traceList[trace];
            raf.seek( t.offsetMessageIndex );
	    raf.writeInt ( t.preciseCount   );
	    raf.writeLong( t.offsetPrecise  );
	    raf.writeInt ( t.messageCount   );
	    raf.writeLong( t.offsetMessages );
	}
	raf.seek( offsetStackSamples );
	raf.writeInt( muxSampleCount );
	if( debug>0 ) System.out.println( 
	    muxSampleCount+" multiplexed samples start at "+offsetStackSamples );

        // Seek file position to save profile
	
	raf.seek( offsetProfile );
	raf.writeInt( 0 );  // Dummy - to be updated later
    }

    public void closeSTDFile() 
                throws IOException {
	raf.close();
    }

    public boolean getSamples() 
                throws EOFException, IOException {

        double  minTime     = Double.MAX_VALUE;
	int     minTrace    = 0,
	        total       = traceCount;
	Trace   tnext       = null;
	boolean moreSamples = false;

	for (int trace = 0; trace < traceCount; trace++) {
	    Trace t = traceList[trace];
            if (t.moreSamples && t.time < minTime) {
	        minTime = t.time;
		tnext   = t;
	    }
	}

	// TODO: Treat case tnext == null -> No Samples found!
        if (tnext.precise) tnext.preciseTime = minTime;
        while( minTime >= nextSampleTime ) {
	    prevTime = time;
	    time = nextSampleTime;
	    storeMuxSample();
	    storePerfSample();
	    nextSampleTime += timeInterval;
	}

	for(int trace = 0; trace < traceCount; trace++) {
	    Trace t = traceList[trace];
            if( t.moreSamples && t.time <= minTime ) {
	        t.moreSamples = getSample(t);
	    }
	    if( t.moreSamples ) moreSamples = true;
        }
	return( moreSamples && time < maxtime );
    }
    
    private void storeMuxSample() throws IOException {
	if (mintime <= prevTime && prevTime <= maxtime) {
            for (int trace = 0; trace < traceCount; trace++) {
		Trace t = traceList[trace];
                int id = time < t.firstTime ? 0 : (int)(t.stackID);
		outdata.storeInt (id);
                if (debug > 3 && t.stackID != -1) {
		    StackInfo si = (StackInfo)stackInfo.elementAt(t.stackID);
                    System.out.print( ">["+t.admin.traceID+":"+
                                          t.admin.task   +","+
                                          t.admin.thread +"] "+
                                      muxSampleCount+" "+
				      prevTime+":"+time+" stack: " );
		    int n = si.depth;
		    for (int i = 0; i < n; i++) {
                        String name = functionList.name(si.functionID);
                        System.out.print ((i > 0 ? "<" : "") + name);
			si = (StackInfo)stackInfo.elementAt(si.caller);
                    }
                    System.out.println( "" );
                }
            }
	    muxSampleCount++;
	}
    }

    private void storePerfSample () throws IOException {
	    if (mintime <= prevTime && prevTime <= maxtime) {
		    for (int iPerf = 0; iPerf < traceList[0].nPerfValues; iPerf++) {
		    	for (int trace = 0; trace < traceCount; trace++) {
		    	        Trace t = traceList[trace];
				double toStore = time < t.firstTime ? 0 : t.perfValues[iPerf];
		    	        outdata.storeDouble (toStore);
		    	}
		    }
		    perfSampleCount++;
	    }
    }

    public int getSampleCount() {
        return muxSampleCount;
    }

    public double getRunTime() {
        return runTime;
    }

    private boolean getSample (Trace t) throws IOException {

        int    sidw, sampleCode, nlev = 0;

        if( t.count >= t.admin.sampleCount ) {
	    t.stackID = -1;
	    return false;
        }

	t.prevTime = t.time;
        for( ; t.count < t.admin.sampleCount; t.count++ ) {

            sidw = t.indata.readInt();
	    if (sidw == SID_MESSAGE) {
	        int dir = t.indata.readInt();
                int peer = t.indata.readInt();
                int type_idx = t.indata.readInt();
                int count = t.indata.readInt();
                int type_size = t.indata.readInt();
                int tag = t.indata.readInt();
		long tstart = t.indata.readLong();
		long tstop = t.indata.readLong();
                short  src    = (short) (dir==0 ? t.task : peer);
                short  dst    = (short) (dir==1 ? t.task : peer);

		double rate;
                double dtstart = tstart * 1.0e-6;
                double dtstop = tstop * 1.0e-6;
                rate = (count * type_size) / (dtstop - dtstart);
		if (rate < 0) System.out.println ("NEGATIVE RATE: " + rate);
		transfers.add (new TransferInc (dtstart, rate, src, dst, dir));
		transfers.add (new TransferInc (dtstop, -rate, src, dst, dir));
		t.tmpMessage.storeMessageInfo (dtstart, dtstop, t.MPImsgID, 
                      type_idx, type_size, count, t.task, peer, tag, dir);
	    } else if(sidw == SID_ENTRY || sidw == SID_EXIT) {
                int oldStackID = t.indata.readInt();
                long ltime = t.indata.readLong();
		for (int i = 0; i < t.nPerfValues; i++) {
			t.perfValues[i] = t.indata.readDouble();
		}	
                String      what       = sidw == SID_ENTRY ? call : exit;
                // CW 20200118: I have no idea why Jan had to do this and
                // it does not seem necessary to me (anymore).
		// if( oldStackID < 0 ) {
                //     oldStackID= 0;   //HACK 20180703JB
                // }
		// if( oldStackID > t.stacksCount ) {
                //     oldStackID= 0;   //HACK 20190103JB
                // }
		int         stackID = t.commonStackID[oldStackID];
                StackInfo   si = (StackInfo)stackInfo.elementAt(stackID);
                t.MPImsgID = stackID;
                t.stackID = sidw == SID_ENTRY ? si.caller : stackID;

                t.precise = si.precise;
                t.time = ltime * 1.0e-6;

		if( sidw == SID_ENTRY && t.precise ) {
		    // Mark start time of precise sample
                    t.startTimes.push (t.time);
		}

                if( sidw == SID_EXIT && t.precise ) {
		    // Save precise sample
                    
                    double timeBegin;
                    try {
                        timeBegin = ((Double)t.startTimes.pop()).doubleValue();
                    } catch( EmptyStackException e ) {
                        // Stack empty: this happens in MPI_Init, which clears the samples
                        // up to the return from the MPI_Init wrapper, so only the return
                        // is logged, causing a stack underflow here.
                        timeBegin = t.time;
		    }
		    t.tmpPrecise.writePrecise( timeBegin, t.time, stackID );
		}

                if( debug>3 ) {
                    System.out.print( "<["+t.admin.traceID+":"+
                                          t.admin.task   +","+
                                          t.admin.thread +"] "+
                                          t.time+" "+what+" " );
                    int id = stackID;
		    int n  = si.depth;
		    for (int i = 0; i < n; i++) {
                        String name = functionList.name(si.functionID);
                        System.out.print ((i > 0 ? "<" : "") + name );
			si = (StackInfo)stackInfo.elementAt(si.caller);
                    }
                    System.out.println( "" );
                }
                t.count++;
		break;
            }
        }
	return true;
    }
}
