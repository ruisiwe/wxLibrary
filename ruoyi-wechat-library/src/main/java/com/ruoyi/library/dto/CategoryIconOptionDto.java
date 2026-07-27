package com.ruoyi.library.dto;

/** 后台分类图标选项。 */
public class CategoryIconOptionDto
{
    private final String name;
    private final String label;
    private final String keywords;

    public CategoryIconOptionDto(String name, String label, String keywords)
    {
        this.name = name;
        this.label = label;
        this.keywords = keywords;
    }

    public String getName() { return name; }
    public String getLabel() { return label; }
    public String getKeywords() { return keywords; }
}
