package lk.ac.ruhuna.fot.ams.business.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.business.calculator.GradeCalculator.GradeResult;
import lk.ac.ruhuna.fot.ams.data.dao.GradeDao;
import lk.ac.ruhuna.fot.ams.data.dao.GradeDao.GradeRow;
import lk.ac.ruhuna.fot.ams.error.exception.NotFoundException;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;

public final class GradeService {
    private final GradeDao gradeDao;

    public GradeService(GradeDao gradeDao) {
        this.gradeDao = Objects.requireNonNull(gradeDao);
    }

    public GradeResult grade(String schemeVersion, BigDecimal finalMark) {
        if (schemeVersion == null || schemeVersion.isBlank()
                || finalMark == null || finalMark.signum() < 0
                || finalMark.compareTo(new BigDecimal("100")) > 0) {
            throw new ValidationException("Grade scheme version and a mark from 0 to 100 are required.");
        }
        List<GradeRow> scheme = gradeDao.findScheme(schemeVersion);
        if (scheme.isEmpty()) {
            throw new NotFoundException("The requested grade scheme version is unavailable.");
        }
        GradeRow match = scheme.stream()
                .filter(row -> finalMark.compareTo(row.minimumMark()) >= 0
                        && finalMark.compareTo(row.maximumMark()) <= 0)
                .findFirst()
                .orElseThrow(() -> new ValidationException("Final mark is not covered by the grade scheme."));
        return new GradeResult(schemeVersion, match.letter(), match.gradePoint());
    }
}
