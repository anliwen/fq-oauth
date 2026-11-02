package com.hczq.fq.authn.dto;

import java.util.List;

public record MeResponse(UserInfo user, List<String> permissions) {
}
