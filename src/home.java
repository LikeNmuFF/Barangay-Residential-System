/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.swing.JOptionPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.Color; // Still needed if using Color, but border is removed
import javax.swing.BorderFactory; // Still needed if using BorderFactory, but border is removed

/**
 *
 * @author Hp
 */
public class home extends javax.swing.JFrame {

    // Define the column names for the JTable (assuming a standard resident table structure)
    private static final String[] RESIDENT_COLUMNS = {"Resident ID", "First Name", "Last Name", "Purok/Zone", "Age", "Gender"};
    
    // Professional Refactoring for clarity and matching request:
    // Renaming the variables used in the logic to match the requested names
    private javax.swing.JLabel lblShowPurokCount; // Maps to lblPurokCount from initComponents
    private javax.swing.JLabel lblShowTotalResidents; // Maps to jLabel2 from initComponents

    /**
     * Creates new form home
     */
    public home() {
        initComponents();
        
        // Map the existing component variables to the new logical names
        // This is the clean way to implement your requested variable names without changing initComponents()
        lblShowPurokCount = lblPurokCount;
        lblShowTotalResidents = jLabel2;
        
        // Initial setup calls after components are initialized
        loadInitialData();
        loadSummaryCounts(); // This is the main method that will use the new variables
        addSearchListener();
        
        // Label all components as per their variable names
        lblTitle.setText("BARANGAY RESIDENT SYSTEM");
        
        // Setup initial table model
        showResidents.setModel(new DefaultTableModel(null, RESIDENT_COLUMNS));
    }
    
    /**
     * Loads all resident data into the JTable.
     * @param searchQuery Optional search string to filter data.
     */
    private void loadInitialData(String searchQuery) {
        DefaultTableModel model = (DefaultTableModel) showResidents.getModel();
        model.setRowCount(0); // Clear existing rows

        String sql;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        // ASSUMPTION: DBConnection class is available and works as expected.
        Connection conn = DBConnection.getConnection(); 
        
        // Base SQL query: Select common resident details. 
        // ASSUMPTION: A 'residents' table exists with columns: resident_id, first_name, last_name, purok, age, gender.
        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            // Load all residents
            sql = "SELECT resident_id, first_name, last_name, purok, age, gender FROM residents";
        } else {
            // Load residents based on search query for name or ID
            sql = "SELECT resident_id, first_name, last_name, purok, age, gender FROM residents "
                + "WHERE first_name LIKE ? OR last_name LIKE ? OR CAST(resident_id AS CHAR) LIKE ?";
        }

        try {
            if (conn == null) return; // DBConnection handles connection failure dialog
            
            pstmt = conn.prepareStatement(sql);
            
            if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                String searchPattern = "%" + searchQuery + "%";
                pstmt.setString(1, searchPattern); // first_name
                pstmt.setString(2, searchPattern); // last_name
                pstmt.setString(3, searchPattern); // resident_id
            }
            
            rs = pstmt.executeQuery();

