package lk.ac.ruhuna.fot.ams.business.service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import lk.ac.ruhuna.fot.ams.error.exception.BusinessRuleException;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;

public final class TimetableService {

    public record TimeSlot(
            int slotId,
            int offeringId,
            int lecturerId,
            String location,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    ) {
        public TimeSlot {
            if (dayOfWeek == null || startTime == null || endTime == null) {
                throw new ValidationException("Day of week and time bounds are required.");
            }
            if (!startTime.isBefore(endTime)) {
                throw new ValidationException("Start time must be strictly before end time.");
            }
        }

        public boolean overlapsWith(TimeSlot other) {
            if (this.dayOfWeek != other.dayOfWeek) {
                return false;
            }
            return this.startTime.isBefore(other.endTime) && other.startTime.isBefore(this.endTime);
        }
    }

    public interface TimetableLookup {
        List<TimeSlot> findSlotsByLecturer(int lecturerId);
        List<TimeSlot> findSlotsByLocation(String location);
    }

    private final TimetableLookup timetableLookup;

    public TimetableService(TimetableLookup timetableLookup) {
        this.timetableLookup = timetableLookup;
    }

    /**
     * Checks if a new time slot produces conflicts with:
     * 1. The lecturer's existing scheduled slots.
     * 2. The classroom / lab location's existing scheduled slots.
     */
    public void validateNoConflicts(TimeSlot newSlot) {
        if (timetableLookup == null) {
            return;
        }

        List<TimeSlot> lecturerSlots = timetableLookup.findSlotsByLecturer(newSlot.lecturerId());
        for (TimeSlot existing : lecturerSlots) {
            if (existing.slotId() != newSlot.slotId() && existing.overlapsWith(newSlot)) {
                throw new BusinessRuleException(
                        "Lecturer timetable conflict: Already assigned to another session on "
                                + newSlot.dayOfWeek() + " between " + existing.startTime() + " and " + existing.endTime()
                );
            }
        }

        List<TimeSlot> locationSlots = timetableLookup.findSlotsByLocation(newSlot.location());
        for (TimeSlot existing : locationSlots) {
            if (existing.slotId() != newSlot.slotId() && existing.overlapsWith(newSlot)) {
                throw new BusinessRuleException(
                        "Room / location conflict: " + newSlot.location() + " is already occupied on "
                                + newSlot.dayOfWeek() + " between " + existing.startTime() + " and " + existing.endTime()
                );
            }
        }
    }
}
