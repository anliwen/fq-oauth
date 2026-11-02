package com.hczq.fq.common.api;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashSet;
import java.util.Set;

/**
 * Remembers which properties were present in the request body, mirroring pydantic's {@code exclude_unset}:
 * an absent field means "leave unchanged", an explicit null is a real value.
 */
public abstract class PartialUpdate {

    @JsonIgnore
    private final Set<String> present = new HashSet<>();

    protected void mark(String field) {
        present.add(field);
    }

    public boolean has(String field) {
        return present.contains(field);
    }
}
