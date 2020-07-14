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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.RandomAccessFile;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import util.Constants;
import util.DisplayElement;
import util.FunctionList;
import util.MPI_Frame;
import util.MultiStackSample;
import util.PerformanceSample;
import util.SampleReader;
import util.StackInfo;
import util.StackSample;
import util.TimeLine;
import util.TraceAdmin;
import util.Transfer;
import util.TransferInc;
import util.TransferList;
import util.TransferSnapshot;
import profile.Profile;
import comm.CommGraphFrame;
import comm.CommInfo;
import comm.CommunicationGraphics;
import message.MessageInfo;
import message.MessageHeader;

public class TraceInfo extends Thread {

    public FunctionList functionList;
    public int colorTableSize = 0;
    public int traceCount   = 0;
    public int messageCount = 0;
    public int debug        = 0;
    public Profile[]    prof         = null;
    public Profile      profMin, profMax;
    public TraceAdmin[] traceAdmin;

    StackInfo stackInfo;
    Vector<MultiStackSample>  muxSamples = null;
    Vector<PerformanceSample> perfSamples = null;
    double mintime, maxtime, sampleTime;

    Stack<String> traceFiles;
    String         traceFile;
    int sampleCount = 0;
    int maxStackDepth = 0;
    int functionsCount = 0;
    int rgbBufferHeight = 0;

    public TraceView              traceView;
    public TraceGraphics          traceGraphics;
    public Vector<DisplayElement> displayList;
    Vector<TimeLine>              updateRequests;

    public CommunicationGraphics commGraphics;
    public CommInfo commInfo;
    public TransferSnapshot[] transferSnapshots;
    public int indexSnapshot = -1;
    public int snapshotCount = 0;
    double graphTransferTime = 0;
    int graphTransferIndex = 0;
    public TransferList transferList;
    public TransferInc[] transferIncs;
    public double[][] transferMatrix;
    long transferRecord = 0;
    long transferCount = 0;
    double matrixTime = 0;

    long offsetTraceDetails;
    long offsetFunctionTable;
    long offsetStackSamples;
    long offsetPrecise;
    long offsetMessages;
    long offsetTransfers;
    public long offsetTranSnapshots;
    public long offsetProfile;
    
    int indexMessagePairs;

    double pixelTime;
    double runTime;
    double profileTimeStart, profileTimeStop;

    boolean haveSamples, stopThread;
    boolean needSamples = false;
    boolean accessingVectors;
    public boolean commShowCores = false,
                   commShowNodes = false,
                   commShowTasks = true,
                   commMomentary = true;

    public  RandomAccessFile raf;
    private SampleReader sampleReader = null;
    private byte[] msgInputBuffer;
    private DataInputStream  msgInputStream;

    ObjectInputStream msgPairInputStream;

    private int iInc;
    
    private boolean hasPerfInfo = true;

    public TraceInfo (Stack<String> files, int debugLevel, double tmin, double tmax) {
        traceFiles = files;
        traceCount = files.size();
	mintime = tmin;
	maxtime = tmax;
	debug = debugLevel;
	displayList = new Vector<DisplayElement>( 8, 8 );
	updateRequests = new Vector<TimeLine>( 10 );
	haveSamples = false;
	accessingVectors = false;
	stopThread = false;
	functionList = new FunctionList();

	msgInputBuffer = new byte[Constants.MESSAGE_INFO_RECORDSIZE];
	msgInputStream = new DataInputStream(
	                         new ByteArrayInputStream( msgInputBuffer ) );
    }

    public synchronized void stopThisThread() {
        stopThread = true;
	notifyAll();
    }

    private synchronized void waitForFrame() {
	while (traceView.traceFrame == null) {
	    try {
		System.out.println( "Reader thread waiting for graphics..." );
		wait();
	    } catch( InterruptedException e ) {
	    }
	}
    }

    public void run() {
        boolean commInfoOK;

	waitForFrame();
	if (stopThread) return;

	traceGraphics = traceView.traceFrame.trGraphics;
	
	if (traceFiles.isEmpty()) return;

	traceFile = traceFiles.elementAt(0);
        if (traceFile.endsWith (".vfd")) {
	    try {
	        readTraceFile_vfd();
	    } catch (IOException e) {
	        System.out.println ("Can't read " + traceFile + ":" + e);
		e.printStackTrace();
		return;
	    }
	}

	if (traceFile.endsWith(".std")) {
	    try {
	        readTraceFile_std();
	    } catch (IOException e) {
	        System.out.println ("Can't read " + traceFile + ":" + e);
		e.printStackTrace();
		return;
	    }
	}

	traceGraphics.updateFileData();
        traceView.traceFrame.commGraphFrame.setCells(traceCount, 1);
	
	for( ;; ) {
	    boolean ok;
	    try {
	        ok = buildScreenStackSampleList();
		if (ok) traceGraphics.timeLineUpdate();
	    } catch (IOException e) {
	        System.out.println (e);
	    }
	    needSamples = false;

            if (!traceGraphics.showProfileInterval) {
        	// Default profile time range: full interval
                resetProfileTime();
	    }

	    waitForRequest();
	    if (stopThread) return;
        }
    }

    public void setTraceView( TraceView traceView ) {
        this.traceView = traceView;
    }

    public synchronized void waitForRequest () {
	if (debug > 0) System.out.println ("Reader thread waiting for sample requests...");
        while (!needSamples) {
	    try {
		wait();
	    } catch (InterruptedException e) {
	    }
	}
    }

