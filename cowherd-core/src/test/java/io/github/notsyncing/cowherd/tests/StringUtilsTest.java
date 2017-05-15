package io.github.notsyncing.cowherd.tests;

import io.github.notsyncing.cowherd.models.Pair;
import io.github.notsyncing.cowherd.utils.StringUtils;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class StringUtilsTest {
    @Test
    public void testParseQueryString() {
        String s = "a=1&b=2&c=3";
        List<Pair<String, String>> l = StringUtils.parseQueryString(s);
        assertEquals(3, l.size());
        assertEquals("a", l.get(0).getKey());
        assertEquals("1", l.get(0).getValue());
        assertEquals("b", l.get(1).getKey());
        assertEquals("2", l.get(1).getValue());
        assertEquals("c", l.get(2).getKey());
        assertEquals("3", l.get(2).getValue());
    }

    @Test
    public void testParseQueryStringWithFalseQueryString() {
        String s = "<xml>\n" +
                "    <ToUserName><![CDATA[gh_f51d37e809db]]></ToUserName>\n" +
                "    <Encrypt><![CDATA[Zj3Mv1Ur8jjuZnnQv/XkXwsgj9oVft0tQaJ0amxHY27UGsTxZnl0OpUhqCU2gD+Jz8L2BORCs3O4vQT8ImDDKJEFLnuGBB/AXnA7KLumsV//hZCm8eRpim0u4s7gbEIOf6lzhtmS8FUs4HFI/Og/Nx+ZstTf1tzP+MiD8C9vBA+Z1px/8Ee4+IHm5gwIePFwouU4TX4kaay10p820rqBitr7gskyWHVtYylncguC3YR6NshoG7+QZEMaCD+ym9V08DGsQvDiRKifbWc7QGeXfkoUJiMdCt5Bc86ZKLNkEvaKNfxHULwxBlDDuWjT8EWxPSu7tOzAzccNi48stfjvPSPKu8t9BP4kpJG89v837sfC7egP0b5ZPCCHvddlAeRRXHmX5fibLy2to6tMo9Nr7BwGgxU+AMItZm0P41m5tZE=]]></Encrypt>\n" +
                "</xml>";

        List<Pair<String, String>> l = StringUtils.parseQueryString(s);
        assertEquals(0, l.size());
    }
}
