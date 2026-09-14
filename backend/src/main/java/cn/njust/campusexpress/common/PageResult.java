package cn.njust.campusexpress.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.util.List;

/**
 * 统一分页返回结构，屏蔽 MyBatis-Plus Page 的内部字段，只暴露前端需要的分页信息。
 */
@Data
public class PageResult<T> {
    /** 服务层分页查询统一使用的默认每页条数。 */
    public static final int DEFAULT_PAGE_SIZE = 10;

    private List<T> records;
    private long total;
    private long current;
    private long size;
    private long pages;

    public static <T> PageResult<T> of(Page<T> page) {
        PageResult<T> vo = new PageResult<>();
        vo.setRecords(page.getRecords());
        vo.setTotal(page.getTotal());
        vo.setCurrent(page.getCurrent());
        vo.setSize(page.getSize());
        vo.setPages(page.getPages());
        return vo;
    }

    /** 按默认页大小构造分页参数，页码为空时默认第 1 页。 */
    public static <T> Page<T> pageOf(Integer currentPage) {
        return new Page<>(currentPage == null ? 1 : currentPage, DEFAULT_PAGE_SIZE);
    }

    /** 空分页结果，用于无需查库即可确定没有数据的场景。 */
    public static <T> PageResult<T> empty() {
        PageResult<T> vo = new PageResult<>();
        vo.setRecords(List.of());
        return vo;
    }
}
