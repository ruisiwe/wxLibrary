package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlBanner;
import com.ruoyi.library.domain.WlCategory;
import com.ruoyi.library.domain.WlDocument;
import com.ruoyi.library.mapper.WlBannerMapper;
import com.ruoyi.library.mapper.WlCategoryMapper;
import com.ruoyi.library.mapper.WlDocumentMapper;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 后台宣传图片、分类和文档管理服务。 */
@Service
public class DocumentService
{
    private static final Set<String> SUPPORTED_FORMATS = new HashSet<>(
            Arrays.asList("PDF", "DOC", "DOCX", "PPT", "PPTX", "TXT", "XLS"));

    private final WlBannerMapper bannerMapper;
    private final WlCategoryMapper categoryMapper;
    private final WlDocumentMapper documentMapper;

    public DocumentService(WlBannerMapper bannerMapper, WlCategoryMapper categoryMapper,
            WlDocumentMapper documentMapper)
    {
        this.bannerMapper = bannerMapper;
        this.categoryMapper = categoryMapper;
        this.documentMapper = documentMapper;
    }

    public List<WlBanner> listBanners(WlBanner query) { return bannerMapper.selectBannerList(query); }

    public WlBanner getBanner(Long id)
    {
        WlBanner banner = bannerMapper.selectBannerById(id);
        if (banner == null) throw new ServiceException("宣传图片不存在");
        return banner;
    }

    public int addBanner(WlBanner banner, String operator)
    {
        validateBanner(banner);
        banner.setStatus(normalizeStatus(banner.getStatus()));
        banner.setSortOrder(defaultZero(banner.getSortOrder()));
        banner.setCreateBy(operator);
        int rows = bannerMapper.insertBanner(banner);
        if (rows != 1) throw new ServiceException("宣传图片只能关联已上架文档");
        return rows;
    }

    public int updateBanner(WlBanner banner, String operator)
    {
        requireId(banner == null ? null : banner.getId(), "宣传图片编号不能为空");
        getBanner(banner.getId());
        validateBanner(banner);
        banner.setStatus(normalizeStatus(banner.getStatus()));
        banner.setSortOrder(defaultZero(banner.getSortOrder()));
        banner.setUpdateBy(operator);
        int rows = bannerMapper.updateBanner(banner);
        if (rows != 1) throw new ServiceException("宣传图片不存在或关联文档已下架");
        return rows;
    }

    public int removeBanners(Long[] ids, String operator)
    {
        requireIds(ids);
        return bannerMapper.deleteBanners(ids, operator);
    }

    public List<WlCategory> listCategories(WlCategory query) { return categoryMapper.selectCategoryList(query); }

    public WlCategory getCategory(Long id)
    {
        WlCategory category = categoryMapper.selectCategoryById(id);
        if (category == null) throw new ServiceException("文档分类不存在");
        return category;
    }

    public int addCategory(WlCategory category, String operator)
    {
        validateCategory(category, null);
        category.setStatus(normalizeStatus(category.getStatus()));
        category.setSortOrder(defaultZero(category.getSortOrder()));
        category.setCreateBy(operator);
        return categoryMapper.insertCategory(category);
    }

    public int updateCategory(WlCategory category, String operator)
    {
        requireId(category == null ? null : category.getId(), "文档分类编号不能为空");
        getCategory(category.getId());
        validateCategory(category, category.getId());
        category.setStatus(normalizeStatus(category.getStatus()));
        category.setSortOrder(defaultZero(category.getSortOrder()));
        category.setUpdateBy(operator);
        return categoryMapper.updateCategory(category);
    }

    @Transactional
    public int removeCategories(Long[] ids, String operator)
    {
        requireIds(ids);
        for (Long id : ids) getCategory(id);
        if (documentMapper.countDocumentsByCategoryIds(ids) > 0)
            throw new ServiceException("分类下存在文档，不能删除");
        int rows = categoryMapper.deleteCategories(ids, operator);
        if (rows != ids.length) throw new ServiceException("分类下存在文档，不能删除");
        return rows;
    }

    public List<WlDocument> listDocuments(WlDocument query) { return documentMapper.selectDocumentList(query); }

    public WlDocument getDocument(Long id)
    {
        WlDocument document = documentMapper.selectDocumentById(id);
        if (document == null) throw new ServiceException("文档不存在");
        return document;
    }

    public int addDocument(WlDocument document, String operator)
    {
        validateDocument(document);
        normalizeDocument(document);
        document.setPublishStatus("DRAFT");
        document.setConversionStatus("PENDING");
        document.setViewCount(defaultZero(document.getViewCount()));
        document.setCreateBy(operator);
        return documentMapper.insertDocument(document);
    }

    public int updateDocument(WlDocument document, String operator)
    {
        requireId(document == null ? null : document.getId(), "文档编号不能为空");
        WlDocument existing = getDocument(document.getId());
        if ("PUBLISHED".equals(existing.getPublishStatus()))
            throw new ServiceException("请先下架文档后再修改");
        validateDocument(document);
        normalizeDocument(document);
        document.setUpdateBy(operator);
        return documentMapper.updateDocument(document);
    }

    @Transactional
    public int publishDocument(Long id, String operator)
    {
        WlDocument document = getDocument(id);
        if ("PUBLISHED".equals(document.getPublishStatus())) return 0;
        if (!"SUCCESS".equals(document.getConversionStatus()))
            throw new ServiceException("文档转换成功后才能上架");
        WlCategory category = categoryMapper.selectCategoryById(document.getCategoryId());
        if (category == null || !"0".equals(category.getStatus()))
            throw new ServiceException("启用文档分类后才能上架");
        return documentMapper.updatePublishStatus(id, "PUBLISHED", operator);
    }

