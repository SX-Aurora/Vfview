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

import java.awt.*;
import javax.swing.*;

public class TimeLine implements Cloneable {

    // Time line information
    
    final double viewExtension = 5.;

    public double timeScale, timeZoom, prevTimeZoom, timeShift,
                  timeRange, timeMiddle, negativeTime,
	          partTimeStart, partTimeStop, 
	          partTime, halfPartTime,
	          viewTimeStart, viewTimeStop,
	          viewTime, halfViewTime,
                  blankTimeStart, blankTimeStop,
		  minTime, maxTime;
    private int    partWidth, viewWidth, 
                  blankIndexStart, blankIndexRange,
		  paintIndexStart, paintIndexRange;

    public TimeLine( Dimension size, double runTime ) {
	viewWidth     = size.width;
	partWidth     = viewWidth * (int)viewExtension;
	size.width    = partWidth;
	timeRange     = runTime;
	newRun( 0., runTime );
    }

    public void adjustWidth( int width ) {
        double scale = (double) width / (double) viewWidth;
	viewWidth = width;
	partWidth = (int)( scale * (double) partWidth );
	timeScale = ((double)viewWidth / timeRange) * timeZoom;
	timeShift = 0.;
	recompute();
    }

    public void newRun( double mintime, double maxtime ) {
	minTime       = mintime;
	maxTime       = maxtime;
	timeRange     = maxTime - minTime; 		     
	timeScale     = (double)viewWidth / timeRange;  
	timeMiddle    = 0.5 * (minTime + maxTime);		     
        timeZoom      = 1.;
	prevTimeZoom  = 1.;
	timeShift     = 0.;
	recompute();
    }

    // Derive everything else from timeMiddle and timeScale
    private void recompute() {
        viewTime      = viewWidth / timeScale;
	partTime      = viewExtension * viewTime;
	halfPartTime  = 0.5 * partTime;
	halfViewTime  = 0.5 * viewTime;
	partTimeStart = timeMiddle - halfPartTime;
	partTimeStop  = timeMiddle + halfPartTime;
	viewTimeStart = timeMiddle - halfViewTime;
	viewTimeStop  = timeMiddle + halfViewTime;
	negativeTime  = halfPartTime - halfViewTime;

	if (viewTimeStart < minTime) {
		double shift_right= minTime - viewTimeStart;
		partTimeStart += shift_right;
		partTimeStop  += shift_right;
		viewTimeStart += shift_right;
		viewTimeStop  += shift_right;		
	}
	if (viewTimeStop > maxTime) {
		double shift_left= viewTimeStop - maxTime;
		partTimeStart -= shift_left;
		partTimeStop  -= shift_left;
		viewTimeStart -= shift_left;
		viewTimeStop  -= shift_left;		
	}	

	// Compute time interval and index range for new samples
	if( timeShift < 0. ) {
	    blankTimeStart  = partTimeStart;
	    blankTimeStop   = partTimeStart - timeShift;
	    blankIndexStart = computeBufferOffset( blankTimeStart );
	    blankIndexRange = -(int)(timeShift * timeScale);
	} else if( timeShift > 0. ) {
	    blankTimeStart  = partTimeStop - timeShift;
	    blankTimeStop   = partTimeStop;
	    blankIndexStart = computeBufferOffset( blankTimeStart );
	    blankIndexRange = (int)(timeShift * timeScale);
	} else {
	    // No shift: assuming zooming, do everything
	    blankTimeStart  = Math.max( 0, partTimeStart );
	    blankTimeStop   = Math.min( partTimeStop, timeRange );
	    blankIndexStart = computeBufferOffset( blankTimeStart );
	    blankIndexRange = (int)((blankTimeStop-blankTimeStart) * timeScale);
	}
	paintIndexStart = computeBufferOffset( partTimeStart );
	paintIndexRange = partWidth - blankIndexRange;
	if( false ) {
	// FIXME remove after debugging
	System.out.println( 
	   "TimeLine.recompute():\n"+
	   " timeMiddle="     +timeMiddle     +
	   " timeScale="      +timeScale      +
	   " timeZoom="       +timeZoom       +
	   " partTimeStart="  +partTimeStart  +
	   " partTimeStop="   +partTimeStop   +
	   " viewTimeStart="  +viewTimeStart  +
	   " viewTimeStop="   +viewTimeStop   +
	   " blankTimeStart=" +blankTimeStart +
	   " blankTimeStop="  +blankTimeStop  +
	   " blankIndexStart="+blankIndexStart+
	   " blankIndexRange="+blankIndexRange+
	   " paintIndexStart="+paintIndexStart+
	   " paintIndexRange="+paintIndexRange );
	}
    }

    public void setZoom( double factor ) {
	timeZoom  = factor;
	timeScale = ((double)viewWidth / timeRange) * factor;
	timeShift = 0.;
	recompute();
    }
    
    public void shiftTime( double time ) {
        timeShift = time;
	timeMiddle += time;
	recompute();
    }
    
    public void saveZoom() {
        prevTimeZoom = timeZoom;
    }
    
    public double zoomChange() {
        return( timeZoom / prevTimeZoom );
    }

    private int computeBufferOffset( double time ) {
        // Computes offset in display buffer for "time"
	double bufferChunks = (int)((time+negativeTime)/partTime);
	double indexTime    = time+negativeTime - bufferChunks * partTime;
	return( (int)(indexTime * timeScale) );
    }
    
    public TimeLine klone() {
        TimeLine newTimeLine;
	try {
	    newTimeLine = (TimeLine)clone();
	} catch( CloneNotSupportedException e ) {
	    e.printStackTrace();
	    newTimeLine = null;
	}
	return newTimeLine;
    }

    public int getPartWidth () {
	    return partWidth;
    }

    public int getViewWidth () {
	    return viewWidth;
    }
}

