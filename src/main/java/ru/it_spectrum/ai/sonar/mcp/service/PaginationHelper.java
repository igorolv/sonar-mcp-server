package ru.it_spectrum.ai.sonar.mcp.service;

import ru.it_spectrum.ai.sonar.mcp.config.SonarMcpProperties;

/**
 * Converts external offset/limit to Sonar's page-based p/ps.
 * Sonar returns whole pages; if the caller's offset is not a multiple of limit,
 * the page containing that offset is returned.
 */
public final class PaginationHelper {

    private PaginationHelper() {}

    public record Page(int pageIndex, int pageSize) {}

    public static Page toPage(int offset, int limit, SonarMcpProperties.Pagination props) {
        int safeLimit = limit <= 0 ? props.defaultLimit() : Math.min(limit, props.maxLimit());
        int safeOffset = Math.max(offset, 0);
        int pageIndex = (safeOffset / safeLimit) + 1;
        return new Page(pageIndex, safeLimit);
    }

    public static int totalFromResponse(Integer responseTotal, Integer pagingTotal) {
        if (responseTotal != null) {
            return responseTotal;
        }
        if (pagingTotal != null) {
            return pagingTotal;
        }
        return 0;
    }
}