    void resetProfileTime() {
        //TimeLine tl = traceGraphics.timeLine;
	//profileTimeStart = Math.max( 0.,      tl.viewTimeStart );
	//profileTimeStop  = Math.min( runTime, tl.viewTimeStop  );
	profileTimeStart = Math.max (0., traceGraphics.getTimelineStart());
	profileTimeStop  = Math.min (0., traceGraphics.getTimelineStop());
        traceGraphics.showProfileInterval = false;
	newProfile();
    }

    public synchronized void needMoreSamples( TimeLine timeLine ) {
	if( debug>0 ) System.out.println( "needMoreSamples: sample request with timeline data" );
        needSamples = true;
	if( timeLine == null ) System.out.println( "timeline null!" );
	updateRequests.addElement( timeLine );
	notifyAll();
    }

//
//  Read the binary experiment traceFiles
//
    void readTraceFile_vfd() throws IOException {

	double      progressTime = 0.;

        Multiplexer multi = new Multiplexer (traceFiles, mintime, maxtime, debug);
	double      runTime = multi.getRunTime();
	double      progressDelta = runTime / 100.;
	int         progress      = 0;

        if( debug > 0 ) System.out.println( "readTraceFile_vfd: multiplexing the samples" );

        while( multi.getSamples() ) {
	    if( multi.time >= progressTime ) {
		progressTime += progressDelta;
		traceGraphics.setProgressBarValue (progress++);
	    }
	}
	traceGraphics.setProgressBarValue (100);

        multi.closeVFDFiles();
        traceFile = multi.stdFilename;
	
	multi.closeSTDFile();

	if( debug>0 ) 
	    System.out.println( "Trace file "+traceFile+" written." );
    }

//
//  Read multiplexed tracefile
//
    void readTraceFile_std() throws IOException{

        // Create new stack trace data file (root.std)
	if( debug>0 ) System.out.println( "Opening trace file: "+traceFile );
	raf = new RandomAccessFile( traceFile, "r" );

        traceCount           = raf.readInt();
        messageCount         = raf.readInt();

	traceAdmin           = new TraceAdmin[traceCount];

	offsetTraceDetails   = raf.readLong();
	offsetFunctionTable  = raf.readLong();
	offsetStackSamples   = raf.readLong();
	offsetPrecise        = raf.readLong();
	offsetMessages       = raf.readLong();
	offsetTransfers      = raf.readLong();
	//offsetTranSnapshots  = raf.readLong();
	offsetProfile        = raf.readLong();

	if( debug>2 ) System.out.println( "Function list:" );

        functionList = new FunctionList();
	raf.seek( offsetFunctionTable );
	int size = raf.readInt();
	for( int i=0; i<size; i++ ) {
	    String name  = raf.readUTF();
	    int    index = functionList.index( name );
	    if( debug>2 ) System.out.println( "                   "+
	                                      index+" "+name );
	}
	colorTableSize = size; // Needed for color table

	// Reconstruct traceAdmin
	raf.seek (offsetTraceDetails);
	runTime    = 0.;
	int height = 0;
	for(int iTrace = 0; iTrace < traceCount; iTrace++ ) {
	    TraceAdmin t = new TraceAdmin(
	       raf, iTrace, traceCount, functionList, debug );
	    runTime = Math.max (runTime, t.runTime);
	    traceAdmin[iTrace] = t;
        }

	int posInc = 0;
	for(int iTrace = 0; iTrace < traceCount; iTrace++) {
	    TraceAdmin t = traceAdmin[iTrace];
	    if( iTrace == 0 ) {
		    t.getStacksTable();
		    posInc = t.getPosInc();
	    } else {
	        t.stacksCount = traceAdmin[0].stacksCount;
		t.stackArray  = traceAdmin[0].stackArray;
		t.setMaxDepth (traceAdmin[0].getMaxDepth());
		t.functionList= traceAdmin[0].functionList;
	    }
	    t.setPosition (height);
	    height += posInc;
	    if (hasPerfInfo) height += posInc;
        }
	int tmp[] = new int[traceCount];
	for (int iTrace = 0; iTrace < traceCount; iTrace++) {
		tmp[iTrace] = traceAdmin[iTrace].getPosition();
	}
	for (int iTrace = 0; iTrace < traceCount; iTrace++) {
		traceAdmin[traceCount - iTrace - 1].setPosition(tmp[iTrace]);
	}

        rgbBufferHeight = height; // Needed for display image

        // We are now at the start of the function table again.
	// Skip to the stack samples.
	raf.seek( offsetStackSamples );
	sampleCount = raf.readInt();
	mintime     = raf.readDouble();
	maxtime     = raf.readDouble();
	sampleTime  = raf.readDouble();
	
        // Read transfer snapshot info
        raf.seek( offsetTransfers );
        snapshotCount = raf.readInt();
        transferSnapshots = new TransferSnapshot[snapshotCount];
        for (int i = 0; i < snapshotCount; i++) {
            double snapTime  = raf.readDouble();
            long   offsetAbs = raf.readLong();
            long   offsetInc = raf.readLong();
            int    lengthAbs = raf.readInt();
            int    lengthInc = raf.readInt();
            transferSnapshots[i] = new TransferSnapshot(
                                   snapTime,offsetAbs,offsetInc,lengthAbs,lengthInc);
            if(false)System.out.println( "DEBUG TraceInfo.readTraceFile_std "+i+
                " snaptime ="+snapTime +
                " offsetAbs="+offsetAbs+
                " offsetInc="+offsetInc+
                " lengthAbs="+lengthAbs+
                " lengthInc="+lengthInc );
        }

	 //traceView.traceFrame.showCommGraphFrame();
    }
    
