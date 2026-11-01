package lk.ac.ruhuna.fot.ams.presentation.ui.auth;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.UiTheme;

/**
 * The faculty banner shown beside the login form. The picture is scaled to fill the panel (the overflow is
 * cropped on the right, so the headline on the left stays visible). If the image is missing the panel falls back to
 * a plain branded block, so a missing resource can never stop the application from starting.
 */
public final class BannerPanel extends JPanel {
    static final String RESOURCE = "/images/faculty-banner.jpg";
    private static BufferedImage cached;
    private static boolean loaded;

    private final BufferedImage image = load();

    public BannerPanel() {
        setPreferredSize(new Dimension(100, 660));
        setMinimumSize(new Dimension(320, 300));
        setBackground(UiTheme.PRIMARY);
        if (image == null) {
            setLayout(new java.awt.BorderLayout());
            JLabel fallback = new JLabel("Faculty of Technology \u2013 University of Ruhuna", SwingConstants.CENTER);
            fallback.setFont(UiTheme.pageTitle());
            fallback.setForeground(UiTheme.SURFACE);
            add(fallback, java.awt.BorderLayout.CENTER);
        }
        getAccessibleContext().setAccessibleName("Faculty of Technology, University of Ruhuna banner");
    }

    private static synchronized BufferedImage load() {
        if (!loaded) {
            loaded = true;
            try (InputStream input = BannerPanel.class.getResourceAsStream(RESOURCE)) {
                cached = input == null ? null : ImageIO.read(input);
            } catch (IOException unreadable) {
                cached = null;
            }
        }
        return cached;
    }

    /** Scale factor that makes an image of the given size cover the whole area (no empty margins). */
    static double coverScale(int areaWidth, int areaHeight, int imageWidth, int imageHeight) {
        return Math.max((double) areaWidth / imageWidth, (double) areaHeight / imageHeight);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (image == null || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            double scale = coverScale(getWidth(), getHeight(), image.getWidth(), image.getHeight());
            int drawWidth = (int) Math.ceil(image.getWidth() * scale);
            int drawHeight = (int) Math.ceil(image.getHeight() * scale);
            g2.drawImage(image, 0, (getHeight() - drawHeight) / 2, drawWidth, drawHeight, null);
        } finally {
            g2.dispose();
        }
    }
}
