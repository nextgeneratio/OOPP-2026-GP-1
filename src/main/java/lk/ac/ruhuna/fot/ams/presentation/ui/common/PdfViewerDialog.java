package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

/** Lightweight in-application PDF reader for published study material. */
public final class PdfViewerDialog extends JDialog {
    private PdfViewerDialog(java.awt.Window owner, String title, Path file) throws IOException {
        super(owner, title, Dialog.ModalityType.MODELESS);
        setLayout(new BorderLayout(8, 8));
        JLabel pages = new JLabel();
        try (PDDocument document = Loader.loadPDF(file.toFile())) {
            PDFRenderer renderer = new PDFRenderer(document);
            javax.swing.JPanel pagePanel = new javax.swing.JPanel();
            pagePanel.setLayout(new javax.swing.BoxLayout(pagePanel, javax.swing.BoxLayout.Y_AXIS));
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, 130);
                JLabel imageLabel = new JLabel(new ImageIcon(image));
                imageLabel.setAlignmentX(CENTER_ALIGNMENT);
                pagePanel.add(imageLabel);
                pagePanel.add(javax.swing.Box.createVerticalStrut(12));
            }
            pages.setText(document.getNumberOfPages() + " page(s)");
            add(new JScrollPane(pagePanel), BorderLayout.CENTER);
        }
        JButton close = new JButton("Close"); close.addActionListener(event -> dispose());
        javax.swing.JPanel footer = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        footer.add(pages); footer.add(close); add(footer, BorderLayout.SOUTH);
        setSize(820, 760); setLocationRelativeTo(owner);
    }

    public static void show(java.awt.Component owner, String title, Path file) throws IOException {
        java.awt.Window window = javax.swing.SwingUtilities.getWindowAncestor(owner);
        new PdfViewerDialog(window, title, file).setVisible(true);
    }
}
