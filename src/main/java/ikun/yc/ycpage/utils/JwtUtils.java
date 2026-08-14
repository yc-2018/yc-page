//仰晨study 创建时间2023/12/4 1:28 星期一
package ikun.yc.ycpage.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtUtils {
    private static final String JWT_SECRET_ENV = "YC_JWT_SECRET";
    private static final String JWT_SECRET_PROPERTY = "yc.jwt.secret";
    public static final String DEFAULT_EXPIRE = "yz"; // 默认过期时间：一周
    private static final Map<String, Long>EXPIRE_MAP = new HashMap<>();
    static {
        EXPIRE_MAP.put("bt", 43200000L);    //过期时间为12个小时
        EXPIRE_MAP.put("yt", 86400000L);    // 一天
        EXPIRE_MAP.put("yz", 604800000L);   // 一周
        EXPIRE_MAP.put("yy", 2592000000L);  //一个月
        EXPIRE_MAP.put("yn", 31536000000L); // 一年
    }

    /**
     * 生成JWT令牌
     * @param claims JWT第二部分负载 payload 中存储的内容
     * @return JWT令牌
     */
    public static String generateJwt(Map<String, Object> claims, Long expire) {
        return Jwts.builder()
                .claims(claims)                                                      // 添加JWT第二部分负载
                .signWith(signingKey())                                              // 使用HS256加密算法
                .expiration(new Date(System.currentTimeMillis() + expire))           // 设置过期时间
                .compact();                                                       // 压缩为字符串
    }

    /**
     * 生成JWT令牌
     * @param claims JWT第二部分负载 payload 中存储的内容
     * @return JWT令牌
     */
    public static String generateJwt(Map<String, Object> claims,String expire){
        Long expireTime = EXPIRE_MAP.get(expire)==null?EXPIRE_MAP.get(DEFAULT_EXPIRE):EXPIRE_MAP.get(expire);
        return generateJwt(claims, expireTime);
    }

    /**
     * 解析JWT令牌
     * @param jwt JWT令牌
     * @return JWT第二部分负载 payload 中存储的内容
     */
    public static Claims parseJWT(String jwt){
        // 检查去除 'Bearer ' 前缀
        if (jwt.startsWith("Bearer ")) jwt = jwt.substring(7);

        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
    }

    /**
     * 从环境变量读取至少 32 字节的 HMAC 密钥。
     *
     * @return JWT 签名密钥
     */
    private static SecretKey signingKey() {
        String secret = System.getenv(JWT_SECRET_ENV); // 部署环境提供的 JWT 密钥
        if (secret == null) {
            secret = System.getProperty(JWT_SECRET_PROPERTY); // 测试或 JVM 参数提供的 JWT 密钥 (通过 Java 启动参数传进去)
        }
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(JWT_SECRET_ENV + " or -D" + JWT_SECRET_PROPERTY
                    + " must contain at least 32 UTF-8 bytes");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

}
