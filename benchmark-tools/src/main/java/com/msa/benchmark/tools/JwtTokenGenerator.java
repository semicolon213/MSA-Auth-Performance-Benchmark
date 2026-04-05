package com.msa.benchmark.tools;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 애플리케이션의 {@code jwt.secret}(HS256)과 동일한 방식으로 JWT를 생성합니다.
 * Spring Security {@code NimbusJwtDecoder.withSecretKey}로 검증 가능합니다.
 */
public final class JwtTokenGenerator {

    private static final String DEFAULT_SECRET = "benchmark-jwt-hmac-secret-key-min-32-bytes!!";

    private JwtTokenGenerator() {
    }

    public static void main(String[] args) {
        String secret = DEFAULT_SECRET;
        String subject = "bench-user";
        String issuer = "msa-benchmark";
        long ttlMinutes = 60L;

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            switch (a) {
                case "--help":
                case "-h":
                    printUsage();
                    return;
                case "--secret":
                    secret = requireNext(args, ++i, "--secret");
                    break;
                case "--subject":
                    subject = requireNext(args, ++i, "--subject");
                    break;
                case "--issuer":
                    issuer = requireNext(args, ++i, "--issuer");
                    break;
                case "--ttl-minutes":
                    ttlMinutes = Long.parseLong(requireNext(args, ++i, "--ttl-minutes"));
                    break;
                default:
                    System.err.println("알 수 없는 인자: " + a);
                    printUsage();
                    System.exit(2);
            }
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            System.err.println("HS256은 최소 256비트(32바이트) 비밀키가 필요합니다. 현재 길이: " + keyBytes.length);
            System.exit(1);
        }

        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        long now = System.currentTimeMillis();
        long ttlMs = ttlMinutes * 60_000L;

        String token = Jwts.builder()
                .issuer(issuer)
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttlMs))
                .signWith(key)
                .compact();

        System.out.println(token);
    }

    private static String requireNext(String[] args, int i, String opt) {
        if (i >= args.length) {
            System.err.println(opt + " 값이 필요합니다.");
            System.exit(1);
        }
        return args[i];
    }

    private static void printUsage() {
        System.out.println("""
                사용법: java -jar benchmark-tools-*-exec.jar [옵션]
                       mvn -pl benchmark-tools exec:java -Dexec.args="--secret ..."

                옵션:
                  --secret <문자열>     JWT HMAC 비밀 (기본: application.yml 기본값과 동일)
                  --subject <문자열>    sub 클레임 (기본: bench-user)
                  --issuer <문자열>     iss 클레임 (기본: msa-benchmark)
                  --ttl-minutes <분>    유효 기간 (기본: 60)
                  -h, --help            도움말

                예:
                  java -jar target/benchmark-tools-1.0.0-SNAPSHOT-exec.jar --secret "benchmark-jwt-hmac-secret-key-min-32-bytes!!"
                """);
    }
}
