package util;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.table.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

public class BookmarkTable extends JPanel {

	public boolean isOpen;
	private JTable table;
	private int nPerfValues;
	private String[] columnNames;
	private DefaultTableModel tableModel;
        private TableColumnModel columnModel;
        private JScrollPane scrollpane;
	private int selectedTrace;

	public BookmarkTable () {
		this.selectedTrace = 0;
		table = new JTable() {
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

           	table.setFillsViewportHeight(true);
           	scrollpane = new JScrollPane(table);
	   	scrollpane.setPreferredSize(new Dimension(790,290));
           	setLayout(new BorderLayout());
           	add(scrollpane, BorderLayout.CENTER);
           	table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	  }

	public BookmarkTable (Vector<Bookmark> bookmarks, String[] perfNames) {
		this();
		setColumns(perfNames);
		fill (bookmarks);
	}

	public void setSelectedTrace (int selectedTrace) {
		this.selectedTrace = selectedTrace;
	}

	public void setColumns (String [] perfNames) {
		nPerfValues = perfNames.length;
		columnNames = new String [2 + nPerfValues];
		columnNames[0] = "Time [s]";
		columnNames[1] = "Active function";
		for (int i = 2 ; i < 2 + nPerfValues; i++) {
			columnNames[i] = perfNames[i-2];
		}
		tableModel = new DefaultTableModel (columnNames, 0);
		table.setModel(tableModel);
     
		columnModel = table.getColumnModel();
   	   	int col = 0;
           	columnModel.getColumn(col++).setPreferredWidth (40); // Time
           	columnModel.getColumn(col++).setPreferredWidth (80); // Record
	   	while (col < 2 + nPerfValues) {
	   	        columnModel.getColumn(col++).setPreferredWidth (60);
	   	}
	}

	public void fill (Vector<Bookmark> bookmarks) {
           Collections.sort (bookmarks, new BookmarkCompareToSmallest());
	   Object[] row = new Object[2 + nPerfValues];
	   for (Bookmark b : bookmarks) {
		   if (selectedTrace >= 0 && (b.getAssociatedTrace() != selectedTrace)) {
			continue;
		   }
		   int col = 0;
		   row[col++] = b.getTime();
		   row[col++] = b.getActiveFunction();
		   while (col < 2 + nPerfValues) {
			   final int c = col - 2;
			   row[col++] = b.getPerfValue(c);
		   }
	   	   tableModel.addRow(row);
           }
	}
	
	private class BookmarkCompareToLargest implements Comparator<Bookmark> {
		@Override
		public int compare (Bookmark a, Bookmark  b) {
			return a.getTime() < b.getTime() ? -1 : a.getTime() == b.getTime() ? 0 : 1;
		}
	}

	private class BookmarkCompareToSmallest implements Comparator<Bookmark> {
		@Override
		public int compare (Bookmark a, Bookmark  b) {
			return a.getTime() < b.getTime() ? 1 : a.getTime() == b.getTime() ? 0 : -1;
		}
	}
}
