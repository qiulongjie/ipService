package com.zzcm.address.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.zzcm.address.vo.IPEntry;

/**
 * QQ IP 库取地址类
 * @version 	1.0 2013-2-21
 * @author		tangl
 * @history	
 *
 */
public class IPSeeker
{
	private final static String ALL_PROVINCES="北京|上海|天津|重庆|河北|山西|内蒙|辽宁|吉林|黑龙|江苏|浙江|安徽|福建|江西|山东|河南|湖北|湖南|广东|广西|海南|四川|贵州|云南|西藏|陕西|甘肃|青海|宁夏|新疆|台湾|香港|澳门";
	
	private String IP_FILE = "/ip/ip.Dat";
	private Logger logger = Logger.getLogger(IPSeeker.class);
	private final int IP_RECORD_LENGTH = 7;
	private final byte AREA_FOLLOWED = 0x01;
	private final byte NO_AREA = 0x2;
	private final LinkedHashMap<String,String> ipCache = new LinkedHashMap<String,String>()
	{
		private static final long serialVersionUID = 7704147525153620874L;

		@Override
		protected boolean removeEldestEntry(Entry<String,String> eldest)
		{
			return this.size() > 5000;// 只保留5000个数据，多余的从最高的删除
		}
	};
	private RandomAccessFile ipFile;
	private long ipBegin, ipEnd;
	
//	private MappedByteBuffer mbb;
//	private byte[] buf;
//	private byte[] b4;
//	private byte[] b3;
	
	private String encoding = "GBK";
	
	public final String INNER_NET_STR="192.168";
	
	public final String INNER_NET_STR2="127.0.0.1";
	
	public IPSeeker(String ipDatFile)
	{
		File file = new File(ipDatFile == null ? IP_FILE : ipDatFile);
		if (!file.exists())
		{
			logger.error("ip库文件不能找到::" + file.getAbsoluteFile());
			return;
		}
//		loc = new IPLocation();
//		buf = new byte[100];
//		b4 = new byte[4];
//		b3 = new byte[3];
		try
		{
			ipFile = new RandomAccessFile(ipDatFile == null ? IP_FILE : ipDatFile, "r");
		}
		catch (FileNotFoundException e)
		{
			logger.error("文件不能找到["+IP_FILE+"]",e);
			throw new RuntimeException("必须指定文件!",e);
		}
		if (ipFile != null)
		{
			try
			{
				ipBegin = readLong4(0);
				ipEnd = readLong4(4);

				if (ipBegin == -1 || ipEnd == -1)
				{
					ipFile.close();
					ipFile = null;
				}
			}
			catch (IOException e)
			{

				ipFile = null;
			}
		}
	}

	public IPSeeker()
	{
		this(null);
	}

	public IPSeeker(String ipDatFile, String charset)
	{
		this(null);
		this.encoding = charset;
	}

	/**
	 * 清空缓存
	 */
	public void clearCache()
	{
		synchronized (ipCache)
		{
			if (this.ipCache != null && !this.ipCache.isEmpty())
			{
				this.ipCache.clear();
			}
		}
	}

	/**
	 * 返回ip缓存的备份
	 * @return @return {ip=广东省深圳市 移动}
	 */
	public Map<String,String> getIpCache()
	{
		Map<String,String> ipTmp=new LinkedHashMap<String, String>();
		ipTmp.putAll(ipCache);
		return ipTmp;
	}
	
