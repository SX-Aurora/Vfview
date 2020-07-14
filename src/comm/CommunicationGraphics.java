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

package comm;

import java.io.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.text.*;
import java.util.*;
import javax.swing.SwingUtilities;

import util.*;
import trace.*;

public class CommunicationGraphics extends JPanel
             implements WindowFocusListener {

    public final int TASK_SIZE       = 10;
    public final int HORIZONTAL      =  1;
    public final int VERTICAL        =  2;
    public final int DEFAULT_COLORS  =  0;
    public final int MODIFIED_COLORS =  1;
    public final int GREY_COLORS     =  2;
    public final int DISPLAY_COLORS  =  3;
    public final int MIXED_COLORS    =  4;

    public CommGraphFrame
                        frame;
    public TraceInfo    trace;
    public DrawingArea  drawingArea  = null;
    public TaskAxis     horTaskAxis  = null;
    public TaskAxis     verTaskAxis  = null;
    public TaskLine     taskLine     = null;
    public CornerPanel  ulCorner = new CornerPanel( "Dest"   );
    public CornerPanel  llCorner = new CornerPanel( "Source" );
    
    CommInfo    commInfo;

    boolean     generate_grey = true;
    Rectangle   viewRect;
    Dimension   drawingAreaSize, originalSize;
    ZoomSlider  zoomSlider;
    JScrollPane scroller;
    Dimension   scrollerHeaderSize;
    JViewport   viewPort;

    RelScrollbarPosition                  relScrollbarPosition;

    HorizontalScrollBarMouseListener      horizontalScrollBarMouseListener;
    VerticalScrollBarMouseListener        verticalScrollBarMouseListener;
    HorizontalScrollBarAdjustmentListener horizontalScrollBarAdjustmentListener;
    VerticalScrollBarAdjustmentListener   verticalScrollBarAdjustmentListener;


    public int     debug = 0;
    int            cells = 0;
    boolean        drawingAreaReady = false;
    Dimension      size  = new Dimension( 280, 280 );

    private DecimalFormat taskFormat;
        
    public CommunicationGraphics(
             CommGraphFrame frame, int debug  ) {

        JScrollBar scrollBar;
	
	relScrollbarPosition = new RelScrollbarPosition( 0.5, 0.5 );

        this.frame     = frame;
        this.debug     = debug;
	trace    = frame.traceInfo;
        commInfo = frame.commInfo;

	taskFormat = new DecimalFormat();
	taskFormat.setMinimumFractionDigits( 0 );
	taskFormat.setMaximumFractionDigits( 0 );
	taskFormat.setGroupingSize(1000000000);
        setOpaque(true);

	//Set up the drawing area.
        drawingArea  = new DrawingArea();
	originalSize = new Dimension( size );
        drawingArea.setBackground(Color.white);
	drawingArea.setPreferredSize( originalSize );

        drawingArea.addMouseListener(new MyMouseListener());
        drawingArea.addMouseMotionListener(new MatrixMotionListener());
        drawingArea.setFocusable(true);
        drawingArea.requestFocus();
	drawingArea.addKeyListener(new MyKeyListener());

	horTaskAxis = new TaskAxis( HORIZONTAL );
	verTaskAxis = new TaskAxis( VERTICAL   );
        horTaskAxis.addMouseMotionListener(new AxisMotionListener(HORIZONTAL));
        verTaskAxis.addMouseMotionListener(new AxisMotionListener(VERTICAL  ));

        //Put the drawing area in a scroll pane.
        scroller = new JScrollPane(drawingArea);
	scroller.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS );
	// Task header field gets screwed up if we lose the vertical scrollbar
	scroller.setVerticalScrollBarPolicy  ( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS );
	scroller.setColumnHeaderView( horTaskAxis );
	scroller.setRowHeaderView   ( verTaskAxis );
	scroller.setCorner( JScrollPane.UPPER_LEFT_CORNER,  ulCorner    );
	scroller.setCorner( JScrollPane.LOWER_LEFT_CORNER,  llCorner    );

	viewPort = scroller.getViewport();
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
	scrollBar.setUnitIncrement( size.height/10 );
	scrollBar.setBlockIncrement( size.height/2 );

        //Layout.
	//Very important: it fills the available space automatically.
        setLayout(new BorderLayout());
        add(scroller, BorderLayout.CENTER);
	
	viewRect = viewPort.getViewRect();

        drawingArea.revalidate();
	scroller.repaint();
	
    }
    
    public void setCells(int cells) {
        this.cells = cells;
        taskLine = new TaskLine( size, cells );
    }

    public void setZoom( double zoom ) {
        drawingArea.setZoom( zoom );
	//scroller.repaint();
    }

    public class CornerPanel extends JPanel {

        Color bgColor;
	String s;
	public CornerPanel ( String s ) {
	    super();
	    this.s = s;
	}

	public void paint(Graphics g) {

	    Dimension  size = getSize( null );
	    
	    Graphics2D g2 = (Graphics2D) g;
	    g2.setBackground( Color.white );
	    g2.clearRect( 0, 0, size.width, size.height );	    
	    g2.drawString( s, 5, 13 );
	}
    }

    class RelScrollbarPosition {
        public double x, y;
	public RelScrollbarPosition( double xa, double ya ) {
	    x = xa;
	    y = ya;
        }
    }

    public class TaskAxis extends JPanel {

	// This class caters for both orientations
	
	final int HOR_AXIS_HEIGHT = 30;
	final int VER_AXIS_WIDTH  = 50;

	double    taskLeft, taskDelta, taskRight, ticksDeltaRaw,
	          taskScale, factor, ticksDelta, taskTicksLeft;
        Dimension axisSize;
	boolean   horizontal;

	public TaskAxis( int orientation ) {
	    super();
	    horizontal = orientation == HORIZONTAL;
	    if(  horizontal )
	        axisSize  = new Dimension( drawingAreaSize.width, HOR_AXIS_HEIGHT );
	    else
	        axisSize  = new Dimension( VER_AXIS_WIDTH, drawingAreaSize.height );
            setPreferredSize( axisSize );
	}

	public void paint(Graphics g) {

            // NOTE: We are using information from the DrawingArea viewport,
	    //       TaskAxis itself does NOT have one. This makes the scaling
	    //       a bit unusual (but very practical).

	    if( g == null ) return;

	    Graphics2D g2        = (Graphics2D) g;
	    Dimension  size      = viewPort.getViewSize();
	    Rectangle  viewRect  = viewPort.getViewRect();
	    NumberFormat nf = NumberFormat.getInstance();
	    int        offset = horizontal ? viewRect.x     : viewRect.y;      // Viewport position
	    int        range  = horizontal ? viewRect.width : viewRect.height;
	    double     taskScale;
	    double[]   scaledSteps   = {1,   2,   5,  10  };
	    int[]      extraDecimals = {0,   1,   1,   0  };
	    int[]      subTicks      = {1,   1,   1,   1  };
	    int        i, ticksLeft, decimals;
	    int        viewSize = horizontal ? size.width : size.height;
	    double     scale = (double) viewSize / (double)cells;

	    if (horizontal) {
		    axisSize.width  = viewRect.width;
            } else {
		    axisSize.height = viewRect.height;
	    }
    	    
	    //g2.setBackground( zoomSlider.buttonDown? Color.yellow:Color.blue );
	    g2.setBackground( Color.white );
	    g2.clearRect( 0, 0, axisSize.width, axisSize.height );

	    AffineTransform imageTransform = new AffineTransform();
	    if( horizontal ) {
	        if( commInfo.accRecvImage != null ) {
		    double xTranslate = (double)-offset;
		    double yTranslate = (double) HOR_AXIS_HEIGHT - 5.;
		    imageTransform.translate( xTranslate, yTranslate );
		    imageTransform.scale( scale, 5. );
	            g2.drawImage( commInfo.accRecvImage, imageTransform, null );
		}
	    } else {
	        if( commInfo.accSendImage != null ) {
		    double xTranslate = (double) VER_AXIS_WIDTH - 5.;
		    double yTranslate = (double)-offset;
		    imageTransform.translate( xTranslate, yTranslate );
	            imageTransform.scale( 5., scale );
	            g2.drawImage( commInfo.accSendImage, imageTransform, null );
		}
	    }

	    taskLeft  = (double) offset / scale;
	    taskDelta = (double) range  / scale;
	    taskRight = taskLeft + taskDelta;

            // Compute practical tick mark distances and values
	    ticksDeltaRaw = taskDelta / 8.0;
	    taskScale = 1.0;
	    decimals  = 0;
	    factor    = taskDelta < 10. ? 10. : 0.1;
	    while( !( 1 <=ticksDeltaRaw && ticksDeltaRaw < 10 ) && decimals<10) {
	        taskScale     /= factor;
		ticksDeltaRaw *= factor;
		decimals++;
	    }
	    for( i=0; i<scaledSteps.length; i++ ) {
		if( ticksDeltaRaw < scaledSteps[i] ) break;
	    }
	    ticksDelta    = scaledSteps[i] * taskScale;
	    ticksLeft	  = (int) ( taskLeft / ticksDelta );
	    taskTicksLeft = ticksLeft * ticksDelta;
	    
	    if( ticksDelta > 0. ) {
		BasicStroke stroke = new BasicStroke( 1.f );
	        g2.setStroke( stroke );
		g2.setColor( Color.black );
        	double tSubticks = ticksDelta / subTicks[i];
		for( double t=taskTicksLeft ; t<taskRight; t+=ticksDelta ) {
		    double scaledTask = t * scale;
		    double tTicks = t;
		    String s = nf.format(t);
		    // Tick labels
		    if( horizontal ) {
		        g2.drawString( s, (int)scaledTask - offset, 13 );
		    } else {
		        // Vertical axis: right-align tickmark labels
			int stringWidth = SwingUtilities.computeStringWidth( 
			                                       g2.getFontMetrics(), s );
		        g2.drawString( s, VER_AXIS_WIDTH-12-stringWidth, (int)scaledTask - offset + 10 );
		    }
		    // Ticks
		    for( int j=0; j<subTicks[i]; j++ ) {
		        scaledTask = tTicks * scale - offset;
			if( horizontal ) {
			    double y = (double) HOR_AXIS_HEIGHT;
			    g2.draw( new Line2D.Double( scaledTask, y-6., scaledTask, y-10. ) );
			} else {
			    double x = (double) VER_AXIS_WIDTH;
			    g2.draw( new Line2D.Double(  x-10., scaledTask, x-6., scaledTask ) );
			}
			tTicks += tSubticks;
		    }
		}
        	
		// Draw the axis lines
		if( horizontal ) {
		    double y = (double) HOR_AXIS_HEIGHT;
		    g2.draw( new Line2D.Double( 0., y-1., axisSize.width, y-1. ) );
		    g2.draw( new Line2D.Double( 0., y-6., axisSize.width, y-6. ) );
		} else {
		    double x = (double) VER_AXIS_WIDTH;
		    g2.draw( new Line2D.Double( x-1., 0., x-1., axisSize.height  ) );
		    g2.draw( new Line2D.Double( x-6., 0., x-6., axisSize.height  ) );
		}
		
		// Extend the horizontal and vertical auxiliary lines drawn in
		// the matrix area to the axis, but only if there is enough room.
		if( scale > 4. ) {
		    for( int t=(int)taskLeft; t<=(int)taskRight; t++ ) {
			double scaledTask = t * scale - offset;
			Line2D line;
			if( horizontal ) {
			    double y = (double) HOR_AXIS_HEIGHT;
			    line = new Line2D.Double(
				       scaledTask, y-6., scaledTask, y );
			} else {
			    double x = (double) VER_AXIS_WIDTH;
			    line = new Line2D.Double(
				       x-6., scaledTask, x, scaledTask );
			}
			g2.draw( line );
		    }
		}
	    }
            drawingAreaReady = true;
	}
	
	public Dimension getPreferredSize() {
	    return axisSize;
	}
    }

    public class DrawingArea extends JPanel {

	public DrawingArea() {
	    super();
	}

        public void setZoom( double zoom ) {
	    Dimension size       = getPreferredSize();
	    Dimension extentSize = viewPort.getExtentSize();
	    Point     position   = viewPort.getViewPosition();
	    double    newWidth, halfWidth, newHeight, halfHeight;

	    halfWidth   = 0.5 * (double)extentSize.width;
	    newWidth    = (double)originalSize.width * zoom;
	    size.width  = (int)newWidth;
	    position.x  = (int)(relScrollbarPosition.x * newWidth - halfWidth);
	    if( position.x < 0 ) position.x = 0;

	    halfHeight  = 0.5 * (double)extentSize.height;
	    newHeight   = (double)originalSize.height * zoom;
	    size.height = (int)newHeight;
	    position.y  = (int)(relScrollbarPosition.y * newHeight - halfHeight);
	    if( position.y < 0 ) position.y = 0;

	    setPreferredSize( size );
	    viewPort.setViewPosition(position);
	    viewPort.revalidate();
	    horTaskAxis.revalidate();
	    horTaskAxis.repaint();
	    verTaskAxis.revalidate();
	    verTaskAxis.repaint();
	}

        public void refresh() {
	}

	public Dimension getPreferredSize() {
	    return drawingAreaSize;
	}

	public void setPreferredSize( Dimension d ) {
	    drawingAreaSize = new Dimension( d );
	}

        private void howDidIgetHere() {
	     new Exception().printStackTrace();
	}

	public void paint(Graphics g) {

            if( g == null ) return;
	    Graphics2D g2       = (Graphics2D) g;
	    Dimension  size     = viewPort.getViewSize();
	    Rectangle  viewRect = viewPort.getViewRect();
	    NumberFormat nf = NumberFormat.getInstance();
	    double     xscale = (double) size.width  / (double)cells;
	    double     yscale = (double) size.height / (double)cells;
	    double     taskLeft, taskRight, taskTop, taskBottom, taskDeltaH, taskDeltaV;
	    
	    g2.setBackground( Color.white );
	    g2.clearRect( viewRect.x, viewRect.y, viewRect.width, viewRect.height );

	    taskLeft   = (double) viewRect.x / xscale;
	    taskDeltaH = (double) viewRect.width / xscale;
	    taskRight  = taskLeft + taskDeltaH;

	    taskTop    = (double) viewRect.y / yscale;
	    taskDeltaV = (double) viewRect.height  / yscale;
	    taskBottom = taskTop + taskDeltaV;

	    AffineTransform imageTransform = new AffineTransform();
	    imageTransform.scale( xscale, yscale );

	    g2.drawImage( commInfo.image, imageTransform, null );

	    // Set linewidth: one pixel
	    BasicStroke stroke = new BasicStroke( 1.f );
	    g2.setStroke( stroke );
	    g2.setPaint( Color.black );

            if( xscale > 4. ) {
		for( int t=(int)taskLeft; t<=(int)taskRight; t++ ) {
		    double scaledTask = t * xscale;
		    Line2D line = new Line2D.Double(
				   scaledTask, 0.d, scaledTask, (double)size.height );
		    g2.draw( line );
		}
	    }

            if( yscale > 4. ) {
		for( int t=(int)taskTop; t<=(int)taskBottom; t++ ) {
		    double scaledTask = t * yscale;
		    Line2D line = new Line2D.Double(
				   0., scaledTask, (double)size.width, scaledTask );
		    g2.draw( line );
		}
	    }
/*

            // This code section is very useful to check the proper
	    // alignment with the axis scales.

	    for( double t=horTaskAxis.taskTicksLeft ; 
	                t<horTaskAxis.taskRight; 
			t+=horTaskAxis.ticksDelta ) {
		double scaledTask = t * xscale;
		g2.drawString( nf.format(t), (int)scaledTask, 100 );
		Line2D line = new Line2D.Double(
			       scaledTask, 0.d, scaledTask, (double)size.height );
		g2.draw( line );
	    }

	    for( double t=verTaskAxis.taskTicksLeft ; 
	                t<verTaskAxis.taskRight; 
			t+=verTaskAxis.ticksDelta ) {
		double scaledTask = t * yscale;
		g2.drawString( nf.format(t), 100, (int)scaledTask );
		Line2D line = new Line2D.Double(
			       0.d, scaledTask, (double)size.width, scaledTask );
		g2.draw( line );
	    }
*/

	}
    }

    class MyChangeListener implements ChangeListener {
    
        public void stateChanged( ChangeEvent e ) {
	    //System.out.println( "stateChanged: event = " + e );
	}
    }

    class MatrixMotionListener extends MouseMotionAdapter {

        public void mouseDragged(MouseEvent e) {
	    // FIXME: Can't repaint while dragging: find out why
            int    x    = e.getX();
	    //drawingArea.repaint();
        }

        public void mouseEntered(MouseEvent e) {
            trace.traceGraphics.ignoreMouseMotion = false;
	    drawingArea.requestFocus();
	}

	public void mouseMoved(MouseEvent e) {
            if( trace.traceGraphics.ignoreMouseMotion ) return;
	    Dimension  size   = viewPort.getViewSize();
	    double     xscale = (double) size.width  / (double)cells;
	    double     yscale = (double) size.height / (double)cells;
	    
            int x = e.getX();
            int y = e.getY();
            int dst = (int)((double)x / xscale);
            int src = (int)((double)y / yscale);
            frame.mousePairUpdate (src, dst);
	}
    }

    class AxisMotionListener extends MouseMotionAdapter {

        private int axis;

        public AxisMotionListener( int axis ) {
            this.axis = axis;
        }

        public void mouseEntered(MouseEvent e) {
            trace.traceGraphics.ignoreMouseMotion = false;
	    drawingArea.requestFocus();
	}

	public void mouseMoved(MouseEvent e) {
            if( trace.traceGraphics.ignoreMouseMotion ) return;
	    Dimension  size   = viewPort.getViewSize();
	    Rectangle  r      = viewPort.getViewRect();
	    
            if( axis == HORIZONTAL ) {
                int x = e.getX();
	        double xscale = (double) size.width  / (double)cells;
                int dst = (int)((double)(r.x + x) / xscale);
                frame.mouseAxisUpdate (axis, dst);
            } else {
                int y = e.getY();
	        double yscale = (double) size.height / (double)cells;
                int src = (int)((double)(r.y + y) / yscale);
                frame.mouseAxisUpdate( axis, src );
            }
	}
    }

    class MyMouseListener extends MouseInputAdapter {

        public void mouseEntered(MouseEvent e) {
            trace.traceGraphics.ignoreMouseMotion = false;
	    drawingArea.requestFocus();
	}

        public void mousePressed(MouseEvent e) {

            if( SwingUtilities.isRightMouseButton(e) ) {

	        System.out.println( "right mouse button pressed" );

            } else if( SwingUtilities.isLeftMouseButton(e) ) {

	        System.out.println( "left mouse button pressed" );
            }
	}
        public void mouseReleased(MouseEvent e) {

            if( SwingUtilities.isRightMouseButton(e) ) {

	        System.out.println( "right mouse button released" );

            } else if( SwingUtilities.isLeftMouseButton(e) ) {

	        System.out.println( "left mouse button released" );
            }
	    //drawingArea.repaint();

	}
    }

    // MyKeyListener detects pressing the escape key
    class MyKeyListener extends KeyAdapter {
        public void keyPressed( KeyEvent e ) {
	    if( e.getKeyCode() == e.VK_ESCAPE ) {
                trace.traceGraphics.ignoreMouseMotion = true;
	    }
	}
    }


    class HorizontalScrollBarMouseListener extends MouseInputAdapter {
	private boolean buttonDown, buttonClick;

        HorizontalScrollBarMouseListener() {
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
	    horTaskAxis.revalidate();
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
	    //System.out.println( "HorizontalScrollBarAdjustmentListener.adjustmentEvent: event="+event );
	}
	public void adjustmentValueChanged(AdjustmentEvent event) {
            int   type       = event.getAdjustmentType();
	    //System.out.println( "HorizontalScrollBarAdjustmentListener.adjustmentValueChanged: event="+event );
	    // Scrollbar dragged: compute relative bar position.
	    // This position is needed to provide zoom stability.
	    Rectangle r = viewPort.getViewRect();
	    Dimension s = viewPort.getViewSize();
	    if( zoomSlider == null ) return;
	    if( !zoomSlider.buttonDown ) {
		relScrollbarPosition.x = (double)(r.x + r.width/2) / (double)s.width;
		horTaskAxis.revalidate();
		horTaskAxis.repaint();
                drawingArea.revalidate();
		drawingArea.repaint();
	    }
	}
    }

    class VerticalScrollBarAdjustmentListener implements AdjustmentListener {

        VerticalScrollBarAdjustmentListener() {}

	public void adjustmentEvent(AdjustmentEvent event) {
            int type = event.getAdjustmentType();
	    //System.out.println( "VerticalScrollBarAdjustmentListener.adjustmentEvent: event="+event );
	}
	public void adjustmentValueChanged(AdjustmentEvent event) {
            int type = event.getAdjustmentType();
	    //System.out.println( "VerticalScrollBarAdjustmentListener.adjustmentValueChanged: event="+event );
	    // Scrollbar dragged: compute relative bar position.
	    // This position is needed to provide zoom stability.
	    Rectangle r = viewPort.getViewRect();
	    Dimension s = viewPort.getViewSize();
	    if( zoomSlider == null ) return;
	    if( !zoomSlider.buttonDown ) {
	        relScrollbarPosition.y = (double)(r.y + r.height/2) / (double)s.height;
		verTaskAxis.revalidate();
		verTaskAxis.repaint();
		drawingArea.revalidate();
		drawingArea.repaint();
	    }
	}
    }
    
    public void windowGainedFocus( WindowEvent e ) {
    }
    public void windowLostFocus( WindowEvent e ) {
    }
}