    synchronized void setLock() {
	while (accessingVectors) {
	    try {
		wait();
	    } catch( InterruptedException e ) {
	    }
	}
	accessingVectors = true;
	notifyAll();
        return;
    }

    synchronized void unsetLock() {
	accessingVectors = false;
	notifyAll();
        return;
    }

    private void fillInputMsgBlock( MessageInfo mi, Vector<MessageInfo> v, int blkSize ) {
	final boolean GIVE_DETAILS = true;
	try {
	    while( !mi.eof() && v.size() < blkSize ) {
		if( readMessageInfo( mi, GIVE_DETAILS ) )
		    v.addElement( new MessageInfo( mi ) );
		else break;
	    }
	} catch( IOException e ) {
	    e.printStackTrace();
	}
    }

    public boolean showPerfGraph (int i) {
	    if (traceView.traceFrame.showPerfGraph == null) {
		    return false;
	    } else {
		    return traceView.traceFrame.showPerfGraph[i];
	    }
    }


    public boolean buildScreenStackSampleList()
                   throws IOException
    {
        TimeLine tl;

	// Build array of stack samples, resampled with screen resolution
	
	// Return value: true if there was a change in the interval displayed.

        if (traceGraphics  == null || rgbBufferHeight == 0) return false;

	double timeScale = traceGraphics.getTimelineTimescale();
	double timeStart = traceGraphics.getTimelineParttimeStart();
	double timeStop  = traceGraphics.getTimelineParttimeStop();
	double timeRange = traceGraphics.getTimelineTimerange();
	double timeDelta = timeStop - timeStart;
	double delta = timeDelta / traceGraphics.getTimelinePartwidth_double();

	int y_top     = traceGraphics.y_top;
	int y_bottom  = traceGraphics.y_bottom;
	int len	   = traceGraphics.viewPort.getViewRect().width;   

	boolean precise    = traceView.traceFrame.showPreciseStacks;
	boolean markSamples= traceView.traceFrame.markSamples;
	boolean[] showPerfGraph = traceView.traceFrame.showPerfGraph;

	int nPerfValues = traceAdmin[0].getNPerfValues ();
	long stackBase = offsetStackSamples + 4 + 3 * 8;
	/* In the std file, the stackID (traceCount integers) are followed
	 * by the performance counters. Therefore, the performance
	 * offset is as follows:
	 * */
	long perfBase = offsetStackSamples + 4 + 3 * 8 + 4 * traceCount;
	sampleReader = new SampleReader (raf, mintime, maxtime,
	      	           sampleTime, sampleCount,
	       		   stackBase, perfBase,
	       		   traceCount, nPerfValues);

        muxSamples = new Vector<MultiStackSample> (len, len / 4);
	perfSamples = new Vector<PerformanceSample> (len, len / 4);

	double time, time_stop, time_next;
	int stackID, depth;
	long filePointer;
	int recordNumber;
	int nSamples = sampleCount;

	int partWidth      = traceGraphics.getTimelinePartwidth_int();;
	int rgbBufferWidth = partWidth;

	double perfMaxValues[] = new double [nPerfValues];

	// Remember that traceInfo is a separate thread, as well as TraceView, which
	// calls TraceGraphics. In TraceGraphics, displayList is cloned. Thus, 
	// we need to set a lock over the entire time displayList is worked on.
	//
	// There should be a simpler solution - like volatile displayLists? However,
	// I found none that works.
	setLock();
	// Empty all lists before the picture is drawn. Otherwise, upon a repainting,
	// we would start with the list corresponding to the previous view window.
	displayList.clear();
	perfSamples.clear();

	int nComponents = 2 * traceCount;
	DisplayElement displayElements[] = new DisplayElement[nComponents];
	for (int iComponent = 0; iComponent < nComponents; iComponent++) {
		displayElements[iComponent] = new DisplayElement (
		               timeStart, timeDelta, 
			       rgbBufferWidth, rgbBufferHeight, traceAdmin,
			       y_top, y_bottom, precise, markSamples,
			       showPerfGraph);
	}

	MultiStackSample mss = null;
	PerformanceSample perfSample = null;
	StackSample thisSample = null;

	MultiStackSample mssStart = sampleReader.getMss(0);
	MultiStackSample mssStop = sampleReader.getMss(nSamples - 1);
	muxSamples.addElement (mssStart);
	muxSamples.addElement (mssStop);
 	perfSample = sampleReader.getPerformanceSample(0);
	perfSamples.addElement (perfSample);
	int startIndex = (int)((mssStart.computeTime() - timeStart) / delta);
	int stopIndex  = (int)((mssStop.computeTime() - timeStart) / delta);

	if (startIndex > 0) {
	    mssStart.nullifyStackID ();
	} else {
	    startIndex = 0;
	}
	
	if (stopIndex <= partWidth - 1) {
	    mssStop.nullifyStackID ();
	} else {
	    stopIndex = partWidth - 1;
	}
	
	traceGraphics.setProgressBarTotalTime (Math.min (timeStop - timeStart, runTime));
	double fileDelta = sampleTime;
	double fileTimeScale = 1. / fileDelta;
	double sampleDelta = Math.max (delta, fileDelta );

	int iStart = (int)((timeStart - mintime) * fileTimeScale);
	int iStop = (int)((timeStop - mintime) * fileTimeScale + 0.5);
	iInc = (int)(delta * fileTimeScale + 0.5);

	if (iInc < 1) iInc = 1;
	if (iStart < 0 ) iStart = 0;
	if (iStop > nSamples - 1) iStop = nSamples - 1;

	// Need one before start Index:
	if (iStart > 0) {
		perfSample = sampleReader.getPerformanceSample (iStart - 1);
	}

	int gap = (int)(fileDelta * timeScale) + 1;
	int[] i1_list = new int [iStop - iStart + 1];
	int i1, i2;

	for (int i = iStart; i <= iStop; i += iInc) {
		mss = sampleReader.getMss(i);
		i1_list[i - iStart] = (int)((mss.computeTime() - timeStart) * timeScale) + 1;
	}

	int thisStackId, prevStackId;
	Vector<Integer> global_i1 = new Vector<Integer>(); 
	Vector<Integer> global_i2 = new Vector<Integer>(); 
        for (int iComponent = 0; iComponent < nComponents; iComponent += 2) {
		int iTrace = iComponent / 2;
		prevStackId = -1;
		for (int i = iStart; i <= iStop; i += iInc) {
		    i1 = i1_list[i - iStart];
		    i2 = i1 + gap;
		    mss = sampleReader.getMss (i);
		    thisSample = mss.extractStackSample (iTrace);

		    thisStackId = mss.getStackID(iTrace);
		    perfSample = sampleReader.getPerformanceSample (i);
		    boolean functionChanged = prevStackId != thisStackId;
		    if (functionChanged) {
		    	prevStackId = thisStackId;
			perfSample.setIntervalBegin(true);
		    }

		    if (iComponent == 0) {
			    perfSamples.addElement(perfSample);
			    muxSamples.addElement(mss);
	            }
	
		    if (i1 < 0) i1 = 0;
		    if (i2 < 0) i2 = 0;
		    if (i1 > stopIndex) i1 = stopIndex;
		    if (i2 > stopIndex) i2 = stopIndex;

		    if (iComponent == 0) {
		            global_i1.add (i1);
		            global_i2.add (i2);
		    }
		    
		    displayElements[iComponent + 1].addStackSample (thisSample, i1, i2, iTrace);
	
		    traceGraphics.updateProgressBarDiff (delta);
		}
	
		traceGraphics.resetProgressBar ();

	        if (precise) {
			addPreciseSamples (displayElements[iTrace],
					traceCount, stopIndex, timeStart, timeScale);
		}

		displayList.addElement (displayElements[iComponent]);
		displayList.addElement (displayElements[iComponent+1]);
	}

	perfSample = sampleReader.getPerformanceSample(nSamples - 1);
	perfSamples.addElement (perfSample);
	haveSamples = true;

	computeDifferences (perfSamples, mssStart.getIndex());

	// Now that differences have been computed, we can compute the maximal values
	// for normalization.
	double[][] maxPerf = new double[traceCount][nPerfValues];
	double[][] minPerf = new double[traceCount][nPerfValues];
	determinePerfMinMax (perfSamples, traceCount, nPerfValues, maxPerf, minPerf);
	// printPerfMaxima (maxPerf, traceCount, nPerfValues);

	// perfSamples are ready, we add them all to the displayList.
	for (int i = 0; i < displayList.size(); i += 2) {
		DisplayElement d = displayList.elementAt(i);
		for (int iSample = 1; iSample < perfSamples.size() - 1; iSample++) {
			i1 = global_i1.elementAt(iSample - 1);
			i2 = global_i2.elementAt(iSample - 1);
			d.addPerfSample (perfSamples.elementAt(iSample), i1, i2, i / 2);
		}
	}

	scaleAndPrintImage (displayList, maxPerf);

	unsetLock();
		
	newProfile();

        return true;
    }

