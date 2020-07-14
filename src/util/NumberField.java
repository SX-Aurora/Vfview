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

import javax.swing.*; 
import javax.swing.text.*; 

import java.awt.Toolkit;
import java.text.*;

public class NumberField extends JTextField {
    private NumberFormat format;

    public NumberField(long value, int columns, NumberFormat f) {
        super(columns);
        format = (NumberFormat) f;
        setValue(value);
    }

    public long getValue() {
        long retVal = 0;
	String s = "empty";

        try {
	    s = getText();
	    if( s.length() == 0 ) {
	        retVal = 0;
            } else {
	        retVal = format.parse(s).longValue();
	    }
        } catch (ParseException e) {
            // This should never happen because insertString allows
            // only properly formatted data to get in the field.
            Toolkit.getDefaultToolkit().beep();
            System.err.println("getValue: could not parse: <" + s + ">");
        }
        return retVal;
    }

    public void setValue(long value) {
        setText(format.format(value));
    }
}
