package com.withstars.controller;

import com.withstars.domain.LoginLog;
import com.withstars.domain.User;
import com.withstars.service.impl.LoginLogServiceImpl;
import com.withstars.service.impl.TopicServiceImpl;
import com.withstars.service.impl.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.util.Date;
import java.util.StringTokenizer;

/**
 * 用户相关控制类
 */
@Controller
public class UserController {

    @Autowired
    public UserServiceImpl userService;

    @Autowired
    public LoginLogServiceImpl loginLogService;

    @Autowired
    public TopicServiceImpl topicService;

    /**
     * 生成MD5值
     */
    public static String getMD5(String message) {
        String md5 = "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");  // 创建一个md5算法对象
            byte[] messageByte = message.getBytes("UTF-8");
            byte[] md5Byte = md.digest(messageByte);              // 获得MD5字节数组,16*8=128位
            md5 = bytesToHex(md5Byte);                            // 转换为16进制字符串
        } catch (Exception e) {
            e.printStackTrace();
        }
        return md5;
    }

    /**
     * 二进制转十六进制
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuffer hexStr = new StringBuffer();
        int num;
        for (int i = 0; i < bytes.length; i++) {
            num = bytes[i];
            if(num < 0) {
                num += 256;
            }
            if(num < 16){
                hexStr.append("0");
            }
            hexStr.append(Integer.toHexString(num));
        }
        return hexStr.toString().toUpperCase();
    }

    /**
     * 用户注册
     */
    @RequestMapping("/user/add/do")
    public String addUser(HttpServletRequest request, RedirectAttributes redirect){
        //新建User对象
        User user=new User();
        //处理手机号
        String phoneNum=request.getParameter("tel");
        String areaCode=request.getParameter("areaCode");
        String phone=areaCode+phoneNum;
        //用户类型
        Byte type=new Byte("0");
        //密码加密处理
        String password=getMD5(request.getParameter("password"));
        //初始化User对象
        user.setUsername(request.getParameter("username"));
        user.setPassword(password);
        user.setEmail(request.getParameter("email"));
        user.setPhoneNum(phone);
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        user.setCredit(0);
        user.setType(type);

        boolean ifSucc=userService.addUser(user);
        System.out.print(ifSucc);
        return "redirect:/";
    }

    /**
     * 用户登陆
     */
    @RequestMapping("/signin/do")
    public String signin(HttpServletRequest request, RedirectAttributes redirect){
        //处理参数
        String password=getMD5(request.getParameter("password"));
        String username=request.getParameter("username");
        //验证用户名密码
        int loginVerify=userService.login(username,password);

        if(loginVerify == 2){
            System.out.println("登录成功!");
            User user =userService.getUserByUsername(username);
            Integer userId=user.getId();
            //添加积分
            boolean ifSuccAddCredit=userService.addCredit(5,userId);
            //用户信息写入session
            request.getSession().setAttribute("user",user);
            //获取登录信息
            String ip=getRemortIP(request);
            String Agent = request.getHeader("User-Agent");
            StringTokenizer st = new StringTokenizer(Agent,";");
            st.nextToken();
            //获取用户的浏览器名
            String userbrowser = st.nextToken();
            System.out.println(userbrowser);
            //写入登录日志
            LoginLog log=new LoginLog();
            log.setDevice(userbrowser);
            log.setIp(ip);
            log.setUserId(userId);
            log.setLoginTime(new Date());
            boolean ifSuccAddLog=loginLogService.addLog(log);

            return "redirect:/";
        }else if (loginVerify == 1){

            System.out.println("密码错误!");
            return "redirect:/signin";
        }else {

            System.out.println("用户名不存在!");
            return "redirect:/signin";
        }
    }

    /**
     * 用户登出
     */
    @RequestMapping("/signout")
    public String signout(HttpServletRequest request){
        request.getSession().removeAttribute("user");
        return "redirect:/";
    }

    /**
     * 获取客户端IP
     */
    public String getRemortIP(HttpServletRequest request) {
        if (request.getHeader("x-forwarded-for") == null) {
            return request.getRemoteAddr();
        }
        return request.getHeader("x-forwarded-for");
    }

    /**
     * 用户个人主页
     */
    @RequestMapping("/member/{username}")
    public ModelAndView personalCenter(@PathVariable("username")String username,HttpServletRequest request,RedirectAttributes redirect){
        boolean ifExistUser=userService.existUsername(username);
        //获取统计信息
        int topicsNum=topicService.getTopicsNum();
        int usersNum=userService.getUserCount();

        ModelAndView mv=new ModelAndView("user_info");
        if (ifExistUser){
            User user=userService.getUserByUsername(username);
            mv.addObject("userInfo",user);
            mv.addObject("topicsNum",topicsNum);
            mv.addObject("usersNum",usersNum);
            return mv;
        }else {
            String errorInfo=new String("会员未找到");
            mv.addObject("errorInfo",errorInfo);
            return mv;
        }

    }


}
