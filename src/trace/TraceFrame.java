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

package trace;

import java.awt.AWTException;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.util.Stack;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.SoftBevelBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import comm.CommGraphFrame;
import util.AboutFrame;
import util.BookmarkFrame;
import util.BottomPanel;
import util.ColorFrame;
import util.HelpFrame;
import util.HwcFrame;
import util.MPI_Frame;
import util.ProfileFrame;
import util.ViewerFileFilter;
import util.ViewerFileView;
import util.ZoomSlider;

public class TraceFrame extends JFrame {

    final static Border loweredBorder = 
                           new SoftBevelBorder(BevelBorder.LOWERED);
    TraceGraphics trGraphics;
    private TraceInfo     traceInfo;
    BottomPanel   bottomPanel;
    Dimension     size;
    double        tmin, tmax;
    String        traceFile;
    int           debug;
    HelpFrame     helpFrame      = null;
    AboutFrame    aboutFrame     = null;
    ColorFrame    colorFrame     = null;
    HwcFrame      hwcFrame       = null;
    BookmarkFrame bookmarkFrame  = null;
    MPI_Frame     commTableFrame = null;
    CommGraphFrame
                  commGraphFrame = null;
    ProfileFrame  profileFrame   = null;
    String        version, build, helpdir;

    boolean       showPreciseStacks = false;
    boolean       markSamples	    = false;
    boolean[]     showPerfGraph;

    JCheckBoxMenuItem
                  cbPreciseStacks, cbMarkSamples;

