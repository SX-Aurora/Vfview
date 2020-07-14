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

import java.awt.*;
import java.text.*;

public class DecimalField extends JTextField {
    private DecimalFormat format;

    public DecimalField (double value, int columns, int alignment, NumberFormat f) {
        super(columns);
        format = (DecimalFormat) f;
	setHorizontalAlignment (alignment);
        setValue(value);
    }

    public double getValue() {
        double retVal = 0.0;
	String s = "empty";

        try {
	    s = getText();
	    if (s.length() == 0) {
	        retVal = 0.;
            } else {
	        retVal = format.parse(s).doubleValue();
	    }
        } catch (ParseException e) {
            // This should never happen because insertString allows
            // only properly formatted data to get in the field.
            Toolkit.getDefaultToolkit().beep();
            System.err.println("getValue: could not parse: <" + s + ">");
        }
        return retVal;
    }

    public int getIntegerValue() {
        int retVal = 0;
	String s = "empty";

        try {
	    s = getText();
	    if (s.length() == 0) {
	        retVal = 0;
            } else {
	        retVal = format.parse(s).intValue();
	    }
        } catch (ParseException e) {
            // This should never happen because insertString allows
            // only properly formatted data to get in the field.
            Toolkit.getDefaultToolkit().beep();
            System.err.println("getValue: could not parse: <" + s + ">");
        }
        return retVal;
    }

    public void setValue(double value) {
        setText(format.format(value));
    }

    public void setValue(int value) {
        setText(format.format(value));
    }
}
