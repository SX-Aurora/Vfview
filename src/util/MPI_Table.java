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

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Component;
import java.text.DecimalFormat;

public class MPI_Table extends JPanel {
    public  boolean isOpen;
    private JTable table;
    private String[] columnNames = { 
        "#", "Record", "Start", "Stop", "Time", "Self", "", "Peer", "MBytes/s", "Count", "Type", "Tag" };
    private DefaultTableModel tableModel;
    private TableColumnModel columnModel;
    private JScrollPane scrollpane;
    private static final DecimalFormat fmbytes = new DecimalFormat( "###0.000" );
    private static final DecimalFormat ftime   = new DecimalFormat( "###0.000000" );
    private int rowNumber;

    public MPI_Table() {
        int i;
        tableModel = new DefaultTableModel(columnNames,0);
        table = new JTable(tableModel) {
             DefaultTableCellRenderer renderLeft  = new DefaultTableCellRenderer();
             DefaultTableCellRenderer renderRight = new DefaultTableCellRenderer();
             { // initializer block
                  renderLeft.setHorizontalAlignment(SwingConstants.LEFT);
                 renderRight.setHorizontalAlignment(SwingConstants.RIGHT);
             }
             // Override isCellEditable to prevent editing
             public boolean isCellEditable( int row, int col ) {
                 return false;
             }
             // Override getCellRenderer to set required alignment
             public TableCellRenderer getCellRenderer (int row, int col) {
                 return col == 9  ? renderLeft : renderRight;
             }
             public boolean getScrollableTracksViewportWidth()
             {
                 return getPreferredSize().width < getParent().getWidth();
             }
        };
        columnModel = table.getColumnModel();
        int col = 0;
        columnModel.getColumn(col++).setPreferredWidth( 40); // #
        columnModel.getColumn(col++).setPreferredWidth( 50); // Record
        columnModel.getColumn(col++).setPreferredWidth( 60); // Start
        columnModel.getColumn(col++).setPreferredWidth( 60); // Stop
        columnModel.getColumn(col++).setPreferredWidth( 60); // Time
        columnModel.getColumn(col++).setPreferredWidth( 40); // Self
        columnModel.getColumn(col++).setPreferredWidth( 10); // Direction <>
        columnModel.getColumn(col++).setPreferredWidth( 40); // Peer
        columnModel.getColumn(col++).setPreferredWidth(100); // MBytes/s
        columnModel.getColumn(col++).setPreferredWidth(100); // Count
        columnModel.getColumn(col++).setPreferredWidth(120); // Data type
        columnModel.getColumn(col++).setPreferredWidth( 50); // Tag
        table.setFillsViewportHeight(true);
        scrollpane = new JScrollPane(table);
	scrollpane.setPreferredSize(new Dimension(790,290));
        setLayout(new BorderLayout());
        add(scrollpane, BorderLayout.CENTER);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    }
    
    public void update (double tStart, double tStop, int self, int dir, int peer,
          int count, double mbytes, String type, int tag, int record) {
	double deltat = tStop - tStart;
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        Object[] row = { rowNumber++,
                         record,
                         fmbytes.format((Number)tStart),
                         fmbytes.format((Number)tStop),
			 fmbytes.format((Number)(deltat)),
                         self,
                         dir == 1 ? "<": ">",
                         peer,
                         fmbytes.format((Number)(mbytes/deltat)),
                         count,
                         type,
                         tag
                         };
        model.addRow( row );
    }

    public void clear() {
        int i;
        int n = tableModel.getRowCount();
        for( i=0; i<n; i++ ) {
            tableModel.removeRow(0);
        }
        rowNumber = 0;
    }

}
