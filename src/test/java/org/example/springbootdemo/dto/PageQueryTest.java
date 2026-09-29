package org.example.springbootdemo.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 分页参数归一化的行为验证。
 *
 * <p>这些用例都对应实测过的真实故障：改造前 {@code ?page=0} 会拼出 {@code LIMIT -3, 3}
 * 被 MySQL 判为语法错误、整个请求 500。分页参数是外部可控输入，边界必须钉死。
 */
class PageQueryTest {

    @Test
    @DisplayName("合法参数原样保留，offset 计算正确")
    void keepsValidValues() {
        PageQuery query = PageQuery.of(3, 50);

        assertEquals(3, query.getPage());
        assertEquals(50, query.getSize());
        assertEquals(100L, query.getOffset());
        assertEquals("LIMIT 100, 50", query.toLimitClause());
    }

    @Test
    @DisplayName("page 小于 1 时归一为第 1 页，不再拼出负 offset")
    void normalizesPageBelowOne() {
        assertEquals(1, PageQuery.of(0, 20).getPage());
        assertEquals(1, PageQuery.of(-5, 20).getPage());
        assertEquals("LIMIT 0, 20", PageQuery.of(0, 20).toLimitClause());
    }

    @Test
    @DisplayName("size 小于 1 时回落到默认值")
    void normalizesSizeBelowOne() {
        assertEquals(PageQuery.DEFAULT_SIZE, PageQuery.of(1, 0).getSize());
        assertEquals(PageQuery.DEFAULT_SIZE, PageQuery.of(1, -5).getSize());
    }

    @Test
    @DisplayName("size 超过上限时被夹到 MAX_SIZE，分页保护不可绕过")
    void capsSizeAtMax() {
        assertEquals(PageQuery.MAX_SIZE, PageQuery.of(1, 999999).getSize());
        assertEquals(PageQuery.MAX_SIZE, PageQuery.of(1, Integer.MAX_VALUE).getSize());
    }

    @Test
    @DisplayName("page 接近 Integer.MAX_VALUE 时 offset 不溢出成负数")
    void offsetDoesNotOverflow() {
        PageQuery query = PageQuery.of(Integer.MAX_VALUE, PageQuery.MAX_SIZE);

        // (2147483647 - 1) * 100 若用 int 计算会溢出为负数
        assertEquals(214748364600L, query.getOffset());
        assertTrue(query.getOffset() >= 0);
        assertFalse(query.toLimitClause().contains("-"));
    }

    @Test
    @DisplayName("任意极端输入组合，拼出的 LIMIT 都不含负号")
    void limitClauseNeverNegative() {
        int[] pages = {Integer.MIN_VALUE, -1, 0, 1, 2, Integer.MAX_VALUE};
        int[] sizes = {Integer.MIN_VALUE, -1, 0, 1, 20, 100, 101, Integer.MAX_VALUE};

        for (int page : pages) {
            for (int size : sizes) {
                PageQuery query = PageQuery.of(page, size);
                String case_ = "page=" + page + ", size=" + size;

                assertTrue(query.getOffset() >= 0, case_);
                assertTrue(query.getSize() >= 1 && query.getSize() <= PageQuery.MAX_SIZE, case_);
                assertFalse(query.toLimitClause().contains("-"), case_ + " -> " + query.toLimitClause());
            }
        }
    }
}
