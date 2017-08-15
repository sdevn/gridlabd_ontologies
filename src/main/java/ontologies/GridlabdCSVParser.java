package ontologies;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Simple csv file parser with some assumptions because of Gridlabd output format files
 */
public class GridlabdCSVParser
{
	private File mFile;
	private Scanner mScanner;
	private List<String> mHeaders;
	
	public GridlabdCSVParser(File mFile)
	{
		this.mFile = mFile;
	}
	
	private void parseHeaders(CSVParseListener listener) throws FileNotFoundException
	{
		mScanner = new Scanner(mFile);
		
		String line;
		while (mScanner.hasNextLine())
		{
			line = mScanner.nextLine();

			if (line.matches("[# ]*([a-zA-Z0-9_.]+[,\n]+)+[a-zA-Z0-9_.]+")) // is the headers! ...probably :p
			{
				String[] headers = line.replaceAll("[# ]*", "").split(",");
				mHeaders = new ArrayList<String>();
				
				for (String h : headers)
				{
					mHeaders.add(h.replaceAll("[a-z]+\\.", ""));
				}
				
				listener.onHeadersParsed(mHeaders);
				
				break;
			}
		}
	}

	public void parse(CSVParseListener listener) throws FileNotFoundException
	{
		parseHeaders(listener);
		final int n = mHeaders.size();
		int i;
		String line;		
		while (mScanner.hasNextLine())
		{
			Map<String, String> values = new HashMap<String, String>();
			
			line = mScanner.nextLine();
			
			String[] parts = line.split(",");
			
			for (i = 0; i < n; i++)
			{
				values.put(mHeaders.get(i), parts[i]);
			}
			
			listener.onLineParsed(values);
		}
		
	}

	public interface CSVParseListener
	{
		public void onHeadersParsed(List<String> headers);
		public void onLineParsed(Map<String, String> values);
	}
}
