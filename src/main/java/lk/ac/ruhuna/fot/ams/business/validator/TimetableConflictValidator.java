package lk.ac.ruhuna.fot.ams.business.validator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.domain.enums.ComponentType;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;

public final class TimetableConflictValidator {
    public record TimetableSlot(
            long entryId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            String room,
            long lecturerId,
            long offeringId,
            long departmentId,
            long semesterId,
            ComponentType componentType) {
        public TimetableSlot {
            Objects.requireNonNull(date, "Timetable date is required.");
            Objects.requireNonNull(startTime, "Start time is required.");
            Objects.requireNonNull(endTime, "End time is required.");
            Objects.requireNonNull(componentType, "Course component is required.");
            if (room == null || room.isBlank() || lecturerId <= 0 || offeringId <= 0
                    || departmentId <= 0 || semesterId <= 0 || !startTime.isBefore(endTime)) {
                throw new ValidationException("Enter a valid timetable entry.");
            }
        }

        public boolean overlaps(TimetableSlot other) {
            return date.equals(other.date)
                    && startTime.isBefore(other.endTime)
                    && other.startTime.isBefore(endTime);
        }

        public boolean conflictsWith(TimetableSlot other) {
            return overlaps(other)
                    && (room.equalsIgnoreCase(other.room)
                    || lecturerId == other.lecturerId
                    || offeringId == other.offeringId);
        }
    }

    public void validateNoConflict(TimetableSlot candidate, Collection<TimetableSlot> existingEntries) {
        Objects.requireNonNull(candidate, "Candidate timetable entry is required.");
        if (existingEntries == null) {
            throw new ValidationException("Existing timetable entries are required for conflict validation.");
        }
        boolean conflict = existingEntries.stream()
                .filter(Objects::nonNull)
                .filter(existing -> existing.entryId() != candidate.entryId())
                .anyMatch(candidate::conflictsWith);
        if (conflict) {
            throw new ValidationException("The timetable entry conflicts with an existing room, lecturer, or course slot.");
        }
    }
}
