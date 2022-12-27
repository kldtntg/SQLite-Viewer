package viewer;

import org.sqlite.SQLiteDataSource;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class SQLiteViewer extends JFrame {

    Connection con;
    JComboBox<String> comboBox = new JComboBox<>() ;

    JTextArea textArea; //the custom-generated query will be shown here


    JTable table;
    JButton button2;



    public SQLiteViewer() {
        super("SQLite Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 900);
        setLayout(null);
        setResizable(true);
        setLocationRelativeTo(null);

        setName("SQLite Viewer");
        setLayout(new FlowLayout(FlowLayout.LEFT, 1 , 1));

        JTextField text_field = new JTextField(); // type the (relative) database name here
        text_field.setName("FileNameTextField");
        text_field.setPreferredSize(new Dimension(500, 200));

        JButton button = new JButton("Open File");
        button.setName("OpenFileButton");
        button.addActionListener(e -> {

            System.out.println("Open File button pressed");
            String database_name = text_field.getText();

            int count = comboBox.getItemCount();
            for (int i = 0; i < count; i ++){ // remove all existing items in comboBox as we're opening another database
                comboBox.removeItemAt(0);//keep removing the first item inside comboBox until its empty
            }

            textArea.setEnabled(false);
            button2.setEnabled(false);

            openDatabase(database_name);
            populateCombobox();


        });



        comboBox.setName("TablesComboBox");
        comboBox.setSelectedItem(null);
        comboBox.addActionListener(e -> {
            setTextArea("SELECT * FROM " + comboBox.getSelectedItem() +";"); // custom query
        });




        textArea = new JTextArea();
        textArea.setName("QueryTextArea");
        textArea.setEnabled(false);



        button2 = new JButton("Execute");
        button2.setName("ExecuteQueryButton");
        button2.setEnabled(false);
        button2.addActionListener(e -> {
            String query = textArea.getText(); // get the current query
            populateTable(query);

        });


        TableModel tabelModel = new ReturnedTableModel();
        table = new JTable(tabelModel);
        table.setName("Table");
        tabelModel.addTableModelListener(e -> {System.out.println("Table model changed");});



        add(text_field);
        add(button);
        add(button2);
        add(textArea);
        add(comboBox);
        add(table);


        setVisible(true);
    }

    public void openDatabase(String database_name){

        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + database_name);


        try  { // DO NOT USE try-with-resources here because the database will be closed right after
            this.con = dataSource.getConnection();

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(new Frame(), "File doesn't exist");
        }


    }



    public void populateCombobox(){
        String sql = "SELECT name FROM sqlite_master WHERE type ='table' AND name NOT LIKE 'sqlite_%';\n"; // the query to select all tables from the corresponding database in con

        try {
            Statement stmt  = con.createStatement();
            ResultSet rs    = stmt.executeQuery(sql);

            // loop through the result set
            while (rs.next()) {

                ///// debugging
//                if( rs.getString("name") != null){
//                    System.out.println("rs not null");
//                } else {
//                    System.out.println("null");}

                ////////////////

                comboBox.addItem(rs.getString("name")); // add the names of the tables to the comboBox


            }

            if (comboBox.getItemCount() == 0){ // if comboBox is empty, it means the database is empty => disable textArea and the execute button
                JOptionPane.showMessageDialog(new Frame(), "File doesn't exist");
                textArea.setEnabled(false);
                button2.setEnabled(false);
            }
            else {
                textArea.setEnabled(true);
                button2.setEnabled(true);
            }

        } catch (SQLException e) {
            System.out.println("ERROR: " + e.getMessage());
            JOptionPane.showMessageDialog(new Frame(), e.getMessage());
        }


        //////////////debugging
        int itemCount = comboBox.getItemCount();
        if (itemCount == 0){
            System.out.println("ERROR in populating combobox");
        }
        //////////////////


    }

    public void setTextArea(String text){
        textArea.setText(text);
    }




    String[] columns; //the column name

    Object[][] data; // the actual data (the column names not included)
    public void populateTable(String query){ //param query is the current custom query in textArea


        try {
            Statement stmt  = con.createStatement();
            ResultSet rs    = stmt.executeQuery(query); // in this particular program, the query will tell the stmt to
            // retrieve all the columns from the specified table. The result is store in "rs"

            // loop through the result set, get all the column names and store it into "list"
            ArrayList<String> list = new ArrayList<>();
            int column_count = rs.getMetaData().getColumnCount(); // get the column count
            for (int i = 1; i <= column_count; i++) { //index of the first column starts at 1
                //System.out.println(rs.getMetaData().getColumnName(i));////
                list.add(rs.getMetaData().getColumnName(i));
            }
            columns = list.toArray(new String[0]); // assign "list" to "columns"
            //System.out.println(Arrays.toString(columns));////

//------------------------------------------  retrieve ALL the data (all rows)

            //data = new Object[column_count][]; // [colum_count = 3][row = not-known] = {{row1's id, row2's...}, {row1's name ,row2's...}, {row1's email, row2's...}}
            ArrayList<Object[]> arrayList_data = new ArrayList<>();

            while (rs.next()) { // for each row in rs
                ArrayList<Object>innerArrayList = new ArrayList<>(); //create a row to be added to arrayList_data

                System.out.println("");

                // for each row, iterate over the columns
                for (int i = 1; i <= column_count; i++) { //index of the first object starts at 1
                    innerArrayList.add(rs.getObject(i));
                }
                arrayList_data.add(innerArrayList.toArray(new Object[0]));
            }
            data = arrayList_data.toArray(new Object[0][0]);
            System.out.println(Arrays.deepToString(data));

        } catch (SQLException e) {
            System.out.println("ERROR: " + e.getMessage());
            JOptionPane.showMessageDialog(new Frame(), e.getMessage());
        }



        TableModel model = table.getModel();
        System.out.println(model.getColumnCount());
        ((ReturnedTableModel) model).setValue(columns, data);
        System.out.println(model.getColumnCount());








    }




}


class ReturnedTableModel extends AbstractTableModel {

    String[] columns = new String[0];
    Object[][] data = new Object[0][];



    @Override
    public int getRowCount() {
        return data.length;
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return data[rowIndex][columnIndex];
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columns[columnIndex];
    }

    @Override
    public void fireTableDataChanged() {
        super.fireTableDataChanged();
    }

    public void setValue(String[] columns, Object[][] data){
        this.columns = columns;
        this.data = data;
        fireTableStructureChanged(); //updates the JTable, and also notifies the TableModelListeners

    }

}