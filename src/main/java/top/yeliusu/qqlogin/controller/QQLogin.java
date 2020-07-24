package top.yeliusu.qqlogin.controller;

import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import top.yeliusu.qqlogin.common.Common;
import top.yeliusu.qqlogin.util.HttpClientUtil;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * qq登录
 * @author SongJunWei
 */
@Controller
public class QQLogin {

    /**
     * 发起请求
     * @param session session
     * @return
     */
    @GetMapping("/qq/oauth")
    public String qq(HttpSession session){
        //QQ互联中的回调地址
        String backUrl = Common.HTTPSURL + "/Callback/qqLogin";

        //用于第三方应用防止CSRF攻击
        String uuid = UUID.randomUUID().toString().replaceAll("-","");
        session.setAttribute("state",uuid);

        //Step1：获取Authorization Code
        String url = "https://graph.qq.com/oauth2.0/authorize?response_type=code"+
                "&client_id=" + Common.APPID +
                "&redirect_uri=" + URLEncoder.encode(backUrl) +
                "&state=" + uuid;

        return "redirect:" + url;
    }

    /**
     * 后调接口
     * @param response
     * @param request
     * @return
     * @throws IOException
     */
    @GetMapping("/Callback/qqLogin")
    public String callBackQQ(HttpServletResponse response, HttpServletRequest request) throws IOException {
        //qq返回的信息：http://graph.qq.com/demo/index.jsp?code=9A5F*********06AF&state=test
        String code = request.getParameter("code");

        //Step2：通过Authorization Code获取Access Token
        String backUrl = Common.HTTPSURL+ "/qq/callback";
        String url = "https://graph.qq.com/oauth2.0/token";
        Map<String, String> map = new HashMap<>();
        map.put("grant_type","authorization_code");
        map.put("client_id", Common.APPID);
        map.put("client_secret",Common.APPKEY);
        map.put("code",code);
        map.put("redirect_uri",backUrl);
        String access_token="";
        //qq返回信息形式：access_token=FE04******CCE2&expires_in=7776000&refresh_token=88E4**********BE14
        String QQBack= HttpClientUtil.doGet(url,map);
        if(QQBack.indexOf("access_token") >= 0){
            String[] array = QQBack.split("&");
            for (String str : array){
                if(str.indexOf("access_token") >= 0){
                    access_token = str.substring(str.indexOf("=") + 1);
                    break;
                }
            }
        }

        //Step3: 获取回调后的 openid 值
        url = "https://graph.qq.com/oauth2.0/me";
        map.clear();
        map.put("access_token",access_token);
        String result = HttpClientUtil.doGet(url,map);
        //返回形式：callback( {"client_id":"YOUR_APPID","openid":"YOUR_OPENID"} );将（）内部的json串取出
        JSONObject jsonObject = parseJSONP(result);
        String openid = jsonObject.getString("openid");

        //Step4：获取QQ用户信息
        url = "https://graph.qq.com/user/get_user_info?access_token=" + access_token +
                "&oauth_consumer_key="+ Common.APPID +
                "&openid=" + openid;
        map.clear();
        map.put("access_token",access_token);map.put("oauth_consumer_key",Common.APPID);map.put("openid",openid);
        //qq返回信息：{ "ret":0, "msg":"", "nickname":"YOUR_NICK_NAME", ... }
        String step4 = HttpClientUtil.doGet(url,map);
        JSONObject jsonObject1 = JSONObject.parseObject(step4);

        //将返回的信息存到map,用于页面显示
        map.clear();
        map.put("openid",openid);
        map.put("nickname",(String) jsonObject1.get("nickname"));
        map.put("imgUrl",(String)jsonObject1.get("figureurl_qq_2"));
        map.put("sex",(String)jsonObject1.get("gender"));


        //将用户信息存到session中
        Cookie cookie = new Cookie("personInfo", URLEncoder.encode(JSONObject.toJSON(map).toString(), "utf-8"));
        cookie.setMaxAge(60 * 60 * 1);
        cookie.setPath("/");
        response.addCookie(cookie);
        return "redirect:/index.html";
    }

    private static JSONObject parseJSONP(String jsonp){
        int startIndex = jsonp.indexOf("(");
        int endIndex = jsonp.lastIndexOf(")");

        String json = jsonp.substring(startIndex + 1,endIndex);

        return JSONObject.parseObject(json);
    }

    @GetMapping("/sharekongkery")
    @ResponseBody
    public String sharekongkery(){
        String url = "https://sns.qzone.qq.com/cgi-bin/qzshare/cgi_qzshare_onekey";
        Map map = new HashMap();
        map.put("url","https://www.yeliusu.top");
        map.put("title","标题"); map.put("pics","你的分享图片"); map.put("summary","你的分享描述信息");
        String result = HttpClientUtil.doGet(url,map);

        return result;
    }
}
