package com.prdtestgen.service;

import com.prdtestgen.model.HistorySummary;
import com.prdtestgen.model.TestCase;
import com.prdtestgen.model.TestPlanResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class GenerationHistoryServiceTest {

    @Autowired
    private GenerationHistoryService historyService;

    @Test
    void savesAndRetrievesFullRoundTrip() {
        TestPlanResult result = new TestPlanResult();
        result.setSummary("Test summary for round trip");
        result.setScope("Test scope");
        result.setAssumptions(List.of("assumption A", "assumption B"));

        TestCase tc = new TestCase();
        tc.setId("TC-001");
        tc.setTitle("Sample case");
        tc.setType("Functional");
        tc.setPriority("High");
        tc.setPreconditions("None");
        tc.setSteps(List.of("step 1", "step 2"));
        tc.setExpectedResult("It works");
        result.setTestCases(List.of(tc));

        historyService.save("PRD", "sample.md", result);

        List<HistorySummary> summaries = historyService.listSummaries();
        HistorySummary saved = summaries.stream()
                .filter(s -> "Test summary for round trip".equals(s.summary()))
                .findFirst()
                .orElseThrow();
        assertEquals("PRD", saved.sourceType());
        assertEquals("sample.md", saved.sourceFilename());

        Optional<TestPlanResult> fetched = historyService.getFullResult(saved.id());
        assertTrue(fetched.isPresent());
        assertEquals(List.of("assumption A", "assumption B"), fetched.get().getAssumptions());
        assertEquals(1, fetched.get().getTestCases().size());
        assertEquals("Sample case", fetched.get().getTestCases().get(0).getTitle());
        assertEquals(List.of("step 1", "step 2"), fetched.get().getTestCases().get(0).getSteps());
    }
}
