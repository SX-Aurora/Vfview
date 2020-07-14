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

package comm;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.util.Vector;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.SoftBevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.border.BevelBorder;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import util.ZoomSlider;
import util.Transfer;
import trace.TraceInfo;
import comm.CommInfo;
import comm.CommunicationGraphics;
import message.MessageInfo;

public class CommGraphFrame extends JFrame {

    final static Border loweredBorder = 
                           new SoftBevelBorder(BevelBorder.LOWERED);
    String       traceFile;
    int          debug;
    Transfer     mouseSend, mouseRecv;
    int        mouseSrc = -1, mouseDst = -1;

    public TraceInfo             traceInfo;
    public CommInfo              commInfo;
    public CommunicationGraphics commGraphics;

    Transfer     maxRate;
    JCheckBox    checkSend, checkRecv;
    JFormattedTextField
                 inputCell, inputMax;

    ColorScale   colorScale = new ColorScale();
    UpdateThread updateThread;
    
    private class UpdateThread extends Thread {
        CommGraphFrame commGraphFrame;
        public UpdateThread( CommGraphFrame commGraphFrame ) {
            this.commGraphFrame = commGraphFrame;
        }
        public void run() {
            traceInfo.updateCommFrames();
        }
    }

    void newUpdateThread() {
        if( updateThread == null || !updateThread.isAlive() ) {
            updateThread = new UpdateThread(this);
            updateThread.start();
        }
    }
    
