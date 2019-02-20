package cn.icuter.jsql;

import org.junit.Test;

/**
 * @author edward
 * @since 2019-02-25
 */
public class DemoTest {

    @Test
    public void testPath() {
        System.out.println(String.format("/openauth2/api/token?grant_type=%1$s&appid=%2$s&secret=%3$s", 1, 2, 3));
        System.out.println(String.format("/openauth2/api/token?grant_type=%3$s&appid=%2$s&secret=%1$s", "1", "2", "3"));

        System.out.println(String.format("/openauth2/api/getcontext?ticket=%1$s&access_token=%2$s", 1, 2));
        System.out.println(String.format("/openauth2/api/getcontext?ticket=%1$s&access_token=%2$s", "1", "2"));

        System.out.println(String.format("/gateway/ticket/user/acquirecontext?accessToken=%1$s", 1));
        System.out.println(String.format("/gateway/ticket/user/acquirecontext?accessToken=%1$s", "1"));
    }
}
