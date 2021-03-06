package com.my.blog.website.service;

import com.github.pagehelper.PageInfo;
import com.my.blog.website.modal.Vo.AttachVo;
import com.my.blog.website.modal.Vo.UserVo;

/**
 * Created by wangq on 2017/3/20.
 */
public interface IAttachService {
    /**
     * 分页查询附件
     * @param page
     * @param limit
     * @param users 
     * @return
     */
    PageInfo<AttachVo> getAttachs(Integer page,Integer limit, UserVo users);

    /**
     * 保存附件
     *
     * @param fname
     * @param fkey
     * @param ftype
     * @param author
     */
    void save(String fname, String fkey, String ftype, Integer author);

    /**
     * 根据附件id查询附件
     * @param id
     * @return
     */
    AttachVo selectById(Integer id);

    /**
     * 删除附件
     * @param id
     */
    void deleteById(Integer id);
}
