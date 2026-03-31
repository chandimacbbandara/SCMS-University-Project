package Project._6.demo.service;

import Project._6.demo.entity.Admin;
import Project._6.demo.entity.Concern;
import Project._6.demo.entity.ConcernMeetingProposal;
import Project._6.demo.entity.ConcernMeetingSlot;
import Project._6.demo.repository.AdminRepository;
import Project._6.demo.repository.ConcernMeetingProposalRepository;
import Project._6.demo.repository.ConcernMeetingSlotRepository;
import Project._6.demo.repository.ConcernRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ConcernMeetingService {

    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_IN_PROGRESS = "In Progress";
    public static final String STATUS_MEETING_SCHEDULED = "Meeting Scheduled";
    public static final String STATUS_COMPLETE = "Complete";

    private static final String PROPOSAL_PENDING = "PENDING_STUDENT_SELECTION";
    private static final String PROPOSAL_BOOKED = "BOOKED";
    private static final String PROPOSAL_DECLINED = "DECLINED";
    private static final String PROPOSAL_SUPERSEDED = "SUPERSEDED";

    private static final String SLOT_AVAILABLE = "AVAILABLE";
    private static final String SLOT_BOOKED = "BOOKED";
    private static final String SLOT_SKIPPED = "SKIPPED";
    private static final String SLOT_DECLINED = "DECLINED";

    private final ConcernRepository concernRepository;
    private final AdminRepository adminRepository;
    private final ConcernMeetingProposalRepository meetingProposalRepository;
    private final ConcernMeetingSlotRepository meetingSlotRepository;
    private final NotificationService notificationService;

    public ConcernMeetingService(ConcernRepository concernRepository,
                                 AdminRepository adminRepository,
                                 ConcernMeetingProposalRepository meetingProposalRepository,
                                 ConcernMeetingSlotRepository meetingSlotRepository,
                                 NotificationService notificationService) {
        this.concernRepository = concernRepository;
        this.adminRepository = adminRepository;
        this.meetingProposalRepository = meetingProposalRepository;
        this.meetingSlotRepository = meetingSlotRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public ConcernMeetingProposal proposeMeetingSlots(Integer concernId,
                                                      Integer adminUserId,
                                                      List<String> slotStarts,
                                                      List<String> slotEnds,
                                                      String adminNote) {
        if (adminUserId == null) {
            throw new RuntimeException("Admin session expired. Please log in again.");
        }

        Concern concern = concernRepository.findById(concernId)
                .orElseThrow(() -> new RuntimeException("Concern not found."));

        if (STATUS_COMPLETE.equalsIgnoreCase(safe(concern.getStatus()))) {
            throw new RuntimeException("Cannot schedule a meeting for a completed concern.");
        }

        Admin admin = adminRepository.findById(adminUserId)
                .orElseThrow(() -> new RuntimeException("Admin account not found."));

        List<SlotWindow> slotWindows = parseSlotWindows(slotStarts, slotEnds);

        List<ConcernMeetingProposal> existingProposals = meetingProposalRepository
                .findByConcern_ConcernIdOrderByCreatedTimeDesc(concernId);
        LocalDateTime now = LocalDateTime.now();
        for (ConcernMeetingProposal proposal : existingProposals) {
            if (PROPOSAL_PENDING.equals(proposal.getProposalStatus())) {
                proposal.setProposalStatus(PROPOSAL_SUPERSEDED);
                proposal.setRespondedTime(now);
            }
        }
        if (!existingProposals.isEmpty()) {
            meetingProposalRepository.saveAll(existingProposals);
        }

        String previousStatus = safe(concern.getStatus());
        concern.setStatus(STATUS_IN_PROGRESS);
        concern.setMeetingStatus(PROPOSAL_PENDING);
        concern.setMeetingBookedStartTime(null);
        concern.setMeetingBookedEndTime(null);
        concern.setMeetingBookedAt(null);
        concern.setAdmin(admin);
        concernRepository.save(concern);

        ConcernMeetingProposal proposal = new ConcernMeetingProposal();
        proposal.setConcern(concern);
        proposal.setAdmin(admin);
        proposal.setProposalStatus(PROPOSAL_PENDING);
        proposal.setAdminNote(blankToNull(adminNote));

        List<ConcernMeetingSlot> slots = new ArrayList<>();
        for (SlotWindow window : slotWindows) {
            ConcernMeetingSlot slot = new ConcernMeetingSlot();
            slot.setProposal(proposal);
            slot.setStartTime(window.start());
            slot.setEndTime(window.end());
            slot.setSlotStatus(SLOT_AVAILABLE);
            slots.add(slot);
        }
        proposal.setSlots(slots);

        ConcernMeetingProposal savedProposal = meetingProposalRepository.save(proposal);

        if (!STATUS_IN_PROGRESS.equalsIgnoreCase(previousStatus)) {
            notificationService.notifyConcernInProgress(concern);
        }
        notificationService.notifyMeetingSlotsProposed(concern, savedProposal, savedProposal.getSlots());

        return savedProposal;
    }

    @Transactional
    public ConcernMeetingSlot bookMeetingSlot(Integer concernId,
                                              Integer proposalId,
                                              Integer slotId,
                                              Integer studentUserId) {
        Concern concern = concernRepository.findByConcernIdAndStudent_UserId(concernId, studentUserId)
                .orElseThrow(() -> new RuntimeException("Concern not found or access denied."));

        ConcernMeetingProposal proposal = meetingProposalRepository
                .findByProposalIdAndConcern_ConcernId(proposalId, concernId)
                .orElseThrow(() -> new RuntimeException("Meeting proposal not found."));

        if (!PROPOSAL_PENDING.equals(proposal.getProposalStatus())) {
            throw new RuntimeException("This proposal is no longer open for selection.");
        }

        ConcernMeetingSlot selectedSlot = meetingSlotRepository
                .findBySlotIdAndProposal_ProposalId(slotId, proposalId)
                .orElseThrow(() -> new RuntimeException("Meeting slot not found."));

        if (!SLOT_AVAILABLE.equals(selectedSlot.getSlotStatus())) {
            throw new RuntimeException("Selected slot is no longer available.");
        }

        List<ConcernMeetingSlot> proposalSlots = meetingSlotRepository
                .findByProposal_ProposalIdOrderByStartTimeAsc(proposalId);

        for (ConcernMeetingSlot slot : proposalSlots) {
            if (Objects.equals(slot.getSlotId(), slotId)) {
                slot.setSlotStatus(SLOT_BOOKED);
            } else if (SLOT_AVAILABLE.equals(slot.getSlotStatus())) {
                slot.setSlotStatus(SLOT_SKIPPED);
            }
        }
        meetingSlotRepository.saveAll(proposalSlots);

        proposal.setProposalStatus(PROPOSAL_BOOKED);
        proposal.setRespondedTime(LocalDateTime.now());
        proposal.setStudentResponseNote(null);
        meetingProposalRepository.save(proposal);

        concern.setStatus(STATUS_MEETING_SCHEDULED);
        concern.setMeetingStatus(PROPOSAL_BOOKED);
        concern.setMeetingBookedStartTime(selectedSlot.getStartTime());
        concern.setMeetingBookedEndTime(selectedSlot.getEndTime());
        concern.setMeetingBookedAt(LocalDateTime.now());
        concernRepository.save(concern);

        notificationService.notifyMeetingSlotBooked(concern, proposal, selectedSlot);
        return selectedSlot;
    }

    @Transactional
    public ConcernMeetingProposal declineMeetingSlots(Integer concernId,
                                                      Integer proposalId,
                                                      Integer studentUserId,
                                                      String reason) {
        Concern concern = concernRepository.findByConcernIdAndStudent_UserId(concernId, studentUserId)
                .orElseThrow(() -> new RuntimeException("Concern not found or access denied."));

        ConcernMeetingProposal proposal = meetingProposalRepository
                .findByProposalIdAndConcern_ConcernId(proposalId, concernId)
                .orElseThrow(() -> new RuntimeException("Meeting proposal not found."));

        if (!PROPOSAL_PENDING.equals(proposal.getProposalStatus())) {
            throw new RuntimeException("This proposal is no longer open for decline.");
        }

        List<ConcernMeetingSlot> proposalSlots = meetingSlotRepository
                .findByProposal_ProposalIdOrderByStartTimeAsc(proposalId);
        for (ConcernMeetingSlot slot : proposalSlots) {
            if (SLOT_AVAILABLE.equals(slot.getSlotStatus())) {
                slot.setSlotStatus(SLOT_DECLINED);
            }
        }
        meetingSlotRepository.saveAll(proposalSlots);

        proposal.setProposalStatus(PROPOSAL_DECLINED);
        proposal.setStudentResponseNote(blankToNull(reason));
        proposal.setRespondedTime(LocalDateTime.now());
        meetingProposalRepository.save(proposal);

        concern.setStatus(STATUS_PENDING);
        concern.setMeetingStatus("RESCHEDULE_REQUESTED");
        concern.setMeetingBookedStartTime(null);
        concern.setMeetingBookedEndTime(null);
        concern.setMeetingBookedAt(null);
        concernRepository.save(concern);

        notificationService.notifyMeetingProposalDeclined(concern, proposal);
        return proposal;
    }

    @Transactional(readOnly = true)
    public Map<Integer, ConcernMeetingProposal> getLatestProposalMap(List<Concern> concerns) {
        if (concerns == null || concerns.isEmpty()) {
            return Map.of();
        }

        List<Integer> concernIds = concerns.stream()
                .map(Concern::getConcernId)
                .distinct()
                .collect(Collectors.toList());

        List<ConcernMeetingProposal> allProposals = meetingProposalRepository
                .findByConcern_ConcernIdInOrderByCreatedTimeDesc(concernIds);

        Map<Integer, ConcernMeetingProposal> latestMap = new LinkedHashMap<>();
        for (ConcernMeetingProposal proposal : allProposals) {
            Integer concernId = proposal.getConcern() != null ? proposal.getConcern().getConcernId() : null;
            if (concernId != null && !latestMap.containsKey(concernId)) {
                latestMap.put(concernId, proposal);
            }
        }

        return latestMap;
    }

    @Transactional(readOnly = true)
    public List<ConcernMeetingProposal> getProposalHistory(Integer concernId) {
        return meetingProposalRepository.findByConcern_ConcernIdOrderByCreatedTimeDesc(concernId);
    }

    @Transactional(readOnly = true)
    public Map<Integer, List<ConcernMeetingSlot>> getSlotsMapByProposalIds(List<Integer> proposalIds) {
        if (proposalIds == null || proposalIds.isEmpty()) {
            return Map.of();
        }

        List<ConcernMeetingSlot> slots = meetingSlotRepository.findByProposal_ProposalIdInOrderByStartTimeAsc(proposalIds);
        Map<Integer, List<ConcernMeetingSlot>> slotsMap = new HashMap<>();

        for (ConcernMeetingSlot slot : slots) {
            Integer proposalId = slot.getProposal() != null ? slot.getProposal().getProposalId() : null;
            if (proposalId == null) {
                continue;
            }
            slotsMap.computeIfAbsent(proposalId, key -> new ArrayList<>()).add(slot);
        }

        for (Integer proposalId : proposalIds) {
            slotsMap.computeIfAbsent(proposalId, key -> new ArrayList<>());
        }

        return slotsMap;
    }

    @Transactional
    public void deleteByConcernId(Integer concernId) {
        List<ConcernMeetingProposal> proposals = meetingProposalRepository
                .findByConcern_ConcernIdOrderByCreatedTimeDesc(concernId);
        if (!proposals.isEmpty()) {
            meetingProposalRepository.deleteAll(proposals);
        }
    }

    private List<SlotWindow> parseSlotWindows(List<String> slotStarts, List<String> slotEnds) {
        if (slotStarts == null || slotEnds == null || slotStarts.isEmpty()) {
            throw new RuntimeException("Please add at least one available time slot.");
        }

        if (slotStarts.size() != slotEnds.size()) {
            throw new RuntimeException("Invalid slot data. Please submit the form again.");
        }

        List<SlotWindow> windows = new ArrayList<>();
        for (int i = 0; i < slotStarts.size(); i++) {
            String startRaw = slotStarts.get(i);
            String endRaw = slotEnds.get(i);

            if (blank(startRaw) || blank(endRaw)) {
                throw new RuntimeException("Each slot must include both a start and end time.");
            }

            LocalDateTime start = parseDateTime(startRaw, "slot start");
            LocalDateTime end = parseDateTime(endRaw, "slot end");

            if (!end.isAfter(start)) {
                throw new RuntimeException("Every slot end time must be after its start time.");
            }

            if (start.isBefore(LocalDateTime.now().plusMinutes(5))) {
                throw new RuntimeException("Meeting slots must be scheduled in the future.");
            }

            windows.add(new SlotWindow(start, end));
        }

        windows.sort(Comparator.comparing(SlotWindow::start));
        for (int i = 1; i < windows.size(); i++) {
            SlotWindow previous = windows.get(i - 1);
            SlotWindow current = windows.get(i);
            if (current.start().isBefore(previous.end())) {
                throw new RuntimeException("Time slots overlap. Please adjust the schedule.");
            }
        }

        return windows;
    }

    private LocalDateTime parseDateTime(String value, String label) {
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ex) {
            throw new RuntimeException("Invalid " + label + " value.");
        }
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String blankToNull(String value) {
        return blank(value) ? null : value.trim();
    }

    private record SlotWindow(LocalDateTime start, LocalDateTime end) {
    }
}
