package com.my.blog.website.controller.admin;

import com.github.pagehelper.PageInfo;
import com.my.blog.website.constant.WebConst;
import com.my.blog.website.controller.BaseController;
import com.my.blog.website.dto.LogActions;
import com.my.blog.website.exception.TipException;
import com.my.blog.website.modal.Bo.RestResponseBo;
import com.my.blog.website.modal.Vo.ContentVo;
import com.my.blog.website.modal.Vo.UserVo;
import com.my.blog.website.service.IContentService;
import com.my.blog.website.service.ILogService;
import com.my.blog.website.service.IUserService;
import com.my.blog.website.utils.Commons;
import com.my.blog.website.utils.TaleUtils;

import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * 用户后台登录/登出
 * Created by BlueT on 2017/3/11.
 */
@Controller
@RequestMapping("/admin")
@Transactional(rollbackFor = TipException.class)
public class AuthController extends BaseController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

    @Resource
    private IUserService usersService;

    @Resource
    private IContentService contentService;
    
    @Resource
    private ILogService logService;

    @GetMapping(value = "/login")
    public String login() {
        return "admin/login";
    }

	@GetMapping(value = "/register")
	public String register() {
		return "admin/register";
	}
    
	@PostMapping(value = "register")
	@ResponseBody
	public RestResponseBo doRegister(@RequestParam String email,
			@RequestParam String user,
            @RequestParam String password,
            @RequestParam String password1,
            HttpServletRequest request,
            HttpServletResponse response) {
		UserVo userVo=new UserVo();
		userVo.setEmail(email);
		userVo.setUsername(user);
		userVo.setPassword(password);
		UserVo userT=usersService.queryUserByName(user);
		String msg=null;
		if(!password.equals(password1)) {
			msg="密码不一致，请重新输入";
			return RestResponseBo.fail(msg);		
		}else if(userT!=null){
			msg="用户名已经被使用，请重新输入";
			System.out.println(msg);
			return RestResponseBo.fail(msg);
		}else {
			usersService.insertUser(userVo);
		}
		return RestResponseBo.ok();
	}
	
	/**
     * 管理后台登录
     * @param username
     * @param password
     * @param remeber_me
     * @param request
     * @param response
     * @return
     */
    @PostMapping(value = "login")
    @ResponseBody
    public RestResponseBo doLogin(@RequestParam String username,
                                  @RequestParam String password,
                                  @RequestParam(required = false) String remeber_me,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {

        Integer error_count = cache.get("login_error_count");
        try {
            UserVo user = usersService.login(username, password);
            request.getSession().setAttribute(WebConst.LOGIN_SESSION_KEY, user);
            if (StringUtils.isNotBlank(remeber_me)) {
                TaleUtils.setCookie(response, user.getUid());
            }
            logService.insertLog(LogActions.LOGIN.getAction(), null, request.getRemoteAddr(), user.getUid());
        } catch (Exception e) {
            error_count = null == error_count ? 1 : error_count + 1;
            if (error_count > 3) {
                return RestResponseBo.fail("您输入密码已经错误超过3次，请10分钟后尝试");
            }
            cache.set("login_error_count", error_count, 10 * 60);
            String msg = "登录失败";
            if (e instanceof TipException) {
                msg = e.getMessage();
            } else {
                LOGGER.error(msg, e);
            }
            return RestResponseBo.fail(msg);
        }
        return RestResponseBo.ok();
    }

    /**
     * 注销
     * @param session
     * @param response
     */
    @RequestMapping("/logout")
    public void logout(HttpSession session, HttpServletResponse response, HttpServletRequest request) {
        session.removeAttribute(WebConst.LOGIN_SESSION_KEY);
        Cookie cookie = new Cookie(WebConst.USER_IN_COOKIE, "");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        try {
            //response.sendRedirect(Commons.site_url());
            response.sendRedirect(Commons.site_login());
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error("注销失败", e);
        }
    }
    
    /**
     * 用户文章首页
     *
     * @return
     */
    @GetMapping(value = "/auth/articel")
    public String authArticel(HttpServletRequest request, @RequestParam(value = "limit", defaultValue = "12") int limit) {
        return this.authArticel(request, 1, limit);
    }
    
    /**
     * 用户文章首页分页
     *
     * @param request request
     * @param p       第几页
     * @param limit   每页大小
     * @return 主页
     */
    @GetMapping(value = "/auth/articel/page/{p}")
    public String authArticel(HttpServletRequest request, @PathVariable int p, @RequestParam(value = "limit", defaultValue = "12") int limit) {
        p = p < 0 || p > WebConst.MAX_PAGE ? 1 : p;
        Integer uid=this.getUid(request);
        PageInfo<ContentVo> articles = contentService.getContentsByUid(p, limit, uid);
        request.setAttribute("articles", articles);
        if (p > 1) {
            this.title(request, "第" + p + "页");
        }
        return this.render("articel_list");
    }
}
