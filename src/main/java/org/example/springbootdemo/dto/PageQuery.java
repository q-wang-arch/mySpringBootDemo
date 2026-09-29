package org.example.springbootdemo.dto;

/**
 * 列表接口的分页参数，负责把外部传入的 page / size 归一化到安全范围。
 *
 * <p>存在的理由：分页参数是外部可控输入，直接参与 SQL 拼接。之前各 Controller 各写一遍
 * {@code int offset = (page - 1) * size}，一旦 page 或 size 传得不合法就会拼出负的 offset，
 * MySQL 会直接报语法错误（不是"查不到数据"，而是 500）。
 *
 * <p>归一化规则：
 * <ul>
 *   <li>{@code page < 1} → 1（前端某些分页组件从 0 开始计数，会传 page=0）</li>
 *   <li>{@code size < 1} → 20</li>
 *   <li>{@code size > 100} → 100（上限即"分页保护"本身，没有上限就等于没分页）</li>
 * </ul>
 *
 * <p>不建议调用方自行计算 offset 后拼 SQL——归一化保证的非负性只有本类知道，
 * 自行拼接容易重新引入负 offset。请统一用 {@link #toLimitClause()}。
 */
public final class PageQuery {

    /** 默认页码 */
    public static final int DEFAULT_PAGE = 1;

    /** 默认每页条数 */
    public static final int DEFAULT_SIZE = 20;

    /** 每页条数上限，防止调用方用超大 size 绕过 pagination 保护 */
    public static final int MAX_SIZE = 100;

    private final int page;
    private final int size;

    private PageQuery(int page, int size) {
        this.page = page;
        this.size = size;
    }

    /**
     * 归一化构造。传入的原始值不合法时会被夹到合法区间，不抛异常
     * ——分页参数传错不该让整个接口 500。
     */
    public static PageQuery of(int page, int size) {
        int safeSize = (size < 1) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        int safePage = (page < 1) ? DEFAULT_PAGE : page;
        return new PageQuery(safePage, safeSize);
    }

    /** 归一化后的页码，可直接回显给前端 */
    public int getPage() {
        return page;
    }

    /** 归一化后的每页条数，可直接回显给前端 */
    public int getSize() {
        return size;
    }

    /**
     * 跳过多少条。
     *
     * <p>刻意用 long 计算：{@code (page - 1) * size} 在 page 接近 {@link Integer#MAX_VALUE} 时
     * 会 int 溢出成负数（例如 2147483646 * 20），那样又会拼出负 offset。
     */
    public long getOffset() {
        return (long) (page - 1) * size;
    }

    /**
     * 拼好的 LIMIT 子句，交给 {@code QueryWrapper#last(String)}。
     *
     * <p>getOffset() 恒为非负、size 恒在 [1, MAX_SIZE]，因此拼出的语句不会出现
     * {@code LIMIT -3, 3} 这种非法语法。
     */
    public String toLimitClause() {
        return "LIMIT " + getOffset() + ", " + size;
    }

    @Override
    public String toString() {
        return "page=" + page + ", size=" + size;
    }
}