    TraceFrame (boolean printDisclaimer, int maxthr, Stack files,
		int debug, double tmin, double tmax,
		TraceInfo tInfo ) {

        JMenuBar  menuBar;
        JMenu     menu, submenu;
        JMenuItem menuItem;
	
	this.size  = new Dimension (1110, 400);
	this.debug = debug;
	this.tmin  = tmin;
	this.tmax  = tmax;

	traceInfo  = tInfo;

        Manifest mf = new Manifest(); 
        try {
            mf.read(Thread.currentThread().getContextClassLoader().getResourceAsStream("META-INF/MANIFEST.MF")); 
            Attributes atts = mf.getMainAttributes(); 
            version = new String(atts.getValue("Version"));
            build   = new String(atts.getValue("Build"  ));
	    helpdir = new String(atts.getValue("Helpdir"));
         } catch( IOException e ) {
            version = new String("unknown");
            build   = new String("unknown");
	    helpdir = new String("unknown");
        }

        System.out.print( "vftrace "+ version + "\nCopyright " +
                          build + "\n" );

        if( printDisclaimer ) {
            System.out.print( 
              "\nThis program is free software; you can redistribute it and/or modify\n" +
              "it under the terms of the GNU General Public License as published by\n" +
              "the Free Software Foundation; either version 2 of the License , or\n" +
              "(at your option) any later version.\n\n" +
              "This program is distributed in the hope that it will be useful,\n" +
              "but WITHOUT ANY WARRANTY; without even the implied warranty of\n" +
              "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n" +
              "GNU General Public License for more details.\n\n" +
              "You should have received a copy of the GNU General Public License\n" +
              "along with this program. If not, write to\n\n" +
              "   The Free Software Foundation, Inc.\n" +
              "   51 Franklin Street, Fifth Floor\n" +
              "   Boston, MA 02110-1301  USA\n\n"
            );
            return;
        } else {
            System.out.print(
              "This is free software with ABSOLUTELY NO WARRANTY.\n" +
              "For details: vfview -w\n"
            );
        }

	Border loweredbevel = BorderFactory.createLoweredBevelBorder();
	try {
            UIManager.setLookAndFeel(
                     UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) { }

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

	trGraphics = new TraceGraphics (this, size, debug);
	trGraphics.setTrace (traceInfo);

        trGraphics.setBorder( loweredbevel );

        colorFrame     = new ColorFrame (trGraphics);
	hwcFrame       = new HwcFrame (trGraphics);
	bookmarkFrame  = new BookmarkFrame (trGraphics);
        commTableFrame = new MPI_Frame (trGraphics);
	commGraphFrame = new CommGraphFrame (traceInfo);

        // Create the file and function fields
	bottomPanel = new BottomPanel (trGraphics, colorFrame);

        trGraphics.bottomPanel = bottomPanel;

        JPanel trControl = new JPanel();
	trControl.setBorder( loweredbevel );

	GridBagLayout      g = new GridBagLayout();
	GridBagConstraints c = new GridBagConstraints();

        // Create slider controlling the time zoom factor
	JPanel sliderbox = new JPanel(g);
	ZoomSlider s = new ZoomSlider( sliderbox, trGraphics, 400);
	c.gridy = 0;
	c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.; // Slider grows horizontally
        c.weighty = 0.;
	g.setConstraints( s, c );
	sliderbox.add(s);
	trGraphics.zoomSlider = s;

	JProgressBar progressBar = trGraphics.progressBar;

        // Assemble control panel
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

	c.gridy = 1;
	c.gridwidth = 0;
	c.anchor = GridBagConstraints.WEST;
	c.weightx = 0.;
	g.setConstraints (progressBar, c);
	trControl.add(progressBar);

	c.gridy = 2;
	c.gridwidth = 0;
	c.anchor = GridBagConstraints.WEST;
	c.weightx = 0.;
	g.setConstraints (bottomPanel, c);
	trControl.add (bottomPanel);
	
        Container contentPane = getContentPane();

        // Assemble the content pane
	g = new GridBagLayout();
	c = new GridBagConstraints();
        contentPane.setLayout( g );
        c.weightx = 1.;
        c.weighty = 1.;
        c.gridx = 0;
	c.fill = GridBagConstraints.BOTH;
	g.setConstraints( trGraphics, c );
        contentPane.add( trGraphics );
        c.gridy = 1;
	c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.;
        c.weighty = 0.;
        g.setConstraints (trControl, c);
        contentPane.add (trControl);

        //Create the menu bar.
        menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        //Build the first menu.
        menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);
        menu.getAccessibleContext().setAccessibleDescription( "text");
        menuBar.add(menu);

        menuItem = new JMenuItem("Open...");
        menuItem.setMnemonic('o');
	    ActionListener startFileChooser = new ActionListener() {
	      public void actionPerformed(ActionEvent e) {
		  JFileChooser chooser = new JFileChooser();
		  ViewerFileFilter filter_vfd = new ViewerFileFilter(
		    new String[] {"vfd"}, "Vftrace raw sample files"
		  );
		  ViewerFileFilter filter_std = new ViewerFileFilter(
		    new String[] {"std"}, "Vftrace multiplexed sample files"
		  );
		  ViewerFileView fileView = new ViewerFileView();
		  chooser.setFileView(fileView);
		  chooser.addChoosableFileFilter(filter_vfd);
		  chooser.addChoosableFileFilter(filter_std);
		  chooser.setFileFilter(filter_vfd);
		  chooser.setCurrentDirectory(new File("."));
		  chooser.setMultiSelectionEnabled( true );

		  int retval = chooser.showOpenDialog(TraceFrame.this);
		  if(retval == 0) {
		    File[] theFiles = chooser.getSelectedFiles();
		    Stack<String> traceFiles = new Stack<String>();
		    for (int i = 0; i < theFiles.length; i++) {
		        String filename = theFiles[i].getParent()+"/"+
			                  theFiles[i].getName();
			traceFiles.push (filename);
		    }
		    int debug = traceInfo.debug;
                    // Set the time to invalid values
                    // This leads to TraceInfo figuring out the timings itself
		    double tmin =  0.;
		    double tmax = -1.;
                    TraceView traceView = traceInfo.traceView;
		    traceInfo.stopThisThread();
		    traceInfo = new TraceInfo (traceFiles, debug, tmin, tmax );
		    traceInfo.traceGraphics = trGraphics;
		    traceInfo.commGraphics   = commGraphFrame.commGraphics;
		    traceInfo.traceView      = traceView;
		    traceView.traceInfo      = traceInfo;
		    trGraphics.setTrace (traceInfo);
		    commGraphFrame.traceInfo = traceInfo;
		    commGraphFrame.commGraphics.trace 
		                             = traceInfo;
		    traceInfo.start();
		    return;
		  } 
		  JOptionPane.showMessageDialog(TraceFrame.this, "No file chosen");
	    }
	};
	menuItem.addActionListener(startFileChooser);
	menu.add(menuItem);

    menuItem = new JMenuItem("Save Screenshot");
    menuItem.setMnemonic('s');
    
    ActionListener filesaver = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	    JFileChooser saver = new JFileChooser();
	    
	    
	    saver.setDialogTitle("Specify a file to save");   
	    FileNameExtensionFilter FF_png=  new FileNameExtensionFilter("*.png", "png");
	    FileNameExtensionFilter FF_jpg=  new FileNameExtensionFilter("*.jpg", "jpg");
	    
		saver.setFileFilter(FF_png);
		saver.addChoosableFileFilter(FF_jpg);
	    saver.setCurrentDirectory(new File("."));	 
	    
		int userSelection = saver.showSaveDialog(TraceFrame.this);
		
	    if (userSelection == JFileChooser.APPROVE_OPTION) {
	      File fileToSave = saver.getSelectedFile();
	      String filePath = fileToSave.getAbsolutePath();
	      
	      if (saver.getFileFilter()==FF_png) {
	        if(!filePath.endsWith(".png")) {
	    	  fileToSave = new File(filePath + ".png");
	        }
     	    TraceFrame.this.trGraphics.drawingArea.save_image(fileToSave,"png");
	      } else if (saver.getFileFilter()==FF_jpg) {
		    if(!filePath.endsWith(".jpg")) {
			  fileToSave = new File(filePath + ".jpg");
			}
		    TraceFrame.this.trGraphics.drawingArea.save_image(fileToSave,"jpg");
	      } else {
		    if(!filePath.endsWith(".png")) {
			  fileToSave = new File(filePath + ".png");
			}
		    TraceFrame.this.trGraphics.drawingArea.save_image(fileToSave,"png");  
	      }
	    }
      }
    };
	menuItem.addActionListener(filesaver);
	menu.add(menuItem);
	
	
	menuItem = new JMenuItem("About");
        menuItem.setMnemonic('a');
	menuItem.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		if (aboutFrame == null) {
			aboutFrame = new AboutFrame( version, build );
		} else {
			aboutFrame.setVisible(true);
		}
	    }
	}
	);
	menu.add(menuItem);

        menuItem = new JMenuItem("Exit");
        menuItem.setMnemonic('x');
	menuItem.getAccessibleContext().setAccessibleDescription(
	                                            "Exit the application");
	menuItem.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		System.exit(0);
	    }
	}
	);
	menu.add(menuItem);

        menu = new JMenu("Options");
        menu.setMnemonic(KeyEvent.VK_O);
        menuBar.add(menu);

        menuItem = new JMenuItem("Colors");
        menuItem.setMnemonic('c');
	menuItem.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		colorFrame.setVisible(true);
	    }
	});
	menu.add(menuItem);

	menuItem = new JMenuItem ("Hardware Counters");
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			hwcFrame.setVisible(true);
			showPerfGraph = hwcFrame.showPerfGraph;
		}
	});
	menu.add(menuItem);

	menuItem = new JMenuItem ("Bookmarks");
	menuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			bookmarkFrame.update();
			bookmarkFrame.setVisible(true);
		}
	});
	menu.add(menuItem);

        cbPreciseStacks = new JCheckBoxMenuItem("Show precise stacks");
        cbPreciseStacks.setMnemonic('s');
	cbPreciseStacks.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		showPreciseStacks = cbPreciseStacks.getState();
		trGraphics.refresh();
	    }
	});
	menu.add(cbPreciseStacks);

        cbMarkSamples = new JCheckBoxMenuItem("Mark samples");
        cbMarkSamples.setMnemonic('a');
	cbMarkSamples.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		markSamples = cbMarkSamples.getState();
		trGraphics.refresh();
	    }
	});
	menu.add(cbMarkSamples);

        menuItem = new JMenuItem("MPI info");
        menuItem.setMnemonic('m');
	menuItem.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		commTableFrame.setVisible(true);
	    }
	});
	menu.add(menuItem);

        menuItem = new JMenuItem("Profile");
        menuItem.setMnemonic('p');
	menuItem.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
	        showProfileFrame();
	    }
	});
	menu.add(menuItem);

        menuItem = new JMenuItem("Reset profile time");
        menuItem.setMnemonic('t');
	menuItem.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		traceInfo.resetProfileTime();
                trGraphics.showProfileInterval = false;
		trGraphics.repaint();
	    }
	});
	menu.add(menuItem);

        menuItem = new JMenuItem("Comm Matrix");
        menuItem.setMnemonic('c');
	menuItem.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		showCommGraphFrame();
	    }
	});
	menu.add(menuItem);

        menu = new JMenu("Help");
        menu.setMnemonic(KeyEvent.VK_H);
        menuBar.add(menu);

        menuItem = new JMenuItem("Manual");
        menuItem.setMnemonic('b');
	menuItem.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
	        showHelpFrame();
	    }
	});
	menu.add(menuItem);
	
	setTitle ("Vftrace");
	setSize( size );
	setLocation( 0, 340 );
        setVisible( true );      

    }
    
    void showProfileFrame() {
	if( profileFrame == null )  {
	    TraceInfo ti = traceInfo;
	    profileFrame = new ProfileFrame( ti );
	} else 
	    profileFrame.setVisible(true);
    }

    void showCommGraphFrame() {
	commGraphFrame.setVisible(true);
    }

    public CommGraphFrame getCommGraphFrame() {
	return commGraphFrame;
    }

    void showHelpFrame() {
	if (helpFrame == null) {
		helpFrame = new HelpFrame(helpdir);
	} else {
		helpFrame.setVisible(true);
	}
    }

    void updateProfileFrame() {
	if (profileFrame == null) {
		showProfileFrame();
	} else {
		profileFrame.update();
	}
    }

}
