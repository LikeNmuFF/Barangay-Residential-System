/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Hp
 */
public class resident extends javax.swing.JFrame {

    // --- DB and Table Constants ---
    private static final String[] RESIDENT_COLUMNS = {"ID", "First Name", "Last Name", "Purok/Zone", "Address", "Gender", "Civil Status", "Date of Birth"};
    
    // --- State Variables ---
    // Tracks the ID of the resident currently loaded in the form for Update/Delete.
    private int selectedResidentId = -1; 
    
    // --- Component Groups ---
    private ButtonGroup genderGroup = new ButtonGroup();

    /**
     * Creates new form resident
     */
    public resident() {
        initComponents();
        setupForm();
        loadResidentsTable();
    }
    
    /**
     * Initial setup for component models and listeners.
     */
    private void setupForm() {
        // 1. Labeling
        jLabel2.setText("RESIDENT MANAGEMENT");
        
        // 2. Setup Gender Radio Buttons
        genderGroup.add(jRadioButtonGenderMale);
        genderGroup.add(jRadioButtonGenderFEmale);

        // 3. Populate Civil Status ComboBox
        jComboBoxCivilStatus.removeAllItems();
        jComboBoxCivilStatus.addItem("Single");
        jComboBoxCivilStatus.addItem("Married");
        jComboBoxCivilStatus.addItem("Widowed");
        jComboBoxCivilStatus.addItem("Divorced");
        
        // 4. Set initial table model and listener
        tblResidentLists.setModel(new DefaultTableModel(null, RESIDENT_COLUMNS));
        tblResidentLists.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                tableRowSelected();
            }
        });
        
        // 5. Initial form state
        clearFields();
    }

    // ------------------------------------------------------------------------------------------------
    // --- CRUD Operations (Read) ---
    // ------------------------------------------------------------------------------------------------

    /**
     * Loads all resident data from the database into the JTable. (Read operation)
     */
    private void loadResidentsTable() {
        DefaultTableModel model = (DefaultTableModel) tblResidentLists.getModel();
        model.setRowCount(0); // Clear existing rows

        // Removed age from SELECT as it is typically calculated from date_of_birth
        String sql = "SELECT resident_id, first_name, last_name, purok, address, gender, civil_status, date_of_birth FROM residents ORDER BY resident_id DESC";
        
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (conn == null) return; 

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("resident_id"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("purok"),
                    rs.getString("address"),
                    rs.getString("gender"),
                    rs.getString("civil_status"),
                    rs.getDate("date_of_birth")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading resident data: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
    
    /**
     * Fills the form fields when a row in the table is clicked.
     */
    private void tableRowSelected() {
        int selectedRow = tblResidentLists.getSelectedRow();
        if (selectedRow >= 0) {
            DefaultTableModel model = (DefaultTableModel) tblResidentLists.getModel();
            
            // Set the state ID
            selectedResidentId = (int) model.getValueAt(selectedRow, 0); 
            
            // Fill the form fields
            txtFname.setText(model.getValueAt(selectedRow, 1).toString());
            txtLastName.setText(model.getValueAt(selectedRow, 2).toString());
            txtPurok.setText(model.getValueAt(selectedRow, 3).toString());
            // Handles null address gracefully
            txtAddress.setText(model.getValueAt(selectedRow, 4) != null ? model.getValueAt(selectedRow, 4).toString() : "");

            // Set Gender
            Object genderObj = model.getValueAt(selectedRow, 5);
            if (genderObj != null) {
                String gender = genderObj.toString();
                if ("Male".equalsIgnoreCase(gender)) {
                    jRadioButtonGenderMale.setSelected(true);
                } else if ("Female".equalsIgnoreCase(gender)) {
                    jRadioButtonGenderFEmale.setSelected(true);
                }
            } else {
                genderGroup.clearSelection();
            }
            
            // Set Civil Status
            Object statusObj = model.getValueAt(selectedRow, 6);
            if (statusObj != null) {
                jComboBoxCivilStatus.setSelectedItem(statusObj.toString());
            }
            
            // Set Birth Date
            java.sql.Date sqlDate = (java.sql.Date) model.getValueAt(selectedRow, 7);
            if (sqlDate != null) {
                jDateChooser1.setDate(new Date(sqlDate.getTime()));
            } else {
                jDateChooser1.setDate(null);
            }
        }
    }


    // ------------------------------------------------------------------------------------------------
    // --- Utility Methods ---
    // ------------------------------------------------------------------------------------------------

    /**
     * Resets all input fields and the state variable.
     */
    private void clearFields() {
        txtFname.setText("");
        txtLastName.setText("");
        txtPurok.setText("");
        txtAddress.setText("");
        genderGroup.clearSelection();
        jComboBoxCivilStatus.setSelectedIndex(0); // Set to the first item 
        jDateChooser1.setDate(null);
        selectedResidentId = -1; // Indicate no resident is currently selected/being edited
        tblResidentLists.clearSelection();
    }
    
    /**
     * Validates and returns the selected gender.
     * @return "Male", "Female", or null if none selected.
     */
    private String getSelectedGender() {
        if (jRadioButtonGenderMale.isSelected()) {
            return "Male";
        } else if (jRadioButtonGenderFEmale.isSelected()) {
            return "Female";
        }
        return null;
    }
    
    // ------------------------------------------------------------------------------------------------
    // --- CRUD Operations (Action Listeners) ---
    // ------------------------------------------------------------------------------------------------

    private void btnAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddActionPerformed
        // (C) Clear fields and prepare for a new record insertion
        clearFields();
        JOptionPane.showMessageDialog(this, "Form cleared. Enter details for a new resident.", "Ready to Add", JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_btnAddActionPerformed

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        // (C/U) Handle both INSERT (Create) and UPDATE operations
        
        // 1. Get data and validate
        String firstName = txtFname.getText().trim();
        String lastName = txtLastName.getText().trim();
        String purok = txtPurok.getText().trim();
        String address = txtAddress.getText().trim();
        String gender = getSelectedGender();
        String civilStatus = jComboBoxCivilStatus.getSelectedItem().toString();
        Date birthDateUtil = jDateChooser1.getDate();
        java.sql.Date sqlBirthDate = (birthDateUtil != null) ? new java.sql.Date(birthDateUtil.getTime()) : null;

        if (firstName.isEmpty() || lastName.isEmpty() || purok.isEmpty() || gender == null) {
            JOptionPane.showMessageDialog(this, "Please fill in First Name, Last Name, Purok, and Gender.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql;
        int result;
        boolean isUpdate = selectedResidentId != -1;

        if (isUpdate) {
            // UPDATE operation (U) - Age column intentionally omitted, can be calculated from DOB
            sql = "UPDATE residents SET first_name = ?, last_name = ?, purok = ?, address = ?, gender = ?, civil_status = ?, date_of_birth = ? WHERE resident_id = ?";
        } else {
            // INSERT operation (C) - Age column intentionally omitted, can be set as NULL
            sql = "INSERT INTO residents (first_name, last_name, purok, address, gender, civil_status, date_of_birth) VALUES (?, ?, ?, ?, ?, ?, ?)";
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (conn == null) return; 

            // Set parameters
            int i = 1;
            pstmt.setString(i++, firstName);
            pstmt.setString(i++, lastName);
            pstmt.setString(i++, purok);
            pstmt.setString(i++, address);
            pstmt.setString(i++, gender);
            pstmt.setString(i++, civilStatus);
            pstmt.setDate(i++, sqlBirthDate);
            
            if (isUpdate) {
                // Last parameter for WHERE clause in UPDATE
                pstmt.setInt(i, selectedResidentId); 
            }

            result = pstmt.executeUpdate();

            if (result > 0) {
                String message = isUpdate ? "Resident record updated successfully." : "New resident added successfully.";
                JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
                loadResidentsTable(); // Refresh table
                clearFields(); // Clear form after successful save
            } else {
                 JOptionPane.showMessageDialog(this, "Operation failed. No rows affected.", "Failure", JOptionPane.ERROR_MESSAGE);
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database Error during Save: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }//GEN-LAST:event_btnSaveActionPerformed

    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteActionPerformed
        // (D) Delete operation
        if (selectedResidentId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a resident from the table to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete Resident ID " + selectedResidentId + "? This will also affect any associated Certificates.", 
            "Confirm Deletion", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            // Because 'certificates' has a foreign key constraint ON DELETE SET NULL, 
            // deleting a resident will not fail, but will set their IDs to NULL in the certificates table.
            String sql = "DELETE FROM residents WHERE resident_id = ?";
            
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                if (conn == null) return; 
                
                pstmt.setInt(1, selectedResidentId);
                int result = pstmt.executeUpdate();

                if (result > 0) {
                    JOptionPane.showMessageDialog(this, "Resident deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    loadResidentsTable(); // Refresh table
                    clearFields(); // Clear form and reset state
                } else {
                    JOptionPane.showMessageDialog(this, "Deletion failed. Record not found.", "Failure", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database Error during Delete: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }//GEN-LAST:event_btnDeleteActionPerformed

    private void btnClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearActionPerformed
        // Clear all fields and reset state, useful for canceling an edit
        clearFields();
    }//GEN-LAST:event_btnClearActionPerformed
    
    // ------------------------------------------------------------------------------------------------
    // --- Auto-generated Code (DO NOT MODIFY initComponents) ---
    // ------------------------------------------------------------------------------------------------

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel2 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblResidentLists = new javax.swing.JTable();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jRadioButtonGenderMale = new javax.swing.JRadioButton();
        jRadioButtonGenderFEmale = new javax.swing.JRadioButton();
        jLabel8 = new javax.swing.JLabel();
        jDateChooser1 = new com.toedter.calendar.JDateChooser();
        jLabel9 = new javax.swing.JLabel();
        jComboBoxCivilStatus = new javax.swing.JComboBox<>();
        txtPurok = new javax.swing.JTextField();
        txtLastName = new javax.swing.JTextField();
        txtAddress = new javax.swing.JTextField();
        txtFname = new javax.swing.JTextField();
        btnDelete = new javax.swing.JButton();
        btnSave = new javax.swing.JButton();
        btnAdd = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel2.setFont(new java.awt.Font("Arial Black", 0, 36)); // NOI18N
        jLabel2.setText("RESIDENT MANAGEMENT");
        getContentPane().add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(490, 10, 540, -1));

        tblResidentLists.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane1.setViewportView(tblResidentLists);

        getContentPane().add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 140, 980, 260));

        jLabel3.setFont(new java.awt.Font("Arial Black", 0, 14)); // NOI18N
        jLabel3.setText("First Name");
        getContentPane().add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 470, -1, -1));

        jLabel4.setFont(new java.awt.Font("Arial Black", 0, 14)); // NOI18N
        jLabel4.setText("Last Name");
        getContentPane().add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 530, -1, -1));

        jLabel5.setFont(new java.awt.Font("Arial Black", 0, 14)); // NOI18N
        jLabel5.setText("Purok");
        getContentPane().add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 590, -1, -1));

        jLabel6.setFont(new java.awt.Font("Arial Black", 0, 14)); // NOI18N
        jLabel6.setText("Civil Status");
        getContentPane().add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(390, 600, -1, -1));

        jLabel7.setFont(new java.awt.Font("Arial Black", 0, 14)); // NOI18N
        jLabel7.setText("Address");
        getContentPane().add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(400, 480, -1, -1));

        jRadioButtonGenderMale.setFont(new java.awt.Font("Arial Black", 0, 14)); // NOI18N
        jRadioButtonGenderMale.setText("Male");
        getContentPane().add(jRadioButtonGenderMale, new org.netbeans.lib.awtextra.AbsoluteConstraints(570, 520, -1, -1));

        jRadioButtonGenderFEmale.setFont(new java.awt.Font("Arial Black", 0, 14)); // NOI18N
        jRadioButtonGenderFEmale.setText("Female");
        getContentPane().add(jRadioButtonGenderFEmale, new org.netbeans.lib.awtextra.AbsoluteConstraints(490, 520, -1, -1));

        jLabel8.setFont(new java.awt.Font("Arial Black", 0, 14)); // NOI18N
        jLabel8.setText("Gender");
        getContentPane().add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 520, -1, -1));
        getContentPane().add(jDateChooser1, new org.netbeans.lib.awtextra.AbsoluteConstraints(490, 560, 140, -1));

        jLabel9.setFont(new java.awt.Font("Arial Black", 0, 14)); // NOI18N
        jLabel9.setText("Birth Date");
        getContentPane().add(jLabel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(390, 560, -1, -1));

        jComboBoxCivilStatus.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jComboBoxCivilStatus.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        getContentPane().add(jComboBoxCivilStatus, new org.netbeans.lib.awtextra.AbsoluteConstraints(490, 600, 140, -1));

        txtPurok.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        txtPurok.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtPurokActionPerformed(evt);
            }
        });
        getContentPane().add(txtPurok, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 590, 170, 30));

        txtLastName.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        txtLastName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtLastNameActionPerformed(evt);
            }
        });
        getContentPane().add(txtLastName, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 520, 170, 40));

        txtAddress.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        txtAddress.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtAddressActionPerformed(evt);
            }
        });
        getContentPane().add(txtAddress, new org.netbeans.lib.awtextra.AbsoluteConstraints(480, 480, 170, 30));

        txtFname.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        txtFname.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtFnameActionPerformed(evt);
            }
        });
        getContentPane().add(txtFname, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 460, 170, 40));

        btnDelete.setBackground(new java.awt.Color(204, 255, 153));
        btnDelete.setFont(new java.awt.Font("Arial Black", 0, 14)); // NOI18N
        btnDelete.setText("DELETE");
        btnDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteActionPerformed(evt);
            }
        });
        getContentPane().add(btnDelete, new org.netbeans.lib.awtextra.AbsoluteConstraints(830, 560, 170, 40));

        btnSave.setBackground(new java.awt.Color(204, 255, 153));
        btnSave.setFont(new java.awt.Font("Arial Black", 0, 14)); // NOI18N
        btnSave.setText(" SAVE|UPDATE RECORD");
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });
        getContentPane().add(btnSave, new org.netbeans.lib.awtextra.AbsoluteConstraints(770, 460, 230, 40));

        btnAdd.setBackground(new java.awt.Color(204, 255, 153));
        btnAdd.setFont(new java.awt.Font("Arial Black", 0, 14)); // NOI18N
        btnAdd.setText("ADD NEW");
        btnAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddActionPerformed(evt);
            }
        });
        getContentPane().add(btnAdd, new org.netbeans.lib.awtextra.AbsoluteConstraints(830, 510, 170, 40));

        jButton1.setBackground(new java.awt.Color(204, 255, 204));
        jButton1.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pictures/icons8-back-96.png"))); // NOI18N
        jButton1.setText("BACK");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        getContentPane().add(jButton1, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 20, -1, 50));

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pictures/bgg.png"))); // NOI18N
        getContentPane().add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1030, 670));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txtPurokActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtPurokActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtPurokActionPerformed

    private void txtLastNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtLastNameActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtLastNameActionPerformed

    private void txtAddressActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtAddressActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtAddressActionPerformed

    private void txtFnameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtFnameActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtFnameActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // BACK button
        home h = new home();
        h.setVisible(true);
        this.dispose(); // Use dispose() for proper cleanup and resource freeing
    }//GEN-LAST:event_jButton1ActionPerformed

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
            java.util.logging.Logger.getLogger(resident.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(resident.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(resident.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(resident.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new resident().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAdd;
    private javax.swing.JButton btnDelete;
    private javax.swing.JButton btnSave;
    private javax.swing.JButton jButton1;
    private javax.swing.JComboBox<String> jComboBoxCivilStatus;
    private com.toedter.calendar.JDateChooser jDateChooser1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JRadioButton jRadioButtonGenderFEmale;
    private javax.swing.JRadioButton jRadioButtonGenderMale;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable tblResidentLists;
    private javax.swing.JTextField txtAddress;
    private javax.swing.JTextField txtFname;
    private javax.swing.JTextField txtLastName;
    private javax.swing.JTextField txtPurok;
    // End of variables declaration//GEN-END:variables
}