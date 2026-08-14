package ikun.yc.ycpage.utils;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * JWT 生成和解析回归测试。
 */
class JwtUtilsTest {

    /**
     * 清理测试使用的 JVM 密钥，避免污染其他测试。
     */
    @AfterEach
    void clearSecret() {
        System.clearProperty("yc.jwt.secret");
    }

    /**
     * 验证新版 JJWT 生成的令牌可以保留并解析业务载荷。
     */
    @Test
    void shouldGenerateAndParseJwt() {
        System.setProperty("yc.jwt.secret", "test-only-jwt-secret-with-at-least-32-bytes");
        Map<String, Object> claims = new HashMap<>(); // JWT 业务载荷
        claims.put("userId", "test-user");

        String token = JwtUtils.generateJwt(claims, 60_000L); // 测试令牌
        Claims parsedClaims = JwtUtils.parseJWT("Bearer " + token); // 解析后的业务载荷

        assertEquals("test-user", parsedClaims.get("userId"));
    }
}
