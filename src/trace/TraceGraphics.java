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

TraceGraphics - graphics part of Vftrace

*******************************************************************************/

package trace;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Hashtable;
import java.util.Random;
import java.util.Stack;
import java.util.Vector;
import java.io.File;
import java.io.IOException;

import message.MessageInfo;
import util.Bookmark;
import util.BottomPanel;
import util.DecimalField;
import util.DisplayElement;
import util.FunctionList;
import util.MultiStackSample;
import util.PerformanceSample;
import util.StackInfo;
import util.TimeLine;
import util.TraceAdmin;
import util.ZoomSlider;

public class TraceGraphics extends JPanel
             implements WindowFocusListener {

    public final int BAR_HEIGHT      = 5;
    public final int DEFAULT_COLORS  = 0;
    public final int MODIFIED_COLORS = 1;
    public final int GREY_COLORS     = 2;
    public final int DISPLAY_COLORS  = 3;
    public final int MIXED_COLORS    = 4;

    public boolean[]   isGray;

    private TraceFrame  frame;
    public TraceInfo   traceInfo;  // This is where all time intervals are stored
    private TimeLine    timeLine;
    private int         color_table      = DEFAULT_COLORS;
    private long        colorSeed        = 1234;
    private boolean     color_parallel   = false;
    private Color[][]   colors;
    private double      mouseTime        = 0.;
    private int         mouseTraceNumber = 0;
    private double      baseTime         = 0.;
    
    public DrawingArea  drawingArea     = null;
    public TimeAxis     timeAxis        = null;
    public vfdProgressBar progressBar     = null;
    public CornerPanel  ulCorner = new CornerPanel( Color.white );
    public CornerPanel  llCorner = new CornerPanel( Color.lightGray );
    public CornerPanel  urCorner = new CornerPanel( Color.lightGray );
    public CornerPanel  lrCorner = new CornerPanel( Color.lightGray );

    private Stack<Bookmark> bookmarks = new Stack<Bookmark>();
    boolean     generate_grey = true;
    Rectangle   vis_rect, viewRect;
    Dimension   drawingAreaSize;
    ZoomSlider  zoomSlider;

    HorizontalScrollBarMouseListener      horizontalScrollBarMouseListener;
    VerticalScrollBarMouseListener        verticalScrollBarMouseListener;
    HorizontalScrollBarAdjustmentListener horizontalScrollBarAdjustmentListener;
    VerticalScrollBarAdjustmentListener   verticalScrollBarAdjustmentListener;

    JScrollPane     scroller;
    Dimension       scrollerHeaderSize;
    JViewport       viewPort;
    Dimension       extentSize;
    public BottomPanel bottomPanel;

    public int     y_top, y_delta, y_bottom;
    public int     debug = 0;
    double  profileTimeStart, profileTimeStop;
    boolean showProfileInterval = false;
    boolean drawingAreaReady = false;
    public boolean ignoreMouseMotion = false;

    boolean painting  = false;
    boolean needMoreSamples = false;
        
    private Vector[][] samplesDetailed=null, samplesOverview=null;

    private class vfdProgressBar extends JProgressBar {
	    private double timeDone, timeTodo;

	    public vfdProgressBar (int i1, int i2) {
		super(i1, i2);
		this.timeDone = 0;
		this.timeTodo = 0;
	    }
    }
    

    public TraceGraphics (TraceFrame frame, Dimension sz, int debug) {
    
        JScrollBar scrollBar;
        Dimension size;

        this.frame     = frame;
        this.debug     = debug;

	progressBar = new vfdProgressBar (0, 100);
        setOpaque(true);
	drawingAreaSize = new Dimension(sz.width, sz.height * 8);
	size = drawingAreaSize;
	vis_rect = new Rectangle();
	vis_rect.width = size.width;

	//Set up the drawing area.
        drawingArea = new DrawingArea();
        drawingArea.addMouseListener(new MyMouseListener());
	drawingArea.addKeyListener(new MyKeyListener());
        drawingArea.addMouseMotionListener(new MyMouseMotionListener());
	timeAxis     = new TimeAxis();

        //Put the drawing area in a scroll pane.
        scroller = new JScrollPane(drawingArea);
	scroller.setHorizontalScrollBarPolicy (JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
	scroller.setVerticalScrollBarPolicy (JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
	scroller.setColumnHeaderView (timeAxis);
	scroller.setCorner (JScrollPane.UPPER_LEFT_CORNER, ulCorner);
	scroller.setCorner (JScrollPane.LOWER_LEFT_CORNER, llCorner);
	scroller.setCorner (JScrollPane.UPPER_RIGHT_CORNER, urCorner);
	scroller.setCorner (JScrollPane.LOWER_RIGHT_CORNER, lrCorner);

	viewPort = scroller.getViewport();
	viewRect = viewPort.getViewRect();

	scrollBar = scroller.getHorizontalScrollBar();
	horizontalScrollBarMouseListener = new HorizontalScrollBarMouseListener();
	scrollBar.addMouseListener( horizontalScrollBarMouseListener );
	horizontalScrollBarAdjustmentListener = new
	      HorizontalScrollBarAdjustmentListener();
	scrollBar.addAdjustmentListener( horizontalScrollBarAdjustmentListener );
	scrollBar.setUnitIncrement( size.width/10 );
	scrollBar.setBlockIncrement( size.width/2 );

	scrollBar = scroller.getVerticalScrollBar();
	verticalScrollBarMouseListener = new VerticalScrollBarMouseListener();
	scrollBar.addMouseListener( verticalScrollBarMouseListener );
	verticalScrollBarAdjustmentListener = new
	      VerticalScrollBarAdjustmentListener();
	scrollBar.addAdjustmentListener( verticalScrollBarAdjustmentListener );
	scrollBar.setUnitIncrement( size.height/20 );
	scrollBar.setBlockIncrement( size.height/10 );

        //Layout.
	//Very important: it fills the available space automatically.
        setLayout(new BorderLayout());
        add(scroller, BorderLayout.CENTER);
	
	addComponentListener( new MyListener() );

        drawingArea.setBackground(Color.white);

	Point  p = viewPort.getViewPosition();


	y_top      = size.height-p.y;
	y_delta    = viewRect.height;
	y_bottom   = y_top - y_delta;
	
	drawingArea.setPreferredSize( size );

        size.width -= 130;
        timeLine = new TimeLine( size, 10.0 );

	p.x = (timeLine.getPartWidth ()- timeLine.getViewWidth ()) / 2;
	p.y = size.height;
	viewPort.setViewPosition(p);
	viewPort.addChangeListener( new MyChangeListener() );

        drawingArea.revalidate();
	scroller.repaint();
	extentSize = viewPort.getExtentSize();

    }

    public void setZoom( double zoom ) {
        drawingArea.setZoom( zoom );
    }

    public void timeLineUpdate() {
	// Save current zoom factor for image scaling
	timeLine.saveZoom();
	// This is how much we scrolled before the samples update
	// timeLine.shiftTime( timeShift );
	// Move to middle of new image in drawing area
	Point p = viewPort.getViewPosition();
	p.x = (timeLine.getPartWidth () - timeLine.getViewWidth ()) / 2;
	viewPort.setViewPosition (p);
	timeAxis.revalidate(); // FIXME find out why this is needed
	drawingArea.repaint();
    }

    public void updateFileData() {

	if (debug > 0) System.out.println( "updateFileData" );
		    
	Point  p  = viewPort.getViewPosition();

        timeLine.newRun (traceInfo.mintime, traceInfo.maxtime);

        drawingArea.setColors();
	drawingAreaSize.height = traceInfo.rgbBufferHeight * BAR_HEIGHT;
	drawingArea.setPreferredSize (drawingAreaSize);

	p.x = (timeLine.getPartWidth () - timeLine.getViewWidth ()) / 2;
	p.y = drawingAreaSize.height;
	viewPort.setViewPosition(p);

        drawingArea.revalidate();

	this.revalidate();
	this.repaint();
        if (bottomPanel != null) {
		bottomPanel.revalidate();
	}
    }
    
    public class CornerPanel extends JPanel {

        Color bgColor;
	public CornerPanel ( Color color ) {
	    super();
	    bgColor = color;
	}

	public void paint(Graphics g) {

	    Dimension  size = getSize( null );
	    
	    Graphics2D g2 = (Graphics2D) g;
	    g2.setBackground( bgColor );
	    g2.clearRect( 0, 0, size.width, size.height );	    
	}
    }

    public class TimeAxis extends JPanel {

	double timeLeft, timeDelta, timeRight, ticksDeltaRaw,
	       timeScale, factor, ticksDelta, timeTicksLeft;
        Dimension axisSize;

	public TimeAxis() {
	    super();
	    axisSize  = new Dimension( 100, 20 );
	}

	public void paint(Graphics g) {

	    Graphics2D g2 = (Graphics2D) g;
	    Rectangle  r  = viewPort.getViewRect();
	    axisSize.width =  timeLine.getViewWidth ();
	    g2.setBackground(Color.white);
	    g2.clearRect( 0, 0, axisSize.width, axisSize.height );

	    TimeLine   tl = timeLine;
	    double     timeScale;
	    double[]   scaledSteps   = {0.1, 0.2, 0.25, 0.5, 1.0};
	    int[]      extraDecimals = {0,   1,   2,    1,   0  };
	    int[]      subTicks      = {5,   4,   5,    5,   5  };
	    int        i = 0, ticksLeft, decimals;
	    NumberFormat nf = NumberFormat.getInstance();
	    
	    // tl.timeScale in pixels per second
	    	    
	    timeLeft  = tl.partTimeStart + ((double)r.x)/tl.timeScale;
	    timeDelta = ((double) (r.width))/tl.timeScale;
	    timeRight = timeLeft + timeDelta;
	    ticksDeltaRaw = timeDelta / 8.0;
	    timeScale = 1.0;
	    decimals  = 0;
	    factor    = timeDelta < 1. ? 10. : 0.1;
	    while( !( 0.1 <=ticksDeltaRaw && ticksDeltaRaw < 1.0 )) {
	        timeScale     /= factor;
		ticksDeltaRaw *= factor;
		decimals++;
	    }
	    for( i=0; i<scaledSteps.length; i++ ) {
		if( ticksDeltaRaw < scaledSteps[i] ) break;
	    }
	    if( timeDelta < 1. ) {
	        decimals += extraDecimals[i];
		nf.setMinimumFractionDigits( decimals );
	        nf.setMaximumFractionDigits( decimals );
	    }
	    ticksDelta    = scaledSteps[i] * timeScale;
	    ticksLeft	  = (int) ( timeLeft / ticksDelta );
	    timeTicksLeft = ticksLeft * ticksDelta;
	    
	    BasicStroke stroke = new BasicStroke( 1.f );
	    Color[] col = { new Color(230,230,230), Color.white };
	    g2.setColor( Color.black );
            double tSubticks = ticksDelta / subTicks[i];
	    for( double t=timeTicksLeft ; t<timeRight; t+=ticksDelta ) {
	        int x = (int) ((t-timeLeft) * tl.timeScale);
		//Double time = new Double(t);
		//g2.drawString( nf.format(time), x, 13 );
		g2.drawString( nf.format(t), x, 13 );
		double tTicks = t;
		for( int j=0; j<subTicks[i]; j++ ) {
		    x = (int) ((tTicks-timeLeft) * tl.timeScale);
		    g2.draw( new Line2D.Float( (float)x, 20f, (float)x, 15f ) );
		    tTicks += tSubticks;
		}
	    }
	}
	
	public void save_paint(Graphics g, TimeLine tl) {

	    Graphics2D g2 = (Graphics2D) g;
	    Rectangle  r  = viewPort.getViewRect();
	    axisSize.width =  tl.getViewWidth ();
	    g2.setBackground(Color.white);
	    g2.clearRect( 0, 0, 5*axisSize.width, axisSize.height );

	    double     timeScale;
	    double[]   scaledSteps   = {0.1, 0.2, 0.25, 0.5, 1.0};
	    int[]      extraDecimals = {0,   1,   2,    1,   0  };
	    int[]      subTicks      = {5,   4,   5,    5,   5  };
	    int        i = 0, ticksLeft, decimals;
	    NumberFormat nf = NumberFormat.getInstance();
	    
	    // tl.timeScale in pixels per second

	    timeDelta = ((double) (r.width))/tl.timeScale;
	    timeLeft  = tl.partTimeStart + ((double)r.x)/tl.timeScale -2*timeDelta;
	    timeRight = timeLeft + 5*timeDelta;
	    ticksDeltaRaw = timeDelta / 8.0;
	    timeScale = 1.0;
	    decimals  = 0;
	    factor    = timeDelta < 1. ? 10. : 0.1;
	    while( !( 0.1 <=ticksDeltaRaw && ticksDeltaRaw < 1.0 )) {
	        timeScale     /= factor;
		ticksDeltaRaw *= factor;
		decimals++;
	    }
	    for( i=0; i<scaledSteps.length; i++ ) {
		  if( ticksDeltaRaw < scaledSteps[i] ) break;
	    }
	    if( timeDelta < 1. ) {
	      decimals += extraDecimals[i];
		  nf.setMinimumFractionDigits( decimals );
	      nf.setMaximumFractionDigits( decimals );
	    }
	    ticksDelta    = scaledSteps[i] * timeScale;
	    ticksLeft	  = (int) ( timeLeft / ticksDelta );
	    timeTicksLeft = ticksLeft * ticksDelta;
	    
	    BasicStroke stroke = new BasicStroke( 1.f );
	    Color[] col = { new Color(230,230,230), Color.white };
	    g2.setColor( Color.black );
        double tSubticks = ticksDelta / subTicks[i];
	    for( double t=timeTicksLeft ; t<timeRight; t+=ticksDelta ) {
	        int x = (int) ((t-timeLeft) * tl.timeScale);
		//Double time = new Double(t);
		//g2.drawString( nf.format(time), x, 13 );
  		  g2.drawString( nf.format(t), x, 13 );
		  double tTicks = t;
		  for( int j=0; j<subTicks[i]; j++ ) {
		    x = (int) ((tTicks-timeLeft) * tl.timeScale);
		    g2.draw( new Line2D.Float( (float)x, 20f, (float)x, 15f ) );
		    tTicks += tSubticks;
		  }
	    }
	}
	
	// Define header size
	public Dimension getPreferredSize() {
	    return axisSize != null ? axisSize : new Dimension( drawingArea.getSize().width, 20 );
	    //return new Dimension( drawingArea.getSize().width, 20 );
	    //return new Dimension( drawingAreaSize.width, 20 );
	}
    }

    public void traceNeedsMoreSamples () {
		traceInfo.needMoreSamples (timeLine.klone());
	}

    public void setTrace (TraceInfo traceInfo) {
		this.traceInfo = traceInfo;
	}

    public FunctionList getTraceFunctionList () {
	    return traceInfo.functionList;
    }

    public boolean traceInfoIsNull () {
	    return traceInfo == null;
    }

    public int getTraceFunctionListSize () {
	    return traceInfo.functionList.size();
    }

    public String getTraceFunctionListName(int i) {
	    return traceInfo.functionList.name(i);
    }

    //public String getTraceDataTypeName (int i) {
    //        return traceInfo.traceAdmin[0].mpiTypes[i].name;
    //}

    public void traceBuildScreenStackSampleList()
	    throws IOException {
	    try {
	    	traceInfo.buildScreenStackSampleList();
	    } catch (Exception err) {};
    }

    public double getTimelineStart () {
	    return timeLine.viewTimeStart;
    }
    
    public double getTimelineStop () {
	    return timeLine.viewTimeStop;
    }

    public double getTimelineTimescale () {
	    return timeLine.timeScale;
    }

    public double getTimelineParttimeStart () {
	    return timeLine.partTimeStart;
    }
    
    public double getTimelineParttimeStop () {
	    return timeLine.partTimeStop;
    }

    public double getTimelineTimerange () {
	    return timeLine.timeRange;
    }

    public double getTimelinePartwidth_double () {
	    return (double)timeLine.getPartWidth ();
    }

    public int getTimelinePartwidth_int () {
	    return timeLine.getPartWidth ();
    }

    public void setDefaultColors () {
	    color_table = DEFAULT_COLORS;
    }

    public void setGreyColors () {
	    color_table = GREY_COLORS;
    }

    public void setMixedColors () {
	    color_table = MIXED_COLORS;
    }

    public boolean colorIsNotMixed () {
	    return color_table != MIXED_COLORS;
    }

    public void incrementColorSeed (int incr) {
	    colorSeed += (long)(incr * 73);
    }

    public void setColorParallel (boolean val) {
	    color_parallel = val;
    }

    public Color[] getDisplayColors () {
	    return colors [DISPLAY_COLORS];
    }
    
    public Color[] getDefaultColors () {
	    return colors [DEFAULT_COLORS];
    }

    public Color[] getModifiedColors () {
	    return colors [MODIFIED_COLORS];
    }

    public Color[] getGreyColors () {
	    return colors [GREY_COLORS];
    }

    public Color getGreyColor (int i) {
	    return colors [GREY_COLORS][i];
    }


    public Color getModifiedColor (int i) {
	    return colors [MODIFIED_COLORS][i];
    }

    public void setDisplayColor (int i, Color c) {
	    colors[DISPLAY_COLORS][i] = c;
    }

    public void setModifiedColor (int i, Color c) {
	    colors[MODIFIED_COLORS][i] = c;
    }

    public double getMouseTime () {
	    return mouseTime;
    }

    public int getMouseTraceNumber () {
	    return mouseTraceNumber;
    }

    public void drawingAreaSetColors () {
	    drawingArea.setColors ();
    }

    public int getNBookmarks() {
	    return bookmarks.size();
    }

    public Vector<Bookmark> getBookmarks () {
	    return bookmarks;
    }

    public String[] getPerfNames () {
	    return traceInfo.traceAdmin[0].getPerfNames();
    }

    private void addBookmark (double time) {
	    Vector<MultiStackSample> sampleVector = traceInfo.muxSamples;
	    Vector<PerformanceSample> perfVector = traceInfo.perfSamples;
	    String[] perfNames = getPerfNames();
	    int iSample = getSampleIndex (sampleVector, time);
	    for (int iTrace = 0; iTrace < traceInfo.traceCount; iTrace++) {
	    	StackInfo[] stackArray = traceInfo.traceAdmin[iTrace].stackArray;
	    	int id = sampleVector.elementAt(iSample).getStackID(iTrace);
	    	String activeFunction = traceInfo.functionList.name (stackArray[id].functionID);
	    	PerformanceSample p = perfVector.elementAt(iSample);
	    	bookmarks.add (new Bookmark (iTrace, time, activeFunction,
				    p.getValuesDecide(iTrace, traceInfo.traceAdmin[iTrace].perfIsIntegrated())));
	    }
	    // Move to the middle of the drawing area
	    double timeShift = time - timeLine.timeMiddle;
	    timeLine.shiftTime (timeShift);
	    // Move and upplement samples
	    traceInfo.needMoreSamples (timeLine.klone());
    }

    private int getSampleIndex (Vector<MultiStackSample> sampleVector, double time) {
	int iSample, iInc;
	boolean found = false;
	int nSamples = sampleVector.size();
	iInc = traceInfo.getIInc();
	for (iSample = 0; iSample < nSamples; iSample++) {
		MultiStackSample mss = sampleVector.elementAt(iSample);
		if (mss.stackIdIsNull ()) {
			continue;
		}
		if (mss.computeTime() > time) {
			continue;
		}
		if (mss.endTime (iInc) < time) {
			continue;
		}
		found = true;
		break;
    	}
	return found ? iSample - 1 : nSamples;
     }

    private int getIdAtLevel (StackInfo[] stackArray, int firstId, int level) {
	int id = firstId;
	for (int i = stackArray[id].depth - 1; i > level; i--) {
		id = stackArray[id].caller;
	}
	return id;
    }

    public void setProgressBarValue (int value) {
	    progressBar.setValue (value);
    }

    public void setProgressBarTotalTime (double timeTodo) {
	    progressBar.timeTodo = timeTodo;
    }

    public void updateProgressBarDiff (double delta) {
	    progressBar.timeDone += delta;
	    int percentageDone = (int) (progressBar.timeDone * 100 / progressBar.timeTodo);
	    progressBar.setValue (percentageDone);
    }

    public void updateProgressBarAbs (double timeDone) {
	    progressBar.timeDone = timeDone;
	    int percentageDone = (int) (progressBar.timeDone * 100 / progressBar.timeTodo);
	    progressBar.setValue (percentageDone);
    }

    public void resetProgressBar () {
	    progressBar.timeDone = 0;
	    progressBar.setValue (0);
    }

    public class DrawingArea extends JPanel {

	public DrawingArea() {
	    super();
	    setFocusable( true );
	}

        public void setZoom( double tgZoomX ) {

/*
        Point p = viewPort.getViewPosition();
	Dimension s = viewPort.getViewSize();
	int w = s.width;
	p.x = (timeLine.partWidth - timeLine.viewWidth) / 2;
	s.width = timeLine.partWidth;
	viewPort.setViewPosition(p);
	viewPort.setViewSize(s);
	System.out.println( "=== vp width incr from "+w+" to " + s.width );
*/
	    timeLine.setZoom( tgZoomX );
	    drawingArea.repaint();

	}

        public void setColors() {
            int n = traceInfo.getColorTableSize();

	    // Generate distinctive colors

	    colors = new Color[4][n];
	    isGray = new boolean[n];
	    Color cc, cg;

	    long seed = colorSeed;
	    for (int j = 0; j < n; j++) {
	        int red, green, blue, grey;
		// Use function index as seed for default colors
	        colorSeed = (long)traceInfo.functionList.hashcode(j);
		Random randomFixed = new Random (colorSeed);
		red   = Math.abs (randomFixed.nextInt() % 256);
		green = Math.abs (randomFixed.nextInt() % 256);
		blue  = Math.abs (randomFixed.nextInt() % 256);
		grey  = Math.abs (randomFixed.nextInt() % 200);
		cc = new Color (red, green, blue);
		cg = new Color (grey, grey, grey);
		colors[DEFAULT_COLORS ][j] = cc;
		colors[MODIFIED_COLORS][j] = cc;
		colors[DISPLAY_COLORS ][j] = cc;
		colors[GREY_COLORS    ][j] = cg;
		isGray[j] = false;
	    }
	    
	    // Update the stack miniature images
	    setStackImages();

        }

        public void setModifiedColors() {
            int n = traceInfo.getColorTableSize();

	    // Generate distinctive colors

	    Color cc, cg;

	    Random random = new Random (colorSeed);
	    for (int j = 0; j < n; j++) {
	        int red, green, blue, grey;
		red   = Math.abs (random.nextInt() % 256);
		green = Math.abs (random.nextInt() % 256);
		blue  = Math.abs (random.nextInt() % 256);
		grey  = Math.abs (random.nextInt() % 200);
		cc = new Color (red, green, blue);
		cg = new Color (grey, grey, grey);
		colors[MODIFIED_COLORS][j] = cc;
		colors[GREY_COLORS][j] = cg;
	    }
	    
	    // Update the stack miniature images
	    setStackImages();

        }

	public void setColorsGray() {
	    int n = traceInfo.getColorTableSize();
	    for (int i = 0; i < n; i++) {
		colors[DISPLAY_COLORS][i] = colors[GREY_COLORS][i];
		isGray[i] = true;
	    }
	}

        public void setStackImages() {
	    for (int traceID = 0; traceID < traceInfo.traceCount; traceID++) {
		StackInfo[] stackArray = traceInfo.traceAdmin[traceID].stackArray;
		for (int i = 0; i < stackArray.length; i++) {
	            stackArray[i].setPixelMap (colors[DISPLAY_COLORS], stackArray);
		}
	    }
        }

        // Mark stacks with a specific function in call stack
        public void markStacks (int func) {
	    int traceID = 0;
	    StackInfo[] stackArray = traceInfo.traceAdmin[traceID].stackArray;
	    for (int i = 0; i < stackArray.length; i++) {
	        stackArray[i].markStack (func, stackArray);
	    }
        }

        // Set stacks as unmarked
        public void unmarkStacks() {
	    int traceID = 0;
	    StackInfo[] stackArray = traceInfo.traceAdmin[traceID].stackArray;
	    for (int i = 0; i < stackArray.length; i++) {
	        stackArray[i].marked = false;
	    }
        }


        public void refresh() {
	    traceInfo.needMoreSamples (timeLine.klone());
	}

        public void centerRegion() {
	    // Move to the middle of the drawing area
	    double time = (profileTimeStart + profileTimeStop) * 0.5;
	    double timeShift = time - timeLine.timeMiddle;
	    double zoomFactor =  timeLine.timeZoom * timeLine.viewTime /
	                        (profileTimeStop - profileTimeStart);
	    timeLine.shiftTime(timeShift );
	    timeLine.setZoom  (zoomFactor);
	    zoomSlider.update (zoomFactor);
	    // Move and supplement samples
	    traceInfo.needMoreSamples (timeLine.klone());
	}


        public void clearBookmarks() {
	    bookmarks.clear();
	    drawingArea.repaint();
	}
	
	public Dimension getPreferredSize() {
	    return drawingAreaSize;
	}

        private void howDidIgetHere( int[] a ) {
	    try {
	        a[0] = 0;
	    } catch( Exception e ) {
	        e.printStackTrace();
	    }
	}

	private Graphics2D paintFrameAndHeaders (Graphics g) {
	    
	    if (traceInfo == null) {
	        System.out.println( "paint: no trace info defined" );
		return null;
	    }

	    if (!drawingAreaReady) {
	        drawingAreaReady = true;
		traceInfo.needMoreSamples (timeLine.klone());
	    }
	    if (debug > 1) System.out.println ("paint: drawing area ready");

	    viewPort = scroller.getViewport();
	    viewRect = viewPort.getViewRect();

	    if( debug>0 ) System.out.println( 
	        "paint: viewRect="+viewRect );

            if (g == null) return null;
	    if (debug > 1) System.out.println ("paint: g valid");

	    Graphics2D g2 = (Graphics2D) g;
	    g2.setBackground(Color.white);
	    g2.clearRect( viewRect.x, viewRect.y, viewRect.width, viewRect.height );
	    timeAxis.repaint();

	    if (!traceInfo.haveSamples) return null;
	    if (debug > 1) System.out.println ("paint: samples present");

	    if (colors == null) return null;
	    if (debug > 1) System.out.println ("paint: colors valid");

	    if (traceInfo.functionList == null ) return null;
	    if (debug > 1) System.out.println ("paint: functionList valid");

	    return g2;

	}

	private Graphics2D save_paintFrameAndHeaders (Graphics g, TimeLine tl) {
	    
	    if (traceInfo == null) return null;


	    if (!drawingAreaReady) {
	        drawingAreaReady = true;
	        traceInfo.needMoreSamples (tl.klone());
	    }
	    viewPort = scroller.getViewport();
	    viewRect = drawingArea.getBounds();

        if (g == null) return null;

	    Graphics2D g2 = (Graphics2D) g;
	    g2.setBackground(Color.white);
	    g2.clearRect( 0, 0, viewRect.width, viewRect.height );
	    timeAxis.save_paint(g2, tl);

	    if (!traceInfo.haveSamples) return null;
	    
	    if (colors == null) return null;
	    
	    if (traceInfo.functionList == null ) return null;
	    
	    return g2;
	}
	
	public void paintProfileInterval (Graphics2D g2, double h, TimeLine tl) {
	    g2.setPaint( new Color( 128, 128, 255, 80 ) );
	    double time1 =  profileTimeStart; 
	    double time2 =  profileTimeStop; 
	    double x1 = (time1 - tl.partTimeStart) * tl.timeScale;
	    double x2 = (time2 - tl.partTimeStart) * tl.timeScale;
	    Rectangle2D rect = new Rectangle2D.Double (x1, 0., x2 - x1, h);
	    g2.fill (rect);
	}
		
	public void paintTickLines (Graphics2D g2, TimeLine tl, double h) {
	    // Set linewidth: one pixel
    	    double lineWidth = 1.;	    
	    BasicStroke stroke = new BasicStroke ((float)lineWidth);
	    g2.setStroke (stroke);
	    g2.setPaint (Color.gray);
	    for (double t=timeAxis.timeTicksLeft ; 
	                t<timeAxis.timeRight; 
	        	t+=timeAxis.ticksDelta) {
	        double scaledTime = (t - tl.partTimeStart) * tl.timeScale;
	        Line2D line = new Line2D.Double (scaledTime, 0.d, scaledTime, h);
	        g2.draw (line);
	    }
	}

	public void paintAllBookmarks (Graphics2D g2, TimeLine tl, double h) {
		g2.setPaint (Color.red);
	        double timeLeft  = tl.viewTimeStart;
	        double timeRight = tl.viewTimeStop;
		for (Bookmark b : bookmarks) {
	            double time =  b.getTime(); 
		    if (time  > timeLeft  && time <= timeRight) {
			double scaledTime = (time - tl.partTimeStart) * tl.timeScale;
			Line2D line = new Line2D.Double (scaledTime, 0.d, scaledTime, h);
			g2.draw (line);
		    }
		}
	}

	public void paintNewestBookmark (Graphics2D g2, TimeLine tl, double h) {
		g2.setPaint (Color.red);
		double timeLeft  = tl.viewTimeStart;
	        double timeRight = tl.viewTimeStop;
		Bookmark b = bookmarks.peek();
	        double time =  b.getTime(); 
		if (time  > timeLeft  && time <= timeRight) {
		    double scaledTime = (time - tl.partTimeStart) * tl.timeScale;
		    Line2D line = new Line2D.Double (scaledTime, 0.d, scaledTime, h);
		    g2.draw (line);
		}
	}

	public void zoomImage (Image image, AffineTransform imageTransform, 
			double zoomFactor, TimeLine tl, DisplayElement d) {
		Image subImage;
		int height = d.height; 
		int offset, width;
		double xShift = 0.;
	 	if (zoomFactor > 1.) {
	 	    width  = Math.max (1, (int)((double)tl.getViewWidth () / zoomFactor));
	 	    offset = (tl.getPartWidth () - width) / 2;
	 	    subImage = d.getSubImage (offset, 0, width, height);
	 	    image = subImage.getScaledInstance (tl.getViewWidth (), height, Image.SCALE_FAST);
	 	    xShift = (tl.getPartWidth () - tl.getViewWidth ()) / 2;
	 	} else if (zoomFactor < 1.) {
	 	    width  = Math.max (1, (int)((double)tl.getPartWidth () * zoomFactor));
	 	    image = d.getScaledImageInstance (width, height, Image.SCALE_FAST);
	 	    xShift = (double)(tl.getPartWidth () / 2) * (1. - zoomFactor);
	 	}
		imageTransform.translate (xShift, 0.);
	}

	public void paintSamples (Graphics2D g2, TimeLine tl) {
		/*  
		 *  Paints the main picture: Stacks and performance values.
		 *  The display lists contains stack and performance information
		 *  alternating. Thus, even indices (starting at zero) correspond
		 *  to performance plots, and odd ones to stack traces.
		 *
		 *  The original image, created in buildScreenStackSampleList,
		 *  assumes a width of one pixel for each entry in the stack.
		 *  This image is then scaled with a global factor (default
		 *  value 5), using an affine transformation imageTransform.
		 *
		 *  Applied to the performance plots, this leads to
		 *  very large bands where only a line is desired. Therefore,
		 *  a different scaling factor is applied to these plots. The
		 *  two values are pixelScaleStack and pixelScalePerf, chosen
		 *  for each extended trace index. 
		 *   
		 *  Using two different scaling factors leads to the problem
		 *  of the plots changing their relative position with respect
		 *  to each other. For this reason, in addition to the scaling
		 *  transformation, a translation needs to be carried out, too.
		 *  For the form of the transformation matrix, see below.
		 *
		 *  The offsets for the translation are computed by considering
		 *  how the window limits are moved under scaling with both
		 *  pixelScaleStack and pixelScalePerf. These window limits
		 *  define bins, and when being scaled, a plot has to stay
		 *  in its respective bin. For example, the bins for an
		 *  unscaled image of two trace files (four display elements)
		 *  are made up by the intervals in [11, 22, 33, 44] (0 implied).
		 *  This way, the second performance plot lies between the y coordinates
		 *  22 and 33.
		 *  Scaling yields [22, 44, 66, 88] and [55, 110, 165, 220], respectively.
		 *  The second performance plot now lies between 44 and 66, although
		 *  according to the other list it should be between 110 and 165.
		 *  Therefore, we need to shift the entire plot by an amount
		 *  of 110 - 44 = 66. In general, the offset of a performance plot
		 *  is given by the difference of the two lists. Below, it is
		 *  represented in scaledOffset.
		 *
		 *  As a final step in this routine, the current zoom factor is
		 *  obtained. If it is different from 1, the zoomImage method
		 *  is called, where an additional image transformation for
		 *  horizontal translations is applied.
		 */

	        traceInfo.setLock();
		Vector displayList = (Vector)traceInfo.displayList.clone();
		traceInfo.unsetLock();

		int nSections = displayList.size();
		int origBins[] = new int[nSections];
		int scaledBinsStack[] = new int[nSections];
		int scaledBinsPerf[] = new int[nSections];
		int scaledOffset[] = new int[nSections];
		int windowSize = traceInfo.rgbBufferHeight / nSections;
		int pixelScaleStack = 5;
		int pixelScalePerf = 2;
		int thisPixelScale;	
		for (int i = 0; i < nSections; i++) {
			origBins[i] = i * windowSize;
		        scaledBinsStack[i] = origBins[i] * pixelScaleStack;
			scaledBinsPerf[i] = origBins[i] * pixelScalePerf;
			scaledOffset[i] = i % 2 == 0 ? (scaledBinsStack[i] - scaledBinsPerf[i]) : 0;
		}	

		if (debug > 0) System.out.println ("paint: displayList.size() = " + nSections);
		for (int k = 0; k < nSections; k++) {
	            DisplayElement d = (DisplayElement)displayList.elementAt(k);
		    if (debug > 0) {
			    System.out.println ("paint: d.time=" + d.time + " delta=" + d.delta);
		    }
		    if (d.imageIsNull ()) {
		        System.out.println ("no image");
		    } else {
			Image image = d.getImage ();
			/* As described above, we first scale the image with
			 * different values for odd and even indices. To
			 * correct the offset created this way, the translation
			 * amount nOff is obtained from the scaledOffset array.
			 *
			 * ATTENTION: Operations registered last are applied to
			 * the image first. Thus, imageTransform first scales
			 * the image and then translates the performance plots.
			 * The operations do not commute, changing their order
			 * gives an offset of thisPixelScale * nOff instead
			 * of nOff.
			 *
			 * Alternatively, one can also define the transformation
			 * matrix directory. The corresponding command is
			 *       imageTransform = new AffineTransform (1.0, 0.0, 0.0,
			 *			           thisPixelScale, 0.0, nOff);
			 *
			 */
			thisPixelScale = k % 2 == 0 ? pixelScalePerf : pixelScaleStack;
			int nOff = k % 2 == 0 ? scaledOffset[k] : 0;
			AffineTransform imageTransform = new AffineTransform ();
			imageTransform.translate (0, nOff);
			imageTransform.scale (1, thisPixelScale);
			double zoomFactor = tl.zoomChange();
			if (zoomFactor != 1.0) {
				zoomImage (image, imageTransform, zoomFactor, tl, d);
			}
			g2.drawImage (image, imageTransform, null);
		    }
		}
	}
		
	public void paint(Graphics g) {
	    Graphics2D g2 = paintFrameAndHeaders(g);
	    if (g2 == null) return;
	    // Copy the display list

	    vis_rect = drawingArea.getVisibleRect();

	    // Need to recalculate, because the viewport may have changed
	    // since the scrollbar adjustment

	    // Copy required?
	    TimeLine tl = timeLine;

            // First paint profile interval
	    Dimension size = getSize (null);
            double h = size.height;
	    if (showProfileInterval) {
		    paintProfileInterval (g2, h, tl);
	    }

	    g2.setColor (Color.black);

        if (traceInfo.haveSamples) {
		    paintSamples (g2, tl);
	    }

	    paintTickLines (g2, tl, h);

	    if (bookmarks != null && !bookmarks.isEmpty()) {
		    if (false) {
		    	paintAllBookmarks (g2, timeLine, h);
		    } else {
		    	paintNewestBookmark (g2, timeLine, h);
		    }
	    }
	}
	
	public void save_paint(Graphics g, TimeLine tl) {
	    Graphics2D g2 = save_paintFrameAndHeaders(g,tl);
	    if (g2 == null) return;
	    // Copy the display list

	    vis_rect = drawingArea.getVisibleRect();

	    // Need to recalculate, because the viewport may have changed
	    // since the scrollbar adjustment


       // First paint profile interval
	    Dimension size = getSize (null);
            double h = size.height;
	    if (showProfileInterval) {
	    	paintProfileInterval (g2, h, tl);
	    }

	    g2.setColor (Color.black);

        if (traceInfo.haveSamples) {
		    paintSamples (g2, tl);
	    }

	    paintTickLines (g2, tl, h);

	    if (bookmarks != null && !bookmarks.isEmpty()) {
		    if (false) {
		    	paintAllBookmarks (g2, tl, h);
		    } else {
		    	paintNewestBookmark (g2, tl, h);
		    }
	    }
	}
	
	public void save_image(File p_file, String p_type) {
		int save_image_width=drawingArea.getWidth();
		int save_image_height=drawingArea.getHeight();
			
		timeAxis.revalidate(); // FIXME find out why this is needed
		
		BufferedImage save_image2 = new BufferedImage(save_image_width,save_image_height,BufferedImage.TYPE_INT_ARGB);
		Graphics save_g = save_image2.getGraphics();		
		drawingArea.save_paint(save_g,timeLine);
		
		File outputfile = p_file;
		int x = save_image_width/5*2;
		int y = 0;
		int width  = save_image_width/5;
		int height = save_image_height;
		BufferedImage sub_save_image2 = save_image2.getSubimage(x, y, width, height);
		
		try {
			ImageIO.write(sub_save_image2, p_type, outputfile);
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    }

    class MyChangeListener implements ChangeListener {
    
        public void stateChanged( ChangeEvent e ) {
	    /*
	    System.out.println( "viewport change event = " + e );
	    Dimension size = viewPort.getExtentSize();
	    timeLine.adjustWidth( size.width );
	    size = viewPort.getViewSize();
	    size.width = timeLine.partWidth;
	    //viewPort.setViewSize( size );
	    */
	}
    }

    class MyMouseMotionListener extends MouseMotionAdapter {

        public void mouseDragged(MouseEvent e) {
            int    x    = e.getX();
	    profileTimeStop = timeLine.partTimeStart + 
	                    (double) x / timeLine.timeScale;
	    drawingArea.repaint();
        }

	private int getTraceNumber (int yOffset) {
	    if (yOffset >= traceInfo.rgbBufferHeight) {
		return 0;
	    } else {
            	int i;
	    	for (i = traceInfo.traceCount - 1; i >= 0; i--) {
	    	    int nextLevel;
	    	    if (i == 0) {
	    	    	nextLevel = traceInfo.rgbBufferHeight;
	    	    } else {
	    	    	nextLevel = traceInfo.traceAdmin[i-1].getPosition();
	    	    }
	    	    if (yOffset < nextLevel) {
	    	    	break;
	    	    }
	    	}
	    	return i;	    
	    }
	}


	public void mouseMoved(MouseEvent e) {
            if( ignoreMouseMotion ) return;
	    Dimension size = drawingAreaSize;
            int x = e.getX();
            int y  = size.height - e.getY();
	    int yOffset = (int)((double)y / BAR_HEIGHT);
	    int task = 0;
	    int thread = 0;
	    int level = 0;
	    int i, j;
	    int step = 0;
	    int offset = 0;
	    int number = 0;
	    int traceNumber;
	    int count=0, id=0, idMsg=0, type=0, peer=0, tag=0;
	    int sid = -1;
	    String description = "";
	    String output = "";
	    double mflops = 0.;
	    String filepath = "";
	    double time = timeLine.partTimeStart + 
	                    (double) x / timeLine.timeScale;
	    MessageInfo mi = null;
	    
            if (traceInfo == null || !traceInfo.haveSamples || yOffset < 0) {
		    return;
	    }
	    
	    int traceCount = traceInfo.traceCount;

	    task = thread = level = -1;
	    traceNumber = getTraceNumber (yOffset);

            mouseTime        = time;
            mouseTraceNumber = traceNumber;

	    StackInfo[] stackArray = traceInfo.traceAdmin[traceNumber].stackArray;
	    task     = traceInfo.traceAdmin[traceNumber].task;
	    thread   = traceInfo.traceAdmin[traceNumber].thread;
	    offset   = traceInfo.traceAdmin[traceNumber].getPosition();
	    filepath = traceInfo.traceAdmin[traceNumber].pathname;
	    level    = yOffset - offset;
	    
	    if( level < 0 ) {
		    return; // Below the first trace: nothing there
		}

	    Vector<MultiStackSample> sampleVector = traceInfo.muxSamples;

            if( sampleVector == null ) return;

	    Vector<PerformanceSample> perfVector = traceInfo.perfSamples;

	    int n = sampleVector.size();
	    boolean mpi = false;

	    int iSample;
	    MultiStackSample samp;
	    String[] perfNames = getPerfNames();
	    double perfValue;
	    iSample = getSampleIndex (sampleVector, time);
	    if (iSample < n) {
	        samp = sampleVector.elementAt(iSample);
		id = samp.getStackID (traceNumber);
		sid = id;
		int windowHeight = traceInfo.rgbBufferHeight / (2 * traceCount);
		//if( id == -1 ) break; // Beyond end of trace
                try {
		  if (level < stackArray[id].depth) {
		      id = getIdAtLevel (stackArray, id, level);
		      if(!(frame.showPreciseStacks && stackArray[id].funcPrecise[level])) {
			  number  = stackArray[id].functionID;
			  mpi     = stackArray[id].mpi;
			  description = "Function: " + traceInfo.functionList.name (number);
		      }
		  } else if (level > windowHeight){ // Performance Plot?
			PerformanceSample perfSample = perfVector.elementAt(iSample);
			for (i = 0; i < perfSample.getNPerfTypes(); i++) {
				if (traceInfo.showPerfGraph(i)) {
					boolean integrated = traceInfo.traceAdmin[traceNumber].perfIsIntegrated(i);
					description += perfNames[i] + ": " + String.format ("%6.1e", perfSample.getValueDecide (i, traceNumber, integrated)) + " ";
				}
			}
		  }
                } catch( ArrayIndexOutOfBoundsException ex ) {
                  System.out.println(
                    "MyMouseMotionListener: Bad id="+id+" traceNumber="+traceNumber );
	        }
	    } else {
		    // Describe how a too small zoom factor can lead to no samples
		    //System.out.println ("Error: no sample found");
	    }

	    if( frame.showPreciseStacks ||
	        (frame.commTableFrame != null && frame.commTableFrame.isVisible()) ) {
	    	mi = traceInfo.findPreciseSample( traceNumber, time, frame );
	    }
	    if( frame.showPreciseStacks ) {
		if( mi.index < mi.msgCount ) {
		    id = mi.id;
		    if( level < stackArray[id].depth ) {
			for( i=stackArray[id].depth-1; i>=0; i-- ) {
			    if( i == level )break;
			    id = stackArray[id].caller;
			}
			number = stackArray[id].functionID;
			description   = traceInfo.functionList.name (number);
		    }
		}
	    }

	    if( frame.commTableFrame.isVisible() ||
                frame.commGraphFrame.isVisible()   )
	        traceInfo.updateCommFrames();

	    if( sid != -1 &&
	        frame.profileFrame != null  ) {
		frame.profileFrame.highlightEntry( traceNumber, sid );
	    }

	    if (bottomPanel != null) {
	    	bottomPanel.setTimeFieldValue (time - baseTime);
	    	bottomPanel.setDescriptionText (description);
	    	bottomPanel.setFileNameText (filepath);
	    }
	    
	}
    }

    class MyListener extends ComponentAdapter {

        public void componentResized(ComponentEvent e) {
	    Dimension size = viewPort.getExtentSize();
	    Dimension viewSize;
	    if( size.width == 0 ) return;
	    timeLine.adjustWidth( size.width );
	    viewSize = viewPort.getViewSize();
	    extentSize = size;
	    viewSize.width = timeLine.getPartWidth ();
	    size = drawingArea.getSize();
	    size.width = viewSize.width;
	    drawingAreaSize.width = viewSize.width;
	    drawingArea.setSize( size );
	    viewPort.setViewSize( viewSize );
	    Point p = viewPort.getViewPosition();
	    p.x = (timeLine.getPartWidth () - timeLine.getViewWidth ()) / 2;
	    viewPort.setViewPosition( p );
	    traceInfo.needMoreSamples( timeLine.klone() );
        }
    }

    // FIXME: work around a problem caused in the highlighted 
    // profile entries, which appear to be slightly wrong. By
    // adding this  listener, the problem is hidden by
    // bluntly refreshing the drawing area.
    public void refresh() {
	traceInfo.needMoreSamples( timeLine.klone() );
    }

    // MyKeyListener supports time measurement by pressing the
    // shift key and moving the mouse to the end of the interval.
    class MyKeyListener extends KeyAdapter {
        public void keyPressed( KeyEvent e ) {
	    if( e.getKeyCode() == e.VK_SHIFT ) {
	        // Test if already pressed: ignore autorepeat
		if( baseTime == 0. ) {
		    baseTime = mouseTime;
		    bottomPanel.setTimeFieldValue (0.);
		}
	    } else if( e.getKeyCode() == e.VK_ESCAPE ) {
	        ignoreMouseMotion = true;
	    }
	}
        public void keyReleased( KeyEvent e ) {
	    if( e.getKeyCode() == e.VK_SHIFT ) {
		baseTime = 0.;
		bottomPanel.setTimeFieldValue (0.);
	    }
	}
    }

    class MyMouseListener extends MouseInputAdapter {

        public void mouseEntered (MouseEvent e) {
            ignoreMouseMotion = false;
	    drawingArea.requestFocus();
	}

        public void mousePressed (MouseEvent e) {

            if (SwingUtilities.isRightMouseButton(e)) {

		frame.colorFrame.functionColorChooser();

            } else if (SwingUtilities.isLeftMouseButton(e)) {

        	if (traceInfo == null) return;
        	int x = e.getX();
		profileTimeStart = timeLine.partTimeStart + 
	                	(double) x / timeLine.timeScale;
	        showProfileInterval = true;
            }
	}
        public void mouseReleased(MouseEvent e) {

            if (!SwingUtilities.isLeftMouseButton(e)) return;
            if (traceInfo == null) return;

            int x = e.getX();
	    double time = timeLine.partTimeStart + 
	                    (double) x / timeLine.timeScale;

	    if (time == profileTimeStart) {
	        profileTimeStart = traceInfo.profileTimeStart;
		addBookmark (time);
		ignoreMouseMotion = true;
	    } else {
	        profileTimeStop = time;
	        traceInfo.profileTimeStart = profileTimeStart;
	        traceInfo.profileTimeStop  = profileTimeStop;
		drawingArea.centerRegion(); 
	    }
	    drawingArea.repaint();

	}
    }

    class HorizontalScrollBarMouseListener extends MouseInputAdapter {
	private boolean buttonDown, buttonClick;

        HorizontalScrollBarMouseListener() {
	    buttonDown = false;
	    buttonClick = false;
	}

	public void mousePressed(MouseEvent e) {
            if (!SwingUtilities.isLeftMouseButton(e)) return;
            buttonDown = true;
	    buttonClick = true;
	}

        public void mouseReleased(MouseEvent e) {
            if( !SwingUtilities.isLeftMouseButton(e) ) return;
	    buttonDown = false;
	    buttonClick = true;
	    timeShift();
	}
        public boolean grabbed() {
	    return buttonDown;
	}
        public boolean clicked() {
	    boolean returnValue = buttonClick;
	    buttonClick = false;
	    return returnValue;
	}
    }

    public void timeShift() {
	int x = (timeLine.getPartWidth () - timeLine.getViewWidth ()) / 2;
	double xDelta    = viewPort.getViewPosition().x - x;
        double timeShift = xDelta * timeLine.viewTime / 
	                       (double)timeLine.getViewWidth ();
        //timeShift = xDelta * timeLine.timeScale; FIXME: why is this wrong?
	timeLine.shiftTime( timeShift );
	traceInfo.needMoreSamples( timeLine.klone() );
    }

    class VerticalScrollBarMouseListener extends MouseInputAdapter {
	private boolean buttonDown, buttonClick;

        VerticalScrollBarMouseListener() {
	    buttonDown = false;
	    buttonClick = false;
	}

	public void mousePressed(MouseEvent e) {
            if( !SwingUtilities.isLeftMouseButton(e) ) return;
            buttonDown = true;
	    buttonClick = true;
	}

        public void mouseReleased(MouseEvent e) {
            if( !SwingUtilities.isLeftMouseButton(e) ) return;
	    buttonDown = false;
	    buttonClick = true;
	}
        public boolean grabbed() {
	    return buttonDown;
	}
        public boolean clicked() {
	    boolean returnValue = buttonClick;
	    buttonClick = false;
	    return returnValue;
	}
    }

    class HorizontalScrollBarAdjustmentListener implements AdjustmentListener{

        HorizontalScrollBarAdjustmentListener() {}

	public void adjustmentEvent(AdjustmentEvent event) {
            int type = event.getAdjustmentType();
            if( debug>4 ) System.out.println( "horizontal scrollbar adjustmentEvent: "+type );
	}
	public void adjustmentValueChanged(AdjustmentEvent event) {
            int type = event.getAdjustmentType();
	    if( traceInfo == null ) return;
            if( debug>1 ) System.out.println( 
		"horizontal scrollbar adjustmentValueChanged: "+type+" drawingArea="+drawingArea );
            drawingArea.revalidate();
	    if( drawingArea != null && traceInfo.haveSamples ) {
		if( debug>1 ) {
		    System.out.println( "horizontal scrollbar adjustmentValueChanged: "+type );
	        }
	    }
	}
    }

    class VerticalScrollBarAdjustmentListener implements AdjustmentListener {

        VerticalScrollBarAdjustmentListener() {}

	public void adjustmentEvent(AdjustmentEvent event) {
            int type = event.getAdjustmentType();
            if( debug>1 ) System.out.println( "vertical scrollbar adjustmentEvent: "+type );
	}
	public void adjustmentValueChanged(AdjustmentEvent event) {
            int type = event.getAdjustmentType();
	    if( traceInfo == null ) return;
	    viewRect   = viewPort.getViewRect();
	    Point    p = viewPort.getViewPosition();
	    y_top      = drawingAreaSize.height-p.y;
	    y_delta    = viewRect.height;
	    y_bottom   = y_top - y_delta;
            drawingArea.revalidate();
            if( traceInfo != null && traceInfo.haveSamples ) {
		if( debug>1 ) {
		    System.out.println( "vertical scrollbar adjustmentValueChanged: "+type );
	            System.out.println( "y_top="+y_top+" y_bottom="+y_bottom );
		    System.out.println( "versb: viewRect="+viewRect );
	        }
	    }
	}
    }
    
    public void showTrace( int traceNumber ) {
	    viewRect = viewPort.getViewRect();
	    int height = drawingAreaSize.height;
	    int pos    = traceInfo.traceAdmin[traceNumber].getPosition() * BAR_HEIGHT;
	    Point p = new Point( 
	               viewRect.x,
	               Math.max( 0, height - pos - viewRect.height ) );
            viewPort.setViewPosition( p );
	    drawingArea.revalidate();

    }

    public void windowGainedFocus( WindowEvent e ) {
	//trace.needMoreSamples( timeLine.klone() );
    }
    public void windowLostFocus( WindowEvent e ) {
    }
}
