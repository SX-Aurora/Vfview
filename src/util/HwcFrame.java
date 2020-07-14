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

public class HwcFrame {
    private JPanel panel;
    private JFrame frame;
    private TraceGraphics tg;

    public boolean[] showPerfGraph;

    public HwcFrame (TraceGraphics tg) {
        this.tg = tg;
	frame = new JFrame( "HWC Selector" );
        frame.getContentPane().setLayout(new BorderLayout());
	frame.pack();
	frame.setLocation( 900,150 );
    }
    
    public void setVisible(boolean state) {
	if (tg.traceInfo.traceAdmin != null) {
		makeButtons(tg.traceInfo.traceAdmin[0].getNPerfValues(),
			    tg.traceInfo.traceAdmin[0].getPerfNames());
	}
	frame.setVisible(state);
    }
    
    private void makeButtons(int nButtons, String[] buttonNames) {
	showPerfGraph = new boolean [nButtons];
        frame.getContentPane().setLayout(new BorderLayout());
	panel = new JPanel();
	panel.setLayout(new GridLayout(2,2));
	for (int i = 0; i < nButtons; i++) {
		showPerfGraph[i] = false;
		JButton b = new JButton (buttonNames[i]);
		final int ii = i;
		b.addActionListener (new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showPerfGraph[ii] = !showPerfGraph[ii];
				tg.refresh();
			}
		});
		panel.add(b);
		frame.getContentPane().add(panel, BorderLayout.CENTER);
		panel.setPreferredSize(new Dimension(200,50));
	}
	frame.pack();
	frame.setLocation( 900,150 );
    }

    private void updateScreenImage() {
        try {
		tg.traceBuildScreenStackSampleList();
	} catch (IOException err) {
	}
	tg.drawingArea.repaint();
    }
}
