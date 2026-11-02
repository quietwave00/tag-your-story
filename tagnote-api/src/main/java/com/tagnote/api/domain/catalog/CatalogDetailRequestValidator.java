package com.tagnote.api.domain.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CatalogDetailRequestValidator {
    private static final int PAGE_SIZE = 10;

    public int page(int page) {
        if (page < 0 || page > Integer.MAX_VALUE / PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be a non-negative 0-based index");
        }
        return page;
    }

    public String releaseType(String releaseType) {
        if (releaseType != null && !releaseType.equals("ALL") && !releaseType.equals("ALBUM")
                && !releaseType.equals("SINGLE_EP") && !releaseType.equals("COMPILATION")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid releaseType");
        }
        return releaseType;
    }
}
