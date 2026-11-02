package com.hczq.fq.user.security;

import com.hczq.fq.common.exception.BusinessException;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Argon2id hashing. New hashes use the parameters of Python's pwdlib/argon2-cffi defaults (t=3, m=64MiB, p=4) and
 * verification reads the parameters from the stored hash, so hashes written by alphadesk verify here and vice versa.
 */
@Component
public class PasswordService {

    private final Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(16, 32, 4, 65536, 3);

    public String hash(String raw) {
        return encoder.encode(raw);
    }

    public boolean verify(String raw, String hashed) {
        try {
            return encoder.matches(raw, hashed);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** 8-128 chars, at least one letter and one digit. Shared by every place that sets a password. */
    public static void validateStrength(String password) {
        if (password.length() < 8 || password.length() > 128) {
            throw BusinessException.badRequest("密码长度须为 8-128 位");
        }
        boolean letter = password.chars().anyMatch(c -> (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z'));
        boolean digit = password.chars().anyMatch(c -> c >= '0' && c <= '9');
        if (!(letter && digit)) {
            throw BusinessException.badRequest("密码须同时包含字母与数字");
        }
    }
}
