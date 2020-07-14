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

DisplayElement - collection of sample list vectors

*******************************************************************************/

package util;

import java.util.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;

public class DisplayElement {

    final int MPI_SHADOW_GRAY = 0xffe0e0e0;
    final int MPI_SAMPLE_GRAY = 0xffa0a0a0;
    final int[] PERF_COLORS = new int[] {0xffff0000,
	    				 0xffc900d0,
					 0xff00cc00,
					 0xff009999,
					 0xff0000cc,
					 0xffff33ff,
					 0xff808080,
					 0xff000000};
    private TraceAdmin[]  traceAdmin;
    private BufferedImage image = null;
    public double        time, delta, myScaleX;
    public int           height;
    public int[]         bufRGB, stackIDs;

    Vector<MultiStackSample> muxSamples;
    Vector<PerformanceSample> perfSamples;

    int           width, white, black, red, blue, traceCount, y_bottom, y_top,
                  yImage;
    int[]         bar;
    boolean       precise, markSamples;
    boolean[][]   timePixelMarked;

    private int nPerfValues;
    private boolean[] showPerfGraph;
    private boolean logScale;

    private class myPixel {
	public int x;
	public double y;
	public int color;
	public int iPerf;


	public myPixel (int x, double y, int color, int iPerf) {
		this.x = x;
		this.y = y;
		this.color = color;
		this.iPerf = iPerf;
	}

	public myPixel (int x, double y, int color) {
		this.x = x;
		this.y = y;
		this.color = color;
		this.iPerf = -1;
	}
    }

    private Vector<myPixel> pixels;


    public DisplayElement( 
                 double time, double delta, 
		 int width, int height, 
		 TraceAdmin[] traceAdmin, int y_bottom, int y_top,
		 boolean precise, boolean markSamples,
		 boolean[] showPerfGraph) {
        this.time       = time;
	this.delta      = delta;
	this.width      = width;
	this.height     = height;
	this.traceAdmin = traceAdmin;
	this.traceCount = traceAdmin.length;
	this.y_top      = y_top;
	this.y_bottom   = y_bottom;
	this.precise    = precise;
	this.markSamples= markSamples;
	this.showPerfGraph = showPerfGraph;
	this.logScale = true;
	
	pixels = new Vector<myPixel>(height * width);

	stackIDs = new int[width];
	white    = (new Color( 0, 0, 0, 0 )).getRGB(); // Transparent
	black    = Color.BLACK.getRGB();
	blue     = Color.BLUE.getRGB();
	red      = Color.RED.getRGB();
	myScaleX = (double)width / delta;
	timePixelMarked = new boolean[traceCount][width];
	for (int trace = 0; trace < traceCount; trace++) {
	    for(int x = 0; x < width; x++) {
	        timePixelMarked[trace][x] = false;
	    }
	}
        bar = new int[height];
	for (int x = 0; x < height; x++) {
	    bar[x] = red;
	}

	if (showPerfGraph != null) {
		nPerfValues = traceAdmin[0].getNPerfValues();
	} else {
		nPerfValues = 0;
	}
    }

    public void setLogScale (boolean s) {
	logScale = s;
    }

    public boolean hasLogScale () {
	    return logScale;
    }

    
    public double endTime() {
        return time + delta;
    }

    public boolean isVisible (double tmin, double tmax) {
        return (time + delta > tmin && time < tmax);
    }

    public void printImage (int iTrace) {
	    int y;
	    image = new BufferedImage (width, height, BufferedImage.TYPE_INT_ARGB);
	    for (myPixel p : pixels) {
		    if (p.iPerf >= 0) {
			int windowHeight = height / traceCount / 2;
			int y0 = traceAdmin[iTrace].getPosition();// + windowHeight;
			y = height - 1 - y0 - windowHeight - (int)p.y;
		    } else {
			y = (int)p.y;
		    }
		    try{
		    	image.setRGB (p.x, y, p.color);
		    } catch (ArrayIndexOutOfBoundsException e) {
			    System.out.println ("Out of bounds, x: " + p.x + ", y: " + y);
	            }
	    }
    }

    public void addPerfSample (PerformanceSample sample, int first, int last, int iTrace) {
            int thickness = 1;
	    TraceAdmin t = traceAdmin[iTrace];
	    for (int iPerf = 0; iPerf < nPerfValues; iPerf++) {
		if (!showPerfGraph[iPerf]) continue;
			double y;
			if (t.perfIsIntegrated(iPerf)) {
				y = sample.getIntegratedValue(iPerf, iTrace);
			} else {
				y = sample.getDifferentialValue(iPerf, iTrace);
			}
			if (y > 0) {
	    	        	for (int x = first; x <= last; x++) {
					pixels.add (new myPixel (x, y, PERF_COLORS[iPerf], iPerf));
	    	        	}
			}
	     }
    }

    public void scaleY (double[][] maxValues, int iTrace) {
	    int windowHeight = height / traceCount / 2;
	    for (myPixel p : pixels) {
		    if (p.iPerf >= 0) {
			if (logScale && p.y > 0) {
				if (p.y > 1) {
		    			p.y = Math.log10(p.y) / Math.log10(maxValues[iTrace][p.iPerf]) * (windowHeight - 1);
				} else if (p.y < 1 && maxValues[iTrace][p.iPerf] > 1) {
					p.y = - Math.log10(p.y) / Math.log10(maxValues[iTrace][p.iPerf]);
				} else {
		    			p.y = Math.log10(maxValues[iTrace][p.iPerf]) / Math.log10(p.y) * (windowHeight - 1);
				}
					
			} else {
		    		p.y = p.y / maxValues[iTrace][p.iPerf] * (windowHeight - 1);
			}
		    }
	     }
    }

