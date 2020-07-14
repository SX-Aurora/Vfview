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

// Various text fields

package util;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*; 
import javax.swing.event.*; 
import javax.swing.text.*; 
import javax.swing.border.*;
import javax.swing.colorchooser.*;

import java.text.*; 
import java.util.*;

import trace.TraceGraphics;
import util.ColorFrame;
import util.DecimalField;

public class BottomPanel extends JPanel {

    private JTextField descriptionField;
    private JTextField fileNameField;
    private DecimalField timeField; 
    
    private JPanel        panel;

    private TraceGraphics traceGraphics;
    private ColorFrame    colorFrame;
    private Font          boldFont, font;

    private GridBagLayout      gridLayout;
    private GridBagConstraints gridConstraints;

    public BottomPanel (TraceGraphics tg, ColorFrame cf) {

        traceGraphics = tg;
	colorFrame = cf;
	boldFont = new Font("Helvetica", Font.BOLD,  12);
	font	 = new Font("Helvetica", Font.PLAIN, 12);
	
        panel = new JPanel();
        gridLayout = new GridBagLayout();
        gridConstraints = new GridBagConstraints();
        panel.setLayout (gridLayout);

	descriptionField = makeTextField ("Description ");
	fileNameField = makeTextField ("Trace File ");
	timeField = makeDecimalField ("Time ");
	
	// Start color chooser panel through action listener on
	// function name field
	
	descriptionField.addActionListener (new ActionListener() {
            public void actionPerformed (ActionEvent e) {
		colorFrame.defineFunctionColor();
            }
	});

        //Put the panel in the main panel
        this.add(panel);
    }

    private DecimalField makeDecimalField (String name) {
    	DecimalFormat f = new DecimalFormat();
	f.setMinimumFractionDigits (9);
	f.setMaximumFractionDigits (9);
	f.setGroupingSize (1000000000);
	DecimalField decimalField = new DecimalField (0., 10, JTextField.LEFT, f);
	decimalField.addActionListener (new ActionListener () {
		public void actionPerformed (ActionEvent e) {
		}
	});
	JLabel label = new JLabel ("Time", SwingConstants.LEFT);
	label.setFont (boldFont);
	gridConstraints.gridwidth = GridBagConstraints.RELATIVE;
	gridConstraints.weightx = 0.0;
	gridLayout.setConstraints (label, gridConstraints);
	panel.add (label);
	gridConstraints.gridwidth = GridBagConstraints.REMAINDER;
	gridConstraints.weightx = 1.0;
	gridLayout.setConstraints (decimalField, gridConstraints);
	panel.add (decimalField);
	return decimalField;
    }

    private JTextField makeTextField (String name) {
        JLabel label = new JLabel (name, SwingConstants.LEFT);
        label.setFont (boldFont);
	gridConstraints.gridwidth = GridBagConstraints.RELATIVE;
        gridConstraints.weightx = 0.0;
	gridLayout.setConstraints (label, gridConstraints);
	panel.add (label);
        JTextField textField = new JTextField (60);
        textField.setFont (font);
	gridConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridConstraints.weightx = 1.0;
	gridLayout.setConstraints (textField, gridConstraints);
	panel.add (textField);
	return textField;
    }

    public void setDescriptionText (String text) {
	    descriptionField.setText (text);
    }

    public String getDescriptionText () {
	    return descriptionField.getText ();
    }

    public void setFileNameText (String text) {
	    fileNameField.setText (text);
    }

    public String getFileNameText () {
	    return fileNameField.getText ();
    }

    public void setTimeFieldValue (double value) {
	    timeField.setValue (value);
    }

    public double getTimeFieldValue () {
	    return timeField.getValue ();
    }
    
}
