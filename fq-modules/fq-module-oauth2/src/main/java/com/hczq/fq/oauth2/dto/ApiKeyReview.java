package com.hczq.fq.oauth2.dto;

import jakarta.validation.constraints.Size;

public record ApiKeyReview(boolean approved, @Size(max = 500) String comment) {}
