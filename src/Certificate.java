/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import java.io.FileOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;

// --- iText Imports ---
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfWriter;
import javax.swing.event.ListSelectionEvent;


/**
 *
 * @author Hp
 */
public class Certificate extends javax.swing.JFrame {

    // --- DB and Table Constants ---
    private static final String[] RESIDENT_COLUMNS = {"ID", "First Name", "Last Name", "Purok/Zone", "Address"};
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMMM dd, yyyy");
    
    // --- State Variables ---
    private int selectedResidentId = -1; 
    private String selectedResidentFullName = ""; // Stores the name of the selected resident
    
    /**
     * Creates new form Certificate
     */
    public Certificate() {
        initComponents();
        setupForm();
    }

    /**
     * Initializes component models, dropdowns, and listeners.
     */
    private void setupForm() {
        // 1. Setup Table Model
        showResidents.setModel(new DefaultTableModel(null, RESIDENT_COLUMNS));
        
        // 2. Setup Table Selection Listener (To grab the resident ID and name)
        showResidents.getSelectionModel().addListSelectionListener((ListSelectionEvent e) -> {
            if (!e.getValueIsAdjusting()) {
                tableRowSelected();
            }
        });
        
        // 3. Populate ComboBoxes 
        comboType.removeAllItems();
        comboType.addItem("Barangay Clearance");
        comboType.addItem("Indigency Certificate");
        comboType.addItem("Residency Certificate");
        comboType.addItem("Business Permit");
        
        comboPurpose.removeAllItems();
        comboPurpose.addItem("Employment");
        comboPurpose.addItem("School Requirement");
        comboPurpose.addItem("Financial Assistance");
        comboPurpose.addItem("Utility Connection");
        comboPurpose.addItem("Other");
    }

    /**
     * Retrieves the ID and Name of the resident when a table row is selected and updates the state.
     */
    private void tableRowSelected() {
        int selectedRow = showResidents.getSelectedRow();
        if (selectedRow >= 0) {
            DefaultTableModel model = (DefaultTableModel) showResidents.getModel();
            
            // Set the state ID (Assumes ID is in the first column, index 0)
            // Use Integer.valueOf to handle potential class cast issues if column is treated as Long/String
            try {
                selectedResidentId = Integer.valueOf(model.getValueAt(selectedRow, 0).toString()); 
                
                // Set the state Full Name
                String firstName = model.getValueAt(selectedRow, 1).toString();
                String lastName = model.getValueAt(selectedRow, 2).toString();
                selectedResidentFullName = firstName + " " + lastName;

                // Update status label
                jLabel2.setText("Selected: " + selectedResidentFullName + " (ID: " + selectedResidentId + ")");
            } catch (Exception ex) {
                // Should not happen if data is correctly loaded as Integer
                System.err.println("Error reading selected table row data: " + ex.getMessage());
                selectedResidentId = -1;
                selectedResidentFullName = "";
                jLabel2.setText("Error selecting row. Please retry search.");
            }
        }
    }

