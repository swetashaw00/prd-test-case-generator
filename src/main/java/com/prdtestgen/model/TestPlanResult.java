package com.prdtestgen.model;

import java.util.List;

public class TestPlanResult {

    private String summary;
    private String scope;
    private List<String> assumptions;
    private List<TestCase> testCases;

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public List<String> getAssumptions() {
        return assumptions;
    }

    public void setAssumptions(List<String> assumptions) {
        this.assumptions = assumptions;
    }

    public List<TestCase> getTestCases() {
        return testCases;
    }

    public void setTestCases(List<TestCase> testCases) {
        this.testCases = testCases;
    }
}
