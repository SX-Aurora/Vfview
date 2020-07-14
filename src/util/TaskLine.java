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

public class TaskLine implements Cloneable {

    // Task line information
    
    public double taskScale, taskZoom, prevTaskZoom,
                  taskRange, taskMiddle,
	          partTaskStart, partTaskStop, 
	          partTask, halfPartTask,
	          viewTaskStart, viewTaskStop,
	          viewTask,     // The task range in view (strange name :-( )
		  halfViewTask, // Half the task range in view
		  minTask, maxTask;
    public int    partWidth, viewWidth;

    public TaskLine( Dimension size, int taskCount ) {
	viewWidth     = size.width;
	partWidth     = size.width;
	newRun( taskCount );
    }

    public void adjustWidth( int width ) {
        double scale = (double) width / (double) viewWidth;
	viewWidth = width;
	partWidth = (int)( scale * (double) partWidth );
	taskScale = ((double)viewWidth / taskRange) * taskZoom;
	recompute();
    }

    public void newRun( int maxtask ) {
	minTask       = 0;
	maxTask       = maxtask;
	taskRange     = maxTask - minTask; 		     
	taskScale     = (double)viewWidth / taskRange;  
	taskMiddle    = 0.5 * (minTask + maxTask);		     
        taskZoom      = 1.;
	prevTaskZoom  = 1.;
	recompute();
    }

    // Derive everything else from taskMiddle and taskScale
    private void recompute() {
        viewTask      = viewWidth / taskScale;
	partTask      = viewTask;
	halfPartTask  = 0.5 * partTask;
	halfViewTask  = 0.5 * viewTask;
	partTaskStart = taskMiddle - halfPartTask;
	partTaskStop  = taskMiddle + halfPartTask;
	viewTaskStart = taskMiddle - halfViewTask;
	viewTaskStop  = taskMiddle + halfViewTask;
	if( false ) {
	// FIXME remove after debugging
	System.out.println( 
	   "TaskLine.recompute():\n"+
	   " taskMiddle="     +taskMiddle     +
	   " taskScale="      +taskScale      +
	   " taskZoom="       +taskZoom       +
	   " partTaskStart="  +partTaskStart  +
	   " partTaskStop="   +partTaskStop   +
	   " viewTaskStart="  +viewTaskStart  +
	   " viewTaskStop="   +viewTaskStop    );
	}
    }

    public void setZoom( double factor ) {
	taskZoom  = factor;
	taskScale = ((double)viewWidth / taskRange) * factor;
	taskMiddle = 0.5 * (viewTaskStart + viewTaskStop);		     
	recompute();
	System.out.println( "TaskLine.setZoom" );
    }
    
    public void saveZoom() {
        prevTaskZoom = taskZoom;
    }
    
    public double zoomChange() {
        return( taskZoom / prevTaskZoom );
    }

    public TaskLine klone() {
        TaskLine newTaskLine;
	try {
	    newTaskLine = (TaskLine)clone();
	} catch( CloneNotSupportedException e ) {
	    e.printStackTrace();
	    newTaskLine = null;
	}
	return newTaskLine;
    }
}

