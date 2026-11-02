package com.hczq.fq.authn.dto;

import java.util.List;

public record UserInfo(Long id, String username, String fullName, String email, String avatar, List<String> roles) {
}
