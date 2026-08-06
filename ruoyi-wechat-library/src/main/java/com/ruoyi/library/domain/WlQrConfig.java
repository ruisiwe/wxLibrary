package com.ruoyi.library.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/** 通用二维码配置。 */
public class WlQrConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private String menuName;
    private String guideText;
    private String imagePath;
    private Integer sortOrder;
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMenuName() { return menuName; }
    public void setMenuName(String menuName) { this.menuName = menuName; }
    public String getGuideText() { return guideText; }
    public void setGuideText(String guideText) { this.guideText = guideText; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