    private void computeDifferences (Vector<PerformanceSample> perfSamples,
		    int startIndex) {
	double tstart = 0;
	double tstop = 0;
	int ifirst, ilast;
	MultiStackSample mss;
	ifirst = 1;
	tstart = perfSamples.elementAt(0).computeTime(startIndex);
	for (int i = 1; i < perfSamples.size(); i++) {
		if (perfSamples.elementAt(i).isIntervalBegin()) {
			mss = muxSamples.elementAt(i);
			tstop = perfSamples.elementAt(i).computeTime(mss.getIndex());
			for (int j = ifirst; j < i; j++) {
				perfSamples.elementAt(j).setDifference(perfSamples.elementAt(ifirst-1));
				perfSamples.elementAt(j).setTimeDelta(tstop - tstart);
			}
			tstart = tstop;
			ifirst = i;
		}
	}
    }

    private void determinePerfMinMax (Vector<PerformanceSample> perfSamples,
	    int traceCount, int nPerfValues,
	    double[][] maxPerf, double[][] minPerf) {

        for (int iTrace = 0; iTrace < traceCount; iTrace++) {
		for (int iPerf = 0; iPerf < nPerfValues; iPerf++) {
			maxPerf[iTrace][iPerf] = 0;
			minPerf[iTrace][iPerf] = Integer.MAX_VALUE;
		}
	}

	for (PerformanceSample p : perfSamples) {
		for (int iTrace = 0; iTrace < traceCount; iTrace++) {
			for (int iPerf = 0; iPerf < nPerfValues; iPerf++) {
				double tmp = traceAdmin[iTrace].perfIsIntegrated (iPerf) ?
					p.getIntegratedValue(iPerf, iTrace) : p.getDifferentialValue(iPerf, iTrace);
				if (tmp > 0 && tmp > maxPerf[iTrace][iPerf]) {
					maxPerf[iTrace][iPerf] = tmp;
				}
				if (tmp >= 0 && tmp < minPerf[iTrace][iPerf]) {
					minPerf[iTrace][iPerf] = tmp;
				}
			}
		}
	}
    }

