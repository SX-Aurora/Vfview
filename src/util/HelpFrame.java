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
import java.io.*;
import java.net.*;


public class HelpFrame {
    private JPanel panel;
    private JFrame frame;
    JEditorPane html;
    public HelpFrame(String helpdir) {
	panel = new JPanel();
	panel.setLayout(new BorderLayout());
	frame = new JFrame( "Vftrace Help" );
        frame.getContentPane().setLayout(new BorderLayout());
	frame.getContentPane().add(panel, BorderLayout.CENTER);
	panel.setPreferredSize(new Dimension(800, 400));
	frame.pack();
        try {
	    URL url = null;
	    String path = null;
	    try {
		path = "file://" + helpdir + "/vfview.html";
		url = new URL (path);
            } catch (Exception e) {
		System.err.println("Failed to open " + path);
		url = null;
            }
	    
            if(url != null) {
                html = new JEditorPane(url);
                html.setEditable(false);
                html.addHyperlinkListener(createHyperLinkListener());
		
		JScrollPane scroller = new JScrollPane();
		JViewport vp = scroller.getViewport();
		vp.add(html);
                panel.add(scroller, BorderLayout.CENTER);
            } else {
		System.out.println( "Help file not found in " + path );
	    }
        } catch (MalformedURLException e) {
            System.out.println("Malformed URL: " + e);
        } catch (IOException e) {
            System.out.println("IOException: " + e);
        }
	setVisible(true);
    }
    
    public void setVisible(boolean state) {
	frame.setVisible(state);
    }

    public HyperlinkListener createHyperLinkListener() {
	return new HyperlinkListener() {
	    public void hyperlinkUpdate(HyperlinkEvent e) {
		if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
		    if (e instanceof HTMLFrameHyperlinkEvent) {
			((HTMLDocument)html.getDocument()).processHTMLFrameHyperlinkEvent(
			    (HTMLFrameHyperlinkEvent)e);
		    } else {
			try {
			    html.setPage(e.getURL());
			} catch (IOException ioe) {
			    System.out.println("IOE: " + ioe);
			}
		    }
		}
	    }
	};
    }
    
}
