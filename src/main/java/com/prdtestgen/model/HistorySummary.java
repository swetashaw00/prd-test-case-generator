package com.prdtestgen.model;

import java.time.Instant;

public record HistorySummary(Long id, Instant createdAt, String sourceType, String sourceFilename, String summary) {
}
