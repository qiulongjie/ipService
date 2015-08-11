package com.zzcm.address.util;

import java.io.UnsupportedEncodingException;

import org.apache.log4j.Logger;

/**
 * IP工具类
 *
 * @version 	1.0 2013-1-31
 * @author		tangl
 * @history	
 *
 */
public class IPUtils
{
	private static Logger LOGGER=Logger.getLogger(IPUtils.class);
	
	public static byte[] getIpByteArrayFromString(String ip)
	{
		byte[] ret = new byte[4];
		java.util.StringTokenizer st = new java.util.StringTokenizer(ip, ".");
		try
		{
			ret[0] = (byte) (Integer.parseInt(st.nextToken()) & 0xFF);
			ret[1] = (byte) (Integer.parseInt(st.nextToken()) & 0xFF);
			ret[2] = (byte) (Integer.parseInt(st.nextToken()) & 0xFF);
			ret[3] = (byte) (Integer.parseInt(st.nextToken()) & 0xFF);
		}
		catch (Exception e)
		{
			LOGGER.error("IP地址转换异常!",e);
		}
		return ret;
	}

	public static String getString(String s, String srcEncoding, String destEncoding)
	{
		try
		{
			return new String(s.getBytes(srcEncoding), destEncoding);
		}
		catch (UnsupportedEncodingException e)
		{
			return s;
		}
	}

	public static String getString(byte[] b, String encoding)
	{
		try
		{
			return new String(b, encoding);
		}
		catch (UnsupportedEncodingException e)
		{
			return new String(b);
		}
	}

	public static String getString(byte[] b, int offset, int len, String encoding)
	{
		try
		{
			return new String(b, offset, len, encoding);
		}
		catch (UnsupportedEncodingException e)
		{
			return new String(b, offset, len);
		}
	}

	public static String getIpStringFromBytes(byte[] ip)
	{
		StringBuffer sb = new StringBuffer();
		sb.append(ip[0] & 0xFF);
		sb.append('.');
		sb.append(ip[1] & 0xFF);
		sb.append('.');
		sb.append(ip[2] & 0xFF);
		sb.append('.');
		sb.append(ip[3] & 0xFF);
		return sb.toString();
	}

}
