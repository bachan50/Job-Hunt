package com.jobhunter.service;

import java.util.List;

/**
 * Criteria passed to each {@link JobSource}.
 *
 * @param query    free-text query (usually the resume's top keywords)
 * @param keywords individual skills/keywords used for match scoring
 * @param location preferred job location
 * @param limit    maximum number of results to return per source
 */
public record JobSearchRequest(String query, List<String> keywords, String location, int limit) {
}