    public int unpublishDocument(Long id, String operator)
    {
        WlDocument document = getDocument(id);
        if (!"PUBLISHED".equals(document.getPublishStatus())) return 0;
        return documentMapper.updatePublishStatus(id, "DRAFT", operator);
    }

    @Transactional
    public int removeDocuments(Long[] ids, String operator)
    {
        requireIds(ids);
        for (Long id : ids)
        {
            if ("PUBLISHED".equals(getDocument(id).getPublishStatus()))
                throw new ServiceException("请先下架文档后再删除");
        }
        int rows = documentMapper.deleteDocuments(ids, operator);
        if (rows != ids.length) throw new ServiceException("文档状态已变化，请刷新后重试");
        return rows;
    }

    private void validateBanner(WlBanner banner)
    {
        if (banner == null) throw new ServiceException("宣传图片参数不能为空");
        requireText(banner.getTitle(), "宣传图片标题不能为空");
        requireText(banner.getImageUrl(), "宣传图片地址不能为空");
        requireId(banner.getDocumentId(), "关联文档不能为空");
        if (banner.getStartTime() != null && banner.getEndTime() != null
                && !banner.getStartTime().before(banner.getEndTime()))
            throw new ServiceException("展示结束时间必须晚于开始时间");
        WlDocument document = documentMapper.selectDocumentById(banner.getDocumentId());
        if (document == null || !"PUBLISHED".equals(document.getPublishStatus()))
            throw new ServiceException("宣传图片只能关联已上架文档");
    }

    private void validateCategory(WlCategory category, Long excludeId)
    {
        if (category == null) throw new ServiceException("文档分类参数不能为空");
        requireText(category.getName(), "文档分类名称不能为空");
        category.setName(category.getName().trim());
        if ("全部分类".equals(category.getName()))
            throw new ServiceException("全部分类是固定入口，不能作为普通分类保存");
        requireMaxLength(category.getName(), 64, "文档分类名称不能超过64个字符");
        if (categoryMapper.countCategoryName(category.getName(), excludeId) > 0)
            throw new ServiceException("文档分类名称已存在");
    }

    private void validateDocument(WlDocument document)
    {
        if (document == null) throw new ServiceException("文档参数不能为空");
        requireId(document.getCategoryId(), "文档分类不能为空");
        if (categoryMapper.selectCategoryById(document.getCategoryId()) == null)
            throw new ServiceException("文档分类不存在");
        requireText(document.getTitle(), "文档标题不能为空");
        requireMaxLength(document.getTitle().trim(), 255, "文档标题不能超过255个字符");
        requireText(document.getUploaderName(), "上传人不能为空");
        requireMaxLength(document.getUploaderName().trim(), 128, "上传人不能超过128个字符");
        requireText(document.getFileFormat(), "文档格式不能为空");
        String format = document.getFileFormat().trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_FORMATS.contains(format)) throw new ServiceException("文档格式不支持");
        requireText(document.getOriginalObjectKey(), "原文件对象键不能为空");
        if (document.getFileSize() == null || document.getFileSize() < 0)
            throw new ServiceException("文档大小不能小于0");
        if (document.getPageCount() == null || document.getPageCount() < 0)
            throw new ServiceException("文档总页数不能小于0");
        if (document.getPointPrice() == null || document.getPointPrice() < 0)
            throw new ServiceException("兑换积分不能小于0");
        if (document.getPreviewPages() == null || document.getPreviewPages() < 0)
            throw new ServiceException("试读页数不能小于0");
        if (document.getPreviewPages() > document.getPageCount())
            throw new ServiceException("试读页数不能大于文档总页数");
        if (document.getSummary() != null)
            requireMaxLength(document.getSummary(), 1000, "文档摘要不能超过1000个字符");
        if (document.getTags() != null)
            requireMaxLength(document.getTags(), 500, "文档标签不能超过500个字符");
    }

    private void normalizeDocument(WlDocument document)
    {
        document.setTitle(document.getTitle().trim());
        document.setSummary(defaultText(document.getSummary(), ""));
        document.setTags(defaultText(document.getTags(), ""));
        document.setUploaderName(document.getUploaderName().trim());
        document.setFileFormat(document.getFileFormat().trim().toUpperCase(Locale.ROOT));
        document.setOriginalObjectKey(document.getOriginalObjectKey().trim());
        document.setSortOrder(defaultZero(document.getSortOrder()));
    }

    private String normalizeStatus(String status)
    {
        if (status == null || status.trim().isEmpty()) return "0";
        if (!"0".equals(status) && !"1".equals(status)) throw new ServiceException("状态不正确");
        return status;
    }

    private void requireText(String value, String message)
    {
        if (value == null || value.trim().isEmpty()) throw new ServiceException(message);
    }

    private void requireMaxLength(String value, int maxLength, String message)
    {
        if (value.length() > maxLength) throw new ServiceException(message);
    }

    private void requireId(Long id, String message)
    {
        if (id == null || id <= 0) throw new ServiceException(message);
    }

    private void requireIds(Long[] ids)
    {
        if (ids == null || ids.length == 0) throw new ServiceException("请选择要操作的数据");
        for (Long id : ids) requireId(id, "数据编号不正确");
    }

    private int defaultZero(Integer value) { return value == null ? 0 : value; }
    private long defaultZero(Long value) { return value == null ? 0L : value; }
    private String defaultText(String value, String fallback)
    {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
