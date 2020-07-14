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


// Stack information.

package util;

import java.io.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.regex.*;

public class StackInfo {
    public boolean mpi, openmp, precise, marked;
    public int     stackID, depth, functionID, caller;

    int[]     pixelMap, dimPixelMap, brightPixelMap;
    public boolean[] funcPrecise;

    public StackInfo( 
               int stackID, int depth, int caller, int functionID,
               boolean mpi, boolean openmp, boolean precise ) {
	this.depth      = depth;
	this.stackID    = stackID;
	this.caller     = caller;
	this.functionID = functionID;
	this.mpi        = mpi;
	this.openmp     = openmp;
	this.precise    = precise;
	this.marked     = false;
	pixelMap        = new int[depth];
	dimPixelMap     = new int[depth];
	brightPixelMap  = new int[depth];
	funcPrecise     = new boolean[depth];
    }
    
    // Miniature stack images
    public void setPixelMap (Color[] colors, StackInfo[] stackArray) {
	StackInfo stackInfo = this;
	for( int i=depth-1; i>=0; i-- ) {
	    Color c = colors[stackInfo.functionID];
	    int argb  = c.getRGB();
	    pixelMap   [i] = argb;
	    int alpha =  argb & 0xff000000;
	    int red   = (argb & 0x00ff0000)>>16;
	    int green = (argb & 0x0000ff00)>> 8;
	    int blue  =  argb & 0x000000ff;
	    int dimred        = (6*red  )>>3; // Scale by 6/8
	    int dimgreen      = (6*green)>>3;
	    int dimblue       = (6*blue )>>3;
	    dimPixelMap[i]    = alpha | 
	                        dimred<<16 | dimgreen<<8 | dimblue;
	    int brightred     = red   + ((255-red  )>>2); // Brighter
	    int brightgreen   = green + ((255-green)>>2);
	    int brightblue    = blue  + ((255-blue )>>2);
	    brightPixelMap[i] = alpha | 
	                        brightred<<16 | brightgreen<<8 | brightblue;
	    funcPrecise[i]    = stackInfo.precise;
	    stackInfo = stackArray[stackInfo.caller];
	}
    }

    // Mark stack if a specific function in call stack
    public void markStack( int func, StackInfo[] stackArray ) {
	StackInfo stackInfo = this;
	for( int i=depth-1; i>=0; i-- ) {
	    if( func == stackInfo.functionID ) {
	        marked = true;
		return;
	    }
	    stackInfo = stackArray[stackInfo.caller];
	}
    }

    public int getDepth() {
        return( depth );
    }    
}

	

