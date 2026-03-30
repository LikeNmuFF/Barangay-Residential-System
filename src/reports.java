/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.JOptionPane;
import java.io.FileOutputStream;
import java.io.File;

// --- iText Imports (Ensure these libraries are in your classpath) ---
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;


/**
 *
 * @author Hp
 */
public class reports extends javax.swing.JFrame {

    private static final String DEFAULT_PUROK_SELECTION = "ALL PUROK/ZONE";

    /**
     * Creates new form reports
     */
    public reports() {
        initComponents();
        setupForm();
    }

    /**
     * Sets up the combo boxes with options upon form initialization.
     */
    private void setupForm() {
        // 1. Setup Report Type ComboBox
        comboPopulationByPurok.removeAllItems();
        comboPopulationByPurok.addItem("Population Summary Report");
        comboPopulationByPurok.addItem("Population by Age Range");
        // Add more report types if needed

        // 2. Populate Purok ComboBox from database
        populatePurokComboBox();
        
        // Default text for labels
        jLabel8.setText("BARANGAY POPULATION REPORTS");
    }
    
    /**
     * Fetches distinct Purok/Zone names from the database and populates comboPurok.
     */
    private void populatePurokComboBox() {
        comboPurok.removeAllItems();
        comboPurok.addItem(DEFAULT_PUROK_SELECTION); // Option to select all
        
        // SQL assumes a 'purok' column in your 'residents' table
        String sql = "SELECT DISTINCT purok FROM residents ORDER BY purok ASC";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (conn == null) return; 

            while (rs.next()) {
                String purok = rs.getString("purok");
                if (purok != null && !purok.trim().isEmpty()) {
                    comboPurok.addItem(purok);
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database Error during Purok lookup: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
    
    // -------------------------------------------------------------------------
    // --- Data Model Classes (for fetching data) ---
    // -------------------------------------------------------------------------
    
    private static class ResidentData {
        int id;
        String firstName;
        String lastName;
        String purok;
        int age;
        
        public ResidentData(int id, String firstName, String lastName, String purok, int age) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.purok = purok;
            this.age = age;
        }
    }
    
    /**
     * Calculates age using the simplified formula: Current Year - Birth Year.
     * @param dateOfBirth The java.sql.Date from the database column 'date_of_birth'.
     * @return The age in years, or -1 if the dateOfBirth is null.
     */
    private int calculateAge(java.sql.Date dateOfBirth) {
        if (dateOfBirth == null) return -1; 
        
        // Get the year from the database date_of_birth
        int birthYear = dateOfBirth.toLocalDate().getYear(); 
        // Get the current year
        int currentYear = LocalDate.now().getYear();

        // Calculate age based on year difference (as requested)
        return currentYear - birthYear; 
    }

    /**
     * Fetches resident data based on Purok and optional Age filters.
     * **CRITICAL UPDATE: Uses 'date_of_birth' column.**
     */
    private List<ResidentData> getFilteredResidentData(String selectedPurok, int fromAge, int toAge) {
        List<ResidentData> results = new ArrayList<>();
        
        // 1. Build the dynamic SQL query
        // We now select the correct column: date_of_birth
        StringBuilder sqlBuilder = new StringBuilder(
            "SELECT resident_id, first_name, last_name, purok, date_of_birth FROM residents WHERE 1=1"
        );
        
        if (!selectedPurok.equals(DEFAULT_PUROK_SELECTION)) {
            sqlBuilder.append(" AND purok = ?");
        }
        
        String sql = sqlBuilder.toString();
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (conn == null) return results;

            // 2. Set Parameters
            int paramIndex = 1;
            if (!selectedPurok.equals(DEFAULT_PUROK_SELECTION)) {
                pstmt.setString(paramIndex++, selectedPurok);
            }

            // 3. Execute Query and Filter Age in Java
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("resident_id");
                    String firstName = rs.getString("first_name");
                    String lastName = rs.getString("last_name");
                    String purok = rs.getString("purok");
                    
                    // Fetch using the corrected column name: date_of_birth
                    java.sql.Date dateOfBirth = rs.getDate("date_of_birth"); 
                    
                    int age = calculateAge(dateOfBirth);
                    
                    // Apply Age Filter after calculation
                    if (age >= fromAge && age <= toAge) {
                        results.add(new ResidentData(id, firstName, lastName, purok, age));
                    }
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error fetching report data: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
        
        return results;
    }
    
    // -------------------------------------------------------------------------
    // --- PDF Generation Logic (iTextPDF) ---
    // -------------------------------------------------------------------------

    /**
     * Creates and saves a PDF report using iText.
     */
    private void generateReportPDF(String reportTitle, String filterDetails, List<ResidentData> data) {
        // Define PDF filename and path (saves to the user's desktop)
        String safeTitle = reportTitle.replaceAll("[^a-zA-Z0-9 ]", "").replace(" ", "_");
        String fileName = safeTitle + "_" + new Date().getTime() + ".pdf";
        String path = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + fileName;
        
        // Define standard fonts
        Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 10);

        Document document = new Document(PageSize.A4);
        
        try {
            PdfWriter.getInstance(document, new FileOutputStream(path));
            document.open();
            
            // 1. Title
            Paragraph title = new Paragraph(reportTitle.toUpperCase(), fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(15);
            document.add(title);
            
            // 2. Filter Details
            Paragraph filters = new Paragraph("Filters Applied: " + filterDetails, fontNormal);
            filters.setSpacingAfter(10);
            document.add(filters);
            
            // 3. Data Table
            PdfPTable table = new PdfPTable(5); // 5 columns: ID, Name, Purok, Age, Status (optional)
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            
            // Table Headers
            String[] headers = {"ID", "First Name", "Last Name", "Purok/Zone", "Approx. Age"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, fontHeader));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5);
                table.addCell(cell);
            }
            
            // Table Rows
            if (data.isEmpty()) {
                PdfPCell noDataCell = new PdfPCell(new Phrase("No residents found matching the criteria.", fontNormal));
                noDataCell.setColspan(5);
                noDataCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                noDataCell.setPadding(10);
                table.addCell(noDataCell);
            } else {
                for (ResidentData resident : data) {
                    table.addCell(new Phrase(String.valueOf(resident.id), fontNormal));
                    table.addCell(new Phrase(resident.firstName, fontNormal));
                    table.addCell(new Phrase(resident.lastName, fontNormal));
                    table.addCell(new Phrase(resident.purok, fontNormal));
                    table.addCell(new Phrase(String.valueOf(resident.age), fontNormal));
                }
            }
            
            document.add(table);
            
            // 4. Summary Footer
            Paragraph summary = new Paragraph("Total Residents in Report: " + data.size(), fontHeader);
            summary.setSpacingBefore(20);
            summary.setAlignment(Element.ALIGN_RIGHT);
            document.add(summary);
            
            document.close();
            JOptionPane.showMessageDialog(this, 
                "Report PDF successfully created!\nFile saved to: " + path, 
                "PDF Success", 
                JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error generating PDF. Check iText library dependency.\nError: " + ex.getMessage(), "PDF Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }


    // -------------------------------------------------------------------------
    // --- Action Listeners ---
    // -------------------------------------------------------------------------


    
    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // BACK button
        home h = new home();
        h.setVisible(true);
        this.dispose(); // Use dispose() for proper cleanup and resource freeing
    }//GEN-LAST:event_jButton1ActionPerformed

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        comboPopulationByPurok = new javax.swing.JComboBox<>();
        jLabel3 = new javax.swing.JLabel();
        comboPurok = new javax.swing.JComboBox<>();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        txtFromAge = new javax.swing.JTextField();
        txtToAge = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        btnGenerateReport = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        getContentPane().add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 60, 290, -1));

        jLabel2.setFont(new java.awt.Font("Arial Black", 0, 18)); // NOI18N
        jLabel2.setText("Report Type:");
        getContentPane().add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(280, 200, 160, -1));

        comboPopulationByPurok.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        comboPopulationByPurok.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        getContentPane().add(comboPopulationByPurok, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 200, 230, 30));

        jLabel3.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        jLabel3.setText("To:");
        getContentPane().add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(560, 300, 40, -1));

        comboPurok.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        comboPurok.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        getContentPane().add(comboPurok, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 250, 230, 30));

        jLabel4.setFont(new java.awt.Font("Arial Black", 0, 18)); // NOI18N
        jLabel4.setText("Purok:");
        getContentPane().add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(340, 250, 100, 40));

        jLabel5.setFont(new java.awt.Font("Arial Black", 0, 18)); // NOI18N
        jLabel5.setText("AGE");
        getContentPane().add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(340, 300, 50, -1));

        txtFromAge.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        getContentPane().add(txtFromAge, new org.netbeans.lib.awtextra.AbsoluteConstraints(470, 300, 60, -1));

        txtToAge.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        getContentPane().add(txtToAge, new org.netbeans.lib.awtextra.AbsoluteConstraints(600, 300, 70, -1));

        jLabel6.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        jLabel6.setText("From:");
        getContentPane().add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 300, 60, -1));

        jButton1.setBackground(new java.awt.Color(204, 255, 204));
        jButton1.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pictures/icons8-back-96.png"))); // NOI18N
        jButton1.setText("BACK");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        getContentPane().add(jButton1, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 80, -1, 50));

        btnGenerateReport.setBackground(new java.awt.Color(204, 255, 153));
        btnGenerateReport.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        btnGenerateReport.setText("Generate Report");
        btnGenerateReport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGenerateReportActionPerformed(evt);
            }
        });
        getContentPane().add(btnGenerateReport, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 370, -1, 50));

        jLabel8.setFont(new java.awt.Font("Arial Black", 0, 48)); // NOI18N
        jLabel8.setText("BARANGAY POPULATION REPORTS");
        getContentPane().add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 0, 980, -1));

        jLabel7.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pictures/bgg.png"))); // NOI18N
        getContentPane().add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 990, 480));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnGenerateReportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGenerateReportActionPerformed

        // 1. Collect Filters
        String reportType = comboPopulationByPurok.getSelectedItem().toString();
        String selectedPurok = comboPurok.getSelectedItem().toString();
        
        int fromAge = 0; // Default min age
        int toAge = 200; // Default max age
        
        try {
            String fromText = txtFromAge.getText().trim();
            String toText = txtToAge.getText().trim();
            
            if (!fromText.isEmpty()) {
                fromAge = Integer.parseInt(fromText);
            } 
            
            if (!toText.isEmpty()) {
                toAge = Integer.parseInt(toText);
            } 
            
            if (fromAge > toAge) {
                 JOptionPane.showMessageDialog(this, "The 'From Age' cannot be greater than the 'To Age'.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                 return;
            }
            
        } catch (NumberFormatException e) {
             JOptionPane.showMessageDialog(this, "Please enter valid numerical values for the Age fields.", "Validation Error", JOptionPane.WARNING_MESSAGE);
             return;
        }

        // 2. Fetch Data
        List<ResidentData> residentList = getFilteredResidentData(selectedPurok, fromAge, toAge);
        
        // 3. Define Report Metadata
        String reportTitle = "BARANGAY " + reportType.toUpperCase();
        String filterDetails = String.format("Purok: %s, Age Range: %d to %d (Approximate)", selectedPurok, fromAge, toAge);

        // 4. Generate PDF
        generateReportPDF(reportTitle, filterDetails, residentList);
    
    }//GEN-LAST:event_btnGenerateReportActionPerformed

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
            java.util.logging.Logger.getLogger(reports.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(reports.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(reports.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(reports.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new reports().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnGenerateReport;
    private javax.swing.JComboBox<String> comboPopulationByPurok;
    private javax.swing.JComboBox<String> comboPurok;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JTextField txtFromAge;
    private javax.swing.JTextField txtToAge;
    // End of variables declaration//GEN-END:variables
}