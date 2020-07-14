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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Vector;
import java.util.Set;
import java.util.Stack;
import java.util.Iterator;
import java.awt.Color;
import java.awt.image.BufferedImage;

import message.MessageInfo;
import message.MessageHeader;
import trace.TraceInfo;
import util.Constants;
import util.MPI_Types;
import util.Transfer;
import util.TransferList;
import util.TimeLine;

public class CommInfo {

    int           traceCount;
    public int    ranksPerCell = 1;
    public boolean
                  showSends = true, showRecvs = true;
    int           cells;
    public Transfer
                  maxRate, maxAccSends, maxAccRecvs;
    public double[]
                  accSends, accRecvs;
    public double maxColorScale = 0.;
    BufferedImage image = null;
    BufferedImage accSendImage = null;
    BufferedImage accRecvImage = null;
    BufferedImage colorScaleImage = null;
    String        unitName = "Transfer Rate";
    final int     BINS = 100;
    final int     PARTONE = BINS/4;
    final int     PARTTWO = BINS/2;
    int[]         colors = new int[BINS];
    String        barMax;
    int           debug = 0;

    TraceInfo     traceInfo = null;
    Set<Integer>  keys;

    public TransferList
                  transferList;

    public CommInfo( TraceInfo traceInfo ) {
        this.traceInfo = traceInfo;
        traceInfo.commInfo = this;
        debug = traceInfo.debug;
    }
    
    public void setCells(int traceCount, int ranksPerCell) {
        this.traceCount   = traceCount;
        this.ranksPerCell = ranksPerCell;
        cells = traceCount / ranksPerCell;
        makeColors();
    }

    public void setCells(int ranksPerCell) {
        this.ranksPerCell = ranksPerCell;
        cells = traceCount / ranksPerCell;
        makeColors();
    }

    public void setColorScale(double maxColorScale) {
        this.maxColorScale = maxColorScale;
    }

    public void makeColors() {
        int i, j, red, green, blue;
        
        // Build color lookup table
        for(i = 0; i < BINS; i++) {
            if( i<PARTONE ) {
                red   = 0;
                green = i * 255 / PARTONE;
                blue  = 0;
            } else if( i<PARTTWO ) {
                red   = (i-PARTONE) * 255 / (PARTTWO-PARTONE);
                green = 255;
                blue  = 0;
            } else {
                red   = 255;
                green = 255 - (i-PARTTWO) * 255 / (BINS-PARTTWO);
                blue  = 0;
            }
            colors[i] = (new Color( red, green, blue )).getRGB();
        }

        colorScaleImage = new BufferedImage( 1, BINS, BufferedImage.TYPE_INT_RGB );

        for( i=0; i<BINS; i++ ) {
            colorScaleImage.setRGB( 0, BINS-1-i, colors[i] );
        }
   }

    public boolean updateImage() {
        int[][]  matrix;
        int      i, j, colIndex;
        double   value;
        CommGraphFrame
                 commGraphFrame
                       = traceInfo.traceView.traceFrame.getCommGraphFrame();
        int      backGndColor = (new Color( 210, 230, 255 )).getRGB();
        int    dir = 1; // Receives only (FIXME)
        
        if( commGraphFrame         == null ||
            traceInfo.transferList == null    ) return false;

        unitName = new String( "Bytes/sec" );
        accSends = new double[cells];
        accRecvs = new double[cells];

        makeColors();
        
        image        = null;
        image        = new BufferedImage( cells, cells, BufferedImage.TYPE_INT_RGB );
        accSendImage = new BufferedImage( 1,     cells, BufferedImage.TYPE_INT_RGB );
        accRecvImage = new BufferedImage( cells, 1,     BufferedImage.TYPE_INT_RGB );
        for (j = 0; j < cells; j++) {
            for (i = 0; i < cells; i++) {
                image.setRGB (i, j, backGndColor);
	    }
	}

        // Transfers per cell
        transferList = new TransferList (traceInfo.transferList.size() / ranksPerCell);
        keys = traceInfo.transferList.keySet();
        for(Integer key: keys) {
            Transfer t = traceInfo.transferList.get(key);
            int srcCell = t.src / ranksPerCell;
            int dstCell = t.dst / ranksPerCell;
            if( showSends && showRecvs       ||
                showSends && (t.dir == Transfer.SEND) ||
                showRecvs && (t.dir == Transfer.RECV)    ) {
                transferList.put (t.rate, srcCell, dstCell, t.dir);
            }
        }

        maxRate = new Transfer();
        int rateCount = transferList.size();
        keys = transferList.keySet();
        for(Integer key: keys) {
            Transfer t = transferList.get(key);
            if( maxRate.rate < t.rate )
                maxRate = new Transfer( t );
            accSends[t.src] += t.rate;
            accRecvs[t.dst] += t.rate;
        }
        
        for(Integer key: keys) {
            Transfer t = transferList.get(key);
            //if( (t.dir & dir) == 0 ) continue;
            if( t.rate != 0 ) {
                double max = maxColorScale == 0. ? maxRate.rate : maxColorScale;
                double scale = t.rate / max;
                int    cellColor;
                colIndex = (int)(scale * (double)(BINS));
                if(colIndex >= BINS) colIndex = BINS-1;
                cellColor = colors[colIndex];
                image.setRGB( t.dst, t.src, cellColor );
            }
        }

        maxAccSends = new Transfer();
        maxAccRecvs = new Transfer();
        for( j=0; j<cells; j++ ) {
            if( maxAccSends.rate < accSends[j] ) 
                maxAccSends = new Transfer( accSends[j], Transfer.SUM, j, Transfer.SEND );
            if( maxAccRecvs.rate < accRecvs[j] )
                maxAccRecvs = new Transfer( accRecvs[j], j, Transfer.SUM, Transfer.RECV );
        }
        for( j=0; j<cells; j++ ) {
            double max;
            max = maxColorScale == 0. ? maxAccSends.rate : maxColorScale;
            value = accSends[j]/max;
            colIndex = (int)(value * (double)(BINS-1));
            if( colIndex >= BINS ) colIndex = BINS-1;
            accSendImage.setRGB( 0, j, colors[colIndex] );
            max = maxColorScale == 0. ? maxAccRecvs.rate : maxColorScale;
            value = accRecvs[j]/max;
            colIndex = (int)(value * (double)(BINS-1));
            if( colIndex >= BINS ) colIndex = BINS-1;
            accRecvImage.setRGB( j, 0, colors[colIndex] );
        }
        return true;
    }

}
