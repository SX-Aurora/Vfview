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

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.text.DecimalFormat;

import java_utils.SpringUtilities;

import trace.TraceGraphics;
import trace.TraceInfo;
import util.ProfileFrame;
import profile.Profile;
import profile.ProfileEntry;

public class ProfileFrame 
       implements WindowFocusListener {

    private JFrame    frame;
    private final float TOP_ALIGNMENT  = 0.0f;
    private final float LEFT_ALIGNMENT = 0.0f;

    TraceInfo            traceInfo;
    JPanel               p1, p2, profilePanel   = new JPanel();
    Profile[]            profile;
    ProfileContentPanel  profileContent;

    DecimalFormat decFmtIntval = new DecimalFormat( "####0.000" );

    JScrollPane scrollPane     = new JScrollPane();
    Color       paleBlue       = new Color( 199, 208, 217 );

    EmptyBorder  border5       = new EmptyBorder(5,5,5,5);
    EtchedBorder etchedBorder  = new EtchedBorder();

    JTextField typeSelected      = null;
    JTextField functionSelected  = null;
    JTextField stackSelected     = null;
    JTextField traceSelected     = null;
    JTextField timeIntervalField = null;
    
    String[]      buttons = { "Function", "Caller", "Trace" };
    Hashtable<String,String>
                  nextProfile = new Hashtable<String,String>();
    String        currentProfileType="", 
                  currentFunctionName="", 
		  currentCallerName="";
    int           currentStackID, currentTraceNumber, activeTraces;
    int[]   	  sids = null;
    
    boolean       allCallers;
    boolean       allTraces, updateAllTraces, traceSelectionChanged;
    Hashtable<JLabel,Integer>
                  sidTable;
    Hashtable<JCheckBox,Integer>
                  boxTable;
    Integer       sid = null;
    Color         labelColor = null;
    JLabel        highlightedLabel = null;

    public ProfileFrame( TraceInfo trInfo ) {
        traceInfo = trInfo;
	traceSelectionChanged = false;
	JPanel panel = new JPanel();
	panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
	frame = new JFrame( "Vftrace Profile View" );
        frame.getContentPane().setLayout(new BorderLayout());
	frame.getContentPane().add(panel, BorderLayout.CENTER);
	panel.setPreferredSize(new Dimension(1095, 300));
	frame.pack();
	frame.setLocation( 0, 0 );
	frame.addWindowFocusListener( this );
        sidTable = new Hashtable<JLabel,Integer>();
	boxTable = new Hashtable<JCheckBox,Integer>();
	addProfilePanel();
	panel.add(profilePanel);
        //panel.add(profilePanel, BorderLayout.CENTER);
	frame.setVisible(false);
    }
    
    public JPanel createMainPanel() {
    Border loweredBorder = new CompoundBorder(new SoftBevelBorder(SoftBevelBorder.LOWERED), 
					      new EmptyBorder(5,5,5,5));
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentY(TOP_ALIGNMENT);
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setBorder(loweredBorder);
        return p;
    }

    public void update() {
        profile = traceInfo.prof;
	profileContent.update( currentProfileType );
    }
    
    public void setVisible(boolean state) {
	update();
	frame.setVisible(state);
    }
    
    public class Bar extends JPanel {
        final int HEIGHT = 10;
	double p, pmin, pmax, scale;
	Color lightBlue = new Color( 200, 200, 220 );
	Color darkBlueX = new Color( 100, 100, 220 );
	Color darkBlue  = new Color(  90,  90, 200 );
	boolean showLoadbalance;
	public Bar( double p, double pmin, double pmax, double scale ) {
	    this.p     = p;
	    this.scale = 1. / scale;
	    this.pmin  = pmin;
	    this.pmax  = pmax;
	    showLoadbalance = true;
            setMaximumSize( new Dimension( Integer.MAX_VALUE, HEIGHT ) );
	}
	public Bar( double p, double scale ) {
	    this.p     = p;
	    this.scale = 1. / scale;
	    showLoadbalance = false;
            setMaximumSize( new Dimension( Integer.MAX_VALUE, HEIGHT ) );
	}
	public void paint( Graphics g ) {
	    Graphics2D g2 = (Graphics2D) g;
	    int w, h;
	    g2.setBackground( paleBlue );
	    Dimension d = getSize();

	    g2.clearRect( 0, 0, d.width, d.height );

	    h = HEIGHT;

            if( showLoadbalance ) {
		int min  = (int)( scale * pmin * (double)d.width );
		int max  = (int)( scale * pmax * (double)d.width );
		g2.setColor( Color.RED );
		g2.draw( new Rectangle( min, 3, max-min+1, h ) );
	    }

	    w = (int)( scale * p * (double)d.width );
	    g2.setColor( darkBlue );
	    g2.fill( new Rectangle( 0, 5, w, h-3 ) );
	}
    }

    public void addProfilePanel() {
	profilePanel.setLayout(new BoxLayout(profilePanel, BoxLayout.X_AXIS));
        profilePanel.setBorder(border5);

	p1 = createMainPanel();
	p1.setBackground( paleBlue );
	p1.setAlignmentY(TOP_ALIGNMENT);
        scrollPane = new JScrollPane( p1 );
	profilePanel.add( scrollPane );

	// Profile definition pane
	p2 = new JPanel( new BorderLayout() );
	p2.setBackground( paleBlue );
	// FIXME - why is setBorder done twice?
	p2.setBorder(new CompoundBorder(
                      new TitledBorder(
			etchedBorder, "Profile definition",
			TitledBorder.LEFT, TitledBorder.TOP), border5));
	p2.setBorder( new TitledBorder(
			etchedBorder, "Profile definition",
			TitledBorder.LEFT, TitledBorder.TOP) );
        p2.setMaximumSize( new Dimension( Integer.MAX_VALUE, 120 ) );
	p1.add( p2, BorderLayout.WEST );

	// Profile definition pane - part 1
	currentProfileType = new String( "Function" );
	currentTraceNumber = 0;
	for( int i=0; i<buttons.length; i++ ) {
	    nextProfile.put( buttons[i], buttons[(i+1)%buttons.length] );
	}
	
	// Profile definition pane - part 2
	SpringLayout legendLayout = new SpringLayout();
	JPanel p2Total = new JPanel( legendLayout );
	p2Total.setBackground( paleBlue );

        String[] labels = { "Runtime per:", "Function:", "Caller:", "Trace:", "Time interval (s):" };
        String[] fields = { "function",     "all",       "all",     "0",      "123.456"  };
	for( int i=0; i<labels.length; i++ ) {
            JLabel     label = new JLabel    ( labels[i], JLabel.TRAILING );
	    //label.setMaximumSize( new Dimension( 400, 10 ) );
	    p2Total.add( label );
	    JTextField text  = new JTextField( fields[i] );
	    text.setEditable( false );
	    text.setBackground( paleBlue );
	    text.setBorder( null );
	    text.setFont( label.getFont() );
	    text.setMaximumSize( new Dimension( 400, 10 ) );
	    label.setLabelFor( text );
	    p2Total.add( text  );
	    if( i == 0 ) typeSelected      = text;
	    if( i == 1 ) functionSelected  = text;
	    if( i == 2 ) stackSelected     = text;
	    if( i == 3 ) traceSelected     = text;
	    if( i == 4 ) timeIntervalField = text;
	}
	p2.add(p2Total);
	//p2.add(Box.createHorizontalGlue());

	//Lay out the panel.
	SpringUtilities.makeCompactGrid(
	    p2Total,
            labels.length, 2,        //rows, cols
            6, 6,	 //initX, initY
            6, 0);	 //xPad, yPad

	// Profile
	
	profileContent = new ProfileContentPanel( "Function" );
	p1.add(profileContent);
	
    }

    private class ProfileContentPanel extends JPanel {

        String[] rowHeaders = { "Function", "Time", "Sum"  };
	JLabel[] rowLabels;
	LabelMouseListener  labelMouseListener  = new LabelMouseListener();
	ButtonMouseListener buttonMouseListener = new ButtonMouseListener();
	HeaderListener      headerListener      = new HeaderListener ();
	DecimalFormat dec = new DecimalFormat( "#0.00%" );
	SpringLayout profileLayout = new SpringLayout();
	int rows = 0, cols = 0;

        private ProfileContentPanel( String type ) {

	    setBackground( paleBlue );
	    setBorder( new CompoundBorder(
                	    new TitledBorder(
			      etchedBorder, "Profile",
			      TitledBorder.LEFT, TitledBorder.TOP), border5) );

	}
	
	public void update( String type ) {
	    String name = updateProfileView( type );
	    if( type.equals( "Caller" ) && name != null ) {
	        // If only one caller: traces are more interesting
		currentProfileType = new String( "Trace" );
		typeSelected.setText( "Trace" );
		traceSelected.setText( "all");
		stackSelected.setText( name );
		currentCallerName = name;
		name = updateProfileView( currentProfileType );
	    }
	}
	    
	public String updateProfileView( String type ) {

            double p, q;
	    rows = 0;
	    cols = 0;
	    String callerName = null;
	    
	    removeAll();
	    setLayout( profileLayout );
	    
            rowLabels = new JLabel[ rowHeaders.length ];
	    rowHeaders[0] = new String( type );

	    if( type.equals( "Trace" ) ) {
	        // Extra column for enabling checkboxes
		rowLabels[0] = new JLabel( "Act",   JLabel.LEADING );
		rowLabels[1] = new JLabel( "Trace", JLabel.LEADING );
		rowLabels[2] = new JLabel( "Time",  JLabel.LEADING );
		add( rowLabels[0] );
		add( rowLabels[1] );
		add( rowLabels[2] );
		cols = 3;
	    } else {
		for( int i=0; i<rowHeaders.length; i++ ) {
        	    rowLabels[i] = new JLabel( rowHeaders[i], JLabel.LEADING );
		    add( rowLabels[i] );
		    cols++;
		}
	    }
	    
	    if( type.equals( "Caller" ) ) {
		JButton all = new JButton( "Select all" );
		all.setActionCommand( "all" );
		all.addActionListener( headerListener );
		all.addMouseListener( buttonMouseListener );
		add( all );
	    } else {
	        // Blank label ensures practical minimum bar length
		JLabel header = new JLabel( "                         " );
		add( header );
	    }
	    cols++;
	    rows++;
	    
            Profile pr = profile[0];
	    double timeScale = 1. / pr.totalTime;
	    double sum = 0;
	    double intval = pr.timeStop - pr.timeStart;
            timeIntervalField.setText( decFmtIntval.format( intval ) );
	    updateAllTraces = false;
	    q = 0;
	    if( currentProfileType.equals( "Function" ) ) {
		sidTable.clear();
                Hashtable functionList = new Hashtable();
		pr = profile[currentTraceNumber];
		TraceAdmin t = traceInfo.traceAdmin[currentTraceNumber];
		StackInfo[] stackArray = t.stackArray;
		for( int j=0; j<pr.size; j++ ) {
		    int id = pr.entries[j].id;
		    if( pr.entries[j].time > 0.) {
			id = pr.entries[j].id;
			double max = traceInfo.profMax.entries[id].time;
			if( q < max ) q = max;
		    }
		}
		q += 0.02;
		for( int j=0; j<pr.size; j++ ) {
		    int id = pr.entries[j].id;
		    if( pr.entries[j].time > 0.) {
			p = pr.entries[j].time * timeScale;
			sum += p;
			String text = new String();
			int number  = stackArray[id].functionID;
			String name = t.functionList.name( number );
			String nameNomod = name.replaceFirst( "\\w*\\.", "" );
			JLabel func = new JLabel( nameNomod );
			sidTable.put (func, id);
			int depth = stackArray[id].depth-2;
			// Make call tree below function
			id     = stackArray[id].caller;
			for( int i=depth; i>=0; i-- ) {
			    number  = stackArray[id].functionID;
			    text += t.functionList.name( number ) + "<";
			    id = stackArray[id].caller;
			}
			func.addMouseListener( labelMouseListener );
			func.setToolTipText( "Call tree: "+text );
			add( func );
			add(new JLabel( dec.format( p   ), SwingConstants.RIGHT ));
			add(new JLabel( dec.format( sum ), SwingConstants.RIGHT ));
			id = pr.entries[j].id;
			double min = traceInfo.profMin.entries[id].time;
			double max = traceInfo.profMax.entries[id].time;
        		Bar bar = new Bar( p, min, max, q );
			add( bar );
			rows++;

		    }
		}
	    }
	    if( currentProfileType.equals( "Caller" ) ) {
		pr = profile[currentTraceNumber];
		sidTable.clear();
		TraceAdmin t = traceInfo.traceAdmin[currentTraceNumber];
		StackInfo[] stackArray = t.stackArray;
		for( int j=0; j<pr.size; j++ ) {
		    int id = pr.entries[j].id;
		    if( pr.entries[j].time > 0.) {
			p = pr.entries[j].time * timeScale;
			int number  = stackArray[id].functionID;
			String name = t.functionList.name( number );
			if( name.equals( currentFunctionName ) ) {
			    sum += p;
			    if( q == 0 ) q = p;
			    int depth = stackArray[id].depth-3;
			    int idFunc = id;
			    // Use name of caller
			    id     = stackArray[id].caller;
			    number = stackArray[id].functionID;
			    callerName  = t.functionList.name( number );
			    JLabel func = new JLabel( callerName );
			    String text = new String();
			    sidTable.put (func, idFunc);
			    // Make call tree below caller
			    id     = stackArray[id].caller;
			    for( int i=depth; i>=0; i-- ) {
				number  = stackArray[id].functionID;
				text += t.functionList.name( number ) + "<";
				id = stackArray[id].caller;
			    }
			    func.addMouseListener( labelMouseListener );
			    func.setToolTipText( "Call tree: "+text );
			    add( func );
			    add(new JLabel( dec.format( p   ), SwingConstants.RIGHT ));
			    add(new JLabel( dec.format( sum ), SwingConstants.RIGHT ));
        		    Bar bar = new Bar( p, q );
			    add( bar );
			    rows++;
                        }
		    }
		}
	    }
	    if( currentProfileType.equals( "Trace" ) ) {
	        boxTable.clear();
		activeTraces = 0;
		ProfileEntry[] tempProf = new ProfileEntry[traceInfo.traceCount];
	        for( int k=0; k<traceInfo.traceCount; k++ ) {
		    tempProf[k] = new ProfileEntry( k );
		    int traceNumber = k;
		    pr = profile[traceNumber];
		    if( pr.active ) activeTraces++;
		    TraceAdmin t = traceInfo.traceAdmin[currentTraceNumber];
		    StackInfo[] stackArray = t.stackArray;
		    for( int j=0; j<pr.size; j++ ) {
			int id = pr.entries[j].id;
			int number  = stackArray[id].functionID;
			String name = t.functionList.name( number );
			if( (allCallers && currentFunctionName.equals( name )) ||
			    ( sid != null && id == sid.intValue() )    ) {
			    tempProf[k].time += pr.entries[j].time;
			}
		    }
	        }
		Arrays.sort( tempProf );
		for( int k=0; k<traceInfo.traceCount; k++ ) {
		    p = tempProf[k].time * timeScale;
		    int traceNumber = tempProf[k].id;
		    String name = String.valueOf( traceNumber );
		    if( q == 0 ) q = p;
		    JCheckBox check = new JCheckBox();
		    boxTable.put (check, traceNumber);
		    check.setSelected( profile[traceNumber].active );
		    check.addActionListener( new BoxListener() );
		    add( check );
		    JLabel func = new JLabel( name );
		    func.addMouseListener( labelMouseListener );
		    add( func );
		    add(new JLabel( dec.format( p   ), SwingConstants.RIGHT ));
        	    Bar bar = new Bar( p, q );
		    add( bar );
		    rows++;
		}
	    }

	    add(Box.createVerticalGlue());

	    //Lay out the panel.
	    SpringUtilities.makeCompactGrid(
		this,
        	rows, cols,
        	6, 0,	             //initX, initY
        	6, -2);	             //xPad, yPad

	    p1.repaint();
	    p1.revalidate();
	    p2.revalidate();
	    p2.repaint();
	    
	    return( currentProfileType.equals( "Caller" ) && rows == 2 ? 
	               callerName : null );
        }
    }

    private class BoxListener
                  implements ActionListener {

	public void actionPerformed( ActionEvent e ) {

	    String textSelected  = e.getActionCommand();
	    JCheckBox box = (JCheckBox)e.getSource();
	    int trace = ((Integer)boxTable.get( box )).intValue();
	    boolean selected = box.isSelected();
	    int prevActiveTraces = activeTraces;
	    if( selected ) {
	        activeTraces++;
		profile[trace].active = true;
	    } else {
	        // At least one trace must remain active
	        if( activeTraces > 1 ) {
	            activeTraces--;
		    profile[trace].active = false;
		} else {
		    box.setSelected( true );
		}
	    }
	    if( prevActiveTraces != activeTraces ) {
		// Drastic consequence: renew all profile info
		// because min/max values have to be recomputed
		// with one trace more or less
		traceInfo.newProfile();
		String dummy = profileContent.updateProfileView( "Trace" );
	    }
	    System.out.println( "trace="+trace+" active="+profile[trace].active );
	}
    }

    private class HeaderListener
                  implements ActionListener {

	Color labelColor = null;

	public void actionPerformed( ActionEvent e ) {

	    String textSelected  = e.getActionCommand();
	    currentProfileType = getNextProfileType( textSelected );
	    profileContent.update( currentProfileType );
	}
    }

    private class ButtonMouseListener
                  implements MouseListener {
	Color buttonColor = null;
	public void mouseClicked( MouseEvent e ) {
	}
	public void mouseEntered( MouseEvent e ) {
            JButton button = (JButton) e.getComponent();
	    buttonColor  = button.getForeground();
	    button.setForeground( Color.red );
	}
	public void mouseExited( MouseEvent e ) {
            JButton button = (JButton) e.getComponent();
	    button.setForeground( buttonColor );
	}
	public void mousePressed( MouseEvent e ) {
            //System.out.println( "mousePressed" + e );
	}
	public void mouseReleased( MouseEvent e ) {
            //System.out.println( "mouseReleased" + e );
	}
    }

    String getNextProfileType( String textSelected ) {
	String next = (String) nextProfile.get( currentProfileType );
	if( next.equals( "Caller" ) ) {
	    typeSelected.setText( "Caller" );
	    stackSelected.setText( "all" );
	    functionSelected.setText( textSelected );
	    currentFunctionName = textSelected;
	    allTraces = false;
	} else if( next.equals( "Trace" ) ) {
	    typeSelected.setText( "Trace" );
	    traceSelected.setText( "all");
	    stackSelected.setText( textSelected );
	    currentCallerName = textSelected;
	    allCallers = textSelected.equals( "all" );
	    allTraces = true;
	    if( allCallers ) updateImage();
	} else if( next.equals( "Function" ) ) { 
	    typeSelected.setText( "Function" );
	    functionSelected.setText( "all" );
	    stackSelected.setText( "all" );
	    allTraces = textSelected.equals( "all" );
	    allCallers = false;
	    currentTraceNumber = allTraces ? 0 : Integer.valueOf (textSelected);
	    traceSelected.setText( textSelected );
	    restoreImage(); // Remove previous highlights
	    traceInfo.traceGraphics.showTrace( currentTraceNumber  );
	} else {
	    System.out.println( "error in getNextProfileType" );
	}
	return next;
    }

    public void windowGainedFocus( WindowEvent e ) {
	updateAllTraces = false;
    }
    public void windowLostFocus( WindowEvent e ) {
	//restoreImage();
	// Image updates from ProfileFrame introduce small artifact;
	// recompute image when losing focus.
	// Disabled for testing
	//traceInfo.traceGraphics.drawingArea.refresh();
	
	// Update all traces in the image
	updateAllTraces = true;
	updateImage();
    }

    public void restoreImage() {
	DisplayElement de = (DisplayElement)traceInfo.displayList.elementAt( 0 );
        TraceGraphics tg = traceInfo.traceGraphics;
	int y_bottom = tg.y_bottom / tg.BAR_HEIGHT;
	int y_top    = tg.y_top    / tg.BAR_HEIGHT;
	de.restoreImage( tg.getTimelineStart (), tg.getTimelineStop(),
		         y_bottom, y_top );
    }



    public void highlightEntry( int trace, int stackID ) {

        if( currentProfileType.equals( "Trace" ) ) return;

	if( trace != currentTraceNumber ) {
	    currentTraceNumber= trace;
	    traceSelected.setText( String.valueOf( trace ) );
	    profileContent.update( currentProfileType );
	}
	Enumeration e = sidTable.keys();
	for( int i=0; e.hasMoreElements(); i++ ) {
	    JLabel label = (JLabel)e.nextElement();
	    int id = ((Integer)sidTable.get(label)).intValue();
	    if( stackID == id ) {
        	if( highlightedLabel != null ) {
		    highlightedLabel.setForeground( Color.black );
		}
		labelColor = label.getForeground();
	        label.setForeground( Color.red );
		profilePanel.revalidate();
		highlightedLabel = label;
		return;
	    }
	}
	if( highlightedLabel != null ) {
	    highlightedLabel.setForeground( Color.black );
	    highlightedLabel = null;
	}
    }

    private class LabelMouseListener
                  implements MouseListener {
	public void mouseClicked( MouseEvent e ) {
            JLabel label = (JLabel) e.getComponent();
	    int traceNumber = 0;
	    String text  = label.getText();
	    if( currentProfileType.equals( "Trace" ) ) {
	        traceNumber = Integer.valueOf(text);
	        if( !profile[traceNumber].active ) return;
	    }
	    currentProfileType = getNextProfileType( text );
	    profileContent.update( currentProfileType );
	    if( currentProfileType.equals( "Trace" ) ) {
	        allTraces = false;
		if( sidTable.size() > 0 ) {
	            // Remove all stack IDs from hash table, except this one
		    sid = (Integer)sidTable.get( label );
		    if( sid != null ) {
			sidTable.clear();
			sidTable.put( label, sid );
		    }
		}
	    }
	}
	public void mouseEntered( MouseEvent e ) {
            JLabel label = (JLabel) e.getComponent();
	    int traceNumber = 0;
	    if( currentProfileType.equals( "Trace" ) ) {
	        traceNumber = Integer.valueOf (label.getText());
	        if( !profile[traceNumber].active ) return;
	    }
	    highlightedLabel = label;
	    labelColor  = label.getForeground();
	    label.setForeground( Color.red );
            TraceGraphics tg = traceInfo.traceGraphics;
	    //TimeLine tl = tg.timeLine;
	    if( currentProfileType.equals( "Trace" ) ) {
	        if( profile[traceNumber].active ) {
	            // Scroll trace to viewport base
		    tg.showTrace( traceNumber  );
		}
	    } else {
	        sid = (Integer)sidTable.get( label );
	    }
		updateImage();
		tg.drawingArea.repaint();
	}
	public void mouseExited( MouseEvent e ) {
            JLabel label = (JLabel) e.getComponent();
	    if( currentProfileType.equals( "Trace" ) ) {
		int traceNumber = Integer.valueOf (label.getText());
		if( !profile[traceNumber].active ) return;
	    }
	    label.setForeground( Color.black );
	    highlightedLabel = null;
	}
	public void mousePressed( MouseEvent e ) {
            //System.out.println( "mousePressed" + e );
	}
	public void mouseReleased( MouseEvent e ) {
            //System.out.println( "mouseReleased" + e );
	}
    }
    
    void updateImage() {
        TraceGraphics tg = traceInfo.traceGraphics;
	//TimeLine tl = tg.timeLine;
	DisplayElement de = (DisplayElement)traceInfo.displayList.elementAt( 0 );
	int y_bottom = 0;
	int y_top    = Integer.MAX_VALUE;
	if( allCallers || currentProfileType.equals( "Trace" ) ) {
	    Enumeration e = sidTable.elements();
	    sids = new int[sidTable.size()];
	    for( int i=0; e.hasMoreElements(); i++ ) {
		sids[i] = ((Integer)e.nextElement()).intValue();
	    }
	} else if( sid != null ) {
	    sids = new int[1];
	    sids[0] = sid.intValue();
	}
        if( !updateAllTraces ) {
	    y_bottom = tg.y_bottom / tg.BAR_HEIGHT;
	    y_top    = tg.y_top    / tg.BAR_HEIGHT;
	}
	de.dimImage( sids, tg.getTimelineStart(), tg.getTimelineStop(),
		     y_bottom, y_top );
    }
}
