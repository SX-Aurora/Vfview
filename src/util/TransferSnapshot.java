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

// Table of unique stacks

package util;

public class TransferSnapshot {
    public double time;
    public long   offsetAbs;
    public long   offsetInc;
    public int    lengthAbs;
    public int    lengthInc;
    public TransferSnapshot( double time,
                             long offsetAbs, long offsetInc,
                             int  lengthAbs, int  lengthInc )
    {
        this.time      = time;
        this.offsetAbs = offsetAbs;
        this.offsetInc = offsetInc;
        this.lengthAbs = lengthAbs;
        this.lengthInc = lengthInc;
    }
}

