package interdroid.vdb.content;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.content.res.XmlResourceParser;

public class VdbConfig {
	private static final String CONFIG_XML = "vdbconfig.xml";
	
	private final List<RepositoryConf> repositories_ = new ArrayList<RepositoryConf>();
	
	public List<RepositoryConf> getRepositories()
	{
		return repositories_;
	}
	
	public static class RepositoryConf {
		String name_;
		String contentProvider_;
		
		private RepositoryConf() {}
		
		public static RepositoryConf parseFromStartTag(XmlPullParser xpp)
		throws XmlPullParserException, IOException
		{
			RepositoryConf obj = new RepositoryConf();
			
			obj.name_ = xpp.getAttributeValue(/* namespace */ null, "name");
			obj.contentProvider_ = xpp.getAttributeValue(/* namespace */ null,
					"contentProvider");
			if (obj.name_ == null || obj.contentProvider_ == null) {
				throw new XmlPullParserException("Missing mandatory attributes" 
						+ " for repository.");
			}		
			if (xpp.next() != XmlPullParser.END_TAG) {
				throw new XmlPullParserException("Expected end tag for Repository."
						+ "Found " + xpp.getEventType());
			}
			return obj;
		}
	}	
	
	public VdbConfig(Context ctx)
	{
		try {
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(false);
			XmlPullParser xpp = factory.newPullParser();
			xpp.setInput(ctx.getAssets().open(CONFIG_XML), null);
			
			int eventType = xpp.getEventType();
			/* skip root element */
			while (xpp.getEventType() != XmlPullParser.START_TAG) {
				eventType = xpp.next();
			}
			eventType = xpp.next();
			
			while (eventType != XmlPullParser.END_DOCUMENT) {
				switch(eventType) {
				case XmlPullParser.START_TAG:
					if ("repository".equals(xpp.getName())) {
						repositories_.add(RepositoryConf.parseFromStartTag(xpp));
					} else {
						throw new XmlPullParserException("Unexpected element type: "
								+ xpp.getName());
					}
				}
				eventType = xpp.next();
			}
		} catch (IOException e) {
			throw new RuntimeException("Cannot open " + CONFIG_XML, e);
		} catch (XmlPullParserException e) {
			throw new RuntimeException("Could not parse " + CONFIG_XML, e);
		}
	}
}