    private void printPerfMaxima (double[][] maxPerf, int traceCount, int nPerfValues) {
	for (int iTrace = 0; iTrace < traceCount; iTrace++) {
		for (int iPerf = 0; iPerf < nPerfValues; iPerf++) {
			System.out.println ("Maximum for " + iTrace + ", " + iPerf + 
					": " + maxPerf[iTrace][iPerf]);
		}
	}
    }

    private void scaleAndPrintImage (Vector<DisplayElement> displayList, double[][] maxPerf) {
	for (int i = 0; i < displayList.size(); i++) {
		DisplayElement d = displayList.elementAt(i);
		if (i % 2 == 0) {
			d.scaleY (maxPerf, i / 2);
		}
		d.printImage (i / 2);
	}
    }

    private void addPreciseSamples (DisplayElement displayElement, 
		    int traceCount, int stopIndex, 
		    double timeStart, double timeScale) throws IOException {
	    // NOT REVIEWED YET. PRECISE SAMPLES MIGHT STILL BE BROKEN.
	    double pStep = 100.0 / (double)traceCount;
	    double percentageDone = 0;
	    // Straight-forward read-them-all approach (slow but simple)
	    // Add message samples per trace
	    // 20190326 JB This no longer works: message info now separate from stack info!
		    for (int j = 0; j < traceCount; j++) {
			TraceAdmin  t = traceAdmin[j];
			boolean giveDetails = debug > 0; // true for debugging
			MessageInfo mi = new MessageInfo();
			firstMessageInfo (mi, j, timeStart);
			while (readMessageInfo (mi, giveDetails)) {
	                    double timeDiff = mi.timeStop - mi.timeStart;
	                    StackSample s = new StackSample( 
	                                      mi.timeStop, timeDiff, mi.id );
			    int i1  = (int) ((mi.timeStop-timeStart) * timeScale);
			    int inc = (int) (timeDiff * timeScale);
			    int i2  = i1 + inc;
			    if( i1 < 0 ) i1 = 0;
			    if( i2 < 0 ) i2 = 0;
			    if( i1 > stopIndex ) i1 = stopIndex;
			    if( i2 > stopIndex ) i2 = stopIndex;
	        	    if( debug>2 )
				System.out.println(
				    "precise: trace="+j+
				    " timeStart="+mi.timeStart+
				    " timeStop="+mi.timeStop+
				    " i1:i2="+i1+":"+i2 );
			    displayElement.addPreciseSample( s, j, i1, i2 );
			}
			traceGraphics.setProgressBarValue ((int)(percentageDone += pStep));
		    }
		    traceGraphics.setProgressBarValue (100);
    }

    public int getIInc () {
	    return iInc;
    }
    
    public MessageInfo findPreciseSample( 
              int traceNumber, double time, TraceFrame frame ) {

	MessageInfo mi = new MessageInfo();
	
	try {
	    findMessageInfo( mi, traceNumber, time );
	} catch( IOException exception ) {
	    exception.printStackTrace();
	}
	return mi;
    }

/**********************************************************************************************/

    /* updateCommFrames - Update comm table and graph for the time represented by
                          the mouse position in the trace graph (call stacks on
                          time line).
    */
    
    public void updateCommFrames() {

        //matrixTime      = traceView.traceFrame.trGraphics.mouseTime;
        matrixTime      = traceView.traceFrame.trGraphics.getMouseTime();
        //int traceNumber = traceView.traceFrame.trGraphics.mouseTraceNumber;
	int traceNumber = traceView.traceFrame.trGraphics.getMouseTraceNumber();

	if( traceView.traceFrame.commTableFrame.isVisible() ) {
            Vector<MessageInfo> msgInfo = new Vector<MessageInfo>();
            getMessageInfo( msgInfo, traceNumber, matrixTime );  // Current trace only
            traceView.traceFrame.commTableFrame.update( msgInfo );
        }

	if( traceView.traceFrame.commGraphFrame != null &&
	    traceView.traceFrame.commGraphFrame.isVisible() )
        {
            updateTransferMatrix( matrixTime );
            traceView.traceFrame.commGraphFrame.update();

	    traceView.traceFrame.showCommGraphFrame();
	    commGraphics.repaint();
            traceView.traceFrame.commGraphFrame.repaint();
	}
        //System.out.println("DEBUG traceInfo.updateCommFrames called");
    }

    /* updateCommTable - Update comm table for the cell represented by the mouse
                         position in the comm matrix graph (source to dest).
       **** INCOMPLETE ****
    */
    
