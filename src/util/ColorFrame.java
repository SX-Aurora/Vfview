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
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.border.*;
import javax.accessibility.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.net.*;

import java.io.IOException;

import trace.TraceGraphics;

public class ColorFrame {
    private JPanel panel;
    private JFrame frame;
    private TraceGraphics tg;
    private JButton   chooserButton, shuffleUp, shuffleDown;
    private JComboBox<String>
                      colorSelection;
    private boolean   freezeColorTableSelection;

    public ColorFrame (TraceGraphics tg) {
        this.tg = tg;
	frame = new JFrame ("Color Panel");
        frame.getContentPane().setLayout (new BorderLayout());
	panel = new JPanel();
	panel.setLayout(new GridLayout(2,2));
	makeComponents();
	panel.add(colorSelection);
	panel.add(chooserButton);
	panel.add(shuffleUp);
	panel.add(shuffleDown);
	frame.getContentPane().add(panel, BorderLayout.CENTER);
	panel.setPreferredSize(new Dimension(200,50));
	frame.pack();
	frame.setLocation( 900,150 );
    }
    
    public void setVisible(boolean state) {
	frame.setVisible(state);
    }
    
    private void makeComponents() {

        chooserButton = new JButton ("Chooser");

	chooserButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
	        functionColorChooser();
	    }
	});

        shuffleUp = new JButton ("Up");

	shuffleUp.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
		setModifiedColors (1);
            }
	});

        shuffleDown = new JButton ("Down");

	shuffleDown.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
		setModifiedColors (-1);
            }
	});

        final String[] colorChoices = { "Shuffle", "Default", "Reset",
	                          "Gray", "Mark MPI/OpenMP" };

        colorSelection = new JComboBox<String>(colorChoices);
        colorSelection.setSelectedIndex(1);
	//tg.color_table = tg.DEFAULT_COLORS;
	tg.setDefaultColors ();
	//tg.color_parallel = false;
	tg.setColorParallel (false);
	freezeColorTableSelection = false;

        colorSelection.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if( freezeColorTableSelection ) {
	            freezeColorTableSelection = false;
		    return;
		}
                // Because e.getSource() returns an Object we would get
                // an "unchecked" warning without the following suppression
                @SuppressWarnings("unchecked")
                JComboBox<String> cb = (JComboBox<String>)e.getSource();
                String colorChoice = (String)cb.getSelectedItem();
		Color cout[] = tg.getDisplayColors();
                if (colorChoice.equals (colorChoices[1])) { // Default
		    //Color cin[]  = tg.colors[tg.DEFAULT_COLORS];
		    Color cin[]  = tg.getDefaultColors();
		    //tg.color_parallel = false;
		    tg.setColorParallel (false);
	            //tg.color_table = tg.DEFAULT_COLORS;
		    tg.setDefaultColors ();
                } else if (colorChoice.equals(colorChoices[0])) { // Shuffle
		    //tg.color_parallel = false;
		    tg.setColorParallel (false);
		    //tg.color_table = tg.MODIFIED_COLORS;
                } else if (colorChoice.equals(colorChoices[2])) { // Reset
		    //tg.drawingArea.setColors();
		    tg.drawingAreaSetColors ();
		    //Color cin[]  = tg.colors[tg.DEFAULT_COLORS];
		    Color cin[]  = tg.getDefaultColors();
		    for (int i = 0; i < cin.length; i++) {
			cout[i] = cin[i];
		    }
		    //tg.color_parallel = false;
		    tg.setColorParallel (false);
	            //tg.color_table = tg.DEFAULT_COLORS;
		    tg.setDefaultColors();
		    tg.drawingArea.unmarkStacks();
		} else if (colorChoice.equals(colorChoices[3])) { // Gray
		    //Color cin[]  = tg.colors[tg.GREY_COLORS];
		    Color cin[]  = tg.getGreyColors();
		    for( int i=0; i<cin.length; i++ ) {
		        cout[i] = cin[i];
		        tg.isGray[i] = true;
	            }
		    //tg.color_parallel = false;
		    tg.setColorParallel (false);
	            //tg.color_table = tg.GREY_COLORS;
		    tg.setGreyColors();
		} else if (colorChoice.equals(colorChoices[4])) { // Mark MPI
		    tg.drawingArea.setColorsGray();
		    //Color cin[] = tg.colors[tg.MODIFIED_COLORS];
		    Color cin[] = tg.getModifiedColors ();
		    FunctionList fList = tg.getTraceFunctionList();
		    for( int i=0; i<fList.size(); i++ ) {
			if( fList.isParallel(i) ) {
			    cout[i] = cin[i];
		            tg.isGray[i] = false;
                        }
		    }
		    //tg.color_parallel = true;
		    tg.setColorParallel (true);
	            //tg.color_table = tg.MIXED_COLORS;
		    tg.setMixedColors();
		}
		tg.drawingArea.setStackImages();
		updateScreenImage();
            }
	});
    }

    void setModifiedColors (int incr) {
        freezeColorTableSelection = true;
	colorSelection.setSelectedIndex(0);
	tg.incrementColorSeed (incr);
	tg.drawingArea.setModifiedColors();
	Color cout[] = tg.getDisplayColors();
	FunctionList fList = tg.getTraceFunctionList();
	for (int i = 0; i < fList.size(); i++) {
	    cout[i] = tg.isGray[i] ? tg.getGreyColor(i) : tg.getModifiedColor(i);
	}
	tg.drawingArea.setStackImages();
	updateScreenImage();
    }

    public void functionColorChooser() {
	String name = tg.bottomPanel.getDescriptionText();
	name = name.replace ("Function: ","");
	if (name.equals("")) {
	    JOptionPane.showMessageDialog (tg, "No function specified in name field");
	} else if (tg.traceInfoIsNull()) {
	    JOptionPane.showMessageDialog (tg, "No trace info: first read input file");
	} else {
	    // Change the color for the function in the nameField
	    // if( tg.color_table != tg.MIXED_COLORS ) {
	    if (tg.colorIsNotMixed ()) {
	        // First time: copy gray colors
		tg.drawingArea.setColorsGray();
	    }
	    Color color = JColorChooser.showDialog(
		            tg, "Color Chooser for " + name,
			    Color.black);
	    if (color != null)  {
		Pattern pat = Pattern.compile (name);
		int n = tg.getTraceFunctionListSize();
		for (int i = 0; i < n; i++) {
		    String funcName = tg.getTraceFunctionListName(i);
		    if (name.equals (funcName)  ||
		        pat.matcher (funcName).find()) {
			//tg.colors[tg.DISPLAY_COLORS][i] = color;
			tg.setDisplayColor (i, color);
			// FIXME: this is a bit inefficient, a boolean array would help
		        tg.drawingArea.markStacks (i);
			tg.isGray[i] = false;
		    }
		}
	        //tg.color_table = tg.MIXED_COLORS;
		tg.setMixedColors ();
		tg.drawingArea.setStackImages();
                colorSelection.setSelectedIndex(0);
	    }
	}
        updateScreenImage();
    }

    public void defineFunctionColor() {
        String selectedValue;
        String[] possibleValues = { "Multiple", "Single" };
        selectedValue = (String)JOptionPane.showInputDialog(null,
                "Colors:", "Input",
                JOptionPane.INFORMATION_MESSAGE, null,
                possibleValues, possibleValues[0]);
        if( selectedValue.equals( "Single" ) ) {
	    functionColorChooser();
	} else {
	    //if( tg.color_table != tg.MIXED_COLORS )
	    if (tg.colorIsNotMixed ()) {
	        tg.drawingArea.setColorsGray();
	    }
	    // Replace the gray colors by the standard colors
	    // for each match
	    String name = tg.bottomPanel.getDescriptionText();
	    Pattern pat = Pattern.compile( name );
	    int n = tg.getTraceFunctionListSize();
	    for( int i=0; i<n; i++ ) {
		String funcName = tg.getTraceFunctionListName(i);
		if( name.equals( funcName ) ||
		    pat.matcher( funcName ).find() ) {
		    //Color color =  tg.colors[tg.MODIFIED_COLORS][i];
		    Color color = tg.getModifiedColor (i);
		    //tg.colors[tg.DISPLAY_COLORS][i] = color;
		    tg.setDisplayColor (i, color);
		    tg.drawingArea.markStacks( i );
		    tg.isGray[i] = false;
		}
	    }
	    //tg.color_table = tg.MIXED_COLORS;
	    tg.setMixedColors ();
	    tg.drawingArea.setStackImages();
            colorSelection.setSelectedIndex(0);
	}
        updateScreenImage();
    }

    private void updateScreenImage() {
	// Rebuild the screen image with the new colors
	//try {
	    //tg.trace.buildScreenStackSampleList();
	//} catch( Exception err ) {
	//}
        try {
		tg.traceBuildScreenStackSampleList();
	} catch (IOException err) {
	}
	tg.drawingArea.repaint();
    }
}
