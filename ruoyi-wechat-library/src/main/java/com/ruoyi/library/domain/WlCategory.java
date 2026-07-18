package com.ruoyi.library.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/** 文档分类。 */
public class WlCategory extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private String icon;
    private Integer sortOrder;
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