    /**
     * Performs a database search on the residents table and populates the JTable. (Read Operation)
     * Includes case-insensitive search and debugging output.
     */
    private void loadResidentsTable(String searchTerm) {
        DefaultTableModel model = (DefaultTableModel) showResidents.getModel();
        model.setRowCount(0); 
        selectedResidentId = -1; 
        selectedResidentFullName = "";
        jLabel2.setText("");

        if (searchTerm.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a search term (Name or ID).", "Search Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Use LOWER() in SQL for guaranteed case-insensitive matching
        // resident_id is compared directly to the number or -1 if not a number.
        String sql = "SELECT resident_id, first_name, last_name, purok, address FROM residents " +
                     "WHERE resident_id = ? OR LOWER(first_name) LIKE ? OR LOWER(last_name) LIKE ?";
        
        // Convert search term to lowercase and add wildcards
        String wildcardTerm = "%" + searchTerm.toLowerCase() + "%"; 

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // --- CRITICAL DEBUGGING LOG (to address previous issues) ---
            System.out.println("--- DB Search Debugging ---");
            if (conn == null) {
                System.err.println("Database connection failed. Check DBConnection.java and MySQL server status.");
                JOptionPane.showMessageDialog(this, "Connection is NULL. Check database setup.", "Connection Failure", JOptionPane.ERROR_MESSAGE);
                return; 
            }
            
            // Set the ID parameter
            int residentId = -1;
            try {
                residentId = Integer.parseInt(searchTerm);
                pstmt.setInt(1, residentId);
                System.out.println("Param 1 (ID): " + residentId);
            } catch (NumberFormatException e) {
                // If it's not a number, set a value that won't match any ID
                pstmt.setInt(1, -1); 
                System.out.println("Param 1 (ID): -1 (Search term is not a number)");
            }
            
            // Set the name parameters (using the lowercase wildcard)
            pstmt.setString(2, wildcardTerm);
            pstmt.setString(3, wildcardTerm);
            System.out.println("Param 2 & 3 (Name LIKE): " + wildcardTerm);
            System.out.println("---------------------------");

            try (ResultSet rs = pstmt.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    model.addRow(new Object[]{
                        rs.getInt("resident_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("purok"),
                        rs.getString("address")
                    });
                    count++;
                }
                jLabel2.setText("Search Results: " + count + " resident(s) found. Select a row.");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database Error during resident search: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
    
    // ------------------------------------------------------------------------------------------------
    // --- PDF Generation Logic (iTextPDF) ---
    // ------------------------------------------------------------------------------------------------

    /**
     * Creates and saves a PDF certificate using iText.
     */
    private void generateCertificatePDF(int residentId, String residentName, String certType, String purpose, String issuedBy, String fee) {
        // Define PDF filename and path (saves to the user's desktop)
        String fileName = certType.replace(" ", "_").replaceAll("[^a-zA-Z0-9_]", "") + "_" + residentId + "_" + new Date().getTime() + ".pdf";
        String path = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + fileName;
        
        // Define standard fonts
        Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Font.UNDERLINE);
        Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 12);

        Document document = new Document(PageSize.LETTER);
        
        try {
            PdfWriter.getInstance(document, new FileOutputStream(path));
            document.open();
            
            // 1. Title
            Paragraph title = new Paragraph("BARANGAY CERTIFICATE OF " + certType.toUpperCase(), fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);
            
            // 2. Body Content
            document.add(new Paragraph("TO WHOM IT MAY CONCERN:", fontHeader));
            document.add(new Paragraph(Chunk.NEWLINE)); // Blank line
            
            String issueDateText = DATE_FORMAT.format(new Date());

            String bodyText = "This is to certify that " + residentName.toUpperCase() + 
                              ", with Resident ID No. " + residentId + 
                              ", is a resident of this Barangay. " +
                              "This certificate is issued this " + issueDateText + 
                              " for the purpose of " + purpose.toUpperCase() + ".";
            
            Paragraph body = new Paragraph(bodyText, fontNormal);
            body.setAlignment(Element.ALIGN_JUSTIFIED);
            body.setFirstLineIndent(50);
            body.setSpacingAfter(40);
            document.add(body);
            
            // 3. Footer/Issuance Details
            document.add(new Paragraph("Purpose: " + purpose, fontNormal));
            document.add(new Paragraph("Fee Paid: P " + fee, fontNormal));

            Paragraph issuedByDetail = new Paragraph("Issued By: " + issuedBy, fontNormal);
            issuedByDetail.setSpacingAfter(60);
            document.add(issuedByDetail);
            
            // 4. Signatory Placeholder
            Paragraph signatory = new Paragraph("________________________", fontHeader);
            signatory.setAlignment(Element.ALIGN_RIGHT);
            document.add(signatory);
            
            Paragraph officialName = new Paragraph("Barangay Captain", fontNormal);
            officialName.setAlignment(Element.ALIGN_RIGHT);
            document.add(officialName);

            document.close();
            JOptionPane.showMessageDialog(this, 
                "PDF successfully created!\nFile saved to: " + path, 
                "PDF Success", 
                JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error generating PDF. Check iText library dependency.\nError: " + ex.getMessage(), "PDF Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }


    // ------------------------------------------------------------------------------------------------
    // --- Action Listeners ---
    // ------------------------------------------------------------------------------------------------

    private void btnLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLoadActionPerformed
        // (R) Search resident and load table
        String searchTerm = txtSearch.getText().trim();
        loadResidentsTable(searchTerm);
    }//GEN-LAST:event_btnLoadActionPerformed
    
    private void btnSaveTransactionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveTransactionActionPerformed
        // (C) Save new certificate transaction to the database
        
        if (selectedResidentId == -1) {
            JOptionPane.showMessageDialog(this, "Please search for and select a resident from the table first.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 1. Collect required data for the 'certificates' table structure
        String certType = comboType.getSelectedItem().toString();
        java.sql.Date issueDate = new java.sql.Date(new Date().getTime()); 

        // SQL query matches the certificates table structure (id, fk, cert_type, issue_date)
        String sql = "INSERT INTO certificates (resident_id_fk, cert_type, issue_date) VALUES (?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (conn == null) return; 

            // Set parameters
            pstmt.setInt(1, selectedResidentId);
            pstmt.setString(2, certType);
            pstmt.setDate(3, issueDate);
            
            int result = pstmt.executeUpdate();

            if (result > 0) {
                JOptionPane.showMessageDialog(this, "Certificate transaction saved successfully for " + selectedResidentFullName, "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                 JOptionPane.showMessageDialog(this, "Transaction failed. No rows affected.", "Failure", JOptionPane.ERROR_MESSAGE);
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database Error during Save Transaction: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }//GEN-LAST:event_btnSaveTransactionActionPerformed

    private void btnGeneratePDFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGeneratePDFActionPerformed
        // 1. Validate Resident Selection
        if (selectedResidentId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a resident from the table to generate a PDF.", "Action Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 2. Get Data for PDF
        String certType = comboType.getSelectedItem().toString();
        String purpose = comboPurpose.getSelectedItem().toString();
        String issuedBy = txtIssuedBy.getText().trim();
        String fee = txtFeePaid1.getText().trim(); // Note: txtFeePaid1 is the component name

        if (issuedBy.isEmpty() || fee.isEmpty()) {
             JOptionPane.showMessageDialog(this, "Please enter the Issued By name and Fee Paid amount.", "Validation Required", JOptionPane.WARNING_MESSAGE);
             return;
        }

        // 3. Generate PDF
        generateCertificatePDF(selectedResidentId, selectedResidentFullName, certType, purpose, issuedBy, fee);
        
    }//GEN-LAST:event_btnGeneratePDFActionPerformed



    // ------------------------------------------------------------------------------------------------
    // --- Auto-generated Code (NetBeans GUI Code) ---
    // ------------------------------------------------------------------------------------------------

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        txtSearch = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        btnLoad = new javax.swing.JButton();
        btnGeneratePDF = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        showResidents = new javax.swing.JTable();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        txtIssuedBy = new javax.swing.JTextField();
        txtFeePaid1 = new javax.swing.JTextField();
        comboPurpose = new javax.swing.JComboBox<>();
        comboType = new javax.swing.JComboBox<>();
        btnSaveTransaction = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        txtSearch.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtSearch.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        getContentPane().add(txtSearch, new org.netbeans.lib.awtextra.AbsoluteConstraints(450, 90, 287, 41));

        jLabel1.setFont(new java.awt.Font("Arial Black", 0, 14)); // NOI18N
        jLabel1.setText("FEE PAID:");
        getContentPane().add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 540, 80, 41));

        btnLoad.setBackground(new java.awt.Color(204, 255, 153));
        btnLoad.setFont(new java.awt.Font("Arial Black", 0, 12)); // NOI18N
        btnLoad.setText("LOAD");
        btnLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLoadActionPerformed(evt);
            }
        });
        getContentPane().add(btnLoad, new org.netbeans.lib.awtextra.AbsoluteConstraints(780, 90, 119, 40));

