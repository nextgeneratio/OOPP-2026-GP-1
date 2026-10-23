package lk.ac.ruhuna.fot.ams.presentation.ui.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class BannerPanelTest {
    @Test
    void theFacultyBannerIsPackagedWithTheApplicationAndCanBeRead() throws Exception {
        try (InputStream input = BannerPanel.class.getResourceAsStream(BannerPanel.RESOURCE)) {
            assertThat(input).as("banner resource on the classpath").isNotNull();
            var image = ImageIO.read(input);
            assertThat(image).isNotNull();
            assertThat(image.getWidth()).isGreaterThan(image.getHeight());
        }
    }

    @Test
    void coverScaleFillsTheWholeAreaWithoutEmptyMargins() {
        // the panel rounds the drawn size up, so compare the rounded-up size
        double tall = BannerPanel.coverScale(600, 660, 1774, 887);
        assertThat(Math.ceil(1774 * tall)).isGreaterThanOrEqualTo(600);
        assertThat(Math.ceil(887 * tall)).isGreaterThanOrEqualTo(660);

        double wide = BannerPanel.coverScale(2000, 500, 1774, 887);
        assertThat(Math.ceil(1774 * wide)).isGreaterThanOrEqualTo(2000);
        assertThat(Math.ceil(887 * wide)).isGreaterThanOrEqualTo(500);
    }
}