	public List<IPEntry> getIPEntriesDebug(String addr)
	{
		byte[] b4 = new byte[4];
		List<IPEntry> ret = new ArrayList<IPEntry>();
		long endOffset = ipEnd + 4;
		for (long offset = ipBegin + 4; offset <= endOffset; offset += IP_RECORD_LENGTH)
		{
			long temp = readLong3(offset);
			if (temp != -1)
			{
				IPLocation loc = getIPLocation(temp);
				if (loc.country.indexOf(addr) != -1 || loc.area.indexOf(addr) != -1)
				{
					IPEntry entry = new IPEntry();
					entry.country = loc.country;
					entry.area = loc.area;
					readIP(offset - 4, b4);
					entry.beginIp = IPUtils.getIpStringFromBytes(b4);
					readIP(temp, b4);
					entry.endIp = IPUtils.getIpStringFromBytes(b4);
					ret.add(entry);
				}
			}
		}
		return ret;
	}

//	public List<IPEntry> getIPEntries(String s)
//	{
//		List<IPEntry> ret = new ArrayList<IPEntry>();
//		byte[] b4 = new byte[4];
//		try
//		{
//			if (mbb == null)
//			{
//				FileChannel fc = ipFile.getChannel();
//				mbb = fc.map(FileChannel.MapMode.READ_ONLY, 0, ipFile.length());
//				mbb.order(ByteOrder.LITTLE_ENDIAN);
//			}
//			int endOffset = (int) ipEnd;
//			for (int offset = (int) ipBegin + 4; offset <= endOffset; offset += IP_RECORD_LENGTH)
//			{
//				int temp = readInt3(offset);
//				if (temp != -1)
//				{
//					IPLocation loc = getIPLocation(temp);
//					if (loc.country.indexOf(s) != -1 || loc.area.indexOf(s) != -1)
//					{
//						IPEntry entry = new IPEntry();
//						entry.country = loc.country;
//						entry.area = loc.area;
//						readIP(offset - 4, b4);
//						entry.beginIp = IPUtils.getIpStringFromBytes(b4);
//						readIP(temp, b4);
//						entry.endIp = IPUtils.getIpStringFromBytes(b4);
//						ret.add(entry);
//					}
//				}
//			}
//		}
//		catch (IOException e)
//		{
//			System.out.println(e.getMessage());
//		}
//		return ret;
//	}
//
//	private int readInt3(int offset)
//	{
//		mbb.position(offset);
//		return mbb.getInt() & 0x00FFFFFF;
//	}
//
//	private int readInt3()
//	{
//		return mbb.getInt() & 0x00FFFFFF;
//	}

	
	public String getCountry(byte[] ip)
	{
		if (ipFile == null)
		{
			return "IP?";
		}
		IPLocation loc = getIPLocation(ip);
		if(loc==null)
		{
			return "";
		}
		return loc.country;
	}

	public String getCountry(String ip)
	{
		return getCountry(IPUtils.getIpByteArrayFromString(ip));
	}

	public String getArea(byte[] ip)
	{
		if (ipFile == null)
			return "IP?";
		IPLocation loc = getIPLocation(ip);
		if(loc==null)
		{
			return "";
		}
		return loc.area;
	}

	public String getArea(String ip)
	{
		return getArea(IPUtils.getIpByteArrayFromString(ip));
	}

	private IPLocation getIPLocation(byte[] ip)
	{
		IPLocation info = null;
		long offset = locateIP(ip);
		if (offset != -1)
		{
			info = getIPLocation(offset);
		}
		return info;
	}

	private long readLong4(long offset)
	{
		long ret = 0;
		try
		{
			ipFile.seek(offset);
			ret |= (ipFile.readByte() & 0xFF);
			ret |= ((ipFile.readByte() << 8) & 0xFF00);
			ret |= ((ipFile.readByte() << 16) & 0xFF0000);
			ret |= ((ipFile.readByte() << 24) & 0xFF000000);
			return ret;
		}
		catch (IOException e)
		{
			return -1;
		}
	}

	private long readLong3(long offset)
	{
		byte[] b3 = new byte[3];
		long ret = 0;
		try
		{
			ipFile.seek(offset);
			ipFile.readFully(b3);
			ret |= (b3[0] & 0xFF);
			ret |= ((b3[1] << 8) & 0xFF00);
			ret |= ((b3[2] << 16) & 0xFF0000);
			return ret;
		}
		catch (IOException e)
		{
			return -1;
		}
	}

	private long readLong3()
	{
		byte[] b3 = new byte[3];
		long ret = 0;
		try
		{
			ipFile.readFully(b3);
			ret |= (b3[0] & 0xFF);
			ret |= ((b3[1] << 8) & 0xFF00);
			ret |= ((b3[2] << 16) & 0xFF0000);
			return ret;
		}
		catch (IOException e)
		{
			return -1;
		}
	}

	private void readIP(long offset, byte[] ip)
	{
		try
		{
			ipFile.seek(offset);
			ipFile.readFully(ip);
			byte temp = ip[0];
			ip[0] = ip[3];
			ip[3] = temp;
			temp = ip[1];
			ip[1] = ip[2];
			ip[2] = temp;
		}
		catch (IOException e)
		{
			System.out.println(e.getMessage());
		}
	}

//	private void readIP(int offset, byte[] ip)
//	{
//		mbb.position(offset);
//		mbb.get(ip);
//		byte temp = ip[0];
//		ip[0] = ip[3];
//		ip[3] = temp;
//		temp = ip[1];
//		ip[1] = ip[2];
//		ip[2] = temp;
//	}

	private int compareIP(byte[] ip, byte[] beginIp)
	{
		for (int i = 0; i < 4; i++)
		{
			int r = compareByte(ip[i], beginIp[i]);
			if (r != 0)
				return r;
		}
		return 0;
	}

	private int compareByte(byte b1, byte b2)
	{
		if ((b1 & 0xFF) > (b2 & 0xFF))
			return 1;
		else if ((b1 ^ b2) == 0)
			return 0;
		else
			return -1;
	}