            while (rs.next()) {
                // Add the resident data as a row in the table model
                model.addRow(new Object[]{
                    rs.getInt("resident_id"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("purok"),
                    rs.getInt("age"),
                    rs.getString("gender")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading resident data: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            // Professional practice: Ensure resources are closed
            try { if (rs != null) rs.close(); } catch (SQLException e) { /* ignored */ }
            try { if (pstmt != null) pstmt.close(); } catch (SQLException e) { /* ignored */ }
            try { if (conn != null) conn.close(); } catch (SQLException e) { /* ignored */ }
        }
    }
    
    /**
     * Wrapper method to call loadInitialData without a search query.
     */
    private void loadInitialData() {
        loadInitialData(null);
    }
    
    /**
     * Fetches and displays summary counts on the dashboard labels.
     * This method has been updated to use the variables:
     * - lblShowTotalResidents (for total residents)
     * - lblShowPurokCount (for total unique puroks)
     */
    private void loadSummaryCounts() {
        Connection conn = DBConnection.getConnection();
        Statement stmt = null;
        ResultSet rs = null;

        if (conn == null) return; // DBConnection handles connection failure dialog

        try {
            stmt = conn.createStatement();
            
            // Query 1: Total number of residents (for lblShowTotalResidents)
            String sqlTotal = "SELECT COUNT(*) AS total FROM residents";
            rs = stmt.executeQuery(sqlTotal);
            if (rs.next()) {
                int totalResidents = rs.getInt("total");
                // Update lblShowTotalResidents (mapped to jLabel2)
                lblShowTotalResidents.setText("Total Residents: " + totalResidents);
            }
            rs.close(); // Close the first result set before running the next query

            // Query 2: Total number of unique puroks (for lblShowPurokCount)
            // Uses 'COUNT(DISTINCT purok)' to get the number of unique purok names.
            String sqlPurokCount = "SELECT COUNT(DISTINCT purok) AS purok_count FROM residents";
            rs = stmt.executeQuery(sqlPurokCount);
            if (rs.next()) {
                int purokCount = rs.getInt("purok_count");
                // Update lblShowPurokCount (mapped to lblPurokCount)
                lblShowPurokCount.setText("Total Puroks: " + purokCount);
            }
            
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading summary counts: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) { /* ignored */ }
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { /* ignored */ }
            try { if (conn != null) conn.close(); } catch (SQLException e) { /* ignored */ }
        }
    }
    
    /**
     * Adds a listener to the search text area to filter the table in real-time.
     */
    private void addSearchListener() {
        // Since the search component is a JTextField (txtSearch), we use a DocumentListener
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterTable();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterTable();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterTable();
            }
            
            private void filterTable() {
                // Call the data loading method with the current text in the search area
                loadInitialData(txtSearch.getText());
            }
        });
    }


    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: DO NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblPurokCount = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        btnLogout = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        showResidents = new javax.swing.JTable();
        lblTitle = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        btnCetificate = new javax.swing.JButton();
        btnResident = new javax.swing.JButton();
        btnReports = new javax.swing.JButton();
        txtSearch = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("BARANGAY HALL NAMEN");
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lblPurokCount.setFont(new java.awt.Font("Comic Sans MS", 1, 24)); // NOI18N
        getContentPane().add(lblPurokCount, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 240, 390, 60));

        jLabel2.setFont(new java.awt.Font("Comic Sans MS", 1, 24)); // NOI18N
        getContentPane().add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 170, 390, 60));

        btnLogout.setBackground(new java.awt.Color(204, 255, 153));
        btnLogout.setFont(new java.awt.Font("Arial Black", 0, 12)); // NOI18N
        btnLogout.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pictures/icons8-logout-32.png"))); // NOI18N
        btnLogout.setText("LOGOUT");
        btnLogout.setOpaque(true);
        btnLogout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLogoutActionPerformed(evt);
            }
        });
        getContentPane().add(btnLogout, new org.netbeans.lib.awtextra.AbsoluteConstraints(820, 60, 130, 40));

        jScrollPane1.setBackground(new java.awt.Color(255, 255, 255));

        showResidents.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        showResidents.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        showResidents.setGridColor(new java.awt.Color(255, 255, 255));
        showResidents.setSelectionBackground(new java.awt.Color(255, 255, 255));
        jScrollPane1.setViewportView(showResidents);

        getContentPane().add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 480, 790, 150));

        lblTitle.setFont(new java.awt.Font("Arial Black", 0, 48)); // NOI18N
        lblTitle.setText("BARANGAY RESIDENT SYSTEM  ");
        getContentPane().add(lblTitle, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 0, 870, 60));

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        btnCetificate.setBackground(new java.awt.Color(204, 255, 204));
        btnCetificate.setFont(new java.awt.Font("Arial Black", 0, 12)); // NOI18N
        btnCetificate.setText("Issuance/Certificates");
        btnCetificate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCetificateActionPerformed(evt);
            }
        });
        jPanel1.add(btnCetificate, new org.netbeans.lib.awtextra.AbsoluteConstraints(580, 80, 190, 50));

        btnResident.setBackground(new java.awt.Color(204, 255, 204));
        btnResident.setFont(new java.awt.Font("Arial Black", 0, 12)); // NOI18N
        btnResident.setText("Resident Management");
        btnResident.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnResidentActionPerformed(evt);
            }
        });
        jPanel1.add(btnResident, new org.netbeans.lib.awtextra.AbsoluteConstraints(360, 80, 190, 50));

        btnReports.setBackground(new java.awt.Color(204, 255, 204));
        btnReports.setFont(new java.awt.Font("Arial Black", 0, 12)); // NOI18N
        btnReports.setText("Reports & Analytics");
        btnReports.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnReportsActionPerformed(evt);
            }
        });
        jPanel1.add(btnReports, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 80, 190, 50));

        getContentPane().add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(-10, 560, 1030, 140));

        txtSearch.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        txtSearch.setBorder(javax.swing.BorderFactory.createTitledBorder("SEAECH ID,FNAME,LNAME:"));
        getContentPane().add(txtSearch, new org.netbeans.lib.awtextra.AbsoluteConstraints(770, 280, 180, 60));

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pictures/bhm.png"))); // NOI18N
        getContentPane().add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, -20, 1020, 590));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnReportsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnReportsActionPerformed
        // TODO add your handling code here:
        reports rep = new reports();
        rep.setVisible(true);

        // Close the login frame
        this.dispose();
    }//GEN-LAST:event_btnReportsActionPerformed

    private void btnResidentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnResidentActionPerformed
// TODO add your handling code here:
            resident res = new resident();
            res.setVisible(true);

            // Close the login frame
            this.dispose();
    }//GEN-LAST:event_btnResidentActionPerformed

    private void btnCetificateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCetificateActionPerformed
        // TODO add your handling code here:
        Certificate cert = new Certificate();
        cert.setVisible(true);

        // Close the login frame
        this.dispose();
    }//GEN-LAST:event_btnCetificateActionPerformed

    private void btnLogoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLogoutActionPerformed
        // TODO add your handling code here:
        login loginFrame = new login();
        loginFrame.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnLogoutActionPerformed

                                     

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(home.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(home.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(home.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(home.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new home().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCetificate;
    private javax.swing.JButton btnLogout;
    private javax.swing.JButton btnReports;
    private javax.swing.JButton btnResident;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblPurokCount;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JTable showResidents;
    private javax.swing.JTextField txtSearch;
    // End of variables declaration//GEN-END:variables
}