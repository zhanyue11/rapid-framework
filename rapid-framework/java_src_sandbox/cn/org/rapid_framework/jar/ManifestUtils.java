package cn.org.rapid_framework.jar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.util.WeakHashMap;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.io.IOUtils;
/**
 * Manifest读取的工具类
 * 
 * @author badqiu
 */
public class ManifestUtils {
	private static WeakHashMap<URL, Manifest> cache = new WeakHashMap<URL, Manifest>();
	
	/**
	 * 根据Class得到相对应的 META-INF/MANIFEST.MF信息 , 该方法会使用WeakHashMap作为Cache
	 * @param clazz
	 * @return Manifest
	 */
	public static Manifest getManifest(Class<?> clazz) {
		CodeSource codeSource = clazz.getProtectionDomain().getCodeSource();
		
		// check null for java.lang.* class
		if(codeSource == null) { 
			return new Manifest();
		}
		
		URL location = codeSource.getLocation();
		Manifest result = cache.get(location);
		if(result == null) {
			result = readManifest(location);
			cache.put(location, result);
		}
		return result;
	}
	
	public static Manifest getManifest(ClassLoader classLoader,String resourceName) {
		try {
			URL url = classLoader.getResource(resourceName);
			if(url == null) return null;
			if(url.toString().indexOf("!") >= 0) {
				String jarLocation = url.toString().substring(0,url.toString().indexOf("!"))+"!/META-INF/MANIFEST.MF";
//				System.out.println("ManifestUtils.getManifestByResource()"+jarLocation+" resourceName:"+resourceName+" url:"+url);
				return new Manifest(new URL(jarLocation).openStream());
			}else if(new File(url.getFile()).exists()) {
				String resourceFolder = getResourceLoadFolder(resourceName, url);
				return readManifest(new URL(resourceFolder));
			}
			throw new RuntimeException("META-INF/MANIFEST.MF FileNotFound with resourceName:"+resourceName+" url:"+url);
		}catch(MalformedURLException e) {
			throw new RuntimeException("MalformedURLException",e);
		}catch(IOException e) {
			throw new RuntimeException("IOException",e);
		}
	}

	private static String getResourceLoadFolder(String resourceName, URL url) {
		return url.toString().substring(0,url.toString().length() - resourceName.length());
	}
	
	//FIXME 需要测试打为war包后,通过jar可不可以得到MANIFEST
	private static Manifest readManifest(URL location)  {
		File file = new File(location.getFile());
		if(file.isDirectory()) {
			File manifestFile = new File(file,"META-INF/MANIFEST.MF");
			if(manifestFile.exists()) {
				try {
					return new Manifest(new FileInputStream(manifestFile));
				} catch (FileNotFoundException e) {
					//ignore
					return new Manifest();
				} catch (IOException e) {
					throw new RuntimeException("error on read META-INF/MANIFEST.MF  with location:"+location);
				}
			}
			return new Manifest();
		}else if(file.isFile()) {
			try {
				return new JarFile(file).getManifest();
			} catch (IOException e) {
				throw new RuntimeException("error on read META-INF/MANIFEST.MF with location:"+location);
			}
		}else{
			InputStream in = null; 
			try {
				in = new URL(location+"!/META-INF/MANIFEST.MF").openStream();
				return new Manifest(in);
			}catch(IOException e) {
				IOUtils.closeQuietly(in);
				throw new RuntimeException("read META-INF/MANIFEST.MF occer error with location:"+location);
			}
		}
//		throw new RuntimeException("META-INF/MANIFEST.MF FileNotFound with location:"+location);
	}
}