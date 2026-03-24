package Project._6.demo.service;

import Project._6.demo.dto.FeedbackDTO;
import Project._6.demo.entity.Concern;
import Project._6.demo.entity.Feedback;
import Project._6.demo.repository.ConcernRepository;
import Project._6.demo.repository.FeedbackRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final ConcernRepository concernRepository;

    public FeedbackService(FeedbackRepository feedbackRepository,
                           ConcernRepository concernRepository) {
        this.feedbackRepository = feedbackRepository;
        this.concernRepository = concernRepository;
    }

    @Transactional
    public Feedback submitFeedback(FeedbackDTO dto) {
        if (feedbackRepository.existsByConcern_ConcernId(dto.getConcernId())) {
            throw new RuntimeException("Feedback already submitted for this concern.");
        }

        Concern concern = concernRepository.findById(dto.getConcernId())
                .orElseThrow(() -> new RuntimeException("Concern not found."));

        Feedback feedback = new Feedback();
        feedback.setFeedbackId(feedbackRepository.getNextFeedbackId());
        feedback.setConcern(concern);
        feedback.setRating(dto.getRating());
        feedback.setComments(dto.getComments());
        return feedbackRepository.save(feedback);
    }

    /**
     * Build a map of concernId -> Feedback for displaying on the history page.
     */
    public Map<Integer, Feedback> getFeedbackMap(List<Concern> concerns) {
        Map<Integer, Feedback> map = new HashMap<>();
        for (Concern c : concerns) {
            Optional<Feedback> fb = feedbackRepository.findByConcern_ConcernId(c.getConcernId());
            fb.ifPresent(feedback -> map.put(c.getConcernId(), feedback));
        }
        return map;
    }
}
