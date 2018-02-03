package com.mofancn.weixin.controller;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.mofancn.common.pojo.jedisClient;
import com.mofancn.common.utils.HttpClientUtil;
import com.mofancn.common.utils.JsonUtils;
import com.mofancn.common.utils.MofancnResult;
import com.mofancn.weixin.pojo.WeixinSignClass;

import net.sf.json.JSONObject;

@Controller
public class WeixinOauthController {

	@Autowired
	private jedisClient jedisClient;

	@Value("${WEIXIN_ACCESSTOKEN_KEY}")
	private String WEIXIN_ACCESSTOKEN_KEY;
	@Value("${JS_API_TICKET_KEY}")
	private String JS_API_TICKET_KEY;
	@Value("${APP_ID}")
	private String APP_ID;

	@RequestMapping("/index")
	@ResponseBody
	public String index(String signature, String timestamp, String nonce, String echostr) {

		return echostr;

	}

	@RequestMapping("/getaccesstoken")
	@ResponseBody
	public MofancnResult getWeixinAccessToken(HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse) {

		String string = jedisClient.get(WEIXIN_ACCESSTOKEN_KEY);
		if (string != null) {
			System.out.println("redis is ok");
			return MofancnResult.ok(string);
		}

		String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential" + "&appid="
				+ "wx017aa4bd797f4477" + "&secret=" + "f7f30168085cd7928452e2307b0e6e7f";
		String string2 = HttpClientUtil.doGet(url);

		JSONObject object = JSONObject.fromObject(string2);

		String access_token1 = object.getString("access_token");
		System.out.println(access_token1);
		jedisClient.set(WEIXIN_ACCESSTOKEN_KEY, access_token1);
		jedisClient.expire(WEIXIN_ACCESSTOKEN_KEY, 7000);

		// return MofancnResult.ok(JsonUtils.objectToJson(object));
		return MofancnResult.ok(access_token1);
	}

	@RequestMapping("/getjsapi_ticket")
	@ResponseBody
	public MofancnResult getJsApitTicket(HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse) {

		String access_token = jedisClient.get(WEIXIN_ACCESSTOKEN_KEY);
		if (access_token == null) {
			String url = "http://weixin.mofancn.com/weixin/getaccesstoken";
			String doget = HttpClientUtil.doGet(url);
			MofancnResult mofancnResult = MofancnResult.formatToPojo(doget, String.class);
			Object data = mofancnResult.getData();
			access_token = data.toString();
		}

		String url = "https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token=" + access_token + "&type=jsapi";
		String string2 = HttpClientUtil.doGet(url);

		JSONObject object = JSONObject.fromObject(string2);

		String jsapi_ticket = object.getString("ticket");
		System.out.println(jsapi_ticket);
		jedisClient.set(JS_API_TICKET_KEY, jsapi_ticket);
		jedisClient.expire(JS_API_TICKET_KEY, 7000);

		// return MofancnResult.ok(JsonUtils.objectToJson(object));
		return MofancnResult.ok(jsapi_ticket);
	}

	@RequestMapping("/getSignature")
	@ResponseBody
	public MofancnResult getSignature(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {

		
		
		String url = "http://weixin.mofancn.com/weixin/getaccesstoken";
		String getaccesstoken = HttpClientUtil.doGet(url);
		
		String url2 = "http://weixin.mofancn.com/weixin/getjsapi_ticket";
		String getjsapi_ticket = HttpClientUtil.doGet(url2);
		System.out.println("getaccesstoken:"+ getaccesstoken);
		System.out.println("getjsapi_ticket:"+ getjsapi_ticket);
		
		String jsapi_ticket = jedisClient.get(JS_API_TICKET_KEY);
		System.out.println("jsapi_ticket:"+ jsapi_ticket);
/*		if (jsapi_ticket == null) {
			String url = "http://localhost:8090/getjsapi_ticket";
			String doget = HttpClientUtil.doGet(url);
			MofancnResult mofancnResult = MofancnResult.formatToPojo(doget, String.class);
			Object data = mofancnResult.getData();
			jsapi_ticket = data.toString();

		}
		*/
//		String weburl = httpServletRequest.getRequestURL().toString();//得到请求的URL地址
		String weburl = httpServletRequest.getParameter("url");
		String timestamp =System.currentTimeMillis()/1000 + "";
		String noncestr = UUID.randomUUID().toString();

		WeixinSignClass weixinSignClass = new WeixinSignClass();
		weixinSignClass.setDebug(false);
		weixinSignClass.setAppId("wx017aa4bd797f4477");
		weixinSignClass.setTimestamp(timestamp);
		weixinSignClass.setNonceStr(noncestr);
		System.out.println(weburl);
		String string1 = "jsapi_ticket=" + jsapi_ticket + "&noncestr=" + noncestr + "&timestamp=" + timestamp + "&url="
				+ weburl;
		// 生成签名sign
		MessageDigest md = null;
		String strDes = null;
		byte[] bt = string1.getBytes();
		String signature = null;

		try {

			////////////// 使用SHA-1的方式进行加密////////////

			md = MessageDigest.getInstance("SHA-1"); // 获取加密方式

			md.update(bt);// 加密
			byte[] digest = md.digest();

			////////////// 加密过程/////////
			StringBuffer hexString = new StringBuffer();
			// 字节数组转换为 十六进制 数
			for (int i = 0; i < digest.length; i++) {
				String shaHex = Integer.toHexString(digest[i] & 0xFF);
				if (shaHex.length() < 2) {
					hexString.append(0);
				}
				hexString.append(shaHex);
			}
			signature = hexString.toString();

		} catch (NoSuchAlgorithmException e) {

			System.out.println("Invalid algorithm.");

			return MofancnResult.build(500, "sha1加密失败");

		}
		weixinSignClass.setSignature(signature);


		// return MofancnResult.ok(JsonUtils.objectToJson(object));
		return MofancnResult.ok(JsonUtils.objectToJson(weixinSignClass));
	}

}
