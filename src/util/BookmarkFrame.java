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

public class BookmarkFrame {
    private JPanel panel;
    private JFrame frame;
    private TraceGraphics tg;
    public boolean isOpen;
    private JComboBox<String> traceSelection;

    private BookmarkTable table;

    private boolean[] showBookmark;
    private Vector<Bookmark> bookmarks;

    private GridBagLayout g;
    private GridBagConstraints c;

    private int selectedTrace;


    public BookmarkFrame (TraceGraphics tg) {
        this.tg = tg;
	this.selectedTrace = 0;
	frame = new JFrame ("Bookmarks");
	frame.addWindowListener (new WindowListener() {
		public void windowOpened (WindowEvent e) {
			isOpen = true;
			updateScreen();
		}

		public void windowClosing (WindowEvent e) {
			isOpen = false;
		}
		public void windowActivated (WindowEvent e) {
		}
	 public void windowDeactivated (WindowEvent e) {}
	 public void windowIconified (WindowEvent e) {}
	 public void windowDeiconified (WindowEvent e) {}
	 public void windowClosed (WindowEvent e) {}
	});

	panel = new JPanel();
	g = new GridBagLayout();
	c = new GridBagConstraints();
	panel.setLayout (g);

	@SuppressWarnings("unchecked")
	Vector<String> traceFiles = (Vector<String>)tg.traceInfo.getTraceFilesVector().clone();
	traceFiles.add("All");
	int nFiles = traceFiles.size();
	traceSelection = new JComboBox<String>(traceFiles);
	traceSelection.setSelectedIndex(selectedTrace);

	traceSelection.addActionListener (new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			@SuppressWarnings("unchecked")
			JComboBox<String> cb = (JComboBox<String>)e.getSource();
			String traceChoice = (String)cb.getSelectedItem();
			for (int f = 0; f < nFiles; f++) {
				if (traceFiles.elementAt(f) == traceChoice) {
					table.setSelectedTrace (traceChoice == "All" ? -1 : f);
					break;
				}
			}
			update();
		}
	});

	panel.add(traceSelection);

	table = new BookmarkTable ();
	table.setOpaque (true);
	panel.add(table);
	frame.getContentPane().add (panel, BorderLayout.CENTER);

	frame.pack();
	frame.setLocation (1120, 415);
	frame.setVisible (false);
	
    }

    public void update () {
	table.setColumns (tg.getPerfNames());
	table.fill (tg.getBookmarks());
    }


    private void updateScreen () {
    }
    
    public void setVisible(boolean state) {
	frame.setVisible(state);
    }
}
