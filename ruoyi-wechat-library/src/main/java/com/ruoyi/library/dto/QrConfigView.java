package com.ruoyi.library.dto;

/** 二维码菜单与展示信息，不包含服务器文件路径。 */
public class QrConfigView
{
    private final Long id;
    private final String menuName;
    private final String guideText;
    private final Integer sortOrder;
    private final String status;
    private final boolean imageConfigured;
    private final String imageUrl;

    public QrConfigView(Long id, String menuName, String guideText, Integer sortOrder,
            String status, boolean imageConfigured, String imageUrl)
    {
        this.id = id;
        this.menuName = menuName;
        this.guideText = guideText;
        this.sortOrder = sortOrder;
        this.status = status;
        this.imageConfigured = imageConfigured;
        this.imageUrl = imageUrl;
    }

    public Long getId() { return id; }
    public String getMenuName() { return menuName; }
    public String getGuideText() { return guideText; }
    public Integer getSortOrder() { return sortOrder; }
    public String getStatus() { return status; }
    public boolean isImageConfigured() { return imageConfigured; }
    public String getImageUrl() { return imageUrl; }
}
