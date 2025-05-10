//仰晨study 创建时间2023/12/4 1:28 星期一
package ikun.yc.ycpage.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtUtils {
    private static final String SIGN_KEY = "ikun";
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
    public static String generateJwt(Map<String, Object> claims,Long expire){
      return Jwts.builder()
                .addClaims(claims)                                                   // 添加JWT第二部分负载
                .signWith(SignatureAlgorithm.HS256, SIGN_KEY)                       // 使用HS256加密算法
                .setExpiration(new Date(System.currentTimeMillis() + expire))      // 设置过期时间
                .compact();                                                       // 压缩为字符串
    }

    /**
     * 生成JWT令牌
     * @param claims JWT第二部分负载 payload 中存储的内容
     * @return JWT令牌
     */
    public static String generateJwt(Map<String, Object> claims,String expire){
        Long expireTime = EXPIRE_MAP.get(expire)==null?EXPIRE_MAP.get("bt"):EXPIRE_MAP.get(expire);
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
                .setSigningKey(SIGN_KEY)
                .parseClaimsJws(jwt)
                .getBody();
    }

}