	private long locateIP(byte[] ip)
	{
		byte[] b4 = new byte[4];
		long m = 0;
		int r;
		readIP(ipBegin, b4);
		r = compareIP(ip, b4);
		if (r == 0)
			return ipBegin;
		else if (r < 0)
			return -1;
		for (long i = ipBegin, j = ipEnd; i < j;)
		{
			m = getMiddleOffset(i, j);
			readIP(m, b4);
			r = compareIP(ip, b4);

			if (r > 0)
				i = m;
			else if (r < 0)
			{
				if (m == j)
				{
					j -= IP_RECORD_LENGTH;
					m = j;
				}
				else
					j = m;
			}
			else
				return readLong3(m + 4);
		}
		m = readLong3(m + 4);
		readIP(m, b4);
		r = compareIP(ip, b4);
		if (r <= 0)
			return m;
		else
			return -1;
	}

	private long getMiddleOffset(long begin, long end)
	{
		long records = (end - begin) / IP_RECORD_LENGTH;
		records >>= 1;
		if (records == 0)
			records = 1;
		return begin + records * IP_RECORD_LENGTH;
	}

	private IPLocation getIPLocation(long offset)
	{
		IPLocation loc = new IPLocation();
		try
		{
			ipFile.seek(offset + 4);
			byte b = ipFile.readByte();
			if (b == AREA_FOLLOWED)
			{
				long countryOffset = readLong3();
				ipFile.seek(countryOffset);
				b = ipFile.readByte();
				if (b == NO_AREA)
				{
					loc.country = readString(readLong3());
					ipFile.seek(countryOffset + 4);
				}
				else
					loc.country = readString(countryOffset);
				loc.area = readArea(ipFile.getFilePointer());
			}
			else if (b == NO_AREA)
			{
				loc.country = readString(readLong3());
				loc.area = readArea(offset + 8);
			}
			else
			{
				loc.country = readString(ipFile.getFilePointer() - 1);
				loc.area = readArea(ipFile.getFilePointer());
			}
			return loc;
		}
		catch (IOException e)
		{
			return null;
		}
	}

//	private IPLocation getIPLocation(int offset)
//	{
//		IPLocation loc = new IPLocation();
//		mbb.position(offset + 4);
//		byte b = mbb.get();
//		if (b == AREA_FOLLOWED)
//		{
//			int countryOffset = readInt3();
//			mbb.position(countryOffset);
//			b = mbb.get();
//			if (b == NO_AREA)
//			{
//				loc.country = readString(readInt3());
//				mbb.position(countryOffset + 4);
//			}
//			else
//				loc.country = readString(countryOffset);
//			loc.area = readArea(mbb.position());
//		}
//		else if (b == NO_AREA)
//		{
//			loc.country = readString(readInt3());
//			loc.area = readArea(offset + 8);
//		}
//		else
//		{
//			loc.country = readString(mbb.position() - 1);
//			loc.area = readArea(mbb.position());
//		}
//		return loc;
//	}

	private String readArea(long offset) throws IOException
	{
		ipFile.seek(offset);
		byte b = ipFile.readByte();
		if (b == 0x01 || b == 0x02)
		{
			long areaOffset = readLong3(offset + 1);
			if (areaOffset == 0)
				return "";
			else
				return readString(areaOffset);
		}
		else
			return readString(offset);
	}

//	private String readArea(int offset)
//	{
//		mbb.position(offset);
//		byte b = mbb.get();
//		if (b == 0x01 || b == 0x02)
//		{
//			int areaOffset = readInt3();
//			if (areaOffset == 0)
//				return "";
//			else
//				return readString(areaOffset);
//		}
//		else
//			return readString(offset);
//	}

	private String readString(long offset)
	{
		byte[] buf = new byte[150];
		try
		{
			ipFile.seek(offset);
			int i=0;
//			此处有bug 有数组越界异常
//			for (i = 0, buf[i] = ipFile.readByte(); buf[i] != 0 ; buf[++i] = ipFile.readByte());
			while(i<buf.length)
			{
				buf[i]=ipFile.readByte();
				if(buf[i]==0)
				{
					break;
				}
				i++;
			}
				
			if (i != 0)
			{
				return IPUtils.getString(buf, 0, i, encoding);
			}
		}
		catch (IOException e)
		{
			System.out.println(e.getMessage());
		}
		return "";
	}

//	private String readString(int offset)
//	{
//		byte[] buf = new byte[150];
//		try
//		{
//			mbb.position(offset);
//			int i;
//			for (i = 0, buf[i] = mbb.get(); buf[i] != 0; buf[++i] = mbb.get())
//				;
//			if (i != 0)
//				return IPUtils.getString(buf, 0, i, encoding);
//		}
//		catch (IllegalArgumentException e)
//		{
//			System.out.println(e.getMessage());
//		}
//		return "";
//	}

