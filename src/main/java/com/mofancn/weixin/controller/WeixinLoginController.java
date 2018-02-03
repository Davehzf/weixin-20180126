package com.mofancn.weixin.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.mofancn.common.utils.HttpClientUtil;
import com.mofancn.common.utils.JsonUtils;
import com.mofancn.common.utils.MofancnResult;
import com.mysql.fabric.xmlrpc.base.Array;

import net.sf.json.JSONObject;

@Controller
public class WeixinLoginController {

	/**
	 * 公众号请求接口
	 * 
	 * @param httpServletRequest
	 * @param httpServletResponse
	 * @throws IOException
	 */
	@RequestMapping("/PCWeixinLogin")

	public void LoginServlet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
			throws IOException {

		// 手机公众号
		String url = "https://open.weixin.qq.com/connect/oauth2/authorize?appid=" + "wx017aa4bd797f4477"
				+ "&redirect_uri=" + URLEncoder.encode("http://sso.mofancn.com/sso/weixinauth/callback")
				+ "&response_type=code" + "&scope=snsapi_userinfo" + "&state=STATE#wechat_redirect";
		// 网站微信登录
		String URL = "https://open.weixin.qq.com/connect/qrconnect?appid=" + "wxd2f3210b442080e1" 
				+ "&redirect_uri=" + URLEncoder.encode("http://weixin.mofancn.com/weixin/PCWeixinOauth/callback") 
				+ "&response_type=code"
				+ "&scope=snsapi_login,snsapi_userinfo"
				+ "&state=STATE" 
				+ "#wechat_redirect";

		httpServletResponse.sendRedirect(URL);

	}

	/**
	 * 回调 地址
	 * 
	 * @param httpServletRequest
	 * @param httpServletResponse
	 * @return
	 */
	@RequestMapping("/PCWeixinOauth/callback")

	public void LoginCallback(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {

		String code = httpServletRequest.getParameter("code");

		String URL = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=wxd2f3210b442080e1"
				+ "&secret=8e8fdabcc669d26b4c5ab24cd2873e25"
				+ "&code=" + code 
				+ "&grant_type=authorization_code";

		String string = HttpClientUtil.doGet(URL);
		JSONObject jsonObject = JSONObject.fromObject(string);
		String access_token = jsonObject.getString("access_token");
		String openid = jsonObject.getString("openid");

		String userInfo = "https://api.weixin.qq.com/sns/userinfo?access_token=" + access_token + "&openid=" + openid
				+ "&lang=zh_CN";

		String string2 = HttpClientUtil.doGet(userInfo);
		JSONObject jsonObject2 = JSONObject.fromObject(string2);
		System.out.println(JsonUtils.objectToJson(jsonObject2));
		String userWeixinId = jsonObject.getString("unionid");

		String objectToJson = JsonUtils.objectToJson(userWeixinId);
		HashMap<String, String> hashMap = new HashMap<>();
		hashMap.put(userWeixinId, jsonObject.getString("unionid"));

		/*		try {
			httpServletResponse.sendRedirect("http://wwwtest.mofancn.com/register");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		String url = "http://sso.mofancn.com/sso/user/loginbyweixin";
		String doGet = HttpClientUtil.doPost(url, hashMap);
		if (doGet.length() >= 0) {
			MofancnResult mofancnResult = MofancnResult.formatToPojo(doGet, MofancnResult.class);

			if (mofancnResult.getStatus() == 200) {
				String token = mofancnResult.getData().toString();
				try {
					httpServletResponse.sendRedirect("http://wwwtest.mofancn.com");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				try {
					httpServletResponse.sendRedirect("http://wwwtest.mofancn.com/register");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		} else {

			try {
				httpServletResponse.sendRedirect("http://wwwtest.mofancn.com/register");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// return MofancnResult.ok(JsonUtils.objectToJson(doGet));
		 
		 
	}

}
