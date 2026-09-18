package com.prdtestgen.service;

import com.prdtestgen.model.GenerationRun;
import com.prdtestgen.model.HistorySummary;
import com.prdtestgen.model.TestCase;
import com.prdtestgen.model.TestCaseRecord;
import com.prdtestgen.model.TestPlanResult;
import com.prdtestgen.repository.GenerationRunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class GenerationHistoryService {

    private final GenerationRunRepository repository;

    public GenerationHistoryService(GenerationRunRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void save(String sourceType, String sourceFilename, TestPlanResult result) {
        GenerationRun run = new GenerationRun();
        run.setSourceType(sourceType);
        run.setSourceFilename(sourceFilename);
        run.setSummary(result.getSummary());
        run.setScope(result.getScope());
        if (result.getAssumptions() != null) {
            run.getAssumptions().addAll(result.getAssumptions());
        }
        if (result.getTestCases() != null) {
            for (TestCase tc : result.getTestCases()) {
                TestCaseRecord record = new TestCaseRecord();
                record.setRun(run);
                record.setExternalId(tc.getId());
                record.setTitle(tc.getTitle());
                record.setType(tc.getType());
                record.setPriority(tc.getPriority());
                record.setPreconditions(tc.getPreconditions());
                if (tc.getSteps() != null) {
                    record.getSteps().addAll(tc.getSteps());
                }
                record.setExpectedResult(tc.getExpectedResult());
                run.getTestCases().add(record);
            }
        }
        repository.save(run);
    }

    @Transactional(readOnly = true)
    public List<HistorySummary> listSummaries() {
        List<HistorySummary> summaries = new ArrayList<>();
        for (GenerationRun run : repository.findAllByOrderByCreatedAtDesc()) {
            summaries.add(new HistorySummary(
                    run.getId(), run.getCreatedAt(), run.getSourceType(), run.getSourceFilename(), run.getSummary()));
        }
        return summaries;
    }

    @Transactional(readOnly = true)
    public Optional<TestPlanResult> getFullResult(Long id) {
        return repository.findById(id).map(this::toTestPlanResult);
    }

    private TestPlanResult toTestPlanResult(GenerationRun run) {
        TestPlanResult result = new TestPlanResult();
        result.setSummary(run.getSummary());
        result.setScope(run.getScope());
        result.setAssumptions(new ArrayList<>(run.getAssumptions()));

        List<TestCase> testCases = new ArrayList<>();
        for (TestCaseRecord record : run.getTestCases()) {
            TestCase tc = new TestCase();
            tc.setId(record.getExternalId());
            tc.setTitle(record.getTitle());
            tc.setType(record.getType());
            tc.setPriority(record.getPriority());
            tc.setPreconditions(record.getPreconditions());
            tc.setSteps(new ArrayList<>(record.getSteps()));
            tc.setExpectedResult(record.getExpectedResult());
            testCases.add(tc);
        }
        result.setTestCases(testCases);
        return result;
    }
}
