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

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.DecimalFormat;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import trace.TraceGraphics;
import comm.CommunicationGraphics;

public class ZoomSlider extends JSlider implements MouseListener, ChangeListener {
    final private int TYPE_TRACE = 1;
    final private int TYPE_COMM  = 2;
    public boolean buttonDown;
    double         sliderScale, logMinZoomfactor, logMaxZoomfactor, zoomfactor, prevZoomfactor;
    int            graphicsType;
    DecimalFormat  nf;
    JPanel         sliderbox;
    TitledBorder   sboxborder;
    Object         graphics;

    public ZoomSlider( JPanel sbox, TraceGraphics graphics,
                       int max ) {
        super(0,max);
        this.graphics = graphics;
	graphicsType = TYPE_TRACE;
	makeZoomSlider( sbox,  max, 0., 6. );
    }

    public ZoomSlider( JPanel sbox, CommunicationGraphics graphics,
                       int max ) {
        super(0,max);
        this.graphics = graphics;
        graphicsType = TYPE_COMM;
        makeZoomSlider( sbox, max, 0., 2. );
    }

    public void makeZoomSlider( JPanel sbox, int intRange, double min, double max ) {
	this.setValue( 0 );
	addMouseListener( this );
	addChangeListener( this );
	sliderbox = sbox;
	sboxborder = new TitledBorder("Zoom");
	sliderbox.setBorder(sboxborder);
	nf = (DecimalFormat) DecimalFormat.getInstance();
	zoomfactor = 1.;
	prevZoomfactor = 1.;
	logMinZoomfactor = min;
	logMaxZoomfactor = max;
	sliderScale = (logMaxZoomfactor-logMinZoomfactor) / (double) intRange;
	setZoomLabel( 0 );
    }
    
    public void mousePressed( MouseEvent e ) {
	buttonDown = true;
    }
    public void mouseReleased(MouseEvent evt) {
	buttonDown = false;
	if( graphicsType == TYPE_TRACE ) {
	    TraceGraphics tracegraphics = (TraceGraphics) graphics;
	    tracegraphics.traceNeedsMoreSamples ();
        }
    }

    public void mouseClicked(MouseEvent evt) { }
    public void mouseEntered(MouseEvent evt) { }
    public void mouseExited(MouseEvent evt) { }
    
    public boolean grabbed() {
        return buttonDown;
    }

    public double zoomChange_disabled() {
        return zoomfactor/prevZoomfactor;
    }

    public void stateChanged(ChangeEvent e) {
	JSlider s = (JSlider)e.getSource();
	int     i  = s.getValue();
	setZoomLabel( i );
	if( graphics != null ) {
	    if( graphicsType == TYPE_TRACE )
	        ((TraceGraphics) graphics).setZoom( zoomfactor );
	    else if( graphicsType == TYPE_COMM )
	        ((CommunicationGraphics) graphics).setZoom( zoomfactor );
        }
    }

    // Slider value 0-400 is converted to a 5-digit float
    // in the range logMinZoomfactor to logMaxZoomfactor 
    // on a logarithmic scale.
    // The slider label is updated with this value.
    // The time axis scale factor is zoomfactor, processed by
    // tracegraphics when the mouse button is released.

    public void setZoomLabel( int i ) {
	double log = logMinZoomfactor + (double)i * sliderScale;
	int    acc = 4 - (int) log;
	zoomfactor = Math.pow( 10., log );
	nf.setMinimumFractionDigits( acc ); // 5 digits
	nf.setMaximumFractionDigits( acc );
	nf.setGroupingSize(1000000);        // No grouping comma
	sboxborder.setTitle("Zoom factor: " + nf.format(zoomfactor) );
	sliderbox.repaint();
    }

    public void update( double factor ) {
	double log = Math.log( factor ) * 0.434294481903251828;
	int    i   = (int)(log/sliderScale);
	int    acc = 4 - (int) log;
	setValue( i );
	nf.setMinimumFractionDigits( acc ); // 5 digits
	nf.setMaximumFractionDigits( acc );
	nf.setGroupingSize(1000000);        // No grouping comma
	sboxborder.setTitle("Zoom factor: " + nf.format(zoomfactor) );
	sliderbox.repaint();
    }

}

