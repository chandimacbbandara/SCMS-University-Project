package Project._6.demo.service;

import Project._6.demo.entity.Faq;
import Project._6.demo.entity.Tip;
import Project._6.demo.repository.FaqRepository;
import Project._6.demo.repository.TipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FaqManagementService {

    @Autowired
    private FaqRepository faqRepository;

    @Autowired
    private TipRepository tipRepository;

    // --- TIP MANAGEMENT ---
    public List<Tip> getAllTips() {
        return tipRepository.findAllByOrderByCreatedAtDesc();
    }

    public Tip getTipById(Long tipId) {
        return tipRepository.findById(tipId).orElse(null);
    }

    public void saveTip(Tip tip) {
        tipRepository.save(tip);
    }

    public void updateTip(Long tipId, String title, String description, String iconClass) {
        Tip tip = tipRepository.findById(tipId).orElseThrow(() -> new IllegalArgumentException("Invalid tip Id"));
        tip.setTitle(title);
        tip.setDescription(description);
        tip.setIconClass(iconClass);
        tipRepository.save(tip);
    }

    public void deleteTip(Long tipId) {
        tipRepository.deleteById(tipId);
    }

    // --- FAQ MANAGEMENT ---
    public List<Faq> getAllFaqs() {
        return faqRepository.findAllByOrderByCreatedAtDesc();
    }

    public Faq getFaqById(Long faqId) {
        return faqRepository.findById(faqId).orElse(null);
    }

    public void saveFaq(Faq faq) {
        faqRepository.save(faq);
    }

    public void updateFaq(Long faqId, String question, String answer) {
        Faq faq = faqRepository.findById(faqId).orElseThrow(() -> new IllegalArgumentException("Invalid faq Id"));
        faq.setQuestion(question);
        faq.setAnswer(answer);
        faqRepository.save(faq);
    }

    public void deleteFaq(Long faqId) {
        faqRepository.deleteById(faqId);
    }
}
