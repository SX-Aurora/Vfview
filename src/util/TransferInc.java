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

// Incremental data transfer class

public class TransferInc{
    public int  src, dst;
    public int dir;
    public double time;
    public double rate;

    public TransferInc (double time, double rate, int src, int dst, int dir) {
        this.time = time;
        this.rate = rate;
        this.src  = src;
        this.dst  = dst;
        this.dir  = dir;
    }

    public TransferInc( TransferInc ti ) {
        this.time = ti.time;
        this.rate = ti.rate;
        this.src  = ti.src;
        this.dst  = ti.dst;
        this.dir  = ti.dir;
    }

    public void print() {
        System.out.println("TransferInc("+time+","+rate+","+src+","+dst+","+dir+")");
    }
}

