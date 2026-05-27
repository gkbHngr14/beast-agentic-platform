package com.beast.risk.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RagService {

    // In-memory curated documents for demo (world-beater ready structure)
    private final List<Document> knowledgeBase = new ArrayList<>();

    public RagService() {
        loadCuratedDocs();
    }

    private void loadCuratedDocs() {
        // Emerging Pattern 1: Romance Scam + Mule Velocity
        knowledgeBase.add(new Document(
                "Romance scam pattern: Small test transfers (<$50) followed by large urgent sends to 'trusted contacts'. High emotional language in notes.",
                Map.of("pattern", "ROMANCE_MULE", "severity", "HIGH", "ruleId", "R1")
        ));

        // Emerging Pattern 2: Synthetic Identity + Device Sharing
        knowledgeBase.add(new Document(
                "Synthetic identity cluster: Multiple accounts sharing device fingerprint + similar behavioral patterns (typing cadence, mouse entropy).",
                Map.of("pattern", "SYNTHETIC_DEVICE", "severity", "HIGH", "ruleId", "R2")
        ));

        // Emerging Pattern 3: Deepfake Urgency
        knowledgeBase.add(new Document(
                "Deepfake social engineering: Urgent language + 'bank calling' or 'family emergency' requests leading to authorized push payments.",
                Map.of("pattern", "DEEPFAKE_URGENCY", "severity", "MEDIUM", "ruleId", "R3")
        ));

        log.info("[RAG] Loaded " + knowledgeBase.size() + " curated fraud pattern documents.");
    }

    public List<Document> retrieve(String query, int topK) {
        // Simple keyword + relevance scoring for demo
        List<Document> results = new ArrayList<>();

        for (Document doc : knowledgeBase) {
            if (doc.getContent().toLowerCase().contains(query.toLowerCase()) ||
                    doc.getMetadata().toString().toLowerCase().contains(query.toLowerCase())) {
                results.add(doc);
            }
        }

        // Return top K
        return results.size() > topK ? results.subList(0, topK) : results;
    }

    // For AI Review Agent grounding
    public List<Document> retrieveByPattern(String patternType) {
        return knowledgeBase.stream()
                .filter(doc -> doc.getMetadata().getOrDefault("pattern", "").equals(patternType))
                .toList();
    }
}