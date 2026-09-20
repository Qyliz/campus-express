package cn.njust.campusexpress.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.util.List;

//向前端返回分页数据
@Data
public class PageResult<T> {
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

    //构建分页对象，页码为空时默认第1页。
    public static <T> Page<T> pageOf(Integer currentPage) {
        return new Page<>(currentPage == null ? 1 : currentPage, DEFAULT_PAGE_SIZE);
    }

    //空分页结果，用于无需查库即可确定没有数据的场景。
    public static <T> PageResult<T> empty() {
        PageResult<T> vo = new PageResult<>();
        vo.setRecords(List.of());
        return vo;
    }
}