    public void updateCommTable( int srcCell, int dstCell ) {

	if( !traceView.traceFrame.commTableFrame.isVisible() ) return;

        Vector<MessageInfo> msgInfo = new Vector<MessageInfo>();
        int srcRankMin =  srcCell    * commInfo.ranksPerCell;
        int srcRankMax = (srcCell+1) * commInfo.ranksPerCell;
        int dstRankMin =  dstCell    * commInfo.ranksPerCell;
        int dstRankMax = (dstCell+1) * commInfo.ranksPerCell;
        for( int i=0; i<traceCount; i++ ) {
            int rank = traceAdmin[i].task;
            if( (commInfo.showSends && (srcRankMin<=rank && rank<srcRankMax)) ||
                (commInfo.showRecvs && (dstRankMin<=rank && rank<dstRankMax))   ) {
                getMessageSelection( msgInfo, i,
                                srcRankMin, srcRankMax, dstRankMin, dstRankMax );
            }
        }
        traceView.traceFrame.commTableFrame.update( msgInfo );

    }

/**********************************************************************************************/

    public MessageInfo findMessageInfo( int traceNumber, double time ) {
        MessageInfo mi = new MessageInfo();
	try {
	    findMessageInfo( mi, traceNumber, time );
	} catch( IOException exception ) {
	    exception.printStackTrace();
	}
	return mi;
    }

    public void firstMessageInfo( 
                   MessageInfo mi, int traceNumber, double time )
	        throws IOException {

	TraceAdmin t = traceAdmin[traceNumber];
	try {
	    raf.seek( t.offsetMessages );
	    mi.msgCount = t.messageCount;
	    mi.base     = t.offsetMessages;
	    int recSize = Constants.MESSAGE_INFO_RECORDSIZE;
	    // Linear search
	    int i;
	    for( i=0; i<mi.msgCount; i++ ) {
		raf.seek( mi.base + i*recSize );
		mi.timeStart = raf.readDouble();
		mi.timeStop  = raf.readDouble();
		if( mi.timeStop > time     ) break;
            }
	    mi.count      = raf.readInt();
	    mi.id         = raf.readInt();
	    mi.self       = raf.readInt();
	    mi.peer       = raf.readInt();
	    mi.index      = i;
	} catch( IOException exception ) {
	    exception.printStackTrace();
	}
	
    }

    public MessageInfo makeMessageInfo( int traceNumber )
	        throws IOException {
        MessageInfo mi = new MessageInfo();

	TraceAdmin t = traceAdmin[traceNumber];
	try {
            raf.seek( t.offsetMessages );
	    mi.msgCount = t.messageCount;
	    mi.base     = t.offsetMessages;
	    mi.index    = 0;
	} catch( IOException exception ) {
	    exception.printStackTrace();
	}
	return mi;
    }

    public MessageHeader getMessageHeader( int traceNumber )
	        throws IOException {
	TraceAdmin t = traceAdmin[traceNumber];
        return new MessageHeader( raf, t.offsetMessages,
                                  Constants.MESSAGE_INFO_RECORDSIZE );
    }

    public void findMessageInfo( 
                   MessageInfo mi, int traceNumber, double time )
	        throws IOException {

	TraceAdmin t = traceAdmin[traceNumber];
	try {
            raf.seek( t.offsetMessages );
	    mi.msgCount = t.messageCount;
	    mi.base     = t.offsetMessages;
	    int recSize = Constants.MESSAGE_INFO_RECORDSIZE;
	    // Linear search (FIXME)
	    int i, id;
	    for( i=0; i<mi.msgCount; i++ ) {
		raf.seek( mi.base + i*recSize );
		mi.timeStart = raf.readDouble();
	        mi.timeStop  = raf.readDouble();
		if( mi.timeStart <= time &&
		    mi.timeStop  >= time     ) break;
            }
	    mi.count = raf.readInt();
	    id       = raf.readInt();
	    mi.self  = raf.readInt();
	    mi.peer  = raf.readInt();
	    mi.index = i;

	} catch( IOException exception ) {
	    exception.printStackTrace();
	}
	
    }

    // Get transfer info at the current time
    
    public void updateTransferMatrix (double timePointer) // timePointer corresponds to the position of the cursor in the main window
    {
	int progress = 0, prev = 0;
        long off = offsetTransfers + 8;
        long len = Constants.TRANSFER_RECORDSIZE;
        int low = 0, mid = 0, top = snapshotCount - 1;
        TransferSnapshot ts = null;
        TransferInc ti;
        int i, inc;
        for(i = snapshotCount; i > 0; i >>= 1) {
            mid = (low + top) / 2;
            ts = transferSnapshots[mid];
            if (timePointer < ts.time) {
		    top = mid;
            } else {
		    low = mid;
            }
        }
        ts = transferSnapshots[low];
        if(false)System.out.println( "DEBUG updateTransferMatrix"+
                            " low="+low+
                            " timePointer="+timePointer+
                            " snapshot time="+ts.time );
        if( (low != indexSnapshot) ) {
            // Different snapshot: read from file
            graphTransferTime = ts.time;
            graphTransferIndex = 0;
            indexSnapshot = low;
            transferList = new TransferList(5000, 1000);
            transferIncs = new TransferInc[ts.lengthInc + 2];
            try {
                // Read new absolute transfer list, if any
                if (ts.offsetAbs > 0) {
                    raf.seek (ts.offsetAbs);
                    for (i = 0; i < ts.lengthAbs; i++) {
                        double rate = raf.readDouble();
                        int src  = raf.readInt();
                        int dst  = raf.readInt();
                        int dir  = raf.readInt();
                        transferList.put (rate, src, dst, dir);
                    }
                }
                // Read new incremental transfer list
                raf.seek( ts.offsetInc );
                if(false)System.out.println(
                  "DEBUG updateTransferMatrix Reading transferIncs"+
                  " ts.offsetInc="+ts.offsetInc );
                transferIncs[0] = new TransferInc (-1.,0., 0, 0, 0);
                for(i = 0; i < ts.lengthInc; i++) {
                    double time = raf.readDouble();
                    double rate = raf.readDouble();
                    int  src  = raf.readInt();
                    int  dst  = raf.readInt();
                    int  dir  = raf.readInt();
                    transferIncs[i+1] = new TransferInc(time, rate, src, dst ,dir);
                }
                transferIncs[ts.lengthInc+1] = new TransferInc (9999999., 0., 0, 0, 0);
	    } catch( IOException exception ) {
	        exception.printStackTrace();
	    }
        }

        // Update the transferlist with incrementals up to the pointer time
        i = graphTransferIndex;
        while (true) {
            if(timePointer < transferIncs[i].time) {
                transferList.put(new Transfer(transferIncs[i--], -1) );
	    } else if(timePointer >= transferIncs[i+1].time) {
                transferList.put(new Transfer(transferIncs[++i],  1) );
	    }else {
		break;
	    }
        }
        graphTransferIndex = i;
        
        if(false) {
            System.out.println( "DEBUG updateTransferMatrix"+
                                " pointer time="+timePointer+
                                " snapshot number="+low+
                                " snapshot time="+ts.time+
                                " snapshot index="+graphTransferIndex+
                                " transfer count="+transferList.size() );
            System.out.println( " results:" );
            Set<Integer> keys = transferList.keySet();
            for(Integer key: keys) {
                Transfer t = transferList.get(key);
                System.out.printf( "%4d %4d %d %6.3f%n",
                 (int)t.src, (int)t.dst, (int)t.dir, t.rate );
            }
        }
    }