    public CommGraphFrame( TraceInfo traceInfo ) {

        Dimension sizeColorScale = new Dimension( 120, 360 );
        Dimension sizeControlBox = new Dimension( 120, 360 );
	GridBagLayout      g;
	GridBagConstraints c;
	
	this.debug     = traceInfo.debug;
	this.traceInfo = traceInfo;

	Border loweredbevel = BorderFactory.createLoweredBevelBorder();

	try {
            UIManager.setLookAndFeel(
                     UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) { }

	commInfo     = new CommInfo( traceInfo );
	commGraphics = new CommunicationGraphics( this, debug );
	commGraphics.trace = traceInfo;
	traceInfo.commGraphics = commGraphics;

        commGraphics.setBorder( loweredbevel );

        JPanel trControl = new JPanel();
	trControl.setBorder( loweredbevel );

        // Create slider controlling the time zoom factor
	g = new GridBagLayout();
	c = new GridBagConstraints();
	JPanel sliderbox = new JPanel(g);
	ZoomSlider s = new ZoomSlider( sliderbox, commGraphics, 500);
	c.gridy = 0;
	c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.; // Slider grows horizontally
        c.weighty = 0.;
	g.setConstraints( s, c );
	sliderbox.add(s);
	commGraphics.zoomSlider = s;

        // Assemble slider control panel
	g = new GridBagLayout();
	c = new GridBagConstraints();
	trControl.setLayout( g );
        c.gridy = 0;
	c.gridwidth = GridBagConstraints.REMAINDER;
	c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.; // Slider box should only grow horizontally
        c.weighty = 0.;
	g.setConstraints( sliderbox, c );
	trControl.add(sliderbox);

	// Assemble color scale box
	g = new GridBagLayout();
	c = new GridBagConstraints();
	JPanel colorPane  = new JPanel(g);
        colorPane.setBorder( loweredbevel );

	colorScale.setMinimumSize  ( sizeColorScale );
	colorScale.setPreferredSize( sizeColorScale );
        c.weightx = 0.;
        c.weighty = 0.;
	g.setConstraints( colorScale, c );
	colorPane.add( colorScale );
	
	// Assemble control box
	g = new GridBagLayout();
	c = new GridBagConstraints();
	JPanel controlPane  = new JPanel(g);
        JTextArea cellLabel = new JTextArea("Ranks per Cell:");
        JTextArea maxLabel  = new JTextArea("Max rate [GB/s]:");
        checkSend = new JCheckBox("Sends", commInfo.showSends);
        checkRecv = new JCheckBox("Recvs", commInfo.showRecvs);
        inputCell = new JFormattedTextField(commInfo.ranksPerCell);
        inputMax  = new JFormattedTextField((double) commInfo.maxColorScale);
	inputCell.setColumns( 6 );
        cellLabel.setEditable(false);
        cellLabel.setFont(checkRecv.getFont());
        cellLabel.setOpaque(false);
	inputMax.setColumns( 6 );
        maxLabel.setEditable(false);
        maxLabel.setFont(checkRecv.getFont());
        maxLabel.setOpaque(false);
        controlPane.setBorder( loweredbevel );

        c.weightx = 0.;
        c.weighty = 0.;
	controlPane.setMinimumSize  ( sizeControlBox );
	controlPane.setPreferredSize( sizeControlBox );
        c.gridx = 0;
        c.gridy = 0;
	g.setConstraints( checkSend, c );
        c.gridx = 0;
        c.gridy = 1;
	g.setConstraints( checkRecv, c );
        c.gridx = 0;
        c.gridy = 2;
	g.setConstraints( cellLabel, c );
        c.gridx = 0;
        c.gridy = 3;
	g.setConstraints( inputCell, c );
        c.gridx = 0;
        c.gridy = 4;
	g.setConstraints( maxLabel, c );
        c.gridx = 0;
        c.gridy = 5;
	g.setConstraints( inputMax, c );
	controlPane.add( checkSend );
	controlPane.add( checkRecv );
	controlPane.add( cellLabel );
	controlPane.add( inputCell );
	controlPane.add( maxLabel );
	controlPane.add( inputMax );
        
        checkSend.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
              AbstractButton abstractButton = (AbstractButton) actionEvent.getSource();
              commInfo.showSends = abstractButton.getModel().isSelected();
              traceInfo.updateCommFrames();
            }
        });
        checkRecv.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
              AbstractButton abstractButton = (AbstractButton) actionEvent.getSource();
              commInfo.showRecvs = abstractButton.getModel().isSelected();
              traceInfo.updateCommFrames();
            }
        });
        inputCell.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
              JFormattedTextField source = (JFormattedTextField) actionEvent.getSource();
              int ranksPerCell = ((Integer)(source.getValue())).intValue();
              if( traceInfo.traceCount % ranksPerCell == 0 ) {
                  int cells = commInfo.traceCount / ranksPerCell;
                  commInfo.setCells(ranksPerCell);
                  commGraphics.setCells(cells);
              } else {
                  JOptionPane.showMessageDialog(
                      null, "Trace count ("+traceInfo.traceCount+
                      ") not a multiple of Ranks per Cell ("+ranksPerCell+")");
                  source.setValue(commInfo.ranksPerCell);
              }
              traceInfo.updateCommFrames();
            }
        });
        inputMax.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
              JFormattedTextField source = (JFormattedTextField) actionEvent.getSource();
              double scale = 1e9*((Double)(source.getValue())).doubleValue();
              commInfo.setColorScale(scale);
              traceInfo.updateCommFrames();
            }
        });
	
        Container contentPane = getContentPane();

        // Assemble the content pane
	g = new GridBagLayout();
	c = new GridBagConstraints();
        contentPane.setLayout( g );
        c.weightx = 1.;
        c.weighty = 1.;
        c.gridx = 0;
	c.fill = GridBagConstraints.BOTH;
	g.setConstraints( commGraphics, c );
        contentPane.add( commGraphics );

        c.gridy = 1;
	c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.;
        c.weighty = 0.;
        g.setConstraints( trControl, c );
        contentPane.add( trControl );

        c.gridx = 1;
        c.gridy = 0;
	c.gridheight = 2;
	c.fill = GridBagConstraints.VERTICAL;
        c.weightx = 0.;
        c.weighty = 1.;
        g.setConstraints( colorPane, c );
        contentPane.add( colorPane );

        c.gridx = 2;
        c.gridy = 0;
	c.gridheight = 2;
	c.fill = GridBagConstraints.VERTICAL;
        c.weightx = 0.;
        c.weighty = 0.;
        g.setConstraints( controlPane, c );
        contentPane.add( controlPane );

	setTitle( "Communication Matrix" );
	setLocation( 1120, 0 );
        pack();
	setVisible(false);
    }
    
    public void setCells(int traceCount, int ranksPerCell) {
        int cells = traceCount / ranksPerCell;
        commInfo.setCells(traceCount,ranksPerCell);
        commGraphics.setCells(cells);
        update();
    }
    
    public void update() {
        commInfo.updateImage();
	traceInfo.commGraphics.repaint();   //FIXME remove "traceInfo."
	colorScale.repaint();
    }
    
    public void mousePairUpdate (int src, int dst) {
        if( commInfo.transferList == null ) return;
        if( src == mouseSrc && mouseDst == dst ) return; // Same coord: nothing new
        mouseSrc = src;
        mouseDst = dst;
        maxRate   = commInfo.maxRate;
        mouseSend = commInfo.transferList.get (src, dst, Transfer.SEND );
        mouseRecv = commInfo.transferList.get (src, dst, Transfer.RECV );
        colorScale.repaint();
        traceInfo.updateCommTable (src, dst);
    }
    
    public void mouseAxisUpdate (int axis, int cell) {
        if( commInfo.transferList == null ) return;
        if( axis == commGraphics.HORIZONTAL ) {
            maxRate   = commInfo.maxAccRecvs;
            mouseRecv = new Transfer( commInfo.accRecvs[cell], cell, Transfer.SUM, Transfer.RECV );
        } else {
            maxRate   = commInfo.maxAccSends;
            mouseSend = new Transfer( commInfo.accSends[cell], Transfer.SUM, cell, Transfer.SEND );
        }
        colorScale.repaint();
    }
    
    public class ColorScale extends JPanel {
	Dimension colorScaleSize;
	JLabel    label = new JLabel();

	public void paint(Graphics g) {

	    Dimension  size = getSize();
	    Graphics2D g2   = (Graphics2D) g;
	    
	    int barWidth  = 20;
	    int barHeight = 80 * size.height / 100;
	    
	    g2.setBackground( new Color( 235, 235, 235 ) );
	    g2.clearRect( 0, 0, size.width, size.height );
	    AffineTransform imageTransform = new AffineTransform();
	    imageTransform.translate( 20, 40 );
	    imageTransform.scale( 20., (double)barHeight/100. );
	    g2.drawImage( commInfo.colorScaleImage, imageTransform, null );
	    g2.setStroke( new BasicStroke( 1.f ) );
	    g2.setColor( Color.black );
	    g2.drawRect( 20, 40, barWidth, barHeight );

	    if( maxRate != null ) {
	        g2.drawString( maxRate.coord(),  20, 18 );
                g2.drawString( maxRate.formatRate(), 20, 36 );
            }

            if( mouseSend == null && mouseRecv == null && mouseSrc != -1 ) {
                g2.drawString( "["+mouseSrc+","+mouseDst+"]", 42, barHeight/2+60 );
            }

            if( mouseSend != null ) {
                g2.drawString( String.format( ">%.4g", mouseSend.rate ),
                               42, barHeight/2+40 );
                g2.drawString( mouseSend.coord(), 42, barHeight/2+60 );
                mouseSend = null;
            }

            if( mouseRecv != null ) {
                g2.drawString( String.format( "<%.4g", mouseRecv.rate ),
                               42, barHeight/2+80 );
                g2.drawString( mouseRecv.coord(), 42, barHeight/2+60 );
                mouseRecv = null;
            }

            g2.drawString( "0", 20, barHeight+52 );
	    //g2.setFont( new Font( "Serif", Font.BOLD, 12 ) );
	    g2.setFont( label.getFont() );
	    g2.drawString( commInfo.unitName, 20, barHeight+70 );
	}

	public Dimension getPreferredSize() {
	    return colorScaleSize;
	}
	public void setPreferredSize( Dimension size ) {
	    colorScaleSize = new Dimension( size );
	}

    }
    

}
