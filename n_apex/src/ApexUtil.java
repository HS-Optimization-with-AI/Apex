import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ApexUtil {

    private JButton button1;
    private JTextArea textArea1;
    private JPanel panelMain;
    private JTable table1;

    private String[] columnNames=new String[3];
    private String[][] dataValues=new String[3][3];

    public class myTableModel extends DefaultTableModel
    {
        myTableModel() {
            super(dataValues,columnNames);
        }
        public boolean isCellEditable(int row,int cols) {
            return true;
        }
    }

    public ApexUtil() {

        TableModel model = new myTableModel();
        table1 = new JTable(64, 64);
        button1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(null, "Hello!");
                textArea1.setText("Button clicked!!");
                TableCellRenderer c = table1.getCellRenderer(32, 32);
                c.s
            }
        });
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }

    public static void main(String[] args){
        JFrame frame = new JFrame("ApexUtil");
        frame.setContentPane(new ApexUtil().panelMain);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
