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

import util.TransferInc;

// Data transfer class

public class Transfer{
    public final static int SEND     =  0;
    public final static int RECV     =  1;
    public final static int SUM      = -1;
    public int dir, src, dst;
    public double rate;
    public Transfer() {
    }
    public Transfer (double rate, int src, int dst, int dir) {
        this.rate = rate;
        this.src  = src;
        this.dst  = dst;
        this.dir  = dir;
    }
    public Transfer (Transfer t) {
        this.rate = t.rate;
        this.src  = t.src;
        this.dst  = t.dst;
        this.dir  = t.dir;
    }
    public Transfer (TransferInc t, int inc) {
        this.rate = inc < 0 ? -t.rate : t.rate ;
        this.src  = t.src;
        this.dst  = t.dst;
        this.dir  = t.dir;
    }
    public String formatRate() {
        if (dir == Transfer.SEND)
             return String.format(">%.4g", rate);
        if (dir == Transfer.RECV)
             return String.format("<%.4g", rate);
        else return String.format("<>%.4g", rate);
    }
    public String coord() {
        // Convert to string "[src,dst]"
        if (src == SUM) return new String("[*,"+dst+"]");
        if (dst == SUM) return new String("["+src+",*]");
        return new String("["+src+","+dst+"]");
    }
    public void print() {
        System.out.println("Transfer("+rate+","+src+","+dst+","+dir+")");
    }
}