        btnGeneratePDF.setBackground(new java.awt.Color(204, 255, 153));
        btnGeneratePDF.setFont(new java.awt.Font("Arial Black", 0, 14)); // NOI18N
        btnGeneratePDF.setText("Generate and Print PDF");
        btnGeneratePDF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGeneratePDFActionPerformed(evt);
            }
        });
        getContentPane().add(btnGeneratePDF, new org.netbeans.lib.awtextra.AbsoluteConstraints(730, 520, 240, 50));

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
        jScrollPane1.setViewportView(showResidents);

        getContentPane().add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 270, 790, 140));

        jLabel3.setFont(new java.awt.Font("Arial Black", 0, 14)); // NOI18N
        jLabel3.setText("SEARCH NAME/ID:");
        getContentPane().add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 90, 161, 41));

        jLabel4.setFont(new java.awt.Font("Arial Black", 0, 14)); // NOI18N
        jLabel4.setText("TYPE:");
        getContentPane().add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 420, 70, 41));

        jLabel5.setFont(new java.awt.Font("Arial Black", 0, 14)); // NOI18N
        jLabel5.setText("PURPOSE:");
        getContentPane().add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 460, 90, 41));

        jLabel6.setFont(new java.awt.Font("Arial Black", 0, 14)); // NOI18N
        jLabel6.setText("ISSUED BY:");
        getContentPane().add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 500, 100, 41));

        txtIssuedBy.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtIssuedBy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtIssuedByActionPerformed(evt);
            }
        });
        getContentPane().add(txtIssuedBy, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 510, 150, 30));

        txtFeePaid1.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        getContentPane().add(txtFeePaid1, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 550, 150, 30));

        comboPurpose.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        comboPurpose.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        getContentPane().add(comboPurpose, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 470, 170, -1));

        comboType.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        comboType.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        getContentPane().add(comboType, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 430, 170, -1));

        btnSaveTransaction.setBackground(new java.awt.Color(204, 255, 153));
        btnSaveTransaction.setFont(new java.awt.Font("Arial Black", 0, 14)); // NOI18N
        btnSaveTransaction.setText("Save Transaction");
        btnSaveTransaction.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveTransactionActionPerformed(evt);
            }
        });
        getContentPane().add(btnSaveTransaction, new org.netbeans.lib.awtextra.AbsoluteConstraints(730, 470, 240, 50));

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

        jLabel7.setFont(new java.awt.Font("Arial Black", 0, 36)); // NOI18N
        jLabel7.setText("CERTIFICATES ISSUER");
        getContentPane().add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(240, 10, -1, -1));

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pictures/bgg.png"))); // NOI18N
        getContentPane().add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1000, 590));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // BACK button
        home h = new home();
        h.setVisible(true);
        this.dispose(); // Use dispose() for proper cleanup and resource freeing
    }//GEN-LAST:event_jButton1ActionPerformed

    private void txtIssuedByActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtIssuedByActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtIssuedByActionPerformed

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
            java.util.logging.Logger.getLogger(Certificate.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Certificate.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Certificate.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Certificate.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Certificate().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnGeneratePDF;
    private javax.swing.JButton btnLoad;
    private javax.swing.JButton btnSaveTransaction;
    private javax.swing.JComboBox<String> comboPurpose;
    private javax.swing.JComboBox<String> comboType;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable showResidents;
    private javax.swing.JTextField txtFeePaid1;
    private javax.swing.JTextField txtIssuedBy;
    private javax.swing.JTextField txtSearch;
    // End of variables declaration//GEN-END:variables
}