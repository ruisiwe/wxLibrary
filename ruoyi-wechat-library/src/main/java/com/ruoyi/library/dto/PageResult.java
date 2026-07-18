package com.ruoyi.library.dto;

import java.util.List;

/** 小程序公开分页结果。 */
public class PageResult<T>
{
    private final List<T> items;
    private final long total;
    private final int pageNum;
    private final int pageSize;

    public PageResult(List<T> items, long total, int pageNum, int pageSize)
    {
        this.items = items;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    public List<T> getItems() { return items; }
    public long getTotal() { return total; }
    public int getPageNum() { return pageNum; }
    public int getPageSize() { return pageSize; }
}