    // Get all message info for the current trace in the current time interval
    public void getMessageInfo( 
             Vector<MessageInfo> msgInfo, int traceNumber, double timeLeft, double timeRight )
    {
	try {
	    TraceAdmin t = traceAdmin[traceNumber];
	    MessageInfo mi = new MessageInfo();
	    int recSize  = Constants.MESSAGE_INFO_RECORDSIZE;
            int msgCount, index;
            long base = t.offsetMessages;
	    // Binary search
	    int i;
            int high, low, mid=0, lastRecord;
            int first, last, record;
            double timeStart=0., timeStop=0.;

            // Find last message record with stop time before current time
            low = 0;
            high = t.messageCount;
            for(i = t.messageCount; i > 0; i >>= 1) {
                mid = (low + high) / 2;
                raf.seek (base + mid * recSize + 8);
	        timeStop  = raf.readDouble();
                if (timeLeft < timeStop) {
			high = mid;
		} else {
			low  = mid;
		}
            }
	    first = low;

            int pending = t.tasks;
            for(i = first; i < t.messageCount; i++) {
                // Read message info
		raf.seek (base + i * recSize);
		mi.timeStart = raf.readDouble();
                if( mi.timeStart > timeRight ) {
                    if( --pending == 0 ) break;
                    continue;
                }
                mi.timeStop  = raf.readDouble();
                if( mi.timeStop < timeLeft ) continue;
	        mi.count     = raf.readInt();
                mi.id        = raf.readInt();
	        mi.self      = raf.readInt();
	        mi.peer      = raf.readInt();
	        mi.type      = raf.readInt();
		mi.type_size = raf.readInt();
	        mi.dir       = raf.readInt();
	        mi.tag       = raf.readInt();
                mi.index     = i;
                msgInfo.add(new MessageInfo(mi));
            }
	} catch( IOException exception ) {
	    exception.printStackTrace();
	}
    }


    // Get all message info for the current matrix time and current matrix cell 

    public void getMessageSelection( 
             Vector<MessageInfo> msgInfo, int traceNumber,
             int srcRankMin, int srcRankMax, int dstRankMin, int dstRankMax )
    {
	try {
	    TraceAdmin  t  = traceAdmin[traceNumber];
	    MessageInfo mi = new MessageInfo();
	    int recSize = Constants.MESSAGE_INFO_RECORDSIZE;
            int msgCount, index;
            long base = t.offsetMessages;
	    // Binary search
	    int i;
            int high, low, mid=0, lastRecord;
            int first, last, record;
            double timeStart=0., timeStop=0.;

            // Find last message record with stop time before current time
            low = 0;
            high = t.messageCount;
            for( i=t.messageCount; i>0; i>>=1 ) {
                mid = (low + high) / 2;
                raf.seek( base + mid*recSize +8 );
	        timeStop  = raf.readDouble();
                if( matrixTime < timeStop ) high = mid;
                else                      low  = mid;
            }
	    first = low;

            int pending = t.tasks;
            for( i=first; i<t.messageCount; i++ ) {
                // Read message info
		raf.seek( base + i*recSize );
		mi.timeStart = raf.readDouble();
                if( mi.timeStart > matrixTime ) {
                    if( --pending == 0 ) break;
                    continue;
                }
                mi.timeStop  = raf.readDouble();
                if( mi.timeStop < matrixTime ) continue;
	        mi.count     = raf.readInt();
                mi.id        = raf.readInt();
	        mi.self      = raf.readInt();
	        mi.peer      = raf.readInt();
	        mi.type      = raf.readInt();
	        mi.dir       = raf.readInt();
	        mi.tag       = raf.readInt();
                mi.index     = i;
                if( (commInfo.showSends  && mi.dir == Transfer.SEND &&
                     srcRankMin<=mi.self && mi.self<srcRankMax      &&
                     dstRankMin<=mi.peer && mi.peer<dstRankMax        ) ||
                    (commInfo.showRecvs  && mi.dir == Transfer.RECV &&
                     srcRankMin<=mi.peer && mi.peer<srcRankMax      &&
                     dstRankMin<=mi.self && mi.self<dstRankMax)          ) {
                    msgInfo.add(new MessageInfo(mi));
                }
            }
	} catch( IOException exception ) {
	    exception.printStackTrace();
	}
    }

