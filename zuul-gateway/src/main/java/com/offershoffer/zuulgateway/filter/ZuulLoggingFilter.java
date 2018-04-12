package com.offershoffer.zuulgateway.filter;

import javax.servlet.http.HttpServletRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import javax.servlet.http.HttpServletResponse;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Value;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
public class ZuulLoggingFilter extends ZuulFilter {
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Value("${Authorities}")
	String Authorities;
	
	@Value("${loginService}")
	String loginService;
	
	@Value("${publicUser}")
	String publicUser;

	@Override
	public Object run() throws ZuulException {
		RequestContext ctx = RequestContext.getCurrentContext();
		HttpServletRequest request = ctx.getRequest();
		logger.info(String.format("%s request to %s", request.getMethod(), request.getRequestURL().toString()));
		
		// extract service request from url
		String[] urlParts = request.getServletPath().split("/");
		String serviceUrl = urlParts[1];
		// check if request is to public service or private service
		JSONParser parser = new JSONParser();
		JSONObject json = null;
		// parse JSON from config server
		try {
			json = (JSONObject) parser.parse(Authorities);
			System.out.println(Authorities);
		} catch (ParseException e3) {
			return HttpServletResponse.SC_FORBIDDEN;
		}
		String services = (String) json.get(publicUser);
		String[] serviceList = services.split(",");
		for (String service : serviceList) {
			// if request to public service
			if (serviceUrl.equals(service))
				// then let go
				return HttpServletResponse.SC_OK;
		}
		// if request is to private service then check
		try {
			// if header contains a token
			String token = request.getHeader("application-token");
			// Xml call to uaa-server
			URL obj;
			HttpURLConnection con = null;
			BufferedReader in = null;
			// StringBuilder sb = null;
			try {
				obj = new URL("http://localhost:7000/verify" + token);
				con = (HttpURLConnection) obj.openConnection();
				con.setRequestMethod("GET");
				con.setRequestProperty("User-Agent", request.getHeader("user-agent"));
				con.setDoOutput(true);
				OutputStream os = con.getOutputStream();
				os.write(request.getReader().toString().getBytes());
				os.flush();
				os.close();
				//
				in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			} catch (MalformedURLException e) {
				return HttpServletResponse.SC_FORBIDDEN;
			} catch (IOException e) {
				return HttpServletResponse.SC_FORBIDDEN;
			}
			String inputLine;
			StringBuffer response = new StringBuffer();
			try {
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
			} catch (IOException e) {
				return HttpServletResponse.SC_FORBIDDEN;
			}
			try {
				in.close();
			} catch (IOException e) {
				return HttpServletResponse.SC_FORBIDDEN;
			}
			// print response
			System.out.println("Login serivce response:" + response.toString());
			String verify = response.toString();
			// if response is true let it go
			String[] data = verify.split(",");
			if (data[0].equals("true")) {
				String role = data[1];
				String roles = (String) json.get("roles");
				String[] roless = roles.split(",");
				for (String tempRole : roless) {
					if (role.equals(tempRole)) {
						String service = (String) json.get(tempRole);
						String serviesArray[] = service.split(",");
						for (String tempService : serviesArray) {
							if (tempService.equalsIgnoreCase(serviceUrl)) {
								// let to go
								return HttpServletResponse.SC_OK;
							}
						}
					}
				}
			}
			// redict to login page
			ctx.setSendZuulResponse(false);
			ctx.put("forward:", loginService);
			ctx.setResponseStatusCode(HttpServletResponse.SC_TEMPORARY_REDIRECT);
			ctx.getResponse().sendRedirect(loginService);
			return HttpServletResponse.SC_TEMPORARY_REDIRECT;
		} catch (Exception e) {
			// else exception will occur while reading the token
			// redirect to login page
			try {
				ctx.setSendZuulResponse(false);
				ctx.put("forward:", loginService);
				ctx.setResponseStatusCode(HttpServletResponse.SC_TEMPORARY_REDIRECT);
				ctx.getResponse().sendRedirect(loginService);
				return HttpServletResponse.SC_TEMPORARY_REDIRECT;
			} catch (Exception e1) {
				return HttpServletResponse.SC_FORBIDDEN;
			}
		}
	}

	@Override
	public boolean shouldFilter() {
		return true;
	}

	@Override
	public int filterOrder() {
		return 1;
	}

	@Override
	public String filterType() {
		return "pre";
	}

}
