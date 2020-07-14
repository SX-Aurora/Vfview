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

public class AboutFrame {
    private JPanel panel;
    private JFrame frame;

    public AboutFrame( String version, String build ) {
	frame = new JFrame( "About Vftrace " );
        frame.getContentPane().setLayout(new BorderLayout());
	panel = new JPanel();
	Font boldFont = new Font("Helvetica", Font.BOLD,  11);
	Font font     = new Font("Helvetica", Font.PLAIN, 11);
	Color bgColor = new Color( 220, 220, 220 );
        GridBagLayout      g = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();

        panel.setLayout(g);

        c.fill = GridBagConstraints.BOTH;
	makeLabel(" Vftrace version:",  boldFont, 
	          " "+version, font, 
		  bgColor, g, c );
	makeLabel(" Vftrace build:",  boldFont, 
	          " "+build, font, 
		  bgColor, g, c );

	frame.getContentPane().add(panel, BorderLayout.CENTER);
	panel.setPreferredSize(new Dimension( 250, 80));
	frame.pack();
	frame.setLocation( 380, 0 );
	setVisible(true);
    }
    
    protected void makeLabel( String name, Font boldFont, 
                              String value, Font font,
                                Color backgroundColor,
                                GridBagLayout g, GridBagConstraints c ) {
        JLabel label = new JLabel( name, SwingConstants.LEFT);
        label.setFont( boldFont );
	c.gridwidth = GridBagConstraints.RELATIVE;
        c.weightx = 0.0;
	g.setConstraints( label, c );
	panel.add( label );
        label = new JLabel( value, SwingConstants.LEFT);
        label.setFont( font );
	label.setBackground( backgroundColor ); // Has no effect - FIXME
	c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
	g.setConstraints( label, c );
	panel.add( label );
    }
	
    public void setVisible(boolean state) {
	frame.setVisible(state);
    }
    
}