    // Get all message info for all traces in current interval

    public void getMessageInfo( 
             Vector<MessageInfo> msgInfo, double timeLeft, double timeRight )
    {
	int progress;
        for( int i=0; i<traceCount; i++ ) {
	    progress = (int)(100.*(float)i/(float)traceCount);
            traceGraphics.setProgressBarValue (progress);
            getMessageInfo( msgInfo, i, timeLeft, timeRight );
        }
	traceGraphics.setProgressBarValue (0);
    }

    // Get all message info for the current trace at current time
    public void getMessageInfo( 
             Vector<MessageInfo> msgInfo, int traceNumber, double time )
    {
        getMessageInfo( msgInfo, traceNumber, time, time );
    }

    // Get all message info for all traces at current time

    public void getMessageInfo( 
             Vector<MessageInfo> msgInfo, double time )
    {
	int progress;
	traceGraphics.setProgressBarValue (50);
        for( int i=0; i<traceCount; i++ ) {
	    progress = (int)(100.*(float)i/(float)traceCount);
            traceGraphics.setProgressBarValue (progress);
            getMessageInfo( msgInfo, i, time );
        }
	traceGraphics.setProgressBarValue (0);
    }

    public boolean readMessageInfo( MessageInfo mi, int index )
	        throws IOException {
        if( index >= mi.msgCount ) return false;
	mi.index = index;
	return readMessageInfo( mi, debug > 0 );
    }

    public boolean readMessageInfo( MessageInfo mi, int index, boolean giveDetails )
	        throws IOException {
        if( index >= mi.msgCount ) return false;
	mi.index = index;
	return readMessageInfo( mi, giveDetails );
    }

    public boolean readMessageInfo( MessageInfo mi, boolean giveDetails )
	        throws IOException {

	if( mi.index >= mi.msgCount ) return false;

	raf.seek( mi.base + mi.index*Constants.MESSAGE_INFO_RECORDSIZE );
	int n = raf.read( msgInputBuffer );
	msgInputStream.reset();
	mi.timeStart = msgInputStream.readDouble();
	mi.timeStop  = msgInputStream.readDouble();
	mi.count     = msgInputStream.readInt();
	mi.id	     = msgInputStream.readInt();
	if( giveDetails ) {
	    mi.self  = msgInputStream.readInt();
	    mi.peer  = msgInputStream.readInt();
	    mi.type  = msgInputStream.readInt();
	    mi.dir   = msgInputStream.readInt();
	    mi.tag   = msgInputStream.readInt();
	}
	mi.index++;
	return true;
    }

    public void newProfile() {

        int stacksCount = traceAdmin[0].stacksCount;
	boolean[] active = new boolean[traceCount];
        
	if( prof == null )
            for( int j=0; j<traceCount; j++ ) active[j] = true;
	else
	    for( int j=0; j<traceCount; j++ ) active[j] = prof[j].active;

        prof       = new Profile[traceCount];

	profMin    = new Profile( stacksCount, profileTimeStart, profileTimeStop );
	profMax    = new Profile( stacksCount, profileTimeStart, profileTimeStop );
	profMin.setMax();

        if( muxSamples == null ) return;

	int n = muxSamples.size();

        for( int j=0; j<traceCount; j++ ) {
	    Profile p = new Profile( stacksCount, profileTimeStart, profileTimeStop );
	    prof[j] = p;
	    p.setActive( active[j] );

	    for( int i=0; i<n; i++ ) {
		MultiStackSample samp = 
		    (MultiStackSample) muxSamples.elementAt(i);
		if (samp.computeTime () < profileTimeStop &&
		    profileTimeStart < samp.endTime() ) {
		    double timeBegin = profileTimeStart > samp.computeTime() ?
		        	       profileTimeStart : samp.computeTime();
        	    double timeEnd   = profileTimeStop < samp.endTime() ?
		        	       profileTimeStop : samp.endTime();
                    // FIXME find out how this exception can happen
		    try {
			int id = samp.getStackID (j);
			if( id >= 0 ) prof[j].add( id, timeEnd - timeBegin );
		    } catch( NullPointerException e ) {
		    }
		}
	    }
	}
        for( int j=0; j<traceCount; j++ ) {
	    if( prof[j].active ) {
        	for( int i=0; i<stacksCount; i++ ) {
	            double time = prof[j].entries[i].time;
		    profMin.findMin( i, time );
		    profMax.findMax( i, time );
		}
	    }
	}

	profMin.scaleTime( prof[0].totalTime );
	profMax.scaleTime( prof[0].totalTime );

        for( int j=0; j<traceCount; j++ ) {
	    prof[j].sort();
	}
	traceView.traceFrame.updateProfileFrame();
    }

    public Vector<String> getTraceFilesVector() {
	    Vector<String> outVector = new Vector<String>();
	    for (String file : traceFiles) {
		    outVector.add(file);
	    }
	    return outVector;
    }

    public int getColorTableSize() {
	    return colorTableSize;
    }
}
