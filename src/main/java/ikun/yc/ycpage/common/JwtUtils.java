//仰晨study 创建时间2023/12/4 1:28 星期一
package ikun.yc.ycpage.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
import java.util.Map;

public class JwtUtils {
    private static String signKey = "ikun";
    private static final Long EXPIRE = 43200000L;   //过期时间为12个小时

    /**
     * 生成JWT令牌
     * @param claims JWT第二部分负载 payload 中存储的内容
     * @return JWT令牌
     */
    public static String generateJwt(Map<String, Object> claims){
        return Jwts.builder()
                .addClaims(claims)                                               // 添加JWT第二部分负载
                .signWith(SignatureAlgorithm.HS256, signKey)                    // 使用HS256加密算法
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE))  // 设置过期时间
                .compact();                                                   // 压缩为字符串
    }

    /**
     * 解析JWT令牌
     * @param jwt JWT令牌
     * @return JWT第二部分负载 payload 中存储的内容
     */
    public static Claims parseJWT(String jwt){
        // 不检查就去除 'Bearer ' 前缀
        jwt = jwt.substring(7);

        return Jwts.parser()
                .setSigningKey(signKey)
                .parseClaimsJws(jwt)
                .getBody();
    }

}