    public void addStackSample (StackSample sample, int first, int last, int iTrace) {
	TraceAdmin t = traceAdmin[iTrace];
	int y0 = t.getPosition();
	if (sample.stackID != -1) {
		try {
			StackInfo info = t.stackArray[sample.stackID];
			int depth = info.pixelMap.length;
			for (int y = 0; y < depth; y++) {
				if (precise && info.funcPrecise[y]) continue;
				int color = info.pixelMap[y];
				yImage = height - 1 - y0 - y;
				for (int x = first; x <= last; x++) {
					pixels.add (new myPixel (x, yImage, color));
				}
			}
			if (info.mpi) {
				int ymin = depth;
				int ymax = t.getMaxDepth() + 1;
				for (int y = ymin; y < ymax; y++) {
					yImage = height - 1 - y0 - y;
					for (int x = first; x <= last; x++) {
						pixels.add (new myPixel (x, yImage, MPI_SHADOW_GRAY));
					}
				}
			}
		} catch (ArrayIndexOutOfBoundsException e) {
                	System.out.println(
                  		"DisplayElement: Bad stackID="+sample.stackID+" trace="+iTrace );
		}
	      }
	if (markSamples) {
		if (last - first > 4) { // MAGIC NUMBER!!
			for (int x = 0; x < height; x++) {
				pixels.add (new myPixel (first, x, black));
			}
		}
	}
    }

    public void addPreciseSample( StackSample sample, int trace, int first, int last ) {
	
        if( first <      0 ) first = 0;
	if( last  >= width ) last  = width-1;

	TraceAdmin t = traceAdmin[trace];
	int y0 = t.getPosition();
	int stackID = sample.stackID;
	//if( stackID != -1 ) {
	if( stackID > 0 ) {
	    StackInfo info = t.stackArray[stackID];
	    int depth = info.pixelMap.length;
	    int x;
	    for( int y=0; y<depth; y++ ) {
		yImage = height-1 - (y0+y);
		//if( bar[y] == red ) bar[y] = blue;
		//else                bar[y] = red;
		for( x=first; x<=last; x++ ) {
        	    if( timePixelMarked[trace][x] ) continue;
		    pixels.add (new myPixel( x, yImage, info.pixelMap[y] ));
		    // If this stack contains a marked function, block this
		    // interval on the screen from being updated by something else
		    if( info.marked ) timePixelMarked[trace][x] = true;
		}
		// Mark precise sample interval
		if( markSamples )
		    if( last-first > 4 ) {
			pixels.add (new myPixel( first,  yImage, black ));
			pixels.add (new myPixel( last,   yImage, black ));
		    }
	    }
	    // Mark MPI activity intervals
	    if( false ) {
	        //FIXME: disabled for debugging
		for( x=first; x<=last; x++ ) {
		    if( info.mpi ) {
			int ymin = depth;
			int ymax = t.getMaxDepth();
			for( int y=ymin; y<ymax; y++ )
			    yImage = height-2 - (y0+y);
			    pixels.add (new myPixel( x, yImage, MPI_SHADOW_GRAY ));
		    }
		}
	    }
	}
    }

    public boolean imageIsNull() {
	    return image == null;
    }

    public Image getImage () {
	    return image;
    }

    public Image getSubImage (int offset, int foo, int width, int height) {
	    return image.getSubimage (offset, foo, width, height);
    }

    public Image getScaledImageInstance (int width, int height, int scale) {
	    return image.getScaledInstance (width, height, Image.SCALE_FAST);
    }

    public void dimImage( int[] sids, double timeLeft, double timeRight,
                          int y_bottom, int y_top ) {

	// Highlight the stack selected in the profile panel
	
	modifyImage( sids, timeLeft, timeRight, y_bottom, y_top );

    }

    public void restoreImage( double timeLeft, double timeRight,
                              int y_bottom, int y_top ) {

	// Highlight the stack selected in the profile panel
	
        modifyImage( null, timeLeft, timeRight, y_bottom, y_top );

    }

    /* modifyImage is called when e.g. the profile frame has to
     * restore the old image when a bookmark is removed.
     * -> Bookmarks branch
     */
    public void modifyImage( int[] sids, double timeLeft, double timeRight,
                          int y_bottom, int y_top ) {

        if( image == null ) return;

	// Highlight the stack selected in the profile panel
	
	for( int i=0; i<muxSamples.size(); i++ ) {
	    MultiStackSample sample = (MultiStackSample)muxSamples.elementAt(i);
	    if( sample.computeTime()      > timeRight ) continue;
	    if( sample.endTime() < timeLeft  ) continue;
            if( sample.stackIdIsNull ()) continue;
	    for( int trace=0; trace<traceCount; trace++ ) {
		int stackID = sample.getStackID (trace);
		if( stackID == -1 ) continue;
	        TraceAdmin t = traceAdmin[trace];
		StackInfo info = t.stackArray[stackID];
		int y0 = t.getPosition();
		int depth = info.pixelMap.length;
		if( y0+depth < y_bottom || y_top < y0 ) continue;
		for( int y=0; y<depth; y++ ) {
		    yImage = height - 1 - y0 + y;
		    int color;
		    if( sids == null )
		        color = info.pixelMap[y];
		    else {
		        color = info.dimPixelMap[y];
		        for( int j=0; j<sids.length; j++ ) {
			    //System.out.println( "de: sid="+sids[j] );
			    if( sids[j] == stackID ) {
			        color = info.brightPixelMap[y];
			        break;
			    }
			}
		    }
	        //    for( int x=sample.getxMin(); x<=sample.getxMax(); x++ )
		//	image.setRGB( x, yImage, color );
		}
	    }
	}
    }

}
