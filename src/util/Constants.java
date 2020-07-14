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

/* STD and temp file message record structure */
/* RECOFF_<item>: record offset in bytes to <item> */

public abstract class Constants {

    public final static int RECOFF_START            = 0;
    public final static int RECOFF_STOP             = RECOFF_START      + 8; /* double */
    public final static int RECOFF_COUNT            = RECOFF_STOP       + 8; /* double */
    public final static int RECOFF_ID               = RECOFF_COUNT      + 4; /* int    */
    public final static int RECOFF_SELF             = RECOFF_ID         + 4; /* int    */
    public final static int RECOFF_PEER             = RECOFF_SELF       + 4; /* int    */
    public final static int RECOFF_TYPE             = RECOFF_PEER       + 4; /* int    */
    public final static int RECOFF_DIR              = RECOFF_TYPE       + 4; /* int    */
    public final static int RECOFF_TAG              = RECOFF_DIR        + 4; /* int    */
    public final static int RECOFF_SIZE             = RECOFF_TAG        + 4; /* int    */
    public final static int MESSAGE_INFO_RECORDSIZE = RECOFF_SIZE        + 4; /* int    */

    public final static int RECOFF_TIME             = 0;
    public final static int RECOFF_RATE             = RECOFF_TIME       + 8; /* double */
    public final static int RECOFF_SRC              = RECOFF_RATE       + 4; /* float  */
    public final static int RECOFF_DST              = RECOFF_SRC        + 4; /* int  */
    public final static int TRANSFER_RECORDSIZE     = RECOFF_DST        + 4; /* int  */

    public final static int XFER_BLOCKSIZE          = 10000;
}