	private String getAddress(String ip)
	{
		if(ipCache.containsKey(ip))
		{
			return ipCache.get(ip);
		}
		
		String country = getCountry(ip);
		String area = getArea(ip);
		country=" CZ88.NET".equals(country) ? "" : country;
		area=" CZ88.NET".equals(area)? "" : area;
		String address = country + " " + area;
		if(address.trim().length()>0)
		{
			ipCache.put(ip,address.trim());
		}
		return address.trim();
	}

	/**
	 * 根据IP地址取所在地,不存在的信息为null对象,(注释内容)<br/>
	 * example: <br/>
	 * 1.{中国,北京 ,null,null}</br> 2.{中国,广东省,深圳市,移动}
	 * 
	 * @param ip ip地址
	 * @return {国家,省代号,市编号,运营商}
	 */
	public String[] getAddress2(String ip)
	{
		String tmpstrs[] = {"中国", null, null, null };
		String tmp = getAddress(ip);
		String tmpstr[] = tmp.split(" ");
		String prov = "", city = "", flag = "其它";
		if (tmpstr[0].matches(".*("+ALL_PROVINCES+").*"))
		{
			if (tmpstr[0].indexOf("省") > 0)
			{
				prov = tmpstr[0].substring(0, tmpstr[0].indexOf("省") + 1);
				if (tmpstr[0].indexOf("市") > 0)
				{
					city = tmpstr[0].substring(tmpstr[0].indexOf("省") + 1, tmpstr[0].indexOf("市") + 1);
				}
			}
			else
			{
				int start=0;
				if (tmpstr[0].startsWith("广西"))
				{
					start=2;
					prov = "广西省";
				}
				else if (tmpstr[0].startsWith("内蒙古"))
				{
					start=3;
					prov = "内蒙古";
				}
				else if (tmpstr[0].startsWith("西藏"))
				{
					start=2;
					prov = "西藏";
				}
				else if (tmpstr[0].startsWith("宁夏"))
				{
					start=2;
					prov = "宁夏";
				}
				else if (tmpstr[0].startsWith("新疆"))
				{
					prov = "新疆";
					start=2;
				}
				else if (tmpstr[0].startsWith("香港"))
				{
					prov = "香港";
					start=2;
				}
				else if (tmpstr[0].startsWith("澳门"))
				{
					prov = "澳门";
					start=2;
				}
				else
				{
					prov = tmpstr[0];
				}
				
				if (tmpstr[0].indexOf("市") > 0)
				{
					city = tmpstr[0].substring(start,tmpstr[0].indexOf("市") + 1);
				}
			}
			
			if(tmpstr.length==2)
			{
				if (tmpstr[1].indexOf("移动") >= 0)
				{
					flag = "移动";
				}
				else if (tmpstr[1].indexOf("联通") >= 0)
				{
					flag = "联通";
				}
				else if (tmpstr[1].indexOf("电信") >= 0)
				{
					flag = "电信";
				}
			}
			
			tmpstrs[1] = prov;
			tmpstrs[2] = city;
			tmpstrs[3] = flag;
		}
		else
		{
			tmpstrs[0] = tmpstr.length==2?tmpstr[0]:tmp;
		}
		return tmpstrs;
	}

	private class IPLocation
	{
		public String country;
		public String area;

		public IPLocation()
		{
			country = area = "";
		}
	}
	
	public boolean inInnerNet(String ip){
		return ip.startsWith(INNER_NET_STR)||ip.startsWith(INNER_NET_STR2);
	}
	
	public static void main(String[] args) throws Exception
	{
		IPSeeker ip = new IPSeeker("F:/IP库/qqwry/qqwry.dat");
//		System.out.println(ip.getIPEntriesDebug("深圳").get(0).country);
//		Random r=new Random();
//		for(int i=0;i<1000000;i++)
//		{
			long start=System.currentTimeMillis();
			String ipaddr = "14.217.60.11";
			String[] s=ip.getAddress2(ipaddr);
			for (String string : s)
			{
				System.out.println(string);
			}
			System.out.println("耗时["+(System.currentTimeMillis()-start)+"] ms");
//		}
//		FileInputStream fis=new FileInputStream("c:/a.dat");
//		fis.skip(4);
//		byte[] b=new byte[3];
//		fis.read(b);
//		for(int i=0;i<b.length;i++)
//		{
//			System.out.println(b[i]);
//		}
//		System.out.println(new String(b));
	}
	

}
